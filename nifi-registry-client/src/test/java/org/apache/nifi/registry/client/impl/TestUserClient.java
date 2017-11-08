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
package org.apache.nifi.registry.client.impl;

import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.UserClient;
import org.apache.nifi.registry.model.authorization.AccessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUserClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestUserClient.class);

    public static void main(String[] args) {
        final NiFiRegistryClientConfig config = new NiFiRegistryClientConfig.Builder()
                .baseUrl("http://localhost:8080")
                .build();

        final NiFiRegistryClient client = new JerseyNiFiRegistryClient.Builder()
                .config(config)
                .build();

        final UserClient userClient = client.getUserClient();

        try {
            final AccessStatus status = userClient.getAccessStatus();
            System.out.println("Identity: " + status.getIdentity());
            System.out.println("Status: " + status.getStatus());
            System.out.println("Message: " + status.getMessage());

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                client.close();
            } catch (Exception e) {

            }
        }
    }

}
