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
package com.intel.jndn.utils.processing.impl;

import com.intel.jndn.utils.client.impl.AdvancedClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * Test {@link CompressionStage}.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class CompressionStageTest {

  private static final Logger logger = Logger.getLogger(AdvancedClient.class.getName());

  /**
   * Test of process method, of class CompressionStage.
   */
  @Test
  public void testProcess() throws Exception {
    Data data = new Data(new Name("/test/packet"));
    data.setContent(new Blob(".............................................."));
    int originalSize = data.getContent().size();
    logger.info("Uncompressed size: " + originalSize);
    logger.info("Uncompressed: " + data.getContent().toString());

    CompressionStage instance = new CompressionStage();
    Data result = instance.process(data);
    logger.info("Compressed size: " + result.getContent().size());
    logger.info("Compressed: " + result.getContent().toString());

    assertTrue(result.getContent().size() < originalSize);
  }
}
