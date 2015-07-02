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
import com.intel.jndn.utils.TestHelper.TestCounter;
import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test DefaultRetryClient
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class DefaultRetryClientTest {

  DefaultRetryClient client = new DefaultRetryClient(3);
  MockFace face = new MockFace();
  Name name = new Name("/test/retry/client");
  Interest interest = new Interest(name, 0.0);
  TestCounter counter = new TestCounter();

  @Test
  public void testRetry() throws Exception {

    client.retry(face, interest, new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        counter.count++;
      }
    }, new OnTimeout() {
      @Override
      public void onTimeout(Interest interest) {
        fail("Should not timeout.");
      }
    });
    assertEquals(1, client.totalRetries());

    timeoutAndVerifyRetry(2);
    timeoutAndVerifyRetry(3);
    respondToRetryAttempt();
  }

  private void timeoutAndVerifyRetry(int retryCount) throws Exception {
    Thread.sleep(1);
    face.processEvents();
    assertEquals(retryCount, client.totalRetries());
    assertEquals(0, counter.count);
  }

  protected void respondToRetryAttempt() throws IOException, EncodingException {
    face.getTransport().respondWith(new Data(name));
    face.processEvents();
    assertEquals(1, counter.count);
  }
}
