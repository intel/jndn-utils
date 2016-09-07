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

import com.intel.jndn.utils.ContentStore;
import com.intel.jndn.utils.Publisher;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnPublisher implements Publisher, OnInterestCallback {
  private static final Logger LOGGER = Logger.getLogger(NdnPublisher.class.getName());
  private final Face face;
  private final Name prefix;
  private final AnnouncementService announcementService;
  private final PendingInterestTable pendingInterestTable;
  private final ContentStore contentStore;
  private final long publisherId;
  private final AtomicLong latestMessageId = new AtomicLong(0);
  private long registrationId;
  private boolean opened = false;

  NdnPublisher(Face face, Name prefix, long publisherId, AnnouncementService announcementService, PendingInterestTable pendingInterestTable, ContentStore contentStore) {
    this.face = face;
    this.prefix = prefix;
    this.publisherId = publisherId;
    this.announcementService = announcementService;
    this.pendingInterestTable = pendingInterestTable;
    this.contentStore = contentStore;
  }

  synchronized void open() throws RegistrationFailureException {
    opened = true;
    CompletableFuture<Void> future = new CompletableFuture<>();
    OnRegistration onRegistration = new OnRegistration(future);

    try {
      registrationId = face.registerPrefix(prefix, this, (OnRegisterFailed) onRegistration, onRegistration);
      // assumes face.processEvents is driven concurrently elsewhere
      future.get(10, TimeUnit.SECONDS);
      announcementService.announceEntrance(publisherId);
    } catch (IOException | SecurityException | InterruptedException | ExecutionException | TimeoutException e) {
      throw new RegistrationFailureException(e);
    }
  }

  // TODO this should not clear content store or remove registered prefix; do that in the future to allow subscribers
  // to retrieve still-alive messages
  @Override
  public synchronized void close() throws IOException {
    if (opened) {
      face.removeRegisteredPrefix(registrationId);
      contentStore.clear();
      announcementService.announceExit(publisherId);
    }
  }

  @Override
  public void publish(Blob message) throws IOException {
    if (!opened) {
      try {
        open();
      } catch (RegistrationFailureException e) {
        throw new IOException(e);
      }
    }

    long id = latestMessageId.getAndIncrement();
    Name name = PubSubNamespace.toMessageName(prefix, publisherId, id);

    contentStore.put(name, message);
    LOGGER.log(Level.INFO, "Published message {0} to content store: {1}", new Object[]{id, name});

    if (pendingInterestTable.has(new Interest(name))) {
      try {
        contentStore.push(face, name);
        // TODO extract satisfied interests
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to send message {0} for pending interests: {1}", new Object[]{id, name, e});
      }
    }
  }

  @Override
  public void onInterest(Name name, Interest interest, Face face, long registrationId, InterestFilter interestFilter) {
    LOGGER.log(Level.INFO, "Client requesting message: {0}", interest.toUri());
    try {
      if (contentStore.has(interest.getName())) {
        contentStore.push(face, interest.getName());
      } else {
        pendingInterestTable.add(interest);
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to publish message for interest: " + interest.toUri(), e);
    }
  }
}
