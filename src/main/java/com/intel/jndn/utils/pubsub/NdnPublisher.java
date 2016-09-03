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

import com.intel.jndn.utils.Publisher;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class NdnPublisher implements Publisher, OnInterestCallback {
  private static final Logger LOGGER = Logger.getLogger(NdnPublisher.class.getName());
  private final Face face; // TODO only needed in start, remove?
  private final Name prefix; // TODO only needed in start, remove?
  private final AnnouncementService announcementService;
  private final PendingInterestTable pendingInterestTable;
  private final ContentStore<Blob> contentStore;
  private final long publisherId;
  private volatile long latestMessageId = 0;
  private long registrationId;
  // TODO need pit

  NdnPublisher(Face face, Name prefix, long publisherId, AnnouncementService announcementService, PendingInterestTable pendingInterestTable, ContentStore<Blob> contentStore) {
    this.face = face;
    this.prefix = prefix;
    this.publisherId = publisherId;
    this.announcementService = announcementService;
    this.pendingInterestTable = pendingInterestTable;
    this.contentStore = contentStore;
  }

  public void start() throws RegistrationFailureException {
    CompletableFuture<Void> future = new CompletableFuture<>();
    OnRegistration onRegistration = new OnRegistration(future);

    try {
      registrationId = face.registerPrefix(prefix, this, (OnRegisterFailed) onRegistration, onRegistration);
      future.get(10, TimeUnit.SECONDS);
      announcementService.announceEntrance(publisherId);
    } catch (IOException | SecurityException | InterruptedException | ExecutionException | TimeoutException e) {
      throw new RegistrationFailureException(e);
    }
  }

  public void stop() throws IOException {
    face.removeRegisteredPrefix(registrationId);
    contentStore.clear();
    announcementService.announceExit(publisherId);
  }

  // TODO should throw IOException?
  @Override
  public void publish(Blob message) {
    long id = latestMessageId++; // TODO synchronize?
    Name name = PubSubNamespace.toMessageName(prefix, publisherId, id);

    contentStore.put(name, message);
    LOGGER.log(Level.INFO, "Published message {0} to content store", id);

    if(pendingInterestTable.has(new Interest(name))){
      try {
        contentStore.push(face, name);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to send message {0} for pending interests: {1}", new Object[]{id, name});
      }
    }
  }

  @Override
  public void onInterest(Name name, Interest interest, Face face, long registrationId, InterestFilter interestFilter) {
    try {
      if(contentStore.has(interest.getName())){
        contentStore.push(face, interest.getName());
      } else {
        pendingInterestTable.add(interest);
      }

      long id = interest.getName().get(-1).toNumberWithMarker(43);
      Blob blob = contentStore.get(interest.getName());
      Data data = new Data(interest.getName());
      data.setContent(blob);
      face.putData(data);
      LOGGER.info("Published message " + id + " for interest: " + interest.toUri());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to publish message for interest: " + interest.toUri(), e);
    } catch (EncodingException e) {
      LOGGER.log(Level.SEVERE, "Failed to decode message ID for interest: " + interest.toUri(), e);
    }
  }

}
