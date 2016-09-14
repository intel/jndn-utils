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

import com.intel.jndn.mock.MockKeyChain;
import com.intel.jndn.utils.Publisher;
import com.intel.jndn.utils.Topic;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.ThreadPoolFace;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.transport.AsyncTcpTransport;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

  private Face connect(String hostName) throws SecurityException {
    ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
    AsyncTcpTransport transport = new AsyncTcpTransport(pool);
    AsyncTcpTransport.ConnectionInfo connectionInfo = new AsyncTcpTransport.ConnectionInfo(hostName, 6363, true);
    ThreadPoolFace face = new ThreadPoolFace(pool, transport, connectionInfo);

    Name signatureName = new Name("/topic/test/it").appendVersion(new Random().nextLong()); // note that using the same signature name seemed to cause registration failures
    KeyChain keyChain = MockKeyChain.configure(signatureName);
    face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

    return face;
  }
}