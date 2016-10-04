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
package com.intel.jndn.utils.server.impl;

import com.intel.jndn.mock.MockFace;
import com.intel.jndn.utils.ProcessingStage;
import com.intel.jndn.utils.ProcessingStageException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Test base server implementation.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class ServerBaseImplTest {

  private Face face;
  private ServerBaseImpl instance;

  @Before
  public void before() throws Exception {
    face = new MockFace();
    instance = new ServerBaseImplImpl(face, new Name("/test/base"));
  }

  /**
   * Test of getPrefix method, of class ServerBaseImpl.
   */
  @Test
  public void testGetPrefix() {
    assertNotNull(instance.getPrefix());
  }

  /**
   * Test of getRegisteredPrefixId method, of class ServerBaseImpl
   */
  public void testGetRegisteredPrefixId() {
    assertEquals(ServerBaseImpl.UNREGISTERED, instance.getRegisteredPrefixId());
  }

  /**
   * Test of register method, of class ServerBaseImpl.
   */
  @Test
  public void testRegister() throws Exception {
    assertFalse(instance.isRegistered());
    instance.register();
    assertTrue(instance.isRegistered());
  }

  /**
   * Test of addProcessingStage method, of class ServerBaseImpl.
   */
  @Test(expected = Exception.class)
  public void testPipeline() throws Exception {
    ProcessingStage<Data, Data> pipelineStage = new ProcessingStage<Data, Data>() {
      @Override
      public Data process(Data context) throws ProcessingStageException {
        throw new ProcessingStageException("Test exceptions with this");
      }
    };
    instance.addPostProcessingStage(pipelineStage);
    instance.processPipeline(new Data());
  }

  /**
   * Test of run method, of class ServerBaseImpl.
   */
  @Test
  public void testRun() throws InterruptedException {
    assertFalse(instance.isRegistered());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(instance);
    Thread.sleep(100);
    assertTrue(instance.isRegistered());
    executor.shutdownNow();
  }

  public class ServerBaseImplImpl extends ServerBaseImpl {

    public ServerBaseImplImpl(Face face, Name prefix) {
      super(face, prefix);
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
}
