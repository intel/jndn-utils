/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
 */

package com.intel.jndn.utils.impl;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.intel.jndn.utils.impl.KeyChainFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
@RunWith(Parameterized.class)
public class KeyChainFactoryTest {
    private final KeyChain instance;

    public KeyChainFactoryTest(KeyChain instance) {
        this.instance = instance;
    }

    @Parameterized.Parameters
    public static Collection<KeyChain[]> data() throws Exception {
        Name identity = new Name("/test/identity");
        return Arrays.asList(new KeyChain[][]{{KeyChainFactory.configureKeyChain(identity)}, {KeyChainFactory.configureTestKeyChain(identity)}});
    }

    /*
     * getAnyCertificate has been removed, so this test might be deprecated.
     */
    @Test
    public void testKeyChainUsage() throws Exception {
        // check certificate
        Name certName = instance.getDefaultCertificateName();
        IdentityCertificate cert = instance.getCertificate(certName); // note that we have to use getAnyCertificate because the generated certificate is not valid
        assertNotNull(cert);

        // check signature
        byte[] signature = cert.getSignature().getSignature().getImmutableArray();
        assertTrue(signature.length > 0);

        // sign a data packet
        Data data = new Data(new Name("/a/b/c"));
        instance.signByIdentity(data);

        // verify that the packet was signed
        CountDownLatch latch = new CountDownLatch(1);
        instance.verifyData(data, data1 -> latch.countDown(), data1 -> {
        });
        latch.await(2, TimeUnit.SECONDS); // verification should be instantaneous... but just in case

        // check that the signature verified
        assertEquals(0, latch.getCount());
    }
}