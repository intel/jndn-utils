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
import com.intel.jndn.utils.client.OnException;
import com.intel.jndn.utils.client.SegmentationType;
import com.intel.jndn.utils.client.SegmentedClient;
import com.intel.jndn.utils.client.StreamingClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

/**
 * Default implementation of {@link StreamingClient}; uses a segmented client to
 * retrieve packets asynchronously and pipes them to a stream as they are
 * received.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class DefaultStreamingClient implements StreamingClient {

  private static final Logger logger = Logger.getLogger(DefaultStreamingClient.class.getName());
  private SegmentedClient client;

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getStreamAsync(Face face, Interest interest, SegmentationType partitionMarker) throws IOException {
    return getStreamAsync(face, interest, partitionMarker, new DefaultOnException());
  }

  /**
   * 
   * @param face
   * @param interest
   * @param partitionMarker
   * @param onException
   * @return
   * @throws IOException
   */
  public InputStream getStreamAsync(Face face, Interest interest, SegmentationType partitionMarker, OnException onException) throws IOException {
    SegmentedClient client = DefaultSegmentedClient.getDefault();
    return getStreamAsync(client.getSegmentsAsync(face, interest), onException);
  }

  /**
   *
   * @param onDataStream
   * @param onException
   * @return
   * @throws IOException
   */
  public InputStream getStreamAsync(DataStream onDataStream, OnException onException) throws IOException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    onDataStream.observe(onException);

    onDataStream.observe(new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        try {
          out.write(data.getContent().getImmutableArray());
        } catch (IOException ex) {
          onDataStream.onException(ex);
        }
      }
    });

    return in;
  }

  /**
   * Helper for logging exceptions
   */
  private class DefaultOnException implements OnException {

    @Override
    public void onException(Exception exception) {
      logger.log(Level.SEVERE, "Streaming failed", exception);
    }
  }
}
