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