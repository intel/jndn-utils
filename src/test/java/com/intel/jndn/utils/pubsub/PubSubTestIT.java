/*
 * jndn-utils
 * Copyright (c) 2016, Intel Corporation.
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

package com.intel.jndn.utils.pubsub;

import com.intel.jndn.utils.Publisher;
import com.intel.jndn.utils.Topic;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.intel.jndn.utils.TestHelper.connect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class PubSubTestIT {
  private static final Logger LOGGER = Logger.getLogger(PubSubTestIT.class.getName());
  private Face pubFace;
  private Face subFace;

  @Before
  public void before() throws Exception {
    String hostname = System.getProperty("nfd.ip");
    LOGGER.info("Testing on NFD at: " + hostname);
    pubFace = connect(hostname);
    subFace = connect(hostname);
  }

  @Test
  public void basicUsage() throws Exception {
    Topic topic = new Topic(new Name("/pub/sub/topic"));
    CountDownLatch latch = new CountDownLatch(1);

    topic.subscribe(pubFace, b -> latch.countDown(), e -> fail(e.getMessage()));

    Publisher publisher = topic.newPublisher(subFace);
    publisher.publish(new Blob("."));
    publisher.publish(new Blob(".."));
    publisher.publish(new Blob("..."));

    latch.await(20, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }
}