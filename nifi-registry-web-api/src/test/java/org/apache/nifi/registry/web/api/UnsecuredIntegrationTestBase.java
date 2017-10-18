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
package org.apache.nifi.registry.web.api;

import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.FileReader;
import java.io.IOException;

/**
 * Extends the base integration and provide a NiFiRegistryProperties Bean that configures the NiFi Registry to be unsecured
 */
public class UnsecuredIntegrationTestBase extends IntegrationTestBase {

    @TestConfiguration
    public static class TestConfigurationClass {

        private NiFiRegistryProperties testProperties;

        @Bean
        public synchronized NiFiRegistryProperties getNiFiRegistryProperties() {
            if (testProperties == null) {
                testProperties = new NiFiRegistryProperties();
                try (final FileReader reader = new FileReader("src/test/resources/conf/unsecured/nifi-registry.properties")) {
                    testProperties.load(reader);
                } catch (final IOException ioe) {
                    throw new RuntimeException("Unable to load properties: " + ioe, ioe);
                }
            }
            return testProperties;
        }
    }

}
