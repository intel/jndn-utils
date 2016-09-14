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

import com.intel.jndn.utils.client.DataStream;
import com.intel.jndn.utils.client.OnComplete;
import com.intel.jndn.utils.client.OnException;
import com.intel.jndn.utils.client.RetryClient;
import com.intel.jndn.utils.client.SegmentationType;
import com.intel.jndn.utils.client.SegmentedClient;
import com.intel.jndn.utils.client.StreamingClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnTimeout;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Implementation of a client that can handle segmented data, retries after
 * failed requests, and streaming of data packets.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class AdvancedClient extends SimpleClient implements SegmentedClient, StreamingClient {

  public static final int DEFAULT_MAX_RETRIES = 3;
  private static final Logger logger = Logger.getLogger(AdvancedClient.class.getName());
  private static AdvancedClient defaultInstance;
  private final SegmentedClient segmentedClient;
  private final RetryClient retryClient;
  private final StreamingClient streamingClient;

  /**
   * Build an advanced client
   *
   * @param sleepTime for synchronous processing, the time to sleep the thread
   * between {@link Face#processEvents()}
   * @param interestLifetime the {@link Interest} lifetime for default
   * Interests; see
   * {@link #getAsync(net.named_data.jndn.Face, net.named_data.jndn.Name)}
   * @param segmentedClient the {@link SegmentedClient} to use for segmented
   * data
   * @param retryClient the {@link RetryClient} to use for retrying failed
   * packets
   * @param streamingClient the {@link StreamingClient} to use for segmented
   * data
   */
  public AdvancedClient(long sleepTime, long interestLifetime, SegmentedClient segmentedClient, RetryClient retryClient, StreamingClient streamingClient) {
    super(sleepTime, interestLifetime);
    this.segmentedClient = segmentedClient;
    this.retryClient = retryClient;
    this.streamingClient = streamingClient;
  }

  /**
   * Build an advanced client using default parameters
   */
  public AdvancedClient() {
    super();
    this.segmentedClient = new DefaultSegmentedClient();
    this.retryClient = new DefaultRetryClient(DEFAULT_MAX_RETRIES);
    this.streamingClient = new DefaultStreamingClient();
  }

  /**
   * Singleton access for simpler client use
   *
   * @return a default client
   */
  public static AdvancedClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new AdvancedClient();
    }
    return defaultInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CompletableFuture<Data> getAsync(Face face, Interest interest) {
    CompletableFuture<Data> future = new CompletableFuture<>();

    try {
      DataStream stream = getSegmentsAsync(face, interest);

      stream.observe(new OnException() {
        public void onException(Exception exception) {
          future.completeExceptionally(exception);
        }
      });

      stream.observe(new OnComplete() {
        public void onComplete() {
          try {
            future.complete(stream.assemble());
          } catch (StreamException ex) {
            stream.onException(ex);
          }
        }
      });
    } catch (IOException ex) {
      future.completeExceptionally(ex);
    }

    return future;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataStream getSegmentsAsync(Face face, Interest interest) throws IOException {
    DataStream stream = segmentedClient.getSegmentsAsync(face, interest);
    stream.observe(new RetryHandler(face, stream));
    return stream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getStreamAsync(Face face, Interest interest, SegmentationType partitionMarker) throws IOException {
    return streamingClient.getStreamAsync(face, interest, partitionMarker);
  }

  /**
   * Helper class for calling the retry client on failed requests
   */
  private class RetryHandler implements OnTimeout {
    private final Face face;
    private final DataStream stream;

    private RetryHandler(Face face, DataStream stream) {
      this.face = face;
      this.stream = stream;
    }

    @Override
    public void onTimeout(Interest failedInterest) {
      logger.info("Timeout: " + failedInterest.toUri());
      try {
        retryClient.retry(face, failedInterest, stream, new OnTimeout() {
          @Override
          public void onTimeout(Interest interest) {
            stream.onException(new TimeoutException("Interest timed out despite retries: " + interest.toUri()));
          }
        });
      } catch (IOException ex) {
        stream.onException(ex);
      }
    }
  }
}
