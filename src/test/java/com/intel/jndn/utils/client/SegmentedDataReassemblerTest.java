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

import com.intel.jndn.utils.TestHelper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test that segment data packets are re-assembled correctly.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedDataReassemblerTest {

  @Test
  public void testReassemble() throws InterruptedException, ExecutionException {
    Name name = new Name("/data/re-assembly");
    List<CompletableFuture<Data>> segments = TestHelper.buildFutureSegments(name, 0, 10);
    SegmentedDataReassembler instance = new SegmentedDataReassembler(name, segments);

    CompletableFuture<Data> future = instance.reassemble();
    assertTrue(future.isDone());
    assertEquals(name.toUri(), future.get().getName().toUri()); // tests name-shortening logic
    assertEquals("0123456789", future.get().getContent().toString());
  }
}
