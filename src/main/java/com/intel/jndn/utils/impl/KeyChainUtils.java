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

import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.jndn.security.identity.BasicIdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.IdentityStorage;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.identity.PrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;

/**
 * Utility class for creating NDN key chains necessary for signing packets.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class KeyChainUtils {
    /**
     * Build and configure an NDN {@link KeyChain} from the file system; looks in the ~/.ndn folder for keys and
     * identity SQLite DB.
     *
     * @param deviceName the identity of the device; this identity will be created if it does not exist
     * @return a configured {@link KeyChain}
     * @throws net.named_data.jndn.security.SecurityException if key chain creation fails
     */
    public static KeyChain configureKeyChain(Name deviceName) throws net.named_data.jndn.security.SecurityException {
        // access key chain in ~/.ndn; creates if necessary
        PrivateKeyStorage keyStorage = new FilePrivateKeyStorage();
        IdentityStorage identityStorage = new BasicIdentityStorage();
        KeyChain keyChain = new KeyChain(new IdentityManager(identityStorage, keyStorage),
                new SelfVerifyPolicyManager(identityStorage));

        // create keys, certs if necessary
        if (!identityStorage.doesIdentityExist(deviceName)) {
            Name certificateName = keyChain.createIdentityAndCertificate(deviceName);
            Name keyName = IdentityCertificate.certificateNameToPublicKeyName(certificateName);
            keyChain.setDefaultKeyForIdentity(keyName);
        }

        // set default identity
        keyChain.getIdentityManager().setDefaultIdentity(deviceName);

        return keyChain;
    }

    /**
     * Retrieve an in-memory test key chain. Similar to {@link #configureKeyChain(Name)} but does not interact with
     * file system; use this for testing.
     *
     * @param name the identity of the device; will be created because the key chain is in memory
     * @return a configured {@link KeyChain} with the created identity as default.
     * @throws net.named_data.jndn.security.SecurityException if key chain creation fails
     */
    public static KeyChain configureTestKeyChain(Name name) throws net.named_data.jndn.security.SecurityException {
        // access key chain in ~/.ndn; create if necessary
        PrivateKeyStorage keyStorage = new MemoryPrivateKeyStorage();
        IdentityStorage identityStorage = new MemoryIdentityStorage();
        KeyChain keyChain = new KeyChain(new IdentityManager(identityStorage, keyStorage),
                new SelfVerifyPolicyManager(identityStorage));

        // create keys, certs if necessary
        if (!identityStorage.doesIdentityExist(name)) {
            Name certName = keyChain.createIdentityAndCertificate(name);
            Name keyName = IdentityCertificate.certificateNameToPublicKeyName(certName);
            keyChain.setDefaultKeyForIdentity(keyName, name);
        }

        // set default identity
        keyChain.getIdentityManager().setDefaultIdentity(name);

        return keyChain;
    }
}