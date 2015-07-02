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

import com.intel.jndn.utils.client.impl.StreamException;
import net.named_data.jndn.Data;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

/**
 * Define a stream of {@link Data} packets as they are retrieved from the
 * network and provide methods for observing stream events
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface DataStream extends OnData, OnTimeout, OnComplete, OnException {

  /**
   * @return true if the stream is complete
   */
  public boolean isComplete();

  /**
   * @return the current list of packets retrieved; this may change as more
   * packets are added
   */
  public Data[] list();

  /**
   * @return an assembled packet containing the concatenated bytes of all
   * received packets
   * @throws StreamException if assembly fails
   */
  public Data assemble() throws StreamException;

  /**
   * Watch all {@link OnData} events
   *
   * @param onData the callback fired
   */
  public void observe(OnData onData);

  /**
   * Watch all {@link OnComplete} events
   *
   * @param onComplete the callback fired
   */
  public void observe(OnComplete onComplete);

  /**
   * Watch all {@link OnException} events
   *
   * @param onException the callback fired
   */
  public void observe(OnException onException);

  /**
   * Watch all {@link OnTimeout} events
   *
   * @param onTimeout the callback fired
   */
  public void observe(OnTimeout onTimeout);

}
