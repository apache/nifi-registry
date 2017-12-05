/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.security.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link CryptoKeyProvider} that loads the key from disk every time it is needed.
 *
 * The persistence-backing of the key is in the bootstrap.conf file, which must be provided to the
 * constructor of this class.
 *
 * As key access for sensitive value decryption is only used a few times during server initialization,
 * this implementation trades efficiency for security by only keeping the key in memory with an
 * in-scope reference for a brief period of time (assuming callers do not maintain an in-scope reference).
 *
 * @see CryptoKeyProvider
 */
public final class VolatileCryptoKeyProvider implements CryptoKeyProvider {

    private static final Logger logger = LoggerFactory.getLogger(VolatileCryptoKeyProvider.class);

    private static final VolatileCryptoKeyProvider EMPTY = new VolatileCryptoKeyProvider(EMPTY_KEY);

    private final String key;

    /**
     * Construct a new instance with an in-memory key.
     *
     * @param key The contents of the key in hexadecimal format.
     *            Must not be null.
     */
    public VolatileCryptoKeyProvider(final String key) {
        if (key == null) {
            throw new IllegalArgumentException(VolatileCryptoKeyProvider.class.getSimpleName() + " cannot be initialized with a null key.");
        }
        this.key = key;
    }

    @Override
    public String getKey() throws MissingCryptoKeyException {
        if (key == null) {
            throw new MissingCryptoKeyException("Key is null.");
        }
        return key;
    }

    @Override
    public String toString() {
        return "VolatileCryptoKeyProvider{" +
                "key='[PROTECTED]'" +
                '}';
    }
}
