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
package com.intel.jndn.utils;

import com.intel.jndn.mock.MockFace;
import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServerTest {

  MockFace face = new MockFace();
  SegmentedServer instance = new SegmentedServer(face, new Name("/test/prefix"));

  @Test
  public void testGetPrefix() {
    assertNotNull(instance.getPrefix());
  }

  @Test
  public void testAddPipelineStage() {
    instance.addPipelineStage(null);
  }

  @Test
  public void testProcessPipeline() throws Exception {
    Data in = new Data(new Name("/test"));
    Data out = instance.processPipeline(in);
    assertEquals(out, in);
  }

  @Test
  public void testServe() throws IOException {
    Data in = new Data(new Name("/test/prefix/serve"));
    in.setContent(new Blob("1234"));
    instance.serve(in);
    Data out = SegmentedClient.getDefault().getSync(face, new Name("/test/prefix/serve"));
    assertEquals(in.getContent(), out.getContent());
    assertEquals(in.getName().toUri(), out.getName().toUri());
    assertEquals("1234", out.getContent().toString());
  }
}
