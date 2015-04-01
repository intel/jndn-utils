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
import com.intel.jndn.mock.MockTransport;
import com.intel.jndn.utils.client.SegmentedFutureData;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
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

  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());

  /**
   * Test of getSync method, of class SegmentedClient.
   *
   * @throws java.lang.Exception
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

  /**
   * Test that a failed request fails with an exception.
   *
   * @throws java.lang.Exception
   */
  @Test(expected = ExecutionException.class)
  public void testFailureToRetrieve() throws Exception {
    // setup face
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // retrieve non-existent data, should timeout
    logger.info("Client expressing interest asynchronously: /test/no-data");
    List<Future<Data>> futureSegments = SegmentedClient.getDefault().getAsyncList(face, new Name("/test/no-data"));

    // the list of future packets should be initialized
    assertEquals(1, futureSegments.size());
    assertTrue(futureSegments.get(0).isDone());

    // should throw error
    futureSegments.get(0).get();
  }

  /**
   * Test that a sync failed request fails with an exception.
   */
  @Test(expected = IOException.class)
  public void testSyncFailureToRetrieve() throws IOException {
    SegmentedClient.getDefault().getSync(new MockFace(), new Name("/test/no-data"));
  }

  /**
   * Ensure Name of the returned Data is the same as was requested; identifies
   * bug where the last Name.Component was always cut off.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  public void testNameShorteningLogic() throws InterruptedException, ExecutionException {
    MockFace face = new MockFace();
    Name name = new Name("/test/123");
    Data data = new Data(name);
    data.setContent(new Blob("...."));
    face.addResponse(name, data);

    SegmentedFutureData future = (SegmentedFutureData) SegmentedClient.getDefault().getAsync(face, name);
    assertEquals(name.toUri(), future.getName().toUri());
    assertEquals(name.toUri(), future.get().getName().toUri());
  }

}
