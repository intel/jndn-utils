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
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class NdnAnnouncementServiceTest {

  private NdnAnnouncementService instance;
  private Face face;

  @Before
  public void before() {
    face = mock(Face.class);
    instance = new NdnAnnouncementService(face, new Name("/topic/prefix"));
  }

  @Test
  public void announceEntrance() throws Exception {
    instance.announceEntrance(42);

    ArgumentCaptor<Interest> interest = ArgumentCaptor.forClass(Interest.class);
    verify(face, times(1)).expressInterest(interest.capture(), any(OnData.class));
    assertEquals(42, PubSubNamespace.parsePublisher(interest.getValue().getName()));
    assertEquals(PubSubNamespace.Announcement.ENTRANCE, PubSubNamespace.parseAnnouncement(interest.getValue().getName()));
  }

  @Test
  public void announceExit() throws Exception {
    instance.announceExit(42);

    ArgumentCaptor<Interest> interest = ArgumentCaptor.forClass(Interest.class);
    verify(face, times(1)).expressInterest(interest.capture(), any(OnData.class));
    assertEquals(42, PubSubNamespace.parsePublisher(interest.getValue().getName()));
    assertEquals(PubSubNamespace.Announcement.EXIT, PubSubNamespace.parseAnnouncement(interest.getValue().getName()));
  }

  @Test
  public void discoverExistingAnnouncements() throws Exception {

  }

  @Test
  public void observeNewAnnouncements() throws Exception {

  }
}