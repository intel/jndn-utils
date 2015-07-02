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

import com.intel.jndn.utils.client.impl.SimpleClient;
import com.intel.jndn.utils.TestHelper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Test SimpleClient.java; requires a hostname to an NFD accepting a generated
 * key to register prefixes, e.g. mvn test -Dnfd.ip=10.10.10.1
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SimpleClientTestIT {

  private static final Logger logger = Logger.getLogger(SimpleClientTestIT.class.getName());
  private static final Name PREFIX_RETRIEVE = new Name("/test/simple-client/retrieve-one").append(TestHelper.buildRandomString(10));
  private static final Name PREFIX_RETRIEVE_MULTIPLE = new Name("/test/simple-client/retrieve-multiple").append(TestHelper.buildRandomString(10));

  SimpleClient instance;
  private final TestHelper.NdnEnvironment environment;

  public SimpleClientTestIT() throws SecurityException {
    String ip = System.getProperty("nfd.ip");
    environment = TestHelper.buildTestEnvironment(ip, 2);
    instance = SimpleClient.getDefault();
  }

  @Test
  public void testRetrieval() throws Exception {
    // setup faces
    Face producerFace = environment.faces.get(0);
    Face consumerFace = environment.faces.get(1);

    // setup server
    Data servedData = new Data();
    servedData.setContent(new Blob("....."));
    servedData.getMetaInfo().setFreshnessPeriod(0);
    producerFace.registerPrefix(PREFIX_RETRIEVE, new DataServer(servedData), null);

    // TODO this must be here until the prefix registration callback is complete
    Thread.sleep(500);

    // send interest
    CompletableFuture<Data> future = instance.getAsync(consumerFace, PREFIX_RETRIEVE);
    Data retrievedData = future.get();

    // verify
    Assert.assertEquals(servedData.getContent().toString(), retrievedData.getContent().toString());
  }

  @Test
  public void testSyncRetrieval() throws Exception {
    String ip = System.getProperty("nfd.ip");
    Face face = new Face(ip);
    Interest interest = new Interest(new Name("/localhop/nfd/rib")); // this query should return some data if NFD is running locally
    interest.setInterestLifetimeMilliseconds(2000);
    Data data = SimpleClient.getDefault().getSync(face, interest);
    assertNotNull(data);
  }

  @Test
  public void testMultipleRetrieval() throws Exception {
    int numInterests = 100;

    // setup faces
    Face producerFace = environment.faces.get(0);
    Face consumerFace = environment.faces.get(1);

    // setup server
    Data servedData = new Data();
    servedData.setContent(new Blob("0123456789"));
    servedData.getMetaInfo().setFreshnessPeriod(0);
    producerFace.registerPrefix(PREFIX_RETRIEVE_MULTIPLE, new DataServer(servedData), null);

    // this must be here until the prefix registration callback is complete
    Thread.sleep(500);

    // request all packets
    Stream<CompletableFuture<Data>> futures = IntStream.range(0, numInterests)
            .boxed().map((i) -> instance.getAsync(consumerFace, PREFIX_RETRIEVE_MULTIPLE));

    // check all returned packets
    futures.map((f) -> TestHelper.retrieve(f))
            .forEach((d) -> assertEquals(servedData.getContent().toString(), d.getContent().toString()));
  }

  private class DataServer implements OnInterestCallback {

    private Data data;

    public DataServer(Data data) {
      this.data = data;
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
      data.setName(interest.getName());
      try {
        face.putData(data);
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Failed to put data.", ex);
      }
    }
  }
}
