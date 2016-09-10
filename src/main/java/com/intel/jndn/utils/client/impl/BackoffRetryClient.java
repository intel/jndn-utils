/*
 * jndn-utils
 * Copyright (c) 2016, Intel Corporation.
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

import com.intel.jndn.utils.client.RetryClient;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BackoffRetryClient implements RetryClient {

  private static final Logger LOGGER = Logger.getLogger(BackoffRetryClient.class.getName());
  private final double cutoffLifetime;
  private final int backoffFactor;

  public BackoffRetryClient(double cutoffLifetime, int backoffFactor) {
    this.cutoffLifetime = cutoffLifetime;
    this.backoffFactor = backoffFactor;
  }

  @Override
  public void retry(Face face, Interest interest, OnData onData, OnTimeout onTimeout) throws IOException {
    retryInterest(face, interest, onData, onTimeout);
  }

  private void retryInterest(Face face, Interest interest, OnData onData, OnTimeout onTimeout) throws IOException {
    double newLifetime = interest.getInterestLifetimeMilliseconds() * backoffFactor;
    if (newLifetime < cutoffLifetime) {
      interest.setInterestLifetimeMilliseconds(newLifetime);
      resend(face, interest, onData, onTimeout);
    } else {
      onTimeout.onTimeout(interest);
    }
  }

  private void resend(Face face, Interest interest, OnData onData, OnTimeout onTimeout) throws IOException {
    LOGGER.log(Level.INFO, "Resending interest with {0}ms lifetime: {1}", new Object[]{interest.getInterestLifetimeMilliseconds(), interest.getName()});
    face.expressInterest(interest, onData, timedOutInterest -> {
      try {
        retryInterest(face, timedOutInterest, onData, onTimeout);
      } catch (IOException e) {
        onTimeout.onTimeout(interest);
      }
    });
  }
}
