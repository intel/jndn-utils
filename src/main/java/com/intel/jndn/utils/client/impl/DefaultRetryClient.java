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

import com.intel.jndn.utils.client.RetryClient;
import java.io.IOException;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

/**
 * Default implementation of {@link RetryClient}; on request failure, this class
 * immediately retries the request until a maximum number of retries is reached.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class DefaultRetryClient implements RetryClient {

  private static final Logger logger = Logger.getLogger(DefaultRetryClient.class.getName());
  private final int numRetriesAllowed;
  private int totalRetries = 0;

  public DefaultRetryClient(int numRetriesAllowed) {
    this.numRetriesAllowed = numRetriesAllowed;
  }

  /**
   * On timeout, retry the request until the maximum number of allowed retries
   * is reached.
   *
   * @param face the {@link Face} on which to retry requests
   * @param interest the {@link Interest} to retry
   * @param onData the application's success callback
   * @param onTimeout the application's failure callback
   * @throws IOException when the client cannot perform the necessary network IO
   */
  @Override
  public void retry(Face face, Interest interest, OnData onData, OnTimeout onTimeout) throws IOException {
    RetryContext context = new RetryContext(face, interest, onData, onTimeout);
    retryInterest(context);
  }

  /**
   * Synchronized helper method to prevent multiple threads from mashing
   * totalRetries
   *
   * @param context the current request context
   * @throws IOException when the client cannot perform the necessary network IO
   */
  synchronized private void retryInterest(RetryContext context) throws IOException {
    logger.info("Retrying interest: " + context.interest.toUri());
    context.face.expressInterest(context.interest, context, context);
    totalRetries++;
  }

  /**
   * @return the total number of retries logged by this client
   */
  public int totalRetries() {
    return totalRetries;
  }

  /**
   * Helper class to separate out each request context; this allows the client
   * to accept multiple concurrent retry operations
   */
  private class RetryContext implements OnData, OnTimeout {

    public int numFailures = 0;
    public final Face face;
    public final Interest interest;
    public final OnData applicationOnData;
    public final OnTimeout applicationOnTimeout;

    public RetryContext(Face face, Interest interest, OnData applicationOnData, OnTimeout applicationOnTimeout) {
      this.face = face;
      this.interest = interest;
      this.applicationOnData = applicationOnData;
      this.applicationOnTimeout = applicationOnTimeout;
    }

    public boolean shouldRetry() {
      return numFailures < numRetriesAllowed;
    }

    @Override
    public void onData(Interest interest, Data data) {
      applicationOnData.onData(interest, data);
    }

    @Override
    public void onTimeout(Interest interest) {
      numFailures++;
      logger.finest("Request failed, count " + numFailures + ": " + interest.toUri());

      if (shouldRetry()) {
        try {
          retryInterest(this);
        } catch (IOException ex) {
          applicationOnTimeout.onTimeout(interest);
        }
      } else {
        applicationOnTimeout.onTimeout(interest);
      }
    }
  }
}
