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
package com.intel.jndn.utils.server.impl;

import com.intel.jndn.utils.ProcessingStage;
import com.intel.jndn.utils.ProcessingStageException;
import com.intel.jndn.utils.Server;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation for a {@link Server}.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public abstract class ServerBaseImpl implements Server {

  public static final long UNREGISTERED = -1;
  private static final Logger logger = Logger.getLogger(ServerBaseImpl.class.getName());
  private final Face face;
  private final Name prefix;
  private final List<ProcessingStage> pipeline = new ArrayList<>();
  private long registeredPrefixId = UNREGISTERED;

  /**
   * Build the base server; register() must run separately and is run
   * automatically on {@link #run()}.
   *
   * @param face a {@link Face} allowing prefix registration (see
   * {@link Face#setCommandSigningInfo(net.named_data.jndn.security.KeyChain, net.named_data.jndn.Name)}
   * @param prefix the {@link Name} to register
   */
  public ServerBaseImpl(Face face, Name prefix) {
    this.face = face;
    this.prefix = prefix;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Name getPrefix() {
    return prefix;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getRegisteredPrefixId() {
    return registeredPrefixId;
  }

  /**
   * @return true if the server has registered a prefix on the face
   */
  public boolean isRegistered() {
    return registeredPrefixId != UNREGISTERED;
  }

  /**
   * Register a prefix for responding to interests.
   *
   * @throws java.io.IOException if IO fails
   */
  public void register() throws IOException {
    try {
      registeredPrefixId = face.registerPrefix(prefix, this, new OnRegisterFailed() {
        @Override
        public void onRegisterFailed(Name prefix) {
          registeredPrefixId = UNREGISTERED;
          logger.log(Level.SEVERE, "Failed to register prefix: " + prefix.toUri());
        }
      }, new ForwardingFlags());
      logger.log(Level.FINER, "Registered a new prefix: " + prefix.toUri());
    } catch (net.named_data.jndn.security.SecurityException e) {
      throw new IOException("Failed to communicate to face due to security error", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addPostProcessingStage(ProcessingStage<Data, Data> pipelineStage) {
    pipeline.add(pipelineStage);
  }

  /**
   * Process the {@link Data} before sending it; this runs the packet through
   * each registered {@link ProcessingStage} in order.
   *
   * @param data the {@link Data} to process
   * @return a processed {@link Data} packet; no guarantee as to whether it is
   * the same instance as passed in as a parameter (and likely not).
   * @throws ProcessingStageException if a pipeline stage fails
   */
  public Data processPipeline(Data data) throws ProcessingStageException {
    for (ProcessingStage<Data, Data> stage : pipeline) {
      data = stage.process(data);
    }
    return data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    if (!isRegistered()) {
      try {
        register();
      } catch (IOException ex) {
        throw new RuntimeException("Failed to register prefix, aborting.", ex);
      }
    }

    // continuously serve packets
    while (true) {
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Failed to process events.", ex);
      } catch (EncodingException ex) {
        logger.log(Level.SEVERE, "Failed to parse bytes.", ex);
      }
    }
  }
}
