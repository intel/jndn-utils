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
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.transport.UdpTransport;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Collect assorted methods to help with testing
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class TestHelper {

  public static Data retrieve(CompletableFuture<Data> future) {
    try {
      return future.get(4000, TimeUnit.MILLISECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static List<CompletableFuture<Data>> buildFutureSegments(Name name, int from, int to) {
    return buildSegments(name, from, to).stream()
            .map((d) -> CompletableFuture.completedFuture(d))
            .collect(Collectors.toList());
  }

  public static List<Data> buildSegments(Name name, int from, int to) {
    return IntStream.range(from, to).boxed()
            .map((i) -> buildData(new Name(name).appendSegment(i), i.toString(), to - 1))
            .collect(Collectors.toList());
  }

  public static Data buildData(Name name, String content) {
    Data data = new Data(name);
    data.setContent(new Blob(content));

    return data;
  }

  public static Data buildData(Name name, String content, int finalBlockId) {
    Data data = buildData(name, content);
    data.getMetaInfo().setFinalBlockId(Name.Component.fromNumberWithMarker(finalBlockId, 0x00));
    return data;
  }

  public static NdnEnvironment buildTestEnvironment(String host, int numFaces) throws SecurityException {
    NdnEnvironment environment = new NdnEnvironment();
    environment.executor = Executors.newScheduledThreadPool(numFaces);
    environment.keyChain = MockKeyChain.configure(new Name("/test/identity").append(buildRandomString(10)));

    for (int i = 0; i < numFaces; i++) {
      Face face = new Face(new UdpTransport(), new UdpTransport.ConnectionInfo(host));
      face.setCommandSigningInfo(environment.keyChain, environment.keyChain.getDefaultCertificateName());
      environment.executor.scheduleAtFixedRate(new EventProcessor(face), 0, 20, TimeUnit.MILLISECONDS);
      environment.faces.add(i, face);
    }

    return environment;
  }

  public static String buildRandomString(int length) { 
    return new String(buildRandomBytes(length));
  }
  
  public static byte[] buildRandomBytes(int length){
    byte[] bytes = new byte[length];
    new Random().nextBytes(bytes);
    return bytes;
  }

  public static class NdnEnvironment {

    public ScheduledExecutorService executor;
    public KeyChain keyChain;
    public final List<Face> faces = new ArrayList<>();
  }

  public static class EventProcessor implements Runnable {

    private final Face face;

    public EventProcessor(Face face) {
      this.face = face;
    }

    @Override
    public void run() {
      try {
        face.processEvents();
      } catch (IOException | EncodingException ex) {
        Logger.getLogger(TestHelper.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public static class TestCounter{
    public int count = 0;
  }
}
