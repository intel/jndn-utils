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

/**
 * Callback fired when an activity throws an exception; this is necessary because
 * some actions are performed asynchronously (e.g. in a thread pool) and exceptions
 * could otherwise be lost.
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface OnException {

  /**
   * Called when an activity throws an exception
   * @param exception the exception thrown
   */
  void onException(Exception exception);
}
