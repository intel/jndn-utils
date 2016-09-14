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

package com.intel.jndn.utils.client.impl;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnTimeout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BackoffRetryClientTest {

  private BackoffRetryClient instance;

  @Before
  public void before() {
    instance = new BackoffRetryClient(10, 2);
  }

  @Test
  public void retry() throws Exception {
    Face face = mock(Face.class);
    ArgumentCaptor<Interest> interestCaptor = ArgumentCaptor.forClass(Interest.class);
    ArgumentCaptor<OnTimeout> onTimeoutCaptor = ArgumentCaptor.forClass(OnTimeout.class);
    when(face.expressInterest(interestCaptor.capture(), any(), onTimeoutCaptor.capture())).then(new Answer<Long>() {
      @Override
      public Long answer(InvocationOnMock invocation) throws Throwable {
        onTimeoutCaptor.getValue().onTimeout(interestCaptor.getValue());
        return -1L;
      }
    });
    Interest interest = new Interest(new Name("/backoff/test"), 1);
    AtomicInteger timeouts = new AtomicInteger();

    instance.retry(face, interest, null, interest1 -> timeouts.incrementAndGet());

    assertEquals(1, timeouts.get());
  }
}