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
package com.intel.jndn.utils.server.impl;

import com.intel.jndn.mock.MockForwarder;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SegmentedServerTest {

  private Face face;
  private SegmentedServer instance;

  @Before
  public void before() throws Exception {
    MockForwarder forwarder = new MockForwarder();
    face = forwarder.connect();
    instance = new SegmentedServer(face, new Name("/test/prefix"));
  }

  @Test
  public void testGetPrefix() {
    assertNotNull(instance.getPrefix());
  }

  @Test
  public void testAddPipelineStage() {
    instance.addPostProcessingStage(null);
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
    Data out = AdvancedClient.getDefault().getSync(face, new Name("/test/prefix/serve"));
    assertEquals(in.getContent(), out.getContent());
    assertEquals(in.getName().toUri(), out.getName().toUri());
    assertEquals("1234", out.getContent().toString());
  }
  
  @Test(expected = IOException.class)
  public void testCleanup() throws Exception{
    Data in = new Data(new Name("/test"));
    in.getMetaInfo().setFreshnessPeriod(0);
    instance.serve(in);
    Thread.sleep(10);
    
    Data out = AdvancedClient.getDefault().getSync(face, new Name("/test"));
    assertNotNull(out);
    assertEquals(in.getName(), out.getName());
    
    instance.cleanup();
    AdvancedClient.getDefault().getSync(face, new Name("/test"));
  }
  
  @Test
  public void testServingNoContent() throws IOException{
    instance.serve(new Data());
  }
  
  @Test
  public void testWhenDataNameIsLongerThanInterestName() throws Exception{
    instance.serve(new Data(new Name("/test/prefix/a/b/c/1")));
    instance.serve(new Data(new Name("/test/prefix/a/b/c/2")));
    
    Interest interest = new Interest(new Name("/test/prefix/a/b"))
            .setChildSelector(Interest.CHILD_SELECTOR_RIGHT).setInterestLifetimeMilliseconds(100);
    Data out = AdvancedClient.getDefault().getSync(face, interest);
    
    assertNotNull(out);
    assertEquals("/test/prefix/a/b/c/1", out.getName().toUri());
    // note that this won't be .../c/2 since .../c/1 satisfies both the Interest
    // name and "c" is the rightmost component (child selectors operate on the
    // next component after the Interest name only)
  }
}
