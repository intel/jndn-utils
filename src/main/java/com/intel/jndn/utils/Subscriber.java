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

package com.intel.jndn.utils;

import java.io.IOException;
import java.util.Set;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface Subscriber extends AutoCloseable {
  /**
   * @return the currently known publishers
   */
  Set<Long> knownPublishers();

  /**
   * Open the subscriber transport mechanisms and retrieve publisher messages
   *
   * @throws IOException if the subscriber fails during network access
   */
  void open() throws IOException;
}
