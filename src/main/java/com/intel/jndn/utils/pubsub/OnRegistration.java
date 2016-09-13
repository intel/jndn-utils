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

package com.intel.jndn.utils.pubsub;

import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Helper for tracking and logging prefix registrations; TODO replace or remove this in the future
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
class OnRegistration implements OnRegisterFailed, OnRegisterSuccess {
  private static final Logger LOGGER = Logger.getLogger(OnRegistration.class.getName());
  private final CompletableFuture<Void> future;

  OnRegistration(CompletableFuture<Void> future) {
    this.future = future;
  }

  @Override
  public void onRegisterSuccess(Name name, long l) {
    LOGGER.info("Registered prefix: " + name);
    future.complete(null);
  }

  @Override
  public void onRegisterFailed(Name name) {
    String message = "Failed to register prefix: " + name;
    LOGGER.severe(message);
    future.completeExceptionally(new RegistrationFailureException(message));
  }
}
