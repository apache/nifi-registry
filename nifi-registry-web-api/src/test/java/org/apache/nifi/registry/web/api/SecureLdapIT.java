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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.SecureLdapTestApiApplication;
import org.apache.nifi.registry.extension.ExtensionManager;
import org.apache.nifi.registry.model.authorization.Tenant;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.AuthorizerFactory;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        classes = SecureLdapTestApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.include=ITSecureLdap")
@Import(SecureITClientConfiguration.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/clearDB.sql")
public class SecureLdapIT extends IntegrationTestBase {

    @TestConfiguration
    @Profile("ITSecureLdap")
    public static class LdapTestConfiguration {

        static AuthorizerFactory af;

        @Primary
        @Bean
        @DependsOn({"directoryServer"}) // Can't load LdapUserGroupProvider until the embedded LDAP server, which creates the "directoryServer" bean, is running
        public static Authorizer getAuthorizer(@Autowired NiFiRegistryProperties properties, ExtensionManager extensionManager) {
            if (af == null) {
                af = new AuthorizerFactory(properties, extensionManager);
            }
            return af.getAuthorizer();
        }

    }

    private String authToken;

    @Before
    public void generateAuthToken() {
        final Form form = new Form()
                .param("username", "nifiadmin")
                .param("password", "password");
        final String token = client
                .target(createURL("access/token"))
                .request()
                .post(Entity.form(form), String.class);
        authToken = token;
    }

    @Test
    public void testTokenGeneration() throws Exception {

        // Note: this test intentionally does not use the token generated
        // for nifiadmin by the @Before method

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJwtPayloadJson = "{" +
                "\"sub\":\"nobel\"," +
                "\"preferred_username\":\"nobel\"," +
                "\"iss\":\"LdapIdentityProvider\"," +
                "\"aud\":\"LdapIdentityProvider\"" +
                "}";
        String expectedAccessStatusJson = "{" +
                "\"identity\":\"nobel\"," +
                "\"status\":\"ACTIVE\"" +
                "}";

        // When: the /access/token endpoint is queried
        final Form form = new Form()
                .param("username", "nobel")
                .param("password", "password");
        final Response tokenResponse = client
                .target(createURL("access/token"))
                .request()
                .post(Entity.form(form), Response.class);

        // Then: the server returns 200 OK with an access token
        assertEquals(201, tokenResponse.getStatus());
        String token = tokenResponse.readEntity(String.class);
        assertTrue(StringUtils.isNotEmpty(token));
        String[] jwtParts = token.split("\\.");
        assertEquals(3, jwtParts.length);
        String jwtPayload = new String(Base64.decodeBase64(jwtParts[1]), "UTF-8");
        JSONAssert.assertEquals(expectedJwtPayloadJson, jwtPayload, false);

        // When: the token is returned in the Authorization header
        final Response accessResponse = client
                .target(createURL("access"))
                .request()
                .header("Authorization", "Bearer " + token)
                .get(Response.class);

        // Then: the server acknowledges the client has access
        assertEquals(200, accessResponse.getStatus());
        String accessStatus = accessResponse.readEntity(String.class);
        JSONAssert.assertEquals(expectedAccessStatusJson, accessStatus, false);

    }

    @Test
    public void testUsers() throws Exception {

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJson = "[" +
                "{\"identity\":\"nifiadmin\",\"userGroups\":[]}," +
                "{\"identity\":\"euler\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"euclid\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"boyle\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"newton\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"riemann\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"gauss\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"galileo\",\"userGroups\":[{\"identity\":\"scientists\"},{\"identity\":\"italians\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"nobel\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"pasteur\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"tesla\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"nogroup\",\"userGroups\":[],\"accessPolicies\":[]}," +
                "{\"identity\":\"einstein\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"curie\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[]}]";

        // When: the /tenants/users endpoint is queried
        final String usersJson = client
                .target(createURL("tenants/users"))
                .request()
                .header("Authorization", "Bearer " + authToken)
                .get(String.class);

        // Then: the server returns a list of all users (see test-ldap-data.ldif)
        JSONAssert.assertEquals(expectedJson, usersJson, false);
    }

    @Test
    public void testUserGroups() throws Exception {

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJson = "[" +
                "{\"identity\":\"chemists\",\"users\":[{\"identity\":\"pasteur\"},{\"identity\":\"boyle\"},{\"identity\":\"curie\"},{\"identity\":\"nobel\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"mathematicians\",\"users\":[{\"identity\":\"gauss\"},{\"identity\":\"euclid\"},{\"identity\":\"riemann\"},{\"identity\":\"euler\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"scientists\",\"users\":[{\"identity\":\"einstein\"},{\"identity\":\"tesla\"},{\"identity\":\"newton\"},{\"identity\":\"galileo\"}],\"accessPolicies\":[]}," +
                "{\"identity\":\"italians\",\"users\":[{\"identity\":\"galileo\"}],\"accessPolicies\":[]}]";

        // When: the /tenants/users endpoint is queried
        final String groupsJson = client
                .target(createURL("tenants/user-groups"))
                .request()
                .header("Authorization", "Bearer " + authToken)
                .get(String.class);

        // Then: the server returns a list of all users (see test-ldap-data.ldif)
        JSONAssert.assertEquals(expectedJson, groupsJson, false);
    }


    public void testCreateTenantFails() throws Exception {

        // Given: the server has been configured with the LdapUserGroupProvider, which is non-configurable,
        //   and: the client wants to create a tenant
        Tenant tenant = new Tenant();
        tenant.setIdentity("new_tenant");

        // When: the POST /tenants/users endpoint is accessed
        final Response createUserResponse = client
                .target(createURL("tenants/users"))
                .request()
                .header("Authorization", "Bearer " + authToken)
                .post(Entity.entity(tenant, MediaType.APPLICATION_JSON_TYPE), Response.class);

        // Then: an error is returned
        assertEquals(405, createUserResponse.getStatus());

        // When: the POST /tenants/users endpoint is accessed
        final Response createUserGroupResponse = client
                .target(createURL("tenants/user-groups"))
                .request()
                .header("Authorization", "Bearer " + authToken)
                .post(Entity.entity(tenant, MediaType.APPLICATION_JSON_TYPE), Response.class);

        // Then: an error is returned because the UserGroupProvider is non-configurable
        assertEquals(405, createUserGroupResponse.getStatus());
    }

}
