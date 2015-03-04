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
