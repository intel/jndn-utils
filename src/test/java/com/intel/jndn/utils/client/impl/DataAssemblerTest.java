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
import net.named_data.jndn.Name;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * Test that segment data packets are re-assembled correctly.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DataAssemblerTest {

  @Test
  public void testReassembleMany() throws InterruptedException, ExecutionException {
    Name name = new Name("/data/assembly");
    Data[] packets = TestHelper.buildSegments(name, 0, 10).toArray(new Data[]{});
    byte marker = 0x00;
    DataAssembler instance = new DataAssembler(packets, marker);

    Data reassembled = instance.assemble();
    assertEquals(name.toUri(), reassembled.getName().toUri()); // tests name-shortening logic
    assertEquals("0123456789", reassembled.getContent().toString());
  }

  @Test
  public void testReassembleOne() throws InterruptedException, ExecutionException {
    Name name = new Name("/data/assembly");
    Data onlyPacket = TestHelper.buildData(name, "...");
    Data[] packets = new Data[]{onlyPacket};
    byte marker = 0x00;
    DataAssembler instance = new DataAssembler(packets, marker);

    Data reassembled = instance.assemble();
    assertEquals(name.toUri(), reassembled.getName().toUri()); // tests name-shortening logic
    assertEquals("...", reassembled.getContent().toString());
  }

  @Test
  public void testReassembleNoContent() throws InterruptedException, ExecutionException {
    Name name = new Name("/data/assembly");
    Data onlyPacket = TestHelper.buildData(name, "");
    Data[] packets = new Data[]{onlyPacket};
    byte marker = 0x00;
    DataAssembler instance = new DataAssembler(packets, marker);

    Data reassembled = instance.assemble();
    assertEquals(name.toUri(), reassembled.getName().toUri()); // tests name-shortening logic
    assertEquals("", reassembled.getContent().toString());
  }

  @Test(expected = IllegalStateException.class)
  public void testReassembleNone() throws InterruptedException, ExecutionException {
    Data[] packets = new Data[]{};
    byte marker = 0x00;
    DataAssembler instance = new DataAssembler(packets, marker);

    instance.assemble();
  }
}
