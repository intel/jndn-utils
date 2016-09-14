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
 * Represents a token used for canceling some ongoing action. This approach was chosen as it is lighter and more
 * flexible than a cancellable {@link java.util.concurrent.Future}; perhaps it could be replaced by {@link
 * AutoCloseable} although this seems to imply something else (however, the language support is a benefit, TODO
 * re-look at this).
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface Cancellation {
  /**
   * Shortcut for a no-op, already-cancelled cancellation
   */
  Cancellation CANCELLED = () -> {/* do nothing */};

  /**
   * Cancel the action referenced by this token
   */
  void cancel();
}
