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

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Base functionality provided by all NDN clients in this package.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface Client {

  /**
   * Asynchronously request the Data for an Interest. This will send the
   * Interest and return immediately; with this method, the user is responsible
   * for calling {@link Face#processEvents()} in order for the
   * {@link CompletableFuture} to complete.
   *
   * @param face the {@link Face} on which to make the request; call
   * {@link Face#processEvents()} separately to complete the request
   * @param interest the {@link Interest} to send over the network
   * @return a future {@link Data} packet
   */
  CompletableFuture<Data> getAsync(Face face, Interest interest);

  /**
   * Convenience method for calling
   * {@link #getAsync(net.named_data.jndn.Face, net.named_data.jndn.Interest)}
   * with a default {@link Interest} packet.
   *
   * @param face the {@link Face} on which to make the request; call
   * {@link Face#processEvents()} separately to complete the request
   * @param name the {@link Name} to wrap inside a default {@link Interest}
   * @return a future {@link Data} packet
   */
  CompletableFuture<Data> getAsync(Face face, Name name);

  /**
   * Synchronously retrieve the {@link Data} for an {@link Interest}; this will
   * block until complete (i.e. either the data is received or the interest
   * times out).
   *
   * @param face the {@link Face} on which to make the request; this method will
   * call {@link Face#processEvents()} at a configurable interval until complete
   * or timeout
   * @param interest the {@link Interest} to send over the network
   * @return a {@link Data} packet
   * @throws java.io.IOException if the request fails
   */
  Data getSync(Face face, Interest interest) throws IOException;
  
  /**
   * Convenience method for calling
   * {@link #getSync(net.named_data.jndn.Face, net.named_data.jndn.Interest)}
   * with a default {@link Interest} packet.
   *
   * @param face the {@link Face} on which to make the request; call
   * {@link Face#processEvents()} separately to complete the request
   * @param name the {@link Name} to wrap inside a default {@link Interest}
   * @return a {@link Data} packet
   * @throws java.io.IOException if the request fails
   */
  Data getSync(Face face, Name name) throws IOException;
}
