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
import com.intel.jndn.utils.client.SegmentedFutureData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 * Test SegmentedClient functionality.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedClientTest {

  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());
  private MockFace face;

  @Before
  public void beforeTest() {
    face = new MockFace();
  }

  /**
   * Test of getSync method, of class SegmentedClient.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetSync() throws Exception {
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
    // retrieve non-existent data, should timeout
    logger.info("Client expressing interest asynchronously: /test/no-data");
    Future<Data> futureData = SegmentedClient.getDefault().getAsync(face, new Name("/test/no-data"));
    futureData.get();
  }

  /**
   * Test that a sync failed request fails with an exception.
   */
  @Test(expected = IOException.class)
  public void testSyncFailureToRetrieve() throws IOException {
    SegmentedClient.getDefault().getSync(face, new Name("/test/no-data"));
  }

  /**
   * Identifies bug where the last Name.Component was always cut off.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  public void testNameShorteningLogic() throws InterruptedException, ExecutionException {
    final List<Data> segments = buildSegmentedData("/test/name", 10);
    for (Data segment : segments) {
      face.addResponse(segment.getName(), segment);
    }

    Name name = new Name("/test/name").appendSegment(0);

    SegmentedFutureData future = (SegmentedFutureData) SegmentedClient.getDefault().getAsync(face, name);
    assertEquals(name.getPrefix(-1).toUri(), future.get().getName().toUri());
  }

  /**
   * Verify that Data returned with a different Name than the Interest is still
   * segmented correctly.
   *
   * @throws Exception
   */
  @Test
  public void testWhenDataNameIsLongerThanInterestName() throws Exception {
    final List<Data> segments = buildSegmentedData("/a/b/c/d", 10);
    for (Data segment : segments) {
      face.addResponse(segment.getName(), segment);
    }

    Name name = new Name("/a/b");
    face.addResponse(name, segments.get(0));

    Data data = SegmentedClient.getDefault().getSync(face, name);
    assertNotNull(data);
    assertEquals("/a/b/c/d", data.getName().toUri());
  }

  /**
   * Verify that Data packets with no content do not cause errors; identifies
   * bug.
   *
   * @throws Exception
   */
  @Test
  public void testNoContent() throws Exception {
    Name name = new Name("/test/no-content").appendSegment(0);
    Data data = buildSegmentedData(name);
    face.addResponse(name, data);

    Future<Data> result = SegmentedClient.getDefault().getAsync(face, name);
    assertEquals("/test/no-content", result.get().getName().toUri());
    assertEquals("", result.get().getContent().toString());
  }

  /**
   * Verify that segmented content is the correct length when retrieved by the
   * client.
   *
   * @throws Exception
   */
  @Test
  public void testContentLength() throws Exception {
    Data data1 = new Data(new Name("/test/content-length").appendSegment(0));
    data1.setContent(new Blob("0123456789"));
    data1.getMetaInfo().setFinalBlockId(Name.Component.fromNumberWithMarker(1, 0x00));
    face.addResponse(data1.getName(), data1);

    Data data2 = new Data(new Name("/test/content-length").appendSegment(1));
    data2.setContent(new Blob("0123456789"));
    data1.getMetaInfo().setFinalBlockId(Name.Component.fromNumberWithMarker(1, 0x00));
    face.addResponse(data2.getName(), data2);

    Future<Data> result = SegmentedClient.getDefault().getAsync(face, new Name("/test/content-length").appendSegment(0));
    assertEquals(20, result.get().getContent().size());
  }

  /**
   * If a Data packet does not have a FinalBlockId, the SegmentedClient should
   * just return the packet.
   *
   * @throws Exception
   */
  @Test
  public void testNoFinalBlockId() throws Exception {
    Name name = new Name("/test/no-final-block-id");
    Data data = new Data(name);
    data.setContent(new Blob("1"));
    face.addResponse(name, data);

    Future<Data> result = SegmentedClient.getDefault().getAsync(face, name);
    assertEquals("/test/no-final-block-id", result.get().getName().toUri());
    assertEquals("1", result.get().getContent().toString());
  }

  /**
   * Helper method, sets FinalBlockId from last Name component
   *
   * @param name
   * @return
   */
  private Data buildSegmentedData(Name name) {
    Data data = new Data(name);
    data.getMetaInfo().setFinalBlockId(name.get(-1));
    return data;
  }

  private List<Data> buildSegmentedData(String name, int numSegments) {
    Name.Component finalBlockId = Name.Component.fromNumberWithMarker(numSegments - 1, 0x00);
    List<Data> segments = new ArrayList<>(numSegments);

    for (int i = 0; i < numSegments; i++) {
      Data data = new Data(new Name(name).appendSegment(i));
      data.setContent(new Blob("0123456789"));
      data.getMetaInfo().setFinalBlockId(finalBlockId);
      segments.add(data);
    }

    return segments;
  }

}
