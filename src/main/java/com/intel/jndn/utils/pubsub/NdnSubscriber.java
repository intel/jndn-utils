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

import com.intel.jndn.utils.Client;
import com.intel.jndn.utils.Subscriber;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnSubscriber implements Subscriber {
  private static final Logger LOGGER = Logger.getLogger(NdnSubscriber.class.getName());
  private final Face face;
  private final Name prefix;
  private final AnnouncementService announcementService;
  private final Client client;
  private final Set<Context> known = new HashSet<>();
  private Cancellation newAnnouncementCancellation;
  private Cancellation existingAnnouncementsCancellation;
  private volatile boolean started = false;

  NdnSubscriber(Face face, Name prefix, AnnouncementService announcementService, Client client) {
    this.face = face;
    this.prefix = prefix;
    this.announcementService = announcementService;
    this.client = client;
  }

  private void start() throws RegistrationFailureException {
    LOGGER.log(Level.INFO, "Starting subscriber");

    existingAnnouncementsCancellation = announcementService.discoverExistingAnnouncements(this::add, null, e -> stop());
    newAnnouncementCancellation = announcementService.observeNewAnnouncements(this::add, this::remove, e -> stop());

    started = true;
  }

  private void add(long publisherId) {
    known.add(new Context(publisherId));
  }

  private void remove(long publisherId) {
    known.remove(publisherId); // TODO incorrect
  }

  private void stop() {
    LOGGER.log(Level.INFO, "Stopping subscriber, knows of {0} publishers: {1} ", new Object[]{known.size(), known});

    if (newAnnouncementCancellation != null) {
      newAnnouncementCancellation.cancel();
    }

    if (existingAnnouncementsCancellation != null) {
      existingAnnouncementsCancellation.cancel();
    }

    for (Context c : known) {
      c.cancel();
    }

    started = false;
  }

  Set<Long> knownPublishers() {
    return known.stream().map(c -> c.publisherId).collect(Collectors.toSet());
  }

  // TODO repeated calls?
  @Override
  public Cancellation subscribe(On<Blob> onMessage, On<Exception> onError) {
    if (!started) {
      try {
        start();
      } catch (RegistrationFailureException e) {
        LOGGER.log(Level.SEVERE, "Failed to start announcement service, aborting subscription", e);
        onError.on(e);
        return Cancellation.CANCELLED;
      }
    }

    for (Context c : known) {
      c.subscribe(onMessage, onError);
    }

    return this::stop;
  }

  private class Context implements Cancellation {
    final long publisherId;
    long messageId;
    boolean subscribed = false;
    On<Blob> onMessage;
    On<Exception> onError;
    CompletableFuture<Data> currentRequest;

    Context(long publisherId) {
      this.publisherId = publisherId;
    }

    synchronized void subscribe(On<Blob> onMessage, On<Exception> onError) {
      this.onMessage = onMessage;
      this.onError = onError;

      if (subscribed) {
        return;
      }

      currentRequest = client.getAsync(face, buildLatestInterest(publisherId)); // TODO backoff
      currentRequest.handle(this::handleResponse);
      subscribed = true;
    }

    @Override
    public synchronized void cancel() {
      if (currentRequest != null) {
        currentRequest.cancel(true);
      }
    }

    private Void handleResponse(Data data, Throwable throwable) {
      if (throwable != null) {
        onError.on((Exception) throwable); // TODO avoid cast?
      } else {
        try {
          Response response = PubSubNamespace.toResponse(data);
          this.messageId = response.messageId();
          onMessage.on(response.content()); // TODO buffer and catch exceptions
        } catch (EncodingException e) {
          onError.on(e);
        }

        // TODO catchup and loop detection (if above fails next() is called again with same params)
        next(this.publisherId, this.messageId);
      }

      return null;
    }

    private void next(long publisherId, long messageId) {
      CompletableFuture<Data> nextRequest = client.getAsync(face, buildNextInterest(publisherId, messageId));
      nextRequest.handle(this::handleResponse);
    }

    private Interest buildLatestInterest(long publisherId) {
      Name name = PubSubNamespace.toPublisherName(prefix, publisherId);
      Interest interest = new Interest(name); // TODO ms lifetime
      interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
      return interest;
    }

    private Interest buildNextInterest(long publisherId, long messageId) {
      Name name = PubSubNamespace.toMessageName(prefix, publisherId, messageId + 1);
      return new Interest(name); // TODO ms lifetime
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Context context = (Context) o;
      return publisherId == context.publisherId;
    }

    @Override
    public int hashCode() {
      return (int) (publisherId ^ (publisherId >>> 32));
    }

    @Override
    public String toString() {
      return "Context{" + "publisherId=" + publisherId + '}';
    }
  }
}