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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test SimpleClient.java; requires a hostname to an NFD accepting a generated
 * key to register prefixes, e.g. mvn test -Dnfd.ip=10.10.10.1
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SimpleClientTestIT {

  private static final Logger logger = Logger.getLogger(SegmentedServerTestIT.class.getName());
  private static final Name PREFIX = new Name("/test/for/simple-client");

  Face face;
  SimpleClient instance;
  String ip;
  ScheduledExecutorService pool;

  public SimpleClientTestIT() throws SecurityException {
    this.ip = System.getProperty("nfd.ip");
    this.face = new Face(ip);
    this.instance = SimpleClient.getDefault();
    this.pool = Executors.newScheduledThreadPool(2);

    KeyChain mockKeyChain = MockKeyChain.configure(new Name("/test/server"));
    face.setCommandSigningInfo(mockKeyChain, mockKeyChain.getDefaultCertificateName());
    pool.scheduleAtFixedRate(new EventProcessor(face), 0, 10, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testCompletableFuture() throws Exception {
    Data servedData = new Data();
    servedData.setContent(new Blob("....."));
    face.registerPrefix(PREFIX, new OnInterestCallback() {
      @Override
      public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        servedData.setName(interest.getName());
        try {
          face.putData(servedData);
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Failed to put data.", ex);
        }
      }
    }, null);

    CompletableFuture<Data> future = instance.getAsync(face, PREFIX);
    Data retrievedData = future.get(200, TimeUnit.MILLISECONDS);

    Assert.assertEquals(servedData.getContent().toString(), retrievedData.getContent().toString());
  }

  private class EventProcessor implements Runnable {

    private final Face face;

    public EventProcessor(Face face) {
      this.face = face;
    }

    @Override
    public void run() {
      try {
        face.processEvents();
      } catch (IOException | EncodingException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
    }
  }
}
