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

import com.intel.jndn.utils.Cancellation;
import com.intel.jndn.utils.On;
import com.intel.jndn.utils.client.impl.BackoffRetryClient;
import net.named_data.jndn.Exclude;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.SecurityException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.intel.jndn.utils.Cancellation.CANCELLED;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnAnnouncementService implements AnnouncementService {
  private static final Logger LOGGER = Logger.getLogger(NdnAnnouncementService.class.getName());
  private static final double STARTING_DISCOVERY_LIFETIME = 100.0;
  private static final double MAX_DISCOVERY_LIFETIME = 45000.0;
  private final Face face;
  private final Name topicPrefix;
  private final Name broadcastPrefix;
  private final Set<Long> known = new HashSet<>();
  private final BackoffRetryClient client;
  private boolean stopped = false;

  private NdnAnnouncementService(Face face, Name broadcastPrefix, Name topicPrefix) {
    this.face = face;
    this.topicPrefix = topicPrefix;
    this.broadcastPrefix = new Name(broadcastPrefix).append(topicPrefix);
    this.client = new BackoffRetryClient(MAX_DISCOVERY_LIFETIME, 2);
  }

  NdnAnnouncementService(Face face, Name topicPrefix) {
    this(face, PubSubNamespace.DEFAULT_BROADCAST_PREFIX, topicPrefix);
  }

  @Override
  public void announceEntrance(long id) throws IOException {
    LOGGER.log(Level.INFO, "Announcing publisher entrance: {0} to {1}", new Object[]{id, broadcastPrefix});
    Name name = PubSubNamespace.toAnnouncement(broadcastPrefix, id, PubSubNamespace.Announcement.ENTRANCE);
    Interest interest = new Interest(name);
    face.expressInterest(interest, null);
  }

  @Override
  public void announceExit(long id) throws IOException {
    LOGGER.log(Level.INFO, "Announcing publisher exit: {0} from {1}", new Object[]{id, broadcastPrefix});
    Name name = PubSubNamespace.toAnnouncement(broadcastPrefix, id, PubSubNamespace.Announcement.EXIT);
    Interest interest = new Interest(name);
    face.expressInterest(interest, null);
  }

  @Override
  public Cancellation discoverExistingAnnouncements(On<Long> onFound, On<Void> onComplete, On<Exception> onError) throws IOException {
    LOGGER.log(Level.INFO, "Discover existing publishers: {0}", topicPrefix);
    if (onFound == null) {
      return CANCELLED;
    }

    for (long id : known) {
      LOGGER.info("Passing known publisher: " + id);
      onFound.on(id);
    }

    discover(client, onFound, onComplete, onError);
    return () -> stopped = true;
  }

  // TODO need special namespace for discovery
  private Interest discover(BackoffRetryClient client, On<Long> onFound, On<Void> onComplete, On<Exception> onError) throws IOException {
    Interest interest = new Interest(topicPrefix);
    interest.setInterestLifetimeMilliseconds(STARTING_DISCOVERY_LIFETIME);
    interest.setExclude(excludeKnownPublishers());
    client.retry(face, interest, (interest1, data) -> {
      if (stopped) {
        return;
      }

      LOGGER.log(Level.INFO, "Received discovery data ({0} bytes): {1}", new Object[]{data.getContent().size(), data.getName()});
      found(data.getName(), onFound, onError);

      // TODO instead of inspecting name should look at content and examine for multiple publishers
      try {
        discover(client, onFound, onComplete, onError); // recursion
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed while discovering publishers, aborting: {0}", new Object[]{broadcastPrefix, e});
        onError.on(e);
        // TODO re-call discover here?
      }
    }, interest2 -> {
      // TODO recall client here, we should never be done
    });
    return interest;
  }

  private Exclude excludeKnownPublishers() {
    Exclude exclude = new Exclude();
    for (long pid : known) {
      exclude.appendComponent(PubSubNamespace.toPublisherComponent(pid));
    }
    return exclude;
  }

  private void found(Name publisherName, On<Long> onFound, On<Exception> onError) {
    try {
      found(PubSubNamespace.parsePublisher(publisherName), onFound);
    } catch (EncodingException e) {
      LOGGER.log(Level.SEVERE, "Failed to parse new publisher name, ignoring: {0}", publisherName);
      onError.on(e);
    }
  }

  private void found(long publisherId, On<Long> onFound) {
    LOGGER.log(Level.INFO, "Found new publisher: {0}", publisherId);
    known.add(publisherId);
    onFound.on(publisherId);
  }

  @Override
  public Cancellation observeNewAnnouncements(On<Long> onAdded, On<Long> onRemoved, On<Exception> onError) throws IOException {
    LOGGER.log(Level.INFO, "Observing new announcements: {0}", broadcastPrefix);
    CompletableFuture<Void> future = new CompletableFuture<>();
    OnRegistration onRegistration = new OnRegistration(future);
    OnAnnouncement onAnnouncement = new OnAnnouncement(onAdded, onRemoved, onError);

    try {
      long registeredPrefix = face.registerPrefix(broadcastPrefix, onAnnouncement, (OnRegisterFailed) onRegistration, onRegistration);
      return () -> face.removeRegisteredPrefix(registeredPrefix);
    } catch (SecurityException e) {
      throw new IOException("Failed while using transport security key chain", e);
    }
  }

  private class OnAnnouncement implements OnInterestCallback {
    private final On<Long> onAdded;
    private final On<Long> onRemoved;
    private final On<Exception> onError;

    OnAnnouncement(On<Long> onAdded, On<Long> onRemoved, On<Exception> onError) {
      this.onAdded = onAdded;
      this.onRemoved = onRemoved;
      this.onError = onError;
    }

    @Override
    public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {
      LOGGER.log(Level.INFO, "Received announcement: {0}", interest.toUri());
      try {
        long publisherId = PubSubNamespace.parsePublisher(interest.getName());
        PubSubNamespace.Announcement announcement = PubSubNamespace.parseAnnouncement(interest.getName());
        switch (announcement) {
          case ENTRANCE:
            add(publisherId);
            break;
          case EXIT:
            remove(publisherId);
            break;
          default:
            LOGGER.warning("Unknown announcement action, ignoring: " + interest.toUri());
        }
      } catch (EncodingException e) {
        LOGGER.log(Level.SEVERE, "Failed to decode announcement: " + interest.toUri(), e);
        onError.on(e);
      }
    }

    private void remove(long publisherId) {
      LOGGER.info("Publisher leaving topic: " + publisherId);
      known.remove(publisherId);
      if (onRemoved != null) {
        onRemoved.on(publisherId);
      }
    }

    private void add(long publisherId) {
      LOGGER.info("Publisher entering topic: " + publisherId);
      known.add(publisherId);
      if (onAdded != null) {
        onAdded.on(publisherId);
      }
    }
  }
}
