/*
 * File name: ClientObserver.java
 * 
 * Purpose: Track asynchronous events from Client and provide simplified API
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ClientObserver implements Observer {

  protected List<ClientEvent> events = new ArrayList<>();
  protected long timestamp;
  protected OnEvent then;
  protected boolean stopThread;

  /**
   * Constructor
   */
  public ClientObserver() {
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
    ClientEvent event = (ClientEvent) arg;
    events.add(event);
    // call onData callbacks
    if (Data.class.isInstance(event.packet) && then != null) {
      then.onEvent(event);
    }
  }

  /**
   * Register a handler for events
   *
   * @param callback
   * @return
   */
  public ClientObserver then(OnEvent callback) {
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
    for (ClientEvent event : events) {
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
  public List<ClientEvent> getEvents() {
    return events;
  }

  /**
   * Retrieve the first event
   *
   * @return event or null
   */
  public ClientEvent getFirst() {
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
  public ClientEvent getLast() {
    if (events.size() > 0) {
      return events.get(events.size() - 1);
    }
    return null;
  }

  /**
   * Stop the current Client thread; used by asynchronous Client methods to
   * stop the request/response thread
   */
  public void stop() {
    stopThread = true;
  }

  /**
   * Check the current stop status; used by asynchronous Client methods to
   * stop the request/response thread
   *
   * @return
   */
  public boolean mustStop() {
    return stopThread;
  }
}
