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

import static com.intel.jndn.utils.pubsub.Cancellation.CANCELLED;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnAnnouncementService implements AnnouncementService {
  private static final Logger LOGGER = Logger.getLogger(NdnAnnouncementService.class.getName());
  private final Face face;
  private final Name broadcastPrefix;
  private final Name topicPrefix;
  private final Name usablePrefix;
  private final Set<Long> known = new HashSet<>();

  private NdnAnnouncementService(Face face, Name broadcastPrefix, Name topicPrefix) {
    this.face = face;
    this.broadcastPrefix = broadcastPrefix;
    this.topicPrefix = topicPrefix;
    this.usablePrefix = new Name(broadcastPrefix).append(topicPrefix);
  }

  NdnAnnouncementService(Face face, Name topicPrefix) {
    this(face, PubSubNamespace.DEFAULT_BROADCAST_PREFIX, topicPrefix);
  }

  @Override
  public void announceEntrance(long id) throws IOException {
    Name.Component publisherId = Name.Component.fromNumberWithMarker(id, PubSubNamespace.PUBLISHER_ID_MARKER);
    Name.Component announcementAction = Name.Component.fromNumberWithMarker(PubSubNamespace.ANNOUNCEMENT_ENTRANCE, PubSubNamespace.ANNOUNCEMENT_ACTION_MARKER);
    Interest interest = new Interest(new Name(broadcastPrefix).append(topicPrefix).append(publisherId).append(announcementAction));
    face.expressInterest(interest, null);
  }

  @Override
  public void announceExit(long id) throws IOException {
    Name.Component publisherId = Name.Component.fromNumberWithMarker(id, PubSubNamespace.PUBLISHER_ID_MARKER);
    Name.Component announcementAction = Name.Component.fromNumberWithMarker(PubSubNamespace.ANNOUNCEMENT_EXIT, PubSubNamespace.ANNOUNCEMENT_ACTION_MARKER);
    Interest interest = new Interest(new Name(broadcastPrefix).append(topicPrefix).append(publisherId).append(announcementAction));
    face.expressInterest(interest, null);
  }

  @Override
  public Cancellation discoverExistingAnnouncements(On<Long> onFound, On<Void> onComplete, On<Exception> onError) {
    if (onFound == null) {
      return CANCELLED;
    }

    for (long id : known) {
      LOGGER.info("Passing known publisher: " + id);
      onFound.on(id);
    }

    // TODO while !backoff interval maxed out, send out interests with excludes
    Interest interest = new Interest();
    try {
      long pendingInterest = face.expressInterest(interest, (i, d) -> {
      }, (i) -> {
      });
      return () -> face.removePendingInterest(pendingInterest);
    } catch (IOException e) {
      onError.on(e);
      return CANCELLED;
    }
  }

  private void found(long publisherId) {
    known.add(publisherId);
  }

  @Override
  public Cancellation observeNewAnnouncements(On<Long> onAdded, On<Long> onRemoved, On<Exception> onError) throws RegistrationFailureException {
    CompletableFuture<Void> future = new CompletableFuture<>();
    OnRegistration onRegistration = new OnRegistration(future);
    OnAnnouncement onAnnouncement = new OnAnnouncement(onAdded, onRemoved, onError);

    try {
      long registeredPrefix = face.registerPrefix(usablePrefix, onAnnouncement, (OnRegisterFailed) onRegistration, onRegistration);
      return () -> face.removeRegisteredPrefix(registeredPrefix);
    } catch (IOException | SecurityException e) {
      throw new RegistrationFailureException(e);
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
      try {
        long publisherId = interest.getName().get(-2).toNumberWithMarker(PubSubNamespace.PUBLISHER_ID_MARKER);
        int announcement = (int) interest.getName().get(-1).toNumberWithMarker(PubSubNamespace.ANNOUNCEMENT_ACTION_MARKER);
        switch (announcement) {
          case PubSubNamespace.ANNOUNCEMENT_ENTRANCE:
            add(publisherId);
            break;
          case PubSubNamespace.ANNOUNCEMENT_EXIT:
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
