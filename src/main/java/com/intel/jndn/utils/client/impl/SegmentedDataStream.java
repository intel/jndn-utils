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

import com.intel.jndn.utils.client.DataStream;
import com.intel.jndn.utils.client.OnComplete;
import com.intel.jndn.utils.client.OnException;
import com.intel.jndn.utils.impl.SegmentationHelper;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * As packets are received, they are mapped by their last component's segment
 * marker; if the last component is not parseable, an exception is thrown and
 * processing completes. The exception to this is if the first packet returned
 * does not have a segment marker as the last component; in this case, the
 * packet is assumed to be the only packet returned and is placed as the first
 * and only packet to assemble. Observers may register callbacks to watch when
 * data is received; if data is received out of order, the callbacks will not be
 * fired until adjoining packets are received.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SegmentedDataStream implements DataStream {

  private static final Logger logger = Logger.getLogger(SegmentedDataStream.class.getName());
  private final byte PARTITION_MARKER = 0x00;
  private volatile long current = -1;
  private volatile long end = Long.MAX_VALUE;
  private final Map<Long, Data> packets = new HashMap<>();
  private final List<Object> observers = new ArrayList<>();
  private Exception exception;

  @Override
  public boolean isComplete() {
    return current == end || isCompletedExceptionally();
  }

  public boolean isCompletedExceptionally() {
    return exception != null;
  }

  public boolean hasEnd() {
    return end != Long.MAX_VALUE;
  }

  public long end() {
    return end;
  }

  public long current() {
    return current;
  }

  @Override
  public Data[] list() {
    return packets.values().toArray(new Data[]{});
  }

  @Override
  public Data assemble() throws StreamException {
    if (isCompletedExceptionally()) {
      throw new StreamException(exception);
    }

    return new DataAssembler(list(), PARTITION_MARKER).assemble();
  }

  @Override
  public void observe(OnData onData) {
    observers.add(onData);
  }

  @Override
  public void observe(OnComplete onComplete) {
    observers.add(onComplete);
  }

  @Override
  public void observe(OnException onException) {
    observers.add(onException);
  }

  @Override
  public void observe(OnTimeout onTimeout) {
    observers.add(onTimeout);
  }

  @Override
  public synchronized void onData(Interest interest, Data data) {
    logger.info("Data received: " + data.getName().toUri());
    long id;

    // no segment component
    if (!SegmentationHelper.isSegmented(data.getName(), PARTITION_MARKER) && packets.size() == 0) {
      id = 0;
      packets.put(id, data);

      // mark processing complete if the first packet has no segment component
      end = 0;
    } // with segment component
    else {
      Name.Component lastComponent = data.getName().get(-1);
      try {
        id = lastComponent.toNumberWithMarker(PARTITION_MARKER);
        packets.put(id, data);
      } catch (EncodingException ex) {
        onException(ex);
        return;
      }
    }

    if (hasFinalBlockId(data)) {
      try {
        end = data.getMetaInfo().getFinalBlockId().toNumberWithMarker(PARTITION_MARKER);
      } catch (EncodingException ex) {
        onException(ex);
      }
    }

    // call data observers
    if (isNextPacket(id)) {
      do {
        current++;
        assert (packets.containsKey(current));
        Data retrieved = packets.get(current);
        observersOfType(OnData.class).forEach((OnData cb) -> {
          cb.onData(interest, retrieved);
        });
      } while (hasNextPacket());
    }

    // call completion observers
    if (isComplete()) {
      onComplete();
    }
  }

  private boolean hasFinalBlockId(Data data) {
    return data.getMetaInfo().getFinalBlockId().getValue().size() > 0;
  }

  private boolean isNextPacket(long id) {
    return current + 1 == id;
  }

  private boolean hasNextPacket() {
    return packets.containsKey(current + 1);
  }

  @Override
  public synchronized void onComplete() {
    observersOfType(OnComplete.class).forEach((OnComplete cb) -> {
      cb.onComplete();
    });
  }

  @Override
  public synchronized void onTimeout(Interest interest) {
    observersOfType(OnTimeout.class).forEach((OnTimeout cb) -> {
      cb.onTimeout(interest);
    });
  }

  @Override
  public synchronized void onException(Exception exception) {
    this.exception = exception;

    observersOfType(OnException.class).forEach((OnException cb) -> {
      cb.onException(exception);
    });
  }

  private <T> List<T> observersOfType(Class<T> type) {
    return observers.stream().filter((Object o) -> type.isAssignableFrom(o.getClass())).map((o) -> (T) o).collect(Collectors.toList());
  }
}
