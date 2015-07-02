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
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.mock.MockFace;
import com.intel.jndn.utils.TestHelper;
import com.intel.jndn.utils.client.SegmentationType;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.util.Blob;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class AdvancedClientTest {

  MockFace face = new MockFace();
  AdvancedClient instance = new AdvancedClient();

  @Test
  public void testRetries() throws Exception {
    Name name = new Name("/test/advanced/client");
    Interest interest = new Interest(name, 1);

    CompletableFuture<Data> future = instance.getAsync(face, interest);

    while (!future.isDone()) {
      face.processEvents();
    }

    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void testGetSync() throws Exception {
    Name name = new Name("/segmented/data");

    face.registerPrefix(name, new OnInterestCallback() {
      private int count = 0;
      private int max = 9;

      @Override
      public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Data data = new Data(interest.getName());
        if (!SegmentationHelper.isSegmented(data.getName(), SegmentationType.SEGMENT.value())) {
          data.getName().appendSegment(0);
        }
        data.getMetaInfo().setFinalBlockId(Name.Component.fromNumberWithMarker(max, 0x00));
        data.setContent(new Blob("."));
        try {
          face.putData(data);
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }, null);

    Data data = instance.getSync(face, new Name(name).appendSegment(0));
    assertEquals(10, data.getContent().size());
  }
  
    /**
   * Verify that Data returned with a different Name than the Interest is still
   * segmented correctly.
   *
   * @throws Exception
   */
  @Test
  public void testWhenDataNameIsLongerThanInterestName() throws Exception {
    final List<Data> segments = TestHelper.buildSegments(new Name("/a/b/c/d"), 0, 10);
    for (Data segment : segments) {
      face.addResponse(segment.getName(), segment);
    }

    Name name = new Name("/a/b");
    face.addResponse(name, segments.get(0));

    Data data = instance.getSync(face, name);
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
    Data data = TestHelper.buildData(name, "", 0);
    face.addResponse(name, data);

    Future<Data> result = instance.getAsync(face, name);
    face.processEvents();
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

    Future<Data> result = instance.getAsync(face, new Name("/test/content-length").appendSegment(0));
    face.processEvents();
    face.processEvents();
    assertEquals(20, result.get().getContent().size());
  }

  /**
   * If a Data packet does not have a FinalBlockId, the AdvancedClient should
 just return the packet.
   *
   * @throws Exception
   */
  @Test
  public void testNoFinalBlockId() throws Exception {
    Name name = new Name("/test/no-final-block-id");
    Data data = new Data(name);
    data.setContent(new Blob("1"));
    face.addResponse(name, data);

    Future<Data> result = instance.getAsync(face, name);
    face.processEvents();
    assertEquals("/test/no-final-block-id", result.get().getName().toUri());
    assertEquals("1", result.get().getContent().toString());
  }
}
