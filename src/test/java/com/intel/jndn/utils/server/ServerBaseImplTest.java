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
package com.intel.jndn.utils.server;

import com.intel.jndn.mock.MockFace;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.transport.Transport;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test base server implementation.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ServerBaseImplTest {

  Face face = new MockFace();
  ServerBaseImpl instance = new ServerBaseImplImpl(face, new Name("/test/base"));

  public class ServerBaseImplImpl extends ServerBaseImpl {

    public ServerBaseImplImpl(Face face, Name prefix) {
      super(face, prefix);
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }

  /**
   * Test of getPrefix method, of class ServerBaseImpl.
   */
  @Test
  public void testGetPrefix() {
    assertNotNull(instance.getPrefix());
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
   * Test of addPipelineStage method, of class ServerBaseImpl.
   */
  @Test(expected = Exception.class)
  public void testPipeline() throws Exception {
    PipelineStage<Data, Data> pipelineStage = new PipelineStage<Data, Data>() {
      @Override
      public Data process(Data context) throws Exception {
        throw new Exception("Test exceptions with this");
      }
    };
    instance.addPipelineStage(pipelineStage);
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
}
