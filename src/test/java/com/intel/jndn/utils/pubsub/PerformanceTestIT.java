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

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.intel.jndn.utils.TestHelper.connect;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
@RunWith(Parameterized.class)
public class PerformanceTestIT {
  private static final Logger LOGGER = Logger.getLogger(PerformanceTestIT.class.getName());
  private static final Logger JNDN_LOGGER = Logger.getLogger("net.named_data");

  static {
    // turn off jndn logging for clarity
    JNDN_LOGGER.setLevel(Level.OFF);
  }

  public int numPackets;
  public int packetSize;

  @Parameterized.Parameters(name = "{0} packets x {1} bytes")
  public static Object[][] data() {
    Table table = new Table().rows(1, 10, 100, 1000, 10000).columns(1, 10, 100, 1000, 8000);
    return table.build();
  }

  public PerformanceTestIT(int numPackets, int dataSize) {
    this.numPackets = numPackets;
    this.packetSize = dataSize;
  }

  @Test
  public void baselineCreateData() throws Exception {
    long ns = clock(() -> {
      Data d;
      for (int i = 0; i < numPackets; i++) {
        d = new Data(new Name(Integer.toString(i)));
        d.setContent(new Blob(new byte[packetSize]));
      }
    });

    logBandwidth("Creating data packets", ns);
  }

  @Test
  public void sendInterestPackets() throws Exception {
    Face face = connect(System.getProperty("nfd.ip"));

    long ns = clock(() -> {
      for (int i = 0; i < numPackets; i++) {
        Name.Component c1 = new Name.Component(Integer.toString(i));
        Name.Component c2 = new Name.Component(new byte[packetSize]);
        Interest in = new Interest(new Name(new Name.Component[]{c1, c2}));

        try {
          face.expressInterest(in, (OnData) null, (OnTimeout) null);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to express interest: {0}", e);
          fail(e.getMessage());
        }
      }
    });

    logBandwidth("Sending pure interests", ns);
  }

  @Test
  public void retrievingOneAtATime() throws Exception {
    Face producer = connect(System.getProperty("nfd.ip"));
    Face consumer = connect(System.getProperty("nfd.ip"));
    Name prefix = new Name("/test/perf/" + (new Random()).nextInt());

    CountDownLatch latch = new CountDownLatch(1);
    producer.registerPrefix(prefix, (prefix1, interest, face, interestFilterId, filter) -> {
      Data data = new Data(interest.getName());
      data.setContent(new Blob(new byte[packetSize]));
      try {
        face.putData(data);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to put data: {0}", e);
        fail(e.getMessage());
      }
    }, p -> fail("Failed to register prefix: " + p), (p, id) -> latch.countDown());
    assertTrue(latch.await(15, TimeUnit.SECONDS));

    Semaphore lock = new Semaphore(1);
    long ns = clock(() -> {
      for (int i = 0; i < numPackets; i++) {
        Interest in = new Interest(new Name(prefix).append(Integer.toString(i)));
        try {
          lock.tryAcquire(5, TimeUnit.SECONDS);
          consumer.expressInterest(in, (interest, data) -> lock.release(), interest -> fail("Interest timed out"));
        } catch (InterruptedException | IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to express interest: {0}", e);
          fail(e.getMessage());
        }
      }
    });

    logBandwidth("Retrieving datas one-at-a-time", ns);
  }

  @Test
  public void retrievingPipelined() throws Exception {

  }

  private static String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if (bytes < unit) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  private long clock(Runnable runnable) {
    System.gc();

    long start = System.nanoTime();
    runnable.run();
    long end = System.nanoTime();
    return end - start;
  }

  private void logBandwidth(String method, long ns) {
    long bytes = numPackets * packetSize;
    long bandwidth = Math.round(bytes / (double) ns * 1000000000);
    LOGGER.log(Level.INFO, "{4}, {0} packets x {1} bytes in {2}ms = {3}ps", new Object[]{numPackets, packetSize, ns / 1000000, humanReadableByteCount(bandwidth, false), method});
  }

  private static class Table {
    private int[] rows;
    private int[] columns;

    public Table rows(int... row) {
      this.rows = row;
      return this;
    }

    public Table columns(int... columns) {
      this.columns = columns;
      return this;
    }

    public Object[][] build() {
      int end = rows.length * columns.length;
      Object[][] result = new Object[end][2];
      int i = 0;
      for (int r = 0; r < rows.length; r++) {
        for (int c = 0; c < columns.length; c++) {
          result[i++] = new Object[]{rows[r], columns[c]};
        }
      }
      return result;
    }
  }
}