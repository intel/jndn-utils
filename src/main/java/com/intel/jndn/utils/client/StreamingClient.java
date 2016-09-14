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
package com.intel.jndn.utils.client;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Define a client that can stream content bytes that are partitioned over
 * multiple packets.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface StreamingClient {

  /**
   * Asynchronously request a packet over the network and retrieve the
   * partitioned content bytes as an input stream
   *
   * @param face the {@link Face} on which to make the request; call
   * {@link Face#processEvents()} separately to complete the request
   * @param interest the {@link Interest} to send over the network
   * @param partitionMarker the byte marker identifying how the data packets are
   * partitioned (e.g. segmentation, see
   * http://named-data.net/doc/tech-memos/naming-conventions.pdf)
   * @return a stream of content bytes
   * @throws IOException if the stream setup fails
   */
  InputStream getStreamAsync(Face face, Interest interest, SegmentationType partitionMarker) throws IOException;
}
