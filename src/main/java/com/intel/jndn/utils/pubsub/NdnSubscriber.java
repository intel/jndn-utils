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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnSubscriber implements Subscriber {
  private static final Logger LOGGER = Logger.getLogger(NdnSubscriber.class.getName());
  private final Face face;
  private final Name prefix;
  private final On<Blob> onMessage = null;
  private final On<Exception> onError = null;
  private final AnnouncementService announcementService;
  private final Client client;
  private final Map<Long, Subscription> subscriptions = new HashMap<>();
  private Cancellation newAnnouncementCancellation;
  private Cancellation existingAnnouncementsCancellation;
  private volatile boolean started = false;

  NdnSubscriber(Face face, Name prefix, AnnouncementService announcementService, Client client) {
    this.face = face;
    this.prefix = prefix;
    this.announcementService = announcementService;
    this.client = client;
  }

  void open() throws RegistrationFailureException {
    LOGGER.log(Level.INFO, "Starting subscriber: {0}", prefix);

    existingAnnouncementsCancellation = announcementService.discoverExistingAnnouncements(this::add, null, e -> close());
    newAnnouncementCancellation = announcementService.observeNewAnnouncements(this::add, this::remove, e -> close());

    started = true;
  }

  void add(long publisherId) {
    if (subscriptions.containsKey(publisherId)) {
      LOGGER.log(Level.WARNING, "Duplicate publisher ID {} received from announcement service; this should not happen and will be ignored", publisherId);
    } else {
      Subscription subscription = new Subscription(publisherId);
      subscriptions.put(publisherId, subscription);
      subscription.subscribe();
    }
  }

  void remove(long publisherId) {
    Subscription removed = subscriptions.remove(publisherId);
    removed.cancel();
  }

  @Override
  public void close() {
    LOGGER.log(Level.INFO, "Stopping subscriber, knows of {0} publishers: {1} ", new Object[]{subscriptions.size(), subscriptions});

    if (newAnnouncementCancellation != null) {
      newAnnouncementCancellation.cancel();
    }

    if (existingAnnouncementsCancellation != null) {
      existingAnnouncementsCancellation.cancel();
    }

    for (Subscription c : subscriptions.values()) {
      c.cancel();
    }

    started = false;
  }

  Set<Long> knownPublishers() {
    return subscriptions.keySet();
  }

  // TODO repeated calls?
  // TODO remove this, do topic.subscribe(On..., On...) instead; or new Subscriber(On..., On..., ...).open()
  @Override
  public Cancellation subscribe(On<Blob> onMessage, On<Exception> onError) {
    if (!started) {
      try {
        open();
      } catch (RegistrationFailureException e) {
        LOGGER.log(Level.SEVERE, "Failed to start announcement service, aborting subscription", e);
        onError.on(e);
        return Cancellation.CANCELLED;
      }
    }

    LOGGER.log(Level.INFO, "Subscribing: {0}", prefix);
    for (Subscription c : subscriptions.values()) {
      c.subscribe();
    }

    return this::close;
  }

  private class Subscription implements Cancellation {
    final long publisherId;
    long messageId;
    boolean subscribed = false;
    CompletableFuture<Data> currentRequest;

    Subscription(long publisherId) {
      this.publisherId = publisherId;
    }

    synchronized void subscribe() {
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
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Subscription subscription = (Subscription) o;
      return publisherId == subscription.publisherId;
    }

    @Override
    public int hashCode() {
      return (int) (publisherId ^ (publisherId >>> 32));
    }

    @Override
    public String toString() {
      return "Subscription{" + "publisherId=" + publisherId + '}';
    }
  }
}