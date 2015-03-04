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

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.transport.Transport;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NDNObservable extends Observable implements OnData, OnTimeout, OnInterest {

  protected List<NDNEvent> events = new ArrayList<>();
  protected List<Interest> incomingInterestPackets = new ArrayList<>();
  protected List<Data> incomingDataPackets;

  /**
   * Generic notification
   *
   * @param <T>
   * @param packet
   */
  public <T> void notify(T packet) {
    setChanged();
    notifyObservers(new NDNEvent(packet));
  }

  /**
   * Handle data packets
   *
   * @param interest
   * @param data
   */
  @Override
  public void onData(Interest interest, Data data) {
    notify(data);
  }

  /**
   * Handle exceptions
   *
   * @param e
   */
  public void onError(Exception e) {
    notify(e);
  }

  /**
   * Handle timeouts
   *
   * @param interest
   */
  @Override
  public void onTimeout(Interest interest) {
    notify(new Exception("Interest timed out: " + interest.getName().toUri()));
  }

  /**
   * Handle incoming interests
   *
   * @param prefix
   * @param interest
   * @param transport
   * @param registeredPrefixId
   */
  @Override
  public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
    notify(interest); // TODO wrap
  }

  /**
   * Helper to reference both outgoing interest and incoming data packets
   */
  class InterestDataPacket {

    private Interest interest;
    private Data data;

    public InterestDataPacket(Interest interest, Data data) {
      this.interest = interest;
      this.data = data;
    }

    public Data getData() {
      return data;
    }

    public Interest getInterest() {
      return interest;
    }
  }

  /**
   * Helper to reference both incoming interest and the transport to send data
   * on
   */
  class InterestTransportPacket {

    private Interest interest;
    private Transport transport;

    public InterestTransportPacket(Interest interest, Transport transport) {
      this.interest = interest;
      this.transport = transport;
    }

    public Interest getInterest() {
      return interest;
    }

    public Transport getTransport() {
      return transport;
    }

  }
}
