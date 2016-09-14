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

package com.intel.jndn.utils.impl;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for reading and writing segmented NDN packets. See <a
 * href="http://named-data.net/doc/tech-memos/naming-conventions.pdf">NDN Naming Conventions</a> for information used
 * in this class
 * <p>
 * For segmentation of streams: the current use of the default segment size of 4096
 * (only for {@link #segment(net.named_data.jndn.Data, java.io.InputStream)} is based on several assumptions: NDN packet
 * size was limited to 8000 at the time this was written and the signature size is unknown.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SegmentationHelper {

  public static final int DEFAULT_SEGMENT_SIZE = 4096;
  private static final byte NDN_SEGMENT_MARKER = 0x00;

  private SegmentationHelper() {
    // do not instantiate this class
  }

  /**
   * Determine if a name is segmented, i.e. if it ends with the correct marker type.
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return true if the name is segmented
   */
  public static boolean isSegmented(Name name, byte marker) {
    return name.size() > 0 && name.get(-1).getValue().buf().get(0) == marker;
  }

  /**
   * Retrieve the segment number from the last component of a name.
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return the segment number
   * @throws EncodingException if the name does not have a final component of the correct marker type
   */
  public static long parseSegment(Name name, byte marker) throws EncodingException {
    if (name.size() == 0) {
      throw new EncodingException("No components to parse.");
    }
    return name.get(-1).toNumberWithMarker(marker);
  }

  /**
   * Remove a segment component from the end of a name
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return the new name with the segment component removed or a copy of the name if no segment component was present
   */
  public static Name removeSegment(Name name, byte marker) {
    return isSegmented(name, marker) ? name.getPrefix(-1) : new Name(name);
  }

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all
   * the bytes first in order to determine the end segment for FinalBlockId.
   *
   * @param template the {@link Data} packet to use for the segment {@link Name}, {@link net.named_data.jndn.MetaInfo},
   * etc.
   * @param bytes an {@link InputStream} to the bytes to segment
   * @return a list of segmented {@link Data} packets
   * @throws IOException if the stream fails
   */
  public static List<Data> segment(Data template, InputStream bytes) throws IOException {
    return segment(template, bytes, DEFAULT_SEGMENT_SIZE);
  }

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all the bytes first in order to determine the
   * end segment for FinalBlockId. TODO this could be smarter and only add segments if necessary
   *
   * @param template the {@link Data} packet to use for the segment {@link Name}, {@link net.named_data.jndn.MetaInfo},
   * etc.
   * @param bytes an {@link InputStream} to the bytes to segment
   * @return a list of segmented {@link Data} packets
   * @throws IOException if the stream fails
   */
  public static List<Data> segment(Data template, InputStream bytes, int segmentSize) throws IOException {
    List<Data> segments = new ArrayList<>();
    byte[] readBytes = readAll(bytes);
    int numBytes = readBytes.length;
    int numPackets = (int) Math.ceil((double) numBytes / segmentSize);
    ByteBuffer buffer = ByteBuffer.wrap(readBytes, 0, numBytes);
    Name.Component lastSegment = Name.Component.fromNumberWithMarker((long) numPackets - 1, NDN_SEGMENT_MARKER);

    for (int i = 0; i < numPackets; i++) {
      Data segment = new Data(template);
      segment.getName().appendSegment(i);
      segment.getMetaInfo().setFinalBlockId(lastSegment);
      byte[] content = new byte[Math.min(segmentSize, buffer.remaining())];
      buffer.get(content);
      segment.setContent(new Blob(content));
      segments.add(segment);
    }

    return segments;
  }

  /**
   * Read all of the bytes in an input stream.
   *
   * @param bytes the {@link InputStream} of bytes to read
   * @return an array of all bytes retrieved from the stream
   * @throws IOException if the stream fails
   */
  public static byte[] readAll(InputStream bytes) throws IOException {
    ByteArrayOutputStream builder = new ByteArrayOutputStream();
    int read = bytes.read();
    while (read != -1) {
      builder.write(read);
      read = bytes.read();
    }
    builder.flush();
    bytes.close();
    return builder.toByteArray();
  }
}
