/*
 * File name: ServerTest.java
 * 
 * Purpose: Test Server.java 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
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
