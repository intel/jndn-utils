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

import com.intel.jndn.utils.TestHelper;
import com.intel.jndn.utils.client.OnException;
import net.named_data.jndn.Name;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DefaultStreamingClientTest {

  private final DefaultStreamingClient instance = new DefaultStreamingClient();

  @Test
  public void testGetAsyncStream() throws Exception {
    SegmentedDataStream in = new SegmentedDataStream();
    InputStream out = instance.getStreamAsync(in, new OnException() {
      @Override
      public void onException(Exception exception) {
        fail("Streaming failed: " + exception);
      }
    });

    Name name = new Name("/test/streaming/client");
    for (int i = 0; i < 10; i++) {
      in.onData(null, TestHelper.buildData(new Name(name).appendSegment(i), "."));
      int c = out.read();
      assertEquals((int) '.', c);
    }
  }
}
