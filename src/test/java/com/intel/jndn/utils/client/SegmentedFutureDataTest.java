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
package com.intel.jndn.utils.client;

import com.intel.jndn.mock.MockFace;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedFutureDataTest {

  SegmentedFutureData instance;

  public SegmentedFutureDataTest() {
    Face face = new MockFace();
    instance = new SegmentedFutureData(face, new Name("/test/packet"));
    for (int i = 0; i < 10; i++) {
      Data data = new Data(new Name("/test/packet").appendSegment(i));
      data.setContent(new Blob("."));
      FutureData future = new FutureData(face, data.getName());
      future.resolve(data);
      instance.add(future);
    }
  }

  @Test
  public void testIsDone() {
    assertTrue(instance.isDone());
  }
  
  @Test
  public void testIsDoneWhenCancelled() {
    instance.cancel(false);
    assertTrue(instance.isDone());
  }

  /**
   * Test of get method, of class SegmentedFutureData.
   */
  @Test
  public void testGet_0args() throws Exception {
    assertEquals(10, instance.get().getContent().size());
  }

  /**
   * Test of get method, of class FutureData.
   */
  @Test(expected = TimeoutException.class)
  public void testGet_long_TimeUnit() throws Exception {
    Face face = new MockFace();
    Data data = new Data(new Name("/test/packet").appendSegment(0));
    FutureData future = new FutureData(face, data.getName());
    
    SegmentedFutureData segmentedFuture = new SegmentedFutureData(face, new Name("/test/packet"));
    segmentedFuture.add(future);
    
    segmentedFuture.get(10, TimeUnit.MILLISECONDS);
  }
}
