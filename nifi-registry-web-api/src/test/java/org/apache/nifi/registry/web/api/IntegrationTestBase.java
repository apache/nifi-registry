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

import org.apache.nifi.registry.NiFiRegistryApiTestApplication;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Deploy the Web API Application using an embedded Jetty Server for local integration testing, with the follow characteristics:
 *
 * - A NiFiRegistryProperties Bean has to be explicitly provided to the application context, i.e. using an inline @TestConfiguration class
 * - @DirtiesContext is providing that each (sub)class gets a fresh context
 * - The database is embed H2 using volatile (in-memory) persistence
 * - Custom SQL is clearing the DB before each test method by default, unless method overrides this behavior
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NiFiRegistryApiTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/clearDB.sql")
public abstract class IntegrationTestBase {

    private static final String CONTEXT_PATH = "/nifi-registry-api-test";

    @TestConfiguration
    public static class TestConfigurationClass {

        private NiFiRegistryProperties testProperties;

        @Bean
        public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
            JettyEmbeddedServletContainerFactory jettyContainerFactory = new JettyEmbeddedServletContainerFactory();
            jettyContainerFactory.setContextPath(CONTEXT_PATH);
            return jettyContainerFactory;
        }

    }

    @LocalServerPort
    int port;

    String createURL(String resourcePathRelativeToBaseUrl) {
        if (resourcePathRelativeToBaseUrl == null) {
            throw new IllegalArgumentException("Resource path cannot be null");
        }

        StringBuilder baseUri = new StringBuilder().append("http://localhost:").append(port).append(CONTEXT_PATH);
        if (!resourcePathRelativeToBaseUrl.startsWith("/")) {
            baseUri.append('/');
        }
        baseUri.append(resourcePathRelativeToBaseUrl);

        return baseUri.toString();
    }

}
