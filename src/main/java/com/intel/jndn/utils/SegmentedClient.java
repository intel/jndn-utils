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

import com.intel.jndn.utils.client.FutureData;
import com.intel.jndn.utils.client.SegmentedFutureData;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
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
public class SegmentedClient implements Client {

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
  public Future<Data> getAsync(final Face face, Interest interest) {
    final long firstSegmentId = parseFirstSegmentId(interest);
    final SegmentedFutureData segmentedData = new SegmentedFutureData(face, interest.getName());
    final FutureData firstData = new FutureData(face, interest.getName());
    segmentedData.add(0, firstData);

    // send first interest
    logger.log(Level.FINER, "Sending first interest for: " + interest.getName().toUri());
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          // request subsequent segments using FinalBlockId and the Interest template
          try {
            long lastSegmentId = parseLastSegmentId(data);
            Interest template = new Interest(interest);
            template.setName(removeSegment(data.getName()));
            requestRemainingSegments(face, segmentedData, template, firstSegmentId + 1, lastSegmentId);
          } catch (EncodingException ex) {
            logger.log(Level.FINER, "No segment ID found in FinalBlockId, assuming first packet is only packet.");
          }
          
          // resolve the first data
          firstData.resolve(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          segmentedData.reject(new TimeoutException());
        }
      });
    } catch (IOException e) {
      logger.log(Level.FINE, "IO failure while sending interest: ", e);
      segmentedData.reject(e);
    }

    return segmentedData;
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
  private void requestRemainingSegments(Face face, SegmentedFutureData segmentedData, Interest template, long fromSegment, long toSegment) {
    // send interests in remaining segments
    for (long i = fromSegment; i <= toSegment; i++) {
      Interest segmentedInterest = new Interest(template);
      segmentedInterest.getName().appendSegment(i);
      Future<Data> futureData = SimpleClient.getDefault().getAsync(face, segmentedInterest);
      segmentedData.add((int) i, futureData);
    }
  }

  /**
   * Asynchronously send Interest packets for a segmented result; see {@link #getAsync(net.named_data.jndn.Face, net.named_data.jndn.Interest)
   * } for more information.
   *
   * @param face the {@link Face} on which to retrieve the packets
   * @param name the {@link Name} of the packet to retrieve using a default
   * interest; may optionally end with the segment component of the first
   * segment to retrieve
   * @return an aggregated data packet from all received segments
   */
  public Future<Data> getAsync(Face face, Name name) {
    return getAsync(face, SimpleClient.getDefaultInterest(name));
  }

  /**
   * Retrieve a segmented Data packet; see {@link #getAsync(net.named_data.jndn.Face, net.named_data.jndn.Interest)
   * } for more information. This method will block and call
   * {@link Face#processEvents()} until the sent interests timeout or the
   * corresponding data packets are retrieved.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number
   * @return a Data packet; the name will inherit from the sent Interest, not
   * the returned packets and the content will be a concatenation of all of the
   * packet contents.
   * @throws java.io.IOException
   */
  @Override
  public Data getSync(Face face, Interest interest) throws IOException {
    try {
      return getAsync(face, interest).get();
    } catch (ExecutionException | InterruptedException e) {
      String message = "Failed to retrieve data: " + interest.toUri();
      logger.log(Level.FINE, message, e);
      throw new IOException(message, e);
    }
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout). This method will block and call
   * {@link Face#processEvents()} until the sent interests timeout or the
   * corresponding data packets are retrieved.
   *
   * @param face
   * @param name
   * @return
   * @throws java.io.IOException
   */
  public Data getSync(Face face, Name name) throws IOException {
    return getSync(face, SimpleClient.getDefaultInterest(name));
  }

  /**
   * Check if a name ends in a segment component; uses marker value found in the
   * NDN naming conventions (see
   * http://named-data.net/doc/tech-memos/naming-conventions.pdf).
   *
   * @param name
   * @return
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
