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
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.encoding.EncodingException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SegmentedDataStreamTest {

  private final SegmentedDataStream instance = new SegmentedDataStream();

  @Test
  public void testAddingSequentialData() throws StreamException {
    Name name = new Name("/test/segmented/data/stream");
    Interest interest = new Interest(name);

    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(0), "."));
    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(1), "."));
    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(2), "."));

    assertEquals(3, instance.list().length);
    assertEquals("...", instance.assemble().getContent().toString());
  }

  @Test
  public void testAddingUnorderedData() throws StreamException {
    Name name = new Name("/test/segmented/data/stream");
    Interest interest = new Interest(name);
    ArrayList<Long> segments = new ArrayList<>();

    instance.observe((i, d) -> {
      try {
        segments.add(d.getName().get(-1).toSegment());
      } catch (EncodingException ex) {
        throw new RuntimeException(ex);
      }
    });

    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(1), "."));
    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(0), "."));
    instance.onData(interest, TestHelper.buildData(new Name(name).appendSegment(2), "."));

    assertEquals(3, instance.list().length);
    assertEquals("...", instance.assemble().getContent().toString());
    assertArrayEquals(new Long[]{(long) 0, (long) 1, (long) 2}, segments.toArray(new Long[]{}));
  }

  @Test
  public void testAddingOneData() throws StreamException {
    Name name = new Name("/test/segmented/data/stream");
    Interest interest = new Interest(name);

    instance.onData(interest, TestHelper.buildData(new Name(name), "."));

    assertEquals(1, instance.list().length);
    assertEquals(".", instance.assemble().getContent().toString());
    assertEquals(0, instance.current());
    assertEquals(0, instance.end());
  }
  
  @Test
  public void testFinalBlockId(){
    Name name = new Name("/test/final/block/id");
    Interest interest = new Interest(name);
    Data data = TestHelper.buildData(name, ".");
    data.getName().appendSegment(0);
    data.getMetaInfo().setFinalBlockId(Component.fromNumberWithMarker(10, 0x00));
    
    instance.onData(interest, data);
    
    assertEquals(10, instance.end());
    assertEquals(0, instance.current());
  }
  
  @Test
  public void testOrderedPackets() {
    int end = 10;
    IntStream.range(0, end).forEach((i) -> {
      addPacketToInstance(i);
      assertEquals(i, instance.current());
    });

    assertEquals(end, instance.list().length);
  }

  @Test
  public void testUnorderedPackets() {
    addPacketToInstance(1);
    assertEquals(-1, instance.current());

    addPacketToInstance(0);
    assertEquals(1, instance.current());

    addPacketToInstance(3);
    assertEquals(1, instance.current());

    addPacketToInstance(5);
    assertEquals(1, instance.current());

    addPacketToInstance(4);
    assertEquals(1, instance.current());

    addPacketToInstance(2);
    assertEquals(5, instance.current());

    addPacketToInstance(99);
    assertEquals(7, instance.list().length);
  }

  private void addPacketToInstance(long i) {
    Name name = new Name().appendSegment(i);
    instance.onData(new Interest(name), new Data(name));
  }
}
