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
import com.intel.jndn.utils.On;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class NdnSubscriberTest {

  private static final Name TOPIC_NAME = new Name("/subscriber");
  private NdnSubscriber instance;
  private AnnouncementService announcementService;

  @Before
  public void before() {
    Face face = mock(Face.class);
    announcementService = mock(AnnouncementService.class);
    Client client = mock(Client.class);
    when(client.getAsync(any(Face.class), any(Interest.class))).thenReturn(new CompletableFuture<>());
    instance = new NdnSubscriber(face, TOPIC_NAME, null, null, announcementService, client);
  }

  @Test
  public void basicUsage() throws Exception {
    instance.open();

    // subscriber is driven by IO from announcements and publisher messages

    instance.close();
  }

  @Test
  public void knownPublishersUsingCallbacks() throws Exception {
    ArgumentCaptor<On<Long>> existingPublishersSignal = ArgumentCaptor.forClass((Class) On.class);
    ArgumentCaptor<On<Long>> newPublishersSignal = ArgumentCaptor.forClass((Class) On.class);
    when(announcementService.discoverExistingAnnouncements(existingPublishersSignal.capture(), any(), any())).thenReturn(null);
    when(announcementService.observeNewAnnouncements(newPublishersSignal.capture(), any(), any())).thenReturn(null);
    instance.open();

    assertEquals(0, instance.knownPublishers().size());

    existingPublishersSignal.getValue().on(42L);
    assertEquals(1, instance.knownPublishers().size());

    newPublishersSignal.getValue().on(42L);
    assertEquals(1, instance.knownPublishers().size());

    existingPublishersSignal.getValue().on(99L);
    assertEquals(2, instance.knownPublishers().size());
  }

  @Test
  public void knownPublishersUsingMethods() throws Exception {
    instance.addPublisher(99);

    assertEquals(1, instance.knownPublishers().size());
    assertTrue(instance.knownPublishers().contains(99L));

    instance.removePublisher(99);

    assertEquals(0, instance.knownPublishers().size());
  }
}