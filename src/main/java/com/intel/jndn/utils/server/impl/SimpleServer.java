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
package com.intel.jndn.utils.server.impl;

import com.intel.jndn.utils.server.DynamicServer;
import com.intel.jndn.utils.server.RespondWithBlob;
import com.intel.jndn.utils.server.RespondWithData;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a {@link DynamicServer} that wraps the {@link OnInterest}
 * callback with some encoding and pipeline support.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SimpleServer extends ServerBaseImpl implements DynamicServer {

  private static final Logger logger = Logger.getLogger(SimpleServer.class.getName());
  private RespondWithData callback;

  /**
   * {@inheritDoc}
   */
  public SimpleServer(Face face, Name prefix) {
    super(face, prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void respondUsing(RespondWithData callback) throws IOException {
    if (!isRegistered()) {
      register();
    }
    this.callback = callback;
  }

  /**
   * Convenience method for responding to an {@link Interest} by returning the
   * {@link Blob} content only; when an Interest arrives, this method wraps the
   * returned Blob with a {@link Data} using the exact {@link Name} of the
   * incoming Interest.
   *
   * @param callback the callback function to retrieve content when an
   * {@link Interest} arrives
   * @throws java.io.IOException if the server fails to register a prefix
   */
  public void respondUsing(final RespondWithBlob callback) throws IOException {
    RespondWithData dataCallback = new RespondWithData() {
      @Override
      public Data onInterest(Name prefix, Interest interest) throws Exception {
        Data data = new Data(interest.getName());
        Blob content = callback.onInterest(prefix, interest);
        data.setContent(content);
        return data;
      }
    };
    respondUsing(dataCallback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter){
    try {
      Data data = callback.onInterest(prefix, interest);
      data = processPipeline(data);
      face.putData(data);
    } catch (Exception e) {
      logger.log(Level.FINE, "Failed to send data for: " + interest.toUri(), e);
    }
  }
}
