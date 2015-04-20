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
package com.intel.jndn.utils.repository;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Define API for storing and retrieving NDN packets
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Repository {

  /**
   * Put a {@link Data} packet in the repository.
   *
   * @param data a {@link Data} packet
   */
  public void put(Data data);

  /**
   * Retrieve a {@link Data} packet in the repository; this method should
   * respect child selectors, exclude selectors, etc.
   *
   * @param interest the {@link Interest}
   * @return a {@link Data} packet
   * @throws DataNotFoundException if the packet is not found
   */
  public Data get(Interest interest) throws DataNotFoundException;

  /**
   * Check if this repository can satisfy the {@link Interest} with a
   * {@link Data} packet; this should check not only name matching but freshness
   * and any other selectors.
   *
   * @param interest the {@link Interest} to attempt to satisfy
   * @return true if a {@link Data} exists that satisfies the {@link Interest}
   */
  public boolean satisfies(Interest interest);

  /**
   * Remove all stale {@link Data} packets from the repository.
   */
  public void cleanup();
}
