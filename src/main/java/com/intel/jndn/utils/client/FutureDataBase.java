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
package com.intel.jndn.utils.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;

/**
 * Implement common functionality for re-use in NDN futures
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public abstract class FutureDataBase implements Future<Data> {

  private final Face face;
  private final Name name;
  private boolean cancelled = false;
  private Throwable error;

  /**
   * Constructor
   *
   * @param face the {@link Face} to use for processing events
   * @param name the {@link Name} of the interest sent
   */
  public FutureDataBase(Face face, Name name) {
    this.face = face;
    this.name = new Name(name);
  }

  /**
   * Block until packet is retrieved; will call face.processEvents() until the
   * future is resolved or rejected.
   *
   * @return the {@link Data} when the packet is retrieved
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Override
  public final Data get() throws InterruptedException, ExecutionException {
    while (!isDone()) {
      // process face events
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (EncodingException | IOException e) {
        throw new ExecutionException("Failed to retrieve packet while processing events: " + name.toUri(), e);
      }
    }

    // case: cancelled
    if (isCancelled()) {
      throw new InterruptedException("Interrupted by user while retrieving packet: " + name.toUri());
    }

    // case: error
    if (isRejected()) {
      throw new ExecutionException("Error while retrieving packet: " + name.toUri(), error);
    }

    return getData();
  }

  /**
   * Block until packet is retrieved or timeout is reached.
   *
   * @param timeout
   * @param unit
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   */
  @Override
  public final Data get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long interval = TimeUnit.MILLISECONDS.convert(timeout, unit);
    long endTime = System.currentTimeMillis() + interval;
    long currentTime = System.currentTimeMillis();
    while (!isDone() && currentTime <= endTime) {
      // process face events
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (EncodingException | IOException e) {
        throw new ExecutionException("Failed to retrieve packet while processing events: " + name.toUri(), e);
      }

      currentTime = System.currentTimeMillis();
    }

    // case: cancelled
    if (isCancelled()) {
      throw new InterruptedException("Interrupted by user while retrieving packet: " + name.toUri());
    }

    // case: error
    if (isRejected()) {
      throw new ExecutionException("Error while retrieving packet: " + name.toUri(), error);
    }

    // case: timed out
    if (currentTime > endTime) {
      throw new TimeoutException("Timed out while retrieving packet: " + name.toUri());
    }

    return getData();
  }

  /**
   * @return true if the request has completed (successfully or not)
   */
  @Override
  public boolean isDone() {
    return isRejected() || isCancelled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean cancel(boolean mayInterruptIfRunning) {
    cancelled = true;
    return cancelled;
  }

  /**
   * @return true if this request is cancelled
   */
  @Override
  public final boolean isCancelled() {
    return cancelled;
  }

  /**
   * @return true if the future has been rejected with an error
   */
  public final boolean isRejected() {
    return error != null;
  }

  /**
   * Set the exception when request failed; unblocks {@link #get()}. Use this
   * method inside an {@link OnTimeout} callback to resolve this future.
   *
   * @param failureCause the error that causes this future to fail
   */
  public final void reject(Throwable failureCause) {
    error = failureCause;
  }

  /**
   * @return the {@link Data} retrieved by this future
   */
  public abstract Data getData() throws InterruptedException, ExecutionException;

  /**
   * @return the {@link Name} used for this future; this is currently only used
   * for debugging purposes, not retrieval
   */
  public Name getName() {
    return name;
  }
}
