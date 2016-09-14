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

import com.intel.jndn.utils.Server;
import net.named_data.jndn.Data;

import java.io.IOException;

/**
 * Defines the API for a {@link Server} producing {@link Data} packets and
 * storing them until they are requested; this server corresponds closely to use
 * cases such as: cache, file system, web server.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface RepositoryServer extends Server {

  /**
   * Store a {@link Data} packet in the server's repository until requested.
   *
   * @param data the {@link Data} packet to store and serve
   * @throws IOException if the underlying server fails to store the packet
   */
  void serve(Data data) throws IOException;
  
  /**
   * Clean up stale {@link Data} packets from the underlying content store.
   */
  void cleanup();
}
