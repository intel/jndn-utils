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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class TestHelper {
  
  public static List<CompletableFuture<Data>> buildFutureSegments(Name name, int from, int to) {
    return buildSegments(name, from, to).stream()
            .map((d) -> CompletableFuture.completedFuture(d))
            .collect(Collectors.toList());
  }

  public static List<Data> buildSegments(Name name, int from, int to) {
    return IntStream.range(0, 10).boxed()
            .map((i) -> buildData(new Name(name).appendSegment(i), i.toString(), to - 1))
            .collect(Collectors.toList());
  }

  public static Data buildData(Name name, String content) {
    Data data = new Data(name);
    data.setContent(new Blob(content));

    return data;
  }
  
  public static Data buildData(Name name, String content, int finalBlockId){
    Data data = buildData(name, content);
    data.getMetaInfo().setFinalBlockId(Name.Component.fromNumberWithMarker(finalBlockId, 0x00));
    return data;
  }
}
