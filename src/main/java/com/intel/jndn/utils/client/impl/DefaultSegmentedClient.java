/*
 * jndn-utils
 * Copyright (c) 2016, Intel Corporation.
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
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.utils.client.DataStream;
import com.intel.jndn.utils.client.SegmentedClient;
import com.intel.jndn.utils.impl.SegmentationHelper;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.OnData;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Retrieve segments one by one until the FinalBlockId indicates an end segment;
 * then request remaining packets. This class currently only handles segmented
 * data (not yet byte-offset segmented data).
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DefaultSegmentedClient implements SegmentedClient {

  private static final Logger logger = Logger.getLogger(DefaultSegmentedClient.class.getName());
  private static DefaultSegmentedClient defaultInstance;
  private final byte marker = 0x00;

  /**
   * Singleton access for simpler client use
   *
   * @return a default client
   */
  public static DefaultSegmentedClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new DefaultSegmentedClient();
    }
    return defaultInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataStream getSegmentsAsync(Face face, Interest interest) throws IOException {
    SegmentedDataStream stream = new SegmentedDataStream();

    // once more packets are received, request more
    stream.observe(new SegmentationContext(stream, face));

    // request first packet
    logger.info("Interest requested: " + interest.toUri());
    face.expressInterest(interest, stream, stream);

    return stream;
  }

  /**
   * Replace the final component of an interest name with a segmented component;
   * if the interest name does not have a segmented component, this will add
   * one.
   *
   * @param interest the request
   * @param segmentNumber a segment number
   * @param marker a marker to use for segmenting the packet
   * @return a segmented interest (a copy of the passed interest)
   */
  protected Interest replaceFinalComponent(Interest interest, long segmentNumber, byte marker) {
    Interest copied = new Interest(interest);
    Component lastComponent = Component.fromNumberWithMarker(segmentNumber, marker);
    Name newName = (SegmentationHelper.isSegmented(copied.getName(), marker))
        ? copied.getName().getPrefix(-1)
        : new Name(copied.getName());
    copied.setName(newName.append(lastComponent));
    return copied;
  }

  /**
   * Helper class to track the last requested segment for a given request
   * context and request follow-on packets
   */
  private class SegmentationContext implements OnData {

    private final SegmentedDataStream stream;
    private final Face face;
    private long lastRequestedSegment;

    public SegmentationContext(SegmentedDataStream stream, Face face) {
      this.stream = stream;
      this.face = face;
    }

    private synchronized void setLastRequestedSegment(long segmentNumber) {
      lastRequestedSegment = segmentNumber;
    }

    @Override
    public void onData(Interest interest, Data data) {
      try {
        if (stream.current() >= lastRequestedSegment) {
          Interest dataBasedName = new Interest(interest).setName(data.getName());
          if (stream.hasEnd()) {
            if (stream.current() < stream.end()) {
              requestRemainingSegments(face, dataBasedName, stream);
            }
          } else {
            requestNext(face, dataBasedName, stream);
          }
        }
      } catch (IOException e) {
        stream.onException(e);
      }
    }

    private void requestRemainingSegments(Face face, Interest interest, SegmentedDataStream stream) throws IOException {
      long from = stream.current() + 1;
      long to = stream.end();
      logger.info("Requesting remaining segments: from #" + from + " to #" + to);

      for (long segmentNumber = stream.current() + 1; segmentNumber <= stream.end(); segmentNumber++) {
        request(face, interest, stream, segmentNumber, marker);
      }
    }

    private void requestNext(Face face, Interest interest, SegmentedDataStream stream) throws IOException {
      long segmentNumber = stream.current() + 1;
      request(face, interest, stream, segmentNumber, marker);
    }

    private void request(Face face, Interest interest, DataStream stream, long segmentNumber, byte marker) throws IOException {
      Interest copiedInterest = replaceFinalComponent(interest, segmentNumber, marker);
      face.expressInterest(copiedInterest, stream, stream);
      logger.info("Interest sent: " + copiedInterest.toUri());
      setLastRequestedSegment(segmentNumber);
    }
  }

}
