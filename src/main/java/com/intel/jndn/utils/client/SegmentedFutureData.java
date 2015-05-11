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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * Represents a list of packets that have been requested asynchronously and have
 * yet to be returned from the network. Usage:
 *
 * <pre><code>
 * SegmentedFutureData segmentedFutureData = new SegmentedFutureData(face, name, futureDataList);
 * Data data = segmentedFutureData.get(); // will block until complete
 * </code></pre>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedFutureData extends FutureDataBase {

  List<Future<Data>> segments = new ArrayList<>();

  public SegmentedFutureData(Face face, Name name) {
    super(face, name);
  }

  /**
   * Add a future data to the list of segments
   *
   * @param index the numeric index in the list, see
   * {@link List#add(int, java.lang.Object)} for more details.
   * @param futureData the {@link Future} to add
   */
  public void add(int index, Future<Data> futureData) {
    segments.add(index, futureData);
  }

  /**
   * Add a future data to the end of the list of segments
   *
   * @param futureData the {@link Future} to add
   */
  public void add(FutureData futureData) {
    segments.add(futureData);
  }

  /**
   * @return true if the request has completed (successfully or not)
   */
  @Override
  public boolean isDone() {
    return isRejected() || isCancelled() || allSegmentsDone();
  }
  
  /**
   * @return true if all segments are done
   */
  private boolean allSegmentsDone(){
    for (Future<Data> futureData : segments) {
      if (!futureData.isDone()) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getData() throws ExecutionException, InterruptedException {
    byte[] content = aggregateBytes();
    Data data = buildAggregatePacket();
    data.setContent(new Blob(content));
    return data;
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
        throw new ExecutionException("Failed while aggregating retrieved packets: " + getName().toUri(), e);
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
      throw new IllegalStateException("Unable to aggregate packets; no segments added with SegmentedFutureData.add().");
    }
    Data firstData = segments.get(0).get();
    Data aggregatedData = new Data(firstData);
    aggregatedData.setName(parseName(firstData));
    return aggregatedData;
  }

  /**
   * @return parse the name of the segmented packet from the first packet; this
   * will remove the segment number if it is the last name component
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private Name parseName(Data data) throws InterruptedException, ExecutionException {
    Name firstPacketName = data.getName();
    if (SegmentedClient.hasSegment(firstPacketName)) {
      firstPacketName = firstPacketName.getPrefix(-1);
    }
    return firstPacketName;
  }
}
