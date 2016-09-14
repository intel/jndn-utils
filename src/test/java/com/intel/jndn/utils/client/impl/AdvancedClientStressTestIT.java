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
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.utils.Client;
import com.intel.jndn.utils.TestHelper;
import com.intel.jndn.utils.impl.SegmentationHelper;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.SecurityException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test AdvancedClient.java; requires a hostname to an NFD accepting a generated
 * key to register prefixes, e.g. mvn test -Dnfd.ip=10.10.10.1
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class AdvancedClientStressTestIT {

  private static final Logger logger = Logger.getLogger(AdvancedClientStressTestIT.class.getName());
  private static final Name PREFIX = new Name("/test/advanced-client").append(TestHelper.buildRandomString(10));
  private static final int NUM_MESSAGES = 100;
  private static final int MESSAGE_SIZE_BYTES = 10000;
  private static final int SEGMENT_SIZE_BYTES = 5000;
  private final TestHelper.NdnEnvironment environment;

  public AdvancedClientStressTestIT() throws SecurityException {
    String ip = System.getProperty("nfd.ip");
    logger.info("Testing on NFD at: " + ip);
    environment = TestHelper.buildTestEnvironment(ip, 2);
  }

  @Test
  public void stressTest() throws Exception {
    Face producer = environment.faces.get(0);
    Face consumer = environment.faces.get(1);

    producer.registerPrefix(PREFIX, new RandomDataServer(MESSAGE_SIZE_BYTES, SEGMENT_SIZE_BYTES), null);

    // TODO this must be here until the prefix registration callback is complete
    Thread.sleep(500);

    long startTime = System.currentTimeMillis();
    AdvancedClient client = new AdvancedClient();
    List<CompletableFuture<Data>> requests = expressInterests(client, consumer, PREFIX, NUM_MESSAGES);
    requests.stream().map((f) -> TestHelper.retrieve(f)).collect(Collectors.toList());
    long endTime = System.currentTimeMillis();

    logger.info(String.format("Transfered %d bytes in %d ms", MESSAGE_SIZE_BYTES * NUM_MESSAGES, endTime - startTime));
  }

  private List<CompletableFuture<Data>> expressInterests(Client client, Face face, Name name, int count) {
    return IntStream.range(0, count).boxed().map((i) -> client.getAsync(face, name)).collect(Collectors.toList());
  }

  private class RandomDataServer implements OnInterestCallback {

    private final int contentSize;
    private final int segmentSize;

    public RandomDataServer(int contentSize, int segmentSize) {
      this.contentSize = contentSize;
      this.segmentSize = segmentSize;
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
      // ignore segmented requests, the data has already been put below
      if (SegmentationHelper.isSegmented(interest.getName(), (byte) 0x00)) {
        return;
      }
      
      // randomly ignore 10% of requests
      if (new Random().nextInt(10) < 2){
        logger.info("Skipping an interest: " + interest.toUri());
        return;
      }

      ByteArrayInputStream bytes = new ByteArrayInputStream(TestHelper.buildRandomString(contentSize).getBytes());
      Data data = TestHelper.buildData(interest.getName(), "");
      data.getMetaInfo().setFreshnessPeriod(0);

      try {
        for (Data segment : SegmentationHelper.segment(data, bytes, segmentSize)) {
          logger.log(Level.INFO, "Put data: " + segment.getName().toUri());
          face.putData(segment);
        }
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Failed to put data.", ex);
      }
    }
  }
}
