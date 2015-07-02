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
package com.intel.jndn.utils.server.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the SegmentedServerHelper
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServerHelperTest {

  /**
   * Test of segment method, of class SegmentedServerHelper.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testSegmentation() throws Exception {
    final Data template = new Data(new Name("/segmented/data"));
    final InputStream content = new ByteArrayInputStream("0123456789".getBytes());
    List<Data> segments = SegmentedServerHelper.segment(template, content, 1);
    assertEquals(10, segments.size());

    // test first packet
    assertEquals(0, segments.get(0).getName().get(-1).toSegment());
    assertEquals(9, segments.get(0).getMetaInfo().getFinalBlockId().toSegment());
    assertEquals("0", segments.get(0).getContent().toString());

    // test last packet
    assertEquals(9, segments.get(9).getName().get(-1).toSegment());
    assertEquals(9, segments.get(9).getMetaInfo().getFinalBlockId().toSegment());
    assertEquals("9", segments.get(9).getContent().toString());
  }

   /**
   * Test of segment method, of class SegmentedServerHelper.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testSegmentationDifferentSizes() throws Exception {
    final Data template = new Data(new Name("/segmented/data"));

    // size 2
    final InputStream content2 = new ByteArrayInputStream("0123456789".getBytes());
    List<Data> segments2 = SegmentedServerHelper.segment(template, content2, 2);
    assertEquals(5, segments2.size());
    assertEquals("89", segments2.get(4).getContent().toString());

    // size 3
    final InputStream content3 = new ByteArrayInputStream("0123456789".getBytes());
    List<Data> segments3 = SegmentedServerHelper.segment(template, content3, 3);
    assertEquals(4, segments3.size());
    assertEquals("9", segments3.get(3).getContent().toString());

    // size 4
    final InputStream content4 = new ByteArrayInputStream("0123456789".getBytes());
    List<Data> segments4 = SegmentedServerHelper.segment(template, content4, 4);
    assertEquals(3, segments4.size());
    assertEquals("89", segments4.get(2).getContent().toString());
  }
}
