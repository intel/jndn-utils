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
package com.intel.jndn.utils.event;

/**
 * Signals an event (from Client or Server) for observers to act on
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NDNEvent<T> {

  protected boolean success;
  protected long timestamp;
  protected T packet;

  /**
   * Constructor
   */
  public NDNEvent() {
    timestamp = System.currentTimeMillis();
    success = false; // an event without a packet is a failed event
  }

  /**
   * Constructor
   *
   * @param packet
   */
  public NDNEvent(T packet) {
    fromPacket(packet);
  }

  /**
   * Build this event from a passed packet; the event is considered a failure if
   * the packet is any type of Exception
   *
   * @param packet
   */
  public final void fromPacket(T packet) {
    this.timestamp = System.currentTimeMillis();
    this.success = !Exception.class.isInstance(packet);
    this.packet = packet;
  }

  /**
   * Retrieve success status
   *
   * @return
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Retrieve event timestamp
   *
   * @return
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Retrieve event packet
   *
   * @return
   */
  public T getPacket() {
    return packet;
  }
}
