/*
 * File name: ClientTest.java
 * 
 * Purpose: Test Client.java
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import com.intel.jndn.mock.MockTransport;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ClientTest {
  
  
  /**
   * Setup logging
   */
  private static final Logger logger = LogManager.getLogger();

  /**
   * Test retrieving data synchronously
   */
  @Test
  public void testGetSync() {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // setup return data
    Data response = new Data(new Name("/a/b/c"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // retrieve data
    logger.info("Client expressing interest synchronously: /a/b/c");
    Client client = new Client();
    Data data = client.getSync(face, new Name("/a/b/c"));
    assertEquals(new Blob("...").buf(), data.getContent().buf());
  }

  /**
   * Test retrieving data asynchronously
   * 
   * @throws InterruptedException 
   */
  @Test
  public void testGetAsync() throws InterruptedException {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // setup return data
    Data response = new Data(new Name("/a/b/c"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // retrieve data
    logger.info("Client expressing interest asynchronously: /a/b/c");
    Client client = new Client();
    ClientObserver observer = client.get(face, new Name("/a/b/c"));

    // wait 
    while (observer.responses() == 0) {
      Thread.sleep(10);
    }
    Data data = (Data) observer.getFirst().getPacket();
    assertEquals(new Blob("...").buf(), data.getContent().buf());
  }
}
