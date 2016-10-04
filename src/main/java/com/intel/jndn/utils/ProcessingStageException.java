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
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class ProcessingStageException extends Exception {
  public ProcessingStageException(String message) {
    super(message);
  }

  public ProcessingStageException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessingStageException(Throwable cause) {
    super(cause);
  }
}
