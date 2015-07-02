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

/**
 * Provide a generic API for processing Interest and Data packets; e.g. a data
 * processing stage may convert a data packet with unencrypted content to one
 * with encrypted content.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface ProcessingStage<T, Y> {

  /**
   * Process the input object.
   *
   * @param input the object to be processed
   * @return a processed object (this may be the same instance as the input or
   * may be a new object)
   * @throws Exception if the processing fails
   */
  public Y process(T input) throws Exception;
}
