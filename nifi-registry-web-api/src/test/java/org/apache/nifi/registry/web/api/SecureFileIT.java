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

import org.apache.nifi.registry.NiFiRegistryTestApiApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * Deploy the Web API Application using an embedded Jetty Server for local integration testing, with the follow characteristics:
 *
 * - A NiFiRegistryProperties has to be explicitly provided to the ApplicationContext using a profile unique to this test suite.
 * - A NiFiRegistryClientConfig has been configured to create a client capable of completing two-way TLS
 * - The database is embed H2 using volatile (in-memory) persistence
 * - Custom SQL is clearing the DB before each test method by default, unless method overrides this behavior
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = NiFiRegistryTestApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.include=ITSecureFile")
@Import(SecureITClientConfiguration.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/clearDB.sql")
public class SecureFileIT extends IntegrationTestBase {

    @Test
    public void testAccessStatus() throws Exception {

        // Given: the client and server have been configured correctly for two-way TLS
        String expectedJson = "{" +
                "\"identity\":\"CN=user1, OU=nifi\"," +
                "\"status\":\"ACTIVE\"" +
                "}";

        // When: the /access endpoint is queried
        final Response response = client
                .target(createURL("access"))
                .request()
                .get(Response.class);

        // Then: the server returns 200 OK with the expected client identity
        assertEquals(200, response.getStatus());
        String actualJson = response.readEntity(String.class);
        JSONAssert.assertEquals(expectedJson, actualJson, false);
    }

    @Test
    public void testRetrieveResources() throws Exception {

        // Given: an empty registry returns these resources
        String expected = "[" +
                "{\"identifier\":\"/policies\",\"name\":\"Access Policies\"}," +
                "{\"identifier\":\"/tenants\",\"name\":\"Tenant\"}," +
                "{\"identifier\":\"/proxy\",\"name\":\"Proxy User Requests\"}," +
                "{\"identifier\":\"/resources\",\"name\":\"NiFi Resources\"}," +
                "{\"identifier\":\"/buckets\",\"name\":\"Buckets\"}" +
                "]";

        // When: the /resources endpoint is queried
        final String resourcesJson = client
                .target(createURL("resources"))
                .request()
                .get(String.class);

        // Then: the expected array of resources is returned
        JSONAssert.assertEquals(expected, resourcesJson, false);
    }

}
