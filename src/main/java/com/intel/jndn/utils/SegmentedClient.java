/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jndn.utils;

import com.intel.jndn.utils.client.SegmentedDataReassembler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

/**
 * Provide a client to simplify retrieving segmented Data packets over the NDN
 * network. This class expects the Data producer to follow the NDN naming
 * conventions (see http://named-data.net/doc/tech-memos/naming-conventions.pdf)
 * and produce Data packets with a valid segment as the last component of their
 * name; additionally, at least the first packet should set the FinalBlockId of
 * the packet's MetaInfo (see
 * http://named-data.net/doc/ndn-tlv/data.html#finalblockid).
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedClient extends SimpleClient {

  private static SegmentedClient defaultInstance;
  private static final Logger logger = Logger.getLogger(SegmentedClient.class.getName());

  /**
   * Singleton access for simpler client use.
   *
   * @return
   */
  public static SegmentedClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new SegmentedClient();
    }
    return defaultInstance;
  }

  /**
   * See {@link SimpleClient#SimpleClient(long, long)}
   *
   * @param sleepTime
   * @param interestLifetime
   */
  public SegmentedClient(long sleepTime, long interestLifetime) {
    super(sleepTime, interestLifetime);
  }

  /**
   * Build a client with default parameters
   */
  public SegmentedClient() {
    super();
  }

  /**
   * Asynchronously send Interest packets for a segmented result; will not
   * block, but will wait for the first packet to return before sending
   * remaining interests until using the specified FinalBlockId. Will retrieve
   * non-segmented packets as well.
   *
   * @param face the {@link Face} on which to retrieve the packets
   * @param interest should include either a ChildSelector or an initial segment
   * number; the initial segment number will be cut off in the de-segmented
   * packet.
   * @return a list of FutureData packets; if the first segment fails, the list
   * will contain one FutureData with the failure exception
   */
  @Override
  public CompletableFuture<Data> getAsync(final Face face, Interest interest) {
    final long firstSegmentId = parseFirstSegmentId(interest);

    logger.log(Level.FINER, "Requesting segmented data packets: " + interest.getName().toUri());
    CompletableFuture<Data> allData = super.getAsync(face, interest).thenCompose(new Function<Data, CompletionStage<Data>>() {
      @Override
      public CompletionStage<Data> apply(Data firstSegment) {
        try {
          logger.log(Level.FINER, "Received first data packet: " + interest.getName().toUri());
          long lastSegmentId = parseLastSegmentId(firstSegment);
          Interest template = new Interest(interest);
          template.setName(removeSegment(firstSegment.getName()));

          // request subsequent segments using FinalBlockId and the Interest template
          return requestRemainingSegments(face, template, firstSegment, firstSegmentId + 1, lastSegmentId);
        } catch (EncodingException ex) {
          logger.log(Level.FINER, "No segment ID found in FinalBlockId, assuming first packet is only packet.");
          return CompletableFuture.completedFuture(firstSegment);
        }
      }
    });

    return allData;
  }

  /**
   * @param interest the request {@link Interest}
   * @return the first segment the interest is requesting, or 0 if none found
   */
  private long parseFirstSegmentId(Interest interest) {
    try {
      return interest.getName().get(-1).toSegment();
    } catch (EncodingException e) {
      if (interest.getChildSelector() == -1) {
        logger.log(Level.WARNING, "No child selector set for a segmented Interest; this may result in incorrect retrieval.");
        // allow this interest to pass without a segment marker since it may still succeed
      }
      return 0;
    }
  }

  /**
   * @param firstData the first returned {@link Data}
   * @return the last segment number available as specified in the FinalBlockId
   * @throws EncodingException
   */
  private long parseLastSegmentId(Data firstData) throws EncodingException {
    return firstData.getMetaInfo().getFinalBlockId().toSegment();
  }

  /**
   * Send interests for remaining segments; adding them to the segmented future
   *
   * @param face
   * @param segmentedData
   * @param template
   * @param fromSegment
   * @param toSegment
   */
  private CompletableFuture<Data> requestRemainingSegments(Face face, Interest template, Data firstSegment, long fromSegment, long toSegment) {
    List<CompletableFuture<Data>> segments = new ArrayList<>();
    segments.add(CompletableFuture.completedFuture(firstSegment));

    for (long i = fromSegment; i <= toSegment; i++) {
      Interest segmentedInterest = new Interest(template);
      segmentedInterest.getName().appendSegment(i);
      CompletableFuture<Data> futureData = super.getAsync(face, segmentedInterest);
      segments.add(futureData);
    }

    return new SegmentedDataReassembler(template.getName(), segments).reassemble();
  }

  /**
   * Check if a name ends in a segment component; uses marker value found in the
   * NDN naming conventions (see
   * http://named-data.net/doc/tech-memos/naming-conventions.pdf).
   *
   * @param name the {@link Name} to check
   * @return true if the {@link Name} ends in a segment component
   */
  public static boolean hasSegment(Name name) {
    return name.get(-1).getValue().buf().get(0) == 0x00;
  }

  /**
   * @param name the {@link Name} to check
   * @return a new instance of {@link Name} with no segment component appended
   */
  public static Name removeSegment(Name name) {
    return hasSegment(name) ? name.getPrefix(-1) : new Name(name);
  }
}
