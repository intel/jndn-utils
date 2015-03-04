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

import com.intel.jndn.utils.event.NDNObserver;
import com.intel.jndn.mock.MockTransport;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test Server.java
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ServerTest {

  /**
   * Setup logging
   */
  private static final Logger logger = Logger.getLogger(Client.class.getName());

  /**
   * Test on functionality
   * TODO more comprehensive tests with InternalFace
   * @throws java.lang.InterruptedException
   */
  @Test
  public void testOn() throws InterruptedException {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // setup server
    NDNObserver observer = Server.getDefault().on(face, new Name("/test/server/on"), new OnServeInterest() {
      @Override
      public Data onInterest(Name prefix, Interest interest) {
        Data data = new Data(interest.getName());
        data.setContent(new Blob("..."));
        return data;
      }
    });
    
    // wait for background threads to run
    Thread.sleep(100);

    // check
    assertEquals(1, transport.getSentInterestPackets().size());
  }
}
