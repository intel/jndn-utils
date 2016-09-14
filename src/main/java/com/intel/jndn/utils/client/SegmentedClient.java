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

/**
 * Define a client that can retrieve segmented packets into a
 * {@link DataStream}.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface SegmentedClient {

  /**
   * Asynchronously request packets until a FinalBlockId is reached. With this
   * method, the user is responsible for calling {@link Face#processEvents()} in
   * order for the data to be retrieved.
   *
   * @param face the {@link Face} on which to retry requests
   * @param interest the {@link Interest} to retry
   * @return a data stream of packets returned
   * @throws IOException if the initial request fails
   */
  DataStream getSegmentsAsync(Face face, Interest interest) throws IOException;
}
