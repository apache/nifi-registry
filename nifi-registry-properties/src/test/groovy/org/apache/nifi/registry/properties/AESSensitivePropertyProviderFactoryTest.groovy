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
package org.apache.nifi.registry.properties

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.crypto.Cipher
import java.security.Security

@RunWith(JUnit4.class)
class AESSensitivePropertyProviderFactoryTest extends GroovyTestCase {
    private static final Logger logger = LoggerFactory.getLogger(AESSensitivePropertyProviderFactoryTest.class)

    private static final String KEY_HEX_128 = "0123456789ABCDEFFEDCBA9876543210"
    private static final String KEY_HEX_256 = KEY_HEX_128 * 2

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.addProvider(new BouncyCastleProvider())

        logger.metaClass.methodMissing = { String name, args ->
            logger.info("[${name?.toUpperCase()}] ${(args as List).join(" ")}")
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testShouldGetProviderWithKey() throws Exception {
        // Arrange
        SensitivePropertyProviderFactory factory = new AESSensitivePropertyProviderFactory(KEY_HEX_128)

        // Act
        SensitivePropertyProvider provider = factory.getProvider()

        // Assert
        assert provider instanceof AESSensitivePropertyProvider
        assert provider.@key
        assert provider.@cipher
    }

    @Test
    public void testShouldGetProviderWith256BitKey() throws Exception {
        // Arrange
        Assume.assumeTrue("JCE unlimited strength crypto policy must be installed for this test", Cipher.getMaxAllowedKeyLength("AES") > 128)
        SensitivePropertyProviderFactory factory = new AESSensitivePropertyProviderFactory(KEY_HEX_256)

        // Act
        SensitivePropertyProvider provider = factory.getProvider()

        // Assert
        assert provider instanceof AESSensitivePropertyProvider
        assert provider.@key
        assert provider.@cipher
    }
}