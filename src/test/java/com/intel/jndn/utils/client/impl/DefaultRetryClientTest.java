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

import com.intel.jndn.mock.MockForwarder;
import com.intel.jndn.utils.TestHelper.TestCounter;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test DefaultRetryClient
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DefaultRetryClientTest {

  private static final double INTEREST_LIFETIME_MS = 1.0;
  private DefaultRetryClient client;
  private final Name name = new Name("/test/retry/client");
  private final Interest interest = new Interest(name, INTEREST_LIFETIME_MS);
  private final TestCounter counter = new TestCounter();

  @Before
  public void before() throws Exception {
    client = new DefaultRetryClient(3);
  }

  @Test
  public void testRetry() throws Exception {
    MockForwarder forwarder = new MockForwarder();
    Face face = forwarder.connect();
    client.retry(face, interest, (interest1, data) -> counter.count++, interest2 -> fail("Should not timeout."));
    assertEquals(1, client.totalRetries());

    timeoutAndVerifyRetry(face, 2);
    timeoutAndVerifyRetry(face, 3);
    respondToRetryAttempt(face);
  }

  private void timeoutAndVerifyRetry(Face face, int retryCount) throws Exception {
    Thread.sleep((long) INTEREST_LIFETIME_MS + 1); // necessary to timeout the pending interest
    face.processEvents();
    assertEquals(retryCount, client.totalRetries());
    assertEquals(0, counter.count);
  }

  private void respondToRetryAttempt(Face face) throws Exception {
    face.putData(new Data(name));
    face.processEvents();
    assertEquals(1, counter.count);
  }
}
