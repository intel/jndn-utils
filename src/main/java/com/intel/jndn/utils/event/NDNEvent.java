/*
 * File name: NDNEvent.java
 * 
 * Purpose: Signals a Client event for observers to act on
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
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
