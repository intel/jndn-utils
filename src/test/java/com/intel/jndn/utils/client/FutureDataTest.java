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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test {@link FutureData}
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class FutureDataTest {
  
  FutureData instance;
  
  public FutureDataTest(){
    instance = new FutureData(new MockFace(), new Name("/test/future"));
  }

  /**
   * Test of getName method, of class FutureData.
   */
  @Test
  public void testGetName() {
    assertNotNull(instance.getName());
  }

  /**
   * Test of cancel method, of class FutureData.
   */
  @Test(expected = InterruptedException.class)
  public void testCancellation() throws InterruptedException, ExecutionException {
    instance.cancel(true);
    assertTrue(instance.isCancelled());
    instance.get();
  }

  /**
   * Test of isDone method, of class FutureData.
   */
  @Test
  public void testIsDone() {
    assertFalse(instance.isDone());
  }

  /**
   * Test of resolve method, of class FutureData.
   */
  @Test
  public void testResolve() {
    instance.resolve(new Data());
    assertTrue(instance.isDone());
  }

  /**
   * Test of reject method, of class FutureData.
   */
  @Test
  public void testReject() {
    instance.reject(new Error());
    assertTrue(instance.isDone());
  }

  /**
   * Test of get method, of class FutureData.
   */
  @Test
  public void testGet_0args() throws Exception {
    instance.resolve(new Data(new Name("/test/packet")));
    Data result = instance.get();
    assertEquals("/test/packet", result.getName().toUri());
  }

  /**
   * Test of get method, of class FutureData.
   */
  @Test(expected = TimeoutException.class)
  public void testGet_long_TimeUnit() throws Exception {
    instance.get(10, TimeUnit.MILLISECONDS);
  }
}
