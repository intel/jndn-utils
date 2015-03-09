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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

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
public class SegmentedClient {

  private static SegmentedClient defaultInstance;
  private static final Logger logger = Logger.getLogger(SegmentedClient.class.getName());
  private static final int SLEEP_TIME_MS = 10;

  /**
   * Singleton access for simpler client use
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
   * Asynchronously send Interest packets for a segmented result; will block
   * until the first packet is received and then send remaining interests until
   * the specified FinalBlockId.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number
   * @return a list of FutureData packets; if the first segment fails, the list
   * will contain one FutureData with the failure exception
   */
  public List<FutureData> getAsync(Face face, Interest interest) {
    // get first segment; default 0 or use a specified start segment
    long firstSegment = 0;
    boolean specifiedSegment = false;
    try {
      firstSegment = interest.getName().get(-1).toSegment();
      specifiedSegment = true;
    } catch (EncodingException e) {
      // check for interest selector if no initial segment found
      if (interest.getChildSelector() == -1) {
        logger.log(Level.WARNING, "No child selector set for a segmented Interest; this may result in incorrect retrieval.");
      }
    }

    // setup segments
    final List<FutureData> segments = new ArrayList<>();
    segments.add(Client.getDefault().getAsync(face, interest));

    // retrieve first packet to find last segment value
    long lastSegment;
    try {
      lastSegment = segments.get(0).get().getMetaInfo().getFinalBlockId().toSegment();
    } catch (ExecutionException | InterruptedException | EncodingException e) {
      logger.log(Level.SEVERE, "Failed to retrieve first segment: ", e);
      return segments;
    }

    // cut interest segment off
    if (specifiedSegment) {
      interest.setName(interest.getName().getPrefix(-1));
    }

    // send interests in remaining segments
    for (long i = firstSegment + 1; i <= lastSegment; i++) {
      Interest segmentedInterest = new Interest(interest);
      segmentedInterest.getName().appendSegment(i);
      FutureData futureData = Client.getDefault().getAsync(face, segmentedInterest);
      segments.add((int) i, futureData);
    }

    return segments;
  }

  /**
   * Asynchronously send Interests for a segmented Data packet using a default
   * interest (e.g. 2 second timeout); this will block until complete (i.e.
   * either data is received or the interest times out). See getAsync(Face face)
   * for more information.
   *
   * @param face
   * @param name
   * @return
   */
  public List<FutureData> getAsync(Face face, Name name) {
    return getAsync(face, Client.getDefaultInterest(name));
  }

  /**
   * Retrieve a segmented Data packet; will block until all segments are
   * received and will re-assemble these.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number
   * @return a Data packet; the name will inherit from the sent Interest, not
   * the returned packets (TODO or should we parse from returned Data? copy
   * first Data?) and the content will be a concatenation of all of the packet
   * contents.
   */
  public Data getSync(Face face, Interest interest) {
    List<FutureData> segments = getAsync(face, interest);

    // process events until complete
    while (!isSegmentListComplete(segments)) {
      try {
        face.processEvents();
        Thread.sleep(SLEEP_TIME_MS);
      } catch (EncodingException | IOException e) {
        logger.log(Level.WARNING, "Failed to retrieve data: ", e);
        return null;
      } catch (InterruptedException ex) {
        // do nothing
      }
    }

    // build final blob
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    for (FutureData futureData : segments) {
      try {
        content.write(futureData.get().getContent().getImmutableArray());
      } catch (ExecutionException | IOException | InterruptedException e) {
        logger.log(Level.WARNING, "Failed to parse retrieved data: ", e);
        return null;
      }
    }

    Data data = new Data(interest.getName()); // TODO this name may not be correct; may need to contain additional suffixes
    data.setContent(new Blob(content.toByteArray()));
    return data;
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out). See getSync(Face face) for more
   * information.
   *
   * @param face
   * @param name
   * @return
   */
  public Data getSync(Face face, Name name) {
    return getSync(face, Client.getDefaultInterest(name));
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
   * Check if a list of segments have returned from the network.
   *
   * @param segments
   * @return
   */
  protected boolean isSegmentListComplete(List<FutureData> segments) {
    for (FutureData futureData : segments) {
      if (!futureData.isDone()) {
        return false;
      }
    }
    return true;
  }
}
