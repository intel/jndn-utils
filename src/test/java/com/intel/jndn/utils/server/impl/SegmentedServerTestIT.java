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

import com.intel.jndn.mock.MockKeyChain;
import com.intel.jndn.utils.TestHelper;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import java.io.IOException;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test the {@link SegmentedServer} on an actual NFD; to run this, pass the NFD
 * IP/host name as a parameter like <code>-Dnfd.ip=10.1.1.1</code>.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServerTestIT {

  private static final Logger logger = Logger.getLogger(SegmentedServerTestIT.class.getName());
  private static final Name PREFIX = new Name("/test/for/segmented-server");
  protected static final int DATA_SIZE_BYTES = 10000;
  Face face;
  SegmentedServer instance;
  private String ip;

  public SegmentedServerTestIT() throws SecurityException {
    this.ip = System.getProperty("nfd.ip");
    this.face = new Face(ip);
    this.instance = new SegmentedServer(face, PREFIX);
    KeyChain mockKeyChain = MockKeyChain.configure(new Name("/test/server"));
    face.setCommandSigningInfo(mockKeyChain, mockKeyChain.getDefaultCertificateName());
  }

  @Test
  public void testRegisterAndRetrieval() throws Exception {
    final Name dataName = new Name(PREFIX).append("1");

    // why a new thread? The server will only operate if someone turns the crank,
    // i.e. someone calls face.processEvents() every so often. 
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          instance.serve(buildDataPacket(dataName));
        } catch (IOException ex) {
          logger.info("Failed to serve data.");
        }

        while (true) {
          try {
            face.processEvents();
          } catch (IOException | EncodingException ex) {
            logger.info("Failed while processing events.");
          }
        }
      }
    }).start();

    // TODO why wait? we want to make sure the thread is running and that the prefix
    // registration has succeeded on the NFD before we send interests
    Thread.sleep(1000);

    // why a different face? because we don't want the abover face.processEvents()
    // to interfere with the SimpleClient's processEvents().
    logger.info("Retrieving data: " + dataName.toUri());
    Interest interest = new Interest(dataName);
    interest.setInterestLifetimeMilliseconds(2000);
    interest.setMustBeFresh(true);
    interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);

    Data retrieved = AdvancedClient.getDefault().getSync(new Face(ip), interest);
    assertNotNull(retrieved);
    assertEquals(DATA_SIZE_BYTES, retrieved.getContent().size());
    logger.info("Retrieved data: " + retrieved.getName().toUri());
  }

  Data buildDataPacket(Name name) {
    Data data = new Data(new Name(name).appendSegment(0));
    data.setContent(new Blob(TestHelper.buildRandomBytes(DATA_SIZE_BYTES)));
    data.getMetaInfo().setFreshnessPeriod(30000);
    return data;
  }
}
