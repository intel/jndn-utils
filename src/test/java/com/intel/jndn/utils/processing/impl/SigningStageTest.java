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
package com.intel.jndn.utils.processing.impl;

import com.intel.jndn.mock.MockKeyChain;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test {@link SigningStage}.
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SigningStageTest {

  private KeyChain keyChain;
  private SigningStage instance;

  public SigningStageTest() throws SecurityException {
    keyChain = MockKeyChain.configure(new Name("/test/signer"));
    instance = new SigningStage(keyChain);
  }

  /**
   * Test of process method, of class SigningStage.
   */
  @Test
  public void testProcess() throws Exception {
    Data data = new Data(new Name("/test/packet"));
    data.setContent(new Blob("....."));
    Data result = instance.process(data);
    assertTrue(result.getSignature().getSignature().size() > 0);
  }

  /**
   * Test of process method, of class SigningStage.
   */
  @Test(expected = Exception.class)
  public void testProcessFailure() throws Exception {
    SigningStage stage = new SigningStage(keyChain, null); // no default certificate set
    Data data = new Data(new Name("/test/packet"));
    data.setContent(new Blob("....."));
    stage.process(data);
  }

}
