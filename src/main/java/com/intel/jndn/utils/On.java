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

/**
 * Generic callback API for receiving some signal T when an event is fired.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
@FunctionalInterface
public interface On<T> {
  /**
   * @param event the event object describing the occurrence
   */
  void on(T event);
}
