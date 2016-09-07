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

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;

import java.io.IOException;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface PushableRepository extends Repository {
  /**
   * Write data to a face. Each name must correspond to one datum, but the implementation may choose to write these
   * as separate data packets (e.g. as segments of a file).
   *
   * @param face the face to write the data to
   * @param name the name of the data to write
   * @throws IOException if the writing fails
   */
  void push(Face face, Name name) throws IOException;
}
