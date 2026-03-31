package com.icebrown.opencsc.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public final class BouncyCastleInitializer {

    private static volatile boolean initialized = false;

    private BouncyCastleInitializer() {
    }

    public static void ensureInstalled() {
        if (!initialized) {
            synchronized (BouncyCastleInitializer.class) {
                if (!initialized) {
                    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                        Security.addProvider(new BouncyCastleProvider());
                    }
                    initialized = true;
                }
            }
        }
    }
}
