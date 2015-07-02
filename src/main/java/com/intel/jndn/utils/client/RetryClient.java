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

import java.io.IOException;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

/**
 * Define a client that can retry {@link Interest} packets on timeout.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface RetryClient {

  /**
   * Retry the given interest according to some implementation-specific
   * strategy; the implementor must correctly call the given callbacks to pass
   * success or failure states up to the application (e.g. if after retrying,
   * the client gives up, the application's OnTimeout should be called). Note
   * that an interest passed to this method may timeout long after it's lifetime
   * due to the implementation's retry strategy.
   *
   * @param face the {@link Face} on which to retry requests
   * @param interest the {@link Interest} to retry
   * @param onData the application's success callback
   * @param onTimeout the application's failure callback
   * @throws IOException when the client cannot perform the necessary network IO
   */
  public void retry(Face face, Interest interest, OnData onData, OnTimeout onTimeout) throws IOException;
}
