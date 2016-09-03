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

package com.intel.jndn.utils.pubsub;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;

import java.io.IOException;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
interface ContentStore<T> {
  void put(Name name, T data);

  boolean has(Name name);

  T get(Name name);

  void push(Face face, Name name) throws IOException;

  void clear();
}
