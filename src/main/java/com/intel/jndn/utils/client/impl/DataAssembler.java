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

import com.intel.jndn.utils.impl.SegmentationHelper;
import net.named_data.jndn.Data;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Internal class for assembling a list of {@link Data} packets into one large
 * data packet; this implementation will use all properties of the first packet
 * in the list and concatenate the content bytes of all packets in order.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
class DataAssembler {

  private final Data[] packets;
  private final byte marker;

  public DataAssembler(Data[] packets, byte marker) {
    this.packets = packets;
    this.marker = marker;
  }

  /**
   * @return a combined packet based on the properties of the first packet and
   * the concatenated bytes of the entire list of packets
   */
  Data assemble() {
    if (packets.length == 0) {
      throw new IllegalStateException("No packets to assemble.");
    }

    // build from first returned packet
    Data combined = new Data(packets[0]);
    combined.setName(SegmentationHelper.removeSegment(combined.getName(), marker));
    try {
      combined.setContent(assembleBlob());
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

    return combined;
  }

  /**
   * @return the concatenated bytes
   * @throws IOException if a stream fails
   */
  private Blob assembleBlob() throws IOException {
    if (packets.length == 0) {
      return new Blob();
    }

    if (packets.length == 1) {
      return packets[0].getContent();
    }

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    for (Data packet : packets) {
      stream.write(packet.getContent().getImmutableArray());
    }
    return new Blob(stream.toByteArray());
  }
}
