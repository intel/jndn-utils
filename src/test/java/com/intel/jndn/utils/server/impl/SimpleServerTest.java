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

import com.intel.jndn.mock.MeasurableFace;
import com.intel.jndn.mock.MockForwarder;
import com.intel.jndn.utils.server.RespondWithBlob;
import com.intel.jndn.utils.server.RespondWithData;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test {@link SimpleServer}
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SimpleServerTest {

  private Face face;
  private SimpleServer instance;

  @Before
  public void before() throws Exception {
    MockForwarder forwarder = new MockForwarder();
    face = forwarder.connect();
    instance = new SimpleServer(face, new Name("/test/prefix"));
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
  public void testRespond() throws IOException, EncodingException {
    instance.respondUsing(new RespondWithData() {
      @Override
      public Data onInterest(Name prefix, Interest interest) throws Exception {
        Data data = new Data(interest.getName());
        data.setContent(new Blob("..."));
        return data;
      }
    });

    sendAndCheckOneInterest(new Name("/test/prefix/response"));
  }

  @Test
  public void testRespondFromBlob() throws IOException, EncodingException {
    instance.respondUsing(new RespondWithBlob() {
      @Override
      public Blob onInterest(Name prefix, Interest interest) throws Exception {
        return new Blob("...");
      }
    });

    sendAndCheckOneInterest(new Name("/test/prefix/response"));
  }

  private void sendAndCheckOneInterest(Name interestName) throws EncodingException, IOException {
    Interest interest = new Interest(interestName);

    face.expressInterest(interest, (interest1, data) -> assertEquals(interestName, data.getName()));
    face.processEvents();

    assertEquals(1, ((MeasurableFace) face).sentDatas().size());
    assertEquals("...", ((MeasurableFace) face).sentDatas().iterator().next().getContent().toString());
  }
}
