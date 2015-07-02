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
package com.intel.jndn.utils.client;

import com.intel.jndn.utils.SegmentedClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedDataReassembler {
  private static final Logger logger = Logger.getLogger(SegmentedDataReassembler.class.getName());
  private final Name interestName;
  private final List<CompletableFuture<Data>> segments;

  public SegmentedDataReassembler(Name interestName, List<CompletableFuture<Data>> segments) {
    this.interestName = interestName;
    this.segments = segments;
  }

  public CompletableFuture<Data> reassemble(){ 
    CompletableFuture[] segmentArray = segments.toArray(new CompletableFuture[]{});
    CompletableFuture<Void> allComplete = CompletableFuture.allOf(segmentArray);
    return allComplete.thenApply(new Function<Void, Data>() {
      @Override
      public Data apply(Void t) {
        try {
          logger.finer("Re-assembling data for request: " + interestName.toUri());
          byte[] content = aggregateBytes();
          Data data = buildAggregatePacket();
          data.setContent(new Blob(content));
          return data;
        } catch (ExecutionException | InterruptedException ex) {
          logger.log(Level.FINER, "Failed to re-assemble packet for request: " + interestName.toUri(), ex);
          throw new RuntimeException(ex);
        }
      }
    });
  }

  /**
   * @return the array of aggregated bytes for all of the segments retrieved
   * @throws ExecutionException
   */
  private byte[] aggregateBytes() throws ExecutionException {
    // aggregate bytes
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    for (Future<Data> futureData : segments) {
      try {
        content.write(futureData.get().getContent().getImmutableArray());
      } catch (ExecutionException | IOException | InterruptedException e) {
        throw new ExecutionException("Failed while aggregating retrieved packets: " + interestName.toUri(), e);
      }
    }
    return content.toByteArray();
  }

  /**
   * @return an aggregated {@link Data} packet with no content set
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private Data buildAggregatePacket() throws InterruptedException, ExecutionException {
    if (segments.isEmpty()) {
      throw new IllegalStateException("Unable to re-assemble packets; no segments added: " + interestName.toUri());
    }
    Data firstData = segments.get(0).get();
    Data aggregatedData = new Data(firstData);
    Name shortenedName = SegmentedClient.removeSegment(firstData.getName());
    aggregatedData.setName(shortenedName);
    return aggregatedData;
  }
}
