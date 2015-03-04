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
import java.util.Observer;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Track asynchronous events from Client and Server
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NDNObserver implements Observer {

  protected List<NDNEvent> events = new ArrayList<>();
  protected long timestamp;
  protected OnEvent then;
  protected boolean stopThread;

  /**
   * Constructor
   */
  public NDNObserver() {
    timestamp = System.currentTimeMillis();
  }

  /**
   * Receive notifications from observables
   *
   * @param o
   * @param arg
   */
  @Override
  public void update(Observable o, Object arg) {
    NDNEvent event = (NDNEvent) arg;
    events.add(event);
    // call onData callbacks
    if (then != null) {
      then.onEvent(event);
    }
  }

  /**
   * Register a handler for events
   *
   * @param callback
   * @return
   */
  public NDNObserver then(OnEvent callback) {
    then = callback;
    return this;
  }

  /**
   * Count the number of eventCount observed
   *
   * @return
   */
  public int eventCount() {
    return events.size();
  }

  /**
   * Count the number of interest packets observed (received or sent)
   *
   * @return
   */
  public int interestCount() {
    return count(Interest.class);
  }

  /**
   * Count the number of Data packets observed
   *
   * @return
   */
  public int dataCount() {
    return count(Data.class);
  }

  /**
   * Count the number of errors observed
   *
   * @return
   */
  public int errorCount() {
    return count(Exception.class);
  }

  /**
   * Count the number of observed packets by type
   *
   * @param type
   * @return
   */
  public int count(Class type) {
    int count = 0;
    for (NDNEvent event : events) {
      if (type.isInstance(event.packet)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Calculate time elapsed since observer started observing until this method
   * is called
   *
   * @return
   */
  public long getTimeSinceStart() {
    if (getLast() != null) {
      return getLast().getTimestamp() - timestamp;
    }
    return -1;
  }

  /**
   * Retrieve a list of observed events
   *
   * @return event or null
   */
  public List<NDNEvent> getEvents() {
    return events;
  }

  /**
   * Retrieve the first event
   *
   * @return event or null
   */
  public NDNEvent getFirst() {
    if (events.size() > 0) {
      return events.get(0);
    }
    return null;
  }

  /**
   * Retrieve the last event
   *
   * @return event or null
   */
  public NDNEvent getLast() {
    if (events.size() > 0) {
      return events.get(events.size() - 1);
    }
    return null;
  }

  /**
   * Stop the current Client thread; used by asynchronous Client methods to stop
   * the request/response thread
   */
  public void stop() {
    stopThread = true;
  }

  /**
   * Check the current stop status; used by asynchronous Client methods to stop
   * the request/response thread
   *
   * @return
   */
  public boolean mustStop() {
    return stopThread;
  }
}
