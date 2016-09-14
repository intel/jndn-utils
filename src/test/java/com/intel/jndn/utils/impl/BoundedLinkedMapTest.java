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

package com.intel.jndn.utils.impl;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test BoundedLinkedMapTest
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedLinkedMapTest {
  private static final Logger LOGGER = Logger.getLogger(BoundedLinkedMapTest.class.getName());
  private BoundedLinkedMap<String, Object> instance;

  @Before
  public void beforeTest() {
    instance = new BoundedLinkedMap<>(2);
  }

  @Test
  public void testUsage() {
    Object object0 = new Object();
    Object object1 = new Object();
    Object object2 = new Object();

    instance.put("0", object0);
    assertEquals(1, instance.size());

    instance.put("1", object1);
    assertEquals(2, instance.size());

    instance.put("2", object2);
    assertEquals(2, instance.size());

    assertNull(instance.get("0"));
    assertEquals("2", instance.latest());
  }

  @Test
  public void testEarliestLatest() {
    assertNull(instance.earliest());
    assertNull(instance.latest());

    instance.put(".", new Object());
    assertEquals(instance.earliest(), instance.latest());

    instance.put("..", new Object());
    assertEquals(".", instance.earliest());
    assertEquals("..", instance.latest());

    instance.put("...", new Object());
    assertEquals("..", instance.earliest());
    assertEquals("...", instance.latest());
  }

  @Test
  public void testIsEmpty() {
    assertTrue(instance.isEmpty());
    instance.put("...", new Object());
    assertFalse(instance.isEmpty());
  }

  @Test
  public void testContainsKey() {
    assertFalse(instance.containsKey("..."));
    instance.put("...", new Object());
    assertTrue(instance.containsKey("..."));
  }

  @Test
  public void testContainsValue() {
    Object o = new Object();
    assertFalse(instance.containsValue(o));
    instance.put("...", o);
    assertTrue(instance.containsValue(o));
  }

  @Test
  public void testRemove() {
    Object o = new Object();
    String key = "...";

    instance.put(key, o);
    assertTrue(instance.containsKey(key));
    assertTrue(instance.containsValue(o));

    instance.remove(key);
    assertFalse(instance.containsKey(key));
    assertFalse(instance.containsValue(o));
    assertEquals(0, instance.size());
  }

  @Test
  public void testPutAll() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("1", new Object());
    map.put("2", new Object());
    map.put("99", new Object());

    instance.putAll(map);
    LOGGER.log(Level.FINE, "Map passed to putAll(): {0}", map.toString());
    LOGGER.log(Level.FINE, "Resulting bounded map after putAll(): {0}", instance.toString());

    assertEquals(2, instance.size()); // note: this is not 3 because the max size is bounded
    assertEquals("2", instance.latest()); // note: put all doesn't do the FIFO replacement
  }

  @Test
  public void testClear() {
    instance.put("...", new Object());

    instance.clear();

    assertEquals(0, instance.size());
    assertNull(instance.get("..."));
    assertNull(instance.latest());
    assertNull(instance.earliest());
  }

  @Test
  public void testConversions() {
    instance.put("...", new Object());

    assertEquals(1, instance.keySet().size());
    assertEquals(1, instance.values().size());
    assertEquals(1, instance.entrySet().size());
  }

  @Test
  public void testPerformanceAgainstArrayList() {
    int numMessages = 10000;
    BoundedLinkedMap<Integer, Object> map = new BoundedLinkedMap<>(numMessages);
    ArrayList<Object> list = new ArrayList<>(numMessages);

    long mapPutTime = measure(numMessages, i -> map.put(i, new Object()));
    long listPutTime = measure(numMessages, i -> list.add(i, new Object()));
    LOGGER.log(Level.FINE, "Custom map put has overhead of {0}% versus list put", toPercent((mapPutTime - listPutTime) / (double) listPutTime));

    long mapGetTime = measure(numMessages, map::get);
    long listGetTime = measure(numMessages, list::get);
    LOGGER.log(Level.FINE, "Custom map get has overhead of {0}% versus list get", toPercent((mapGetTime - listGetTime) / (double) listPutTime));
  }

  private long measure(int numTimes, Consumer<Integer> work) {
    long start = System.nanoTime();
    for (int i = 0; i < numTimes; i++) {
      work.accept(i);
    }
    return System.nanoTime() - start;
  }

  private double toPercent(double number) {
    return Math.round(number * 100);
  }
}