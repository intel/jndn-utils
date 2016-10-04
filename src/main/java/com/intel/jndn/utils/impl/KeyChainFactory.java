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
public class KeyChainFactory {
    /**
     * Private constructor
     */
    private KeyChainFactory() {
    }
    
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