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
 * Test Client.java
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
    Data response = new Data(new Name("/test/sync"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // retrieve data
    logger.info("Client expressing interest synchronously: /test/sync");
    Client client = new Client();
    Data data = client.getSync(face, new Name("/test/sync"));
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
    Data response = new Data(new Name("/test/async"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // retrieve data
    logger.info("Client expressing interest asynchronously: /test/async");
    Client client = new Client();
    ClientObserver observer = client.get(face, new Name("/test/async"));

    // wait 
    while (observer.eventCount() == 0) {
      Thread.sleep(10);
    }
    assertEquals(1, observer.eventCount());
    assertEquals(1, observer.dataCount());
    Data data = (Data) observer.getFirst().getPacket();
    assertEquals(new Blob("...").buf(), data.getContent().buf());
  }

  /**
   * Test that asynchronous client times out correctly
   * 
   * @throws InterruptedException 
   */
  @Test
  public void testTimeout() throws InterruptedException {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // retrieve non-existent data, should timeout
    logger.info("Client expressing interest asynchronously: /test/timeout");
    ClientObserver observer = Client.getDefault().get(face, new Name("/test/timeout"));

    // wait 
    while (observer.errorCount() == 0) {
      Thread.sleep(100);
    }
    Exception e = (Exception) observer.getFirst().getPacket();
    assertEquals(1, observer.errorCount());
  }
  
  /**
   * Test that callback is called on event
   * @throws InterruptedException 
   */
  @Test
  public void testCallback() throws InterruptedException {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);
    
    // setup return data
    Data response = new Data(new Name("/test/callback"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // retrieve non-existent data, should timeout
    logger.info("Client expressing interest asynchronously: /test/callback");
    ClientObserver observer = Client.getDefault().get(face, new Name("/test/callback"));
    observer.then(new OnEvent(){
      @Override
      public void onEvent(ClientEvent event) {
        assertEquals(new Blob("...").buf(), ((Data) event.getPacket()).getContent().buf());
      }
    });

    // wait 
    while (observer.eventCount() == 0) {
      Thread.sleep(100);
    }
    assertEquals(1, observer.eventCount());
  }
}
