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
package com.intel.jndn.utils.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServerHelper {

  public static final int DEFAULT_SEGMENT_SIZE = 4096;

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all
   * the bytes first in order to determine the end segment for FinalBlockId.
   *
   * @param template
   * @param bytes
   * @return
   * @throws IOException
   */
  public static List<Data> segment(Data template, InputStream bytes) throws IOException {
    return segment(template, bytes, DEFAULT_SEGMENT_SIZE);
  }

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all
   * the bytes first in order to determine the end segment for FinalBlockId.
   *
   * @param template
   * @param bytes
   * @param segmentSize
   * @return
   * @throws IOException
   */
  public static List<Data> segment(Data template, InputStream bytes, int segmentSize) throws IOException {
    List<Data> segments = new ArrayList<>();
    byte[] buffer_ = readAll(bytes);
    ByteBuffer buffer = ByteBuffer.wrap(buffer_, 0, buffer_.length);
    int end = (int) Math.floor(buffer_.length / segmentSize);
    Name.Component lastSegment = Name.Component.fromNumberWithMarker(end, 0x00);
    for (int i = 0; i <= end; i++) {
      Data segment = new Data(template);
      segment.getName().appendSegment(i);
      segment.getMetaInfo().setFinalBlockId(lastSegment);
      byte[] content = new byte[segmentSize];
      buffer.get(content, i * segmentSize, segmentSize);
      segment.setContent(new Blob(content));
    }
    return segments;
  }

  /**
   * Read all the bytes in an input stream.
   *
   * @param bytes
   * @return
   * @throws IOException
   */
  public static byte[] readAll(InputStream bytes) throws IOException {
    ByteArrayOutputStream builder = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int read = 0;
    while ((read = bytes.read(buffer)) != -1) {
      builder.write(buffer);
    }
    builder.flush();
    return builder.toByteArray();
  }
}
