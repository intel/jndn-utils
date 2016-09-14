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

import com.intel.jndn.mock.MeasurableFace;
import com.intel.jndn.mock.MockForwarder;
import com.intel.jndn.utils.impl.InMemoryContentStore;
import com.intel.jndn.utils.impl.BoundedInMemoryPendingInterestTable;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class NdnPublisherTest {
  private static final Logger LOGGER = Logger.getLogger(NdnPublisherTest.class.getName());
  private static final Name PUBLISHER_PREFIX = new Name("/publisher");
  private static final long PUBLISHER_ID = new Random().nextLong();
  private NdnPublisher instance;
  private Face face;
  private MockForwarder forwarder;

  @Before
  public void before() throws Exception {
    forwarder = new MockForwarder();
    face = forwarder.connect();
    NdnAnnouncementService announcementService = new NdnAnnouncementService(face, PUBLISHER_PREFIX);
    BoundedInMemoryPendingInterestTable pendingInterestTable = new BoundedInMemoryPendingInterestTable(1024);
    InMemoryContentStore contentStore = new InMemoryContentStore(2000);
    instance = new NdnPublisher(face, PUBLISHER_PREFIX, PUBLISHER_ID, announcementService, pendingInterestTable, contentStore);

    driveFace(face); // TODO preferably do this in MockForwarder; causes failures if run in certain orders
  }

  @Test
  public void basicUsage() throws Exception {
    instance.open();
    assertEquals(1, numSentInterests()); // doesn't count prefix registration, only announcement

    instance.publish(new Blob("..."));

    instance.close();
    assertEquals(2, numSentInterests());
    assertEquals(0, numReceivedDatas());
  }

  @Test
  public void closeWithoutOpen() throws Exception {
    instance.close();

    assertEquals(0, numSentInterests()); // if the publisher has not been opened, it will not announce an exit
    assertEquals(0, numReceivedDatas());
  }

  @Test
  public void publish() throws Exception {
    instance.open();
    assertEquals(1, numSentInterests());

    instance.publish(new Blob("..."));
    assertEquals(0, numSentDatas());
    assertEquals(1, numSentInterests()); // none more than from opening
  }

  @Test
  public void publishWithPendingInterest() throws Exception {
    instance.open();
    assertEquals(1, numSentInterests());

    CountDownLatch latch = new CountDownLatch(1);
    Face client = forwarder.connect();
    client.expressInterest(new Interest(PubSubNamespace.toMessageName(PUBLISHER_PREFIX, PUBLISHER_ID, 0)), (interest, data) -> latch.countDown());

    client.processEvents(); // TODO remove somehow
    face.processEvents();

    instance.publish(new Blob("..."));
    assertEquals(1, numSentDatas());
    assertEquals(1, numSentInterests()); // none more than from opening

    client.processEvents(); // TODO remove somehow
    face.processEvents();

    latch.await(1, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  @Test
  public void publishAndRespondToNewInterest() throws Exception {
    Face client = forwarder.connect();
    instance.open();
    assertEquals(1, numSentInterests());

    instance.publish(new Blob("..."));
    assertEquals(0, numSentDatas());
    assertEquals(1, numSentInterests()); // none more than from opening

    client.processEvents(); // TODO remove somehow
    face.processEvents();

    CountDownLatch latch = new CountDownLatch(1);
    client.expressInterest(new Interest(PubSubNamespace.toMessageName(PUBLISHER_PREFIX, PUBLISHER_ID, 0)), (interest, data) -> latch.countDown());

    client.processEvents(); // TODO remove somehow
    face.processEvents();

    client.processEvents(); // TODO remove somehow
    face.processEvents();

    latch.await(1, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  private int numSentDatas() {
    return ((MeasurableFace) face).sentDatas().size();
  }

  private int numReceivedDatas() {
    return ((MeasurableFace) face).receivedDatas().size();
  }

  private int numSentInterests() {
    return ((MeasurableFace) face).sentInterests().size();
  }

  private void driveFace(Face face) {
    ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
    pool.schedule(() -> {
      try {
        face.processEvents();
      } catch (IOException | EncodingException e) {
        LOGGER.log(Level.SEVERE, "Failed to process face events", e);
        fail(e.getMessage());
      }
    }, 50, TimeUnit.MILLISECONDS);
  }
}