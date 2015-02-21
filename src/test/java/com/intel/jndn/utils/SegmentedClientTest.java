/*
 * File name: SegmentedClientTest.java
 * 
 * Purpose: Test SegmentedClient functionality.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import com.intel.jndn.mock.MockFace;
import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test SegmentedClient functionality.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedClientTest {

  /**
   * Test of getSync method, of class SegmentedClient.
   */
  @Test
  public void testGetSync() throws Exception {
    MockFace face = new MockFace();
    face.registerPrefix(new Name("/segmented/data"), new OnInterest() {
      private int count = 0;
      private int max = 9;

      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        Data data = new Data(interest.getName());
        if (!SegmentedClient.hasSegment(data.getName())) {
          data.getName().appendSegment(0);
        }
        data.getMetaInfo().setFinalBlockId(Component.fromNumberWithMarker(max, 0x00));
        data.setContent(new Blob("."));
        try {
          transport.send(data.wireEncode().buf());
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }, null);

    Data data = SegmentedClient.getDefault().getSync(face, new Name("/segmented/data").appendSegment(0));
    assertEquals(10, data.getContent().size());
  }
}
