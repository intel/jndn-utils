/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
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
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.mock.MockFace;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test SimpleClient.java
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SimpleClientTest {

  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGetSync() throws Exception {
    // setup face
    MockFace face = new MockFace();

    // setup return data
    Data response = new Data(new Name("/test/sync"));
    response.setContent(new Blob("..."));
    face.receive(response);

    // retrieve data
    logger.info("Client expressing interest synchronously: /test/sync");
    SimpleClient client = new SimpleClient();
    Data data = client.getSync(face, new Name("/test/sync"));
    assertEquals(new Blob("...").buf(), data.getContent().buf());
  }

  @Test
  public void testGetAsync() throws Exception {
    // setup face
    MockFace face = new MockFace();

    // setup return data
    Data response = new Data(new Name("/test/async"));
    response.setContent(new Blob("..."));
    face.receive(response);

    // retrieve data
    logger.info("Client expressing interest asynchronously: /test/async");
    SimpleClient client = new SimpleClient();
    Future<Data> futureData = client.getAsync(face, new Name("/test/async"));
    assertTrue(!futureData.isDone());

    // process events to retrieve data
    face.processEvents();
    assertTrue(futureData.isDone());
    assertEquals(new Blob("...").toString(), futureData.get().getContent().toString());
  }

  @Test
  public void testTimeout() throws Exception {
    // setup face
    MockFace face = new MockFace();

    // retrieve non-existent data, should timeout
    logger.info("Client expressing interest asynchronously: /test/timeout");
    Interest interest = new Interest(new Name("/test/timeout"), 1);
    CompletableFuture<Data> futureData = SimpleClient.getDefault().getAsync(face, interest);

    // wait for NDN timeout
    Thread.sleep(2);
    face.processEvents();

    // verify that the client is completing the future with a TimeoutException
    assertTrue(futureData.isDone());
    assertTrue(futureData.isCompletedExceptionally());
    try {
      futureData.get();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof TimeoutException);
    }
  }

  @Test(expected = Exception.class)
  public void testAsyncFailureToRetrieve() throws Exception {
    Face face = new MockFace();

    logger.info("Client expressing interest asynchronously: /test/no-data");
    Interest interest = new Interest(new Name("/test/no-data"), 10);
    Future future = SimpleClient.getDefault().getAsync(face, interest);

    face.processEvents();
    future.get(15, TimeUnit.MILLISECONDS);
  }

  @Test(expected = IOException.class)
  public void testSyncFailureToRetrieve() throws IOException {
    logger.info("Client expressing interest synchronously: /test/no-data");
    Interest interest = new Interest(new Name("/test/no-data"), 10);
    SimpleClient.getDefault().getSync(new Face(), interest);
  }
}
