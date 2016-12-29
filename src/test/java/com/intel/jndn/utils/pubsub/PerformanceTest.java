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
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
@RunWith(Parameterized.class)
public class PerformanceTest {
  private static final Logger LOGGER = Logger.getLogger(PerformanceTest.class.getName());
  public int numPackets;
  public int packetSize;

  @Parameterized.Parameters(name = "{0} packets x {1} bytes")
  public static Object[][] data() {
    Table table = new Table().rows(1, 10, 100, 1000, 10000, 1000000).columns(1, 10, 100, 1000, 10000);
    return table.build();
  }

  public PerformanceTest(int numPackets, int dataSize) {
    this.numPackets = numPackets;
    this.packetSize = dataSize;
  }

  @Test
  public void baselineCreateData() throws Exception {
    long ns = clock(() -> {
      Data d;
      for (int i = 0; i < numPackets; i++) {
        d = new Data(new Name("" + i));
        d.setContent(new Blob(new byte[packetSize]));
      }
    });

    logBandwidth(ns);
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

  private void logBandwidth(long ns){
    long bytes = numPackets * packetSize;
    long bandwidth = Math.round(bytes / (double) ns * 1000000000);
    LOGGER.log(Level.INFO, "{0} packets x {1} bytes in {2}ms = {3}ps", new Object[]{numPackets, packetSize, ns/1000000, humanReadableByteCount(bandwidth, false)});
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