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

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

/**
 * Reference to a Packet that has yet to be returned from the network. Usage:
 *
 * <pre><code>
 * FutureData futureData = new FutureData(face, interest.getName());
 * face.expressInterest(interest, new OnData(){
 *	... futureData.resolve(data); ...
 * }, new OnTimeout(){
 *  ... futureData.reject(new TimeoutException());
 * });
 * Data resolvedData = futureData.get(); // will block and call face.processEvents() until complete
 * </code></pre>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class FutureData extends FutureDataBase {

  private Data data;

  /**
   * Constructor
   *
   * @param face the {@link Face} to use for processing events
   * @param name the {@link Name} of the interest sent
   */
  public FutureData(Face face, Name name) {
    super(face, name);
  }

  /**
   * @return true if the request has completed (successfully or not)
   */
  @Override
  public boolean isDone() {
    return isResolved() || isRejected() || isCancelled();
  }

  /**
   * Set the packet when successfully retrieved; unblocks {@link #get()}. Use
   * this method inside an {@link OnData} callback to resolve this future.
   *
   * @param returnedData the {@link Data} returned from the network
   */
  public void resolve(Data returnedData) {
    data = returnedData;
  }

  /**
   * @return true if the {@link Data} has returned and been resolved with
   * {@link #resolve(net.named_data.jndn.Data)}.
   */
  public boolean isResolved() {
    return data != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getData() {
    return data;
  }
}
