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

import com.intel.jndn.mock.MockKeyChain;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
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
  Name prefix;
  Face face;
  SegmentedServer instance;
  private String ip;

  public SegmentedServerTestIT() throws SecurityException {
    this.ip = System.getProperty("nfd.ip");
    this.prefix = new Name("/test/for/segmented-server");
    this.face = new Face(ip);
    this.instance = new SegmentedServer(face, prefix);
    KeyChain mockKeyChain = MockKeyChain.configure(new Name("/test/server"));
    face.setCommandSigningInfo(mockKeyChain, mockKeyChain.getDefaultCertificateName());
  }

  @Test
  public void testRegisterAndRetrieval() throws Exception {
    final Name name = new Name(prefix).append("1");

    // why a new thread? The server will only operate if someone turns the crank,
    // i.e. someone calls face.processEvents() every so often. 
    new Thread(new Runnable() {
      @Override
      public void run() {
        Data served = new Data(name);
        served.setContent(new Blob("..."));
        served.getMetaInfo().setFreshnessPeriod(30000);

        try {
          instance.serve(served);
          logger.info("Serving data: " + name.toUri());
        } catch (IOException ex) {
          logger.info("Failed to serve data: " + name.toUri());
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

    // why wait? we want to make sure the thread is running and that the prefix
    // registration has succeeded on the NFD before we send interests
    Thread.sleep(1000);

    // why a different face? because we don't want the abover face.processEvents()
    // to interfere with the SimpleClient's processEvents().
    logger.info("Retrieving data: " + name.toUri());
    Data retrieved = SegmentedClient.getDefault().getSync(new Face(ip), name);
    assertNotNull(retrieved);
    assertEquals("...", retrieved.getContent().toString());
  }
}
