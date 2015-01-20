/*
 * File name: PublisherTest.java
 * 
 * Purpose: 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import com.intel.jndn.mock.MockTransport;
import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class PublisherTest {

  /**
   * Test of publish method, of class Publisher.
   */
  @Test
  public void testPublishSubscribe() throws IOException, EncodingException, InterruptedException, SecurityException {
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);
    final Counter counter = new Counter();

    // setup subscriber
    Subscriber subscriber = new Subscriber(face, new Name("/test/channel"));
    subscriber.addType("example", TestExample.class);
    subscriber.on(TestExample.class, new Subscriber.OnPublish<TestExample>() {
      @Override
      public void onPublish(TestExample publishedObject) {
        counter.inc();
        assertEquals(1, publishedObject.a);
        assertEquals(true, publishedObject.b);
      }
    });

    // setup publisher
    Publisher publisher = new Publisher(face, new Name("/test/channel"), false);
    publisher.addType("example", TestExample.class);
    publisher.publish(new TestExample(1, true));

    // process events
    while(counter.get() == 0){
      Thread.sleep(10);
    }
    
    // check
    assertEquals(1, counter.get());
  }
  
   public Data fakeManagementSuccess(Name forName, int statusCode, String statusText){
    TlvEncoder encoder = new TlvEncoder(1500);
    int saveLength = encoder.getLength();

    // encode backwards
    encoder.writeBlobTlv(Tlv.NfdCommand_StatusText, new Blob(statusText).buf());
    encoder.writeNonNegativeIntegerTlv(Tlv.NfdCommand_StatusCode, statusCode);
    encoder.writeTypeAndLength(Tlv.NfdCommand_ControlResponse, encoder.getLength() - saveLength);
    Blob content = new Blob(encoder.getOutput(), false);
    
    // now create data packet
    Data data = new Data(forName);
    data.setContent(content);
    return data;
  }

  class TestExample {

    int a;
    boolean b;

    public TestExample(int a, boolean b) {
      this.a = a;
      this.b = b;
    }
  }

  /**
   * Count reference
   */
  class Counter {

    int count = 0;

    public void inc() {
      count++;
    }

    public int get() {
      return count;
    }
  }

//  /**
//   * Test of clearOldPublished method, of class Publisher.
//   */
//  @Test
//  public void testClearOldPublished() {
//    System.out.println("clearOldPublished");
//    Publisher instance = null;
//    instance.clearOldPublished();
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
}
