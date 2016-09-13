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
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.utils.Client;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provide a client to simplify information retrieval over the NDN network.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SimpleClient implements Client {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());
  private static SimpleClient defaultInstance;
  private final long sleepTime;
  private final long interestLifetime;

  /**
   * Build a simple client
   *
   * @param sleepTime for synchronous processing, the time to sleep the thread
   * between {@link Face#processEvents()}
   * @param interestLifetime the {@link Interest} lifetime for default
   * Interests; see
   * {@link #getAsync(net.named_data.jndn.Face, net.named_data.jndn.Name)}
   */
  public SimpleClient(long sleepTime, long interestLifetime) {
    this.sleepTime = sleepTime;
    this.interestLifetime = interestLifetime;
  }

  /**
   * Build a simple client using default parameters
   */
  public SimpleClient() {
    this(DEFAULT_SLEEP_TIME, DEFAULT_TIMEOUT);
  }

  /**
   * Singleton access for simpler client use
   *
   * @return a default client
   */
  public static SimpleClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new SimpleClient();
    }
    return defaultInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CompletableFuture<Data> getAsync(Face face, Interest interest) {
    final CompletableFuture futureData = new CompletableFuture<>();

    // send interest
    logger.log(Level.FINER, "Sending interest for: " + interest.getName().toUri());
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          logger.log(Level.FINER, "Retrieved data: " + data.getName().toUri());
          futureData.complete(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          String message = interest.getInterestLifetimeMilliseconds() + "ms timeout exceeded";
          futureData.completeExceptionally(new TimeoutException(message));
        }
      });
    } catch (IOException e) {
      logger.log(Level.FINE, "IO failure while sending interest: ", e);
      futureData.completeExceptionally(e);
    }

    return futureData;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CompletableFuture<Data> getAsync(Face face, Name name) {
    return getAsync(face, getDefaultInterest(name));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getSync(Face face, Interest interest) throws IOException {
    CompletableFuture<Data> future = getAsync(face, interest);

    try {
      // process events until complete
      while (!future.isDone()) {
        synchronized (face) {
          face.processEvents();
        }

        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      return future.get();
    } catch (InterruptedException | ExecutionException | EncodingException e) {
      logger.log(Level.FINE, "Failed to retrieve data.", e);
      throw new IOException("Failed to retrieve data.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getSync(Face face, Name name) throws IOException {
    return getSync(face, getDefaultInterest(name));
  }

  /**
   * Create a default interest for a given {@link Name} using the client's
   * passed settings (see {@link #SimpleClient(long, long)})
   *
   * @param name the {@link Name} of the data to retrieve
   * @return a default interest for the given name
   */
  public Interest getDefaultInterest(Name name) {
    return new Interest(name, interestLifetime);
  }
}
