/*
 * File name: Client.java
 * 
 * Purpose: Provide a client to simplify information retrieval over the NDN
 * network.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import java.util.logging.Logger;

/**
 * Provide a client to simplify information retrieval over the NDN network.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Client {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = Logger.getLogger(Client.class.getName());
  private static Client defaultInstance;

  /**
   * Singleton access for simpler client use
   *
   * @return
   */
  public static Client getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new Client();
    }
    return defaultInstance;
  }

  /**
   * Asynchronously request the Data for an Interest. This will send the 
   * Interest and return immediately; use futureData.get() to block until the
   * Data returns (see FutureData) or manage the event processing independently.
   *
   * @param face
   * @param interest
   * @return
   */
  public FutureData getAsync(Face face, Interest interest) {
    final FutureData futureData = new FutureData(face, interest.getName());

    // send interest
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          futureData.resolve(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          futureData.reject(new TimeoutException());
        }
      });
    } catch (IOException e) {
      logger.warning("IO failure while sending interest: " + e);
      futureData.reject(e);
    }

    return futureData;
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out).
   *
   * @param face
   * @param name
   * @return
   */
  public FutureData getAsync(Face face, Name name) {
    return getAsync(face, getDefaultInterest(name));
  }

  /**
   * Synchronously retrieve the Data for an Interest; this will block until
   * complete (i.e. either data is received or the interest times out).
   *
   * @param face
   * @param interest
   * @return Data packet or null
   */
  public Data getSync(Face face, Interest interest) {
    // setup event
    long startTime = System.currentTimeMillis();

    // get future data
    FutureData futureData = getAsync(face, interest);

    // process eventCount until a response is received or timeout
    try {
      Data data = futureData.get();
      logger.fine("Request time (ms): " + (System.currentTimeMillis() - startTime));
      return data;
    } catch (ExecutionException | InterruptedException e) {
      logger.warning("Failed to retrieve data: " + e);
      return null;
    }
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out).
   *
   * @param face
   * @param name
   * @return
   */
  public Data getSync(Face face, Name name) {
    return getSync(face, getDefaultInterest(name));
  }

  /**
   * Create a default interest for a given Name using some common settings: -
   * lifetime: 2 seconds
   *
   * @param name
   * @return
   */
  public static Interest getDefaultInterest(Name name) {
    Interest interest = new Interest(name, DEFAULT_TIMEOUT);
    return interest;
  }
}
