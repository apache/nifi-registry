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
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.extension.ExtensionManager;
import org.apache.nifi.registry.model.authorization.AccessPolicy;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    private String adminAuthToken;

    @Before
    public void generateAuthToken() {
        final Form form = new Form()
                .param("username", "nifiadmin")
                .param("password", "password");
        final String token = client
                .target(createURL("access/token"))
                .request()
                .post(Entity.form(form), String.class);
        adminAuthToken = token;
    }

    @Test
    public void testTokenGenerationAndAccessStatus() throws Exception {

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
                .header("Authorization", "Bearer " + adminAuthToken)
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
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(String.class);

        // Then: the server returns a list of all users (see test-ldap-data.ldif)
        JSONAssert.assertEquals(expectedJson, groupsJson, false);
    }

    @Test
    public void testCreateTenantFails() throws Exception {

        // Given: the server has been configured with the LdapUserGroupProvider, which is non-configurable,
        //   and: the client wants to create a tenant
        Tenant tenant = new Tenant();
        tenant.setIdentity("new_tenant");

        // When: the POST /tenants/users endpoint is accessed
        final Response createUserResponse = client
                .target(createURL("tenants/users"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .post(Entity.entity(tenant, MediaType.APPLICATION_JSON_TYPE), Response.class);

        // Then: an error is returned
        assertEquals(409, createUserResponse.getStatus());

        // When: the POST /tenants/users endpoint is accessed
        final Response createUserGroupResponse = client
                .target(createURL("tenants/user-groups"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .post(Entity.entity(tenant, MediaType.APPLICATION_JSON_TYPE), Response.class);

        // Then: an error is returned because the UserGroupProvider is non-configurable
        assertEquals(409, createUserGroupResponse.getStatus());
    }

    @Test
    public void testAccessPolicyCreation() throws Exception {

        // Given: the server has been configured with an initial admin "nifiadmin" and a user with no accessPolicies "nobel"
        String nobelId = getTenantIdentifierByIdentity("nobel");
        String chemistsId = getTenantIdentifierByIdentity("chemists"); // a group containing user "nobel"
        final Form form = new Form()
                .param("username", "nobel")
                .param("password", "password");
        final String nobelAuthToken = client
                .target(createURL("access/token"))
                .request()
                .post(Entity.form(form), String.class);


        // When: nifiadmin creates a bucket
        final Bucket bucket = new Bucket();
        bucket.setName("Integration Test Bucket");
        bucket.setDescription("A bucket created by an integration test.");
        Response adminCreatesBucketResponse = client
                .target(createURL("buckets"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .post(Entity.entity(bucket, MediaType.APPLICATION_JSON), Response.class);

        // Then: the server returns a 200 OK
        assertEquals(200, adminCreatesBucketResponse.getStatus());
        Bucket createdBucket = adminCreatesBucketResponse.readEntity(Bucket.class);


        // When: user nobel initial queries /buckets
        final Bucket[] buckets1 = client
                .target(createURL("buckets"))
                .request()
                .header("Authorization", "Bearer " + nobelAuthToken)
                .get(Bucket[].class);

        // Then: an empty list is returned (nobel has no read access yet)
        assertNotNull(buckets1);
        assertEquals(0, buckets1.length);


        // When: nifiadmin grants read access on createdBucket to 'chemists' a group containing nobel
        Set<String> policiesToCleanup = new HashSet<>();
        AccessPolicy readPolicy = new AccessPolicy();
        readPolicy.setResource("/buckets/" + createdBucket.getIdentifier());
        readPolicy.setAction("read");
        readPolicy.addUserGroups(Arrays.asList(new Tenant(chemistsId, "chemists")));
        Response adminGrantsReadAccessResponse = client
                .target(createURL("policies"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .post(Entity.entity(readPolicy, MediaType.APPLICATION_JSON), Response.class);

        // Then: the server returns a 201 Created
        assertEquals(201, adminGrantsReadAccessResponse.getStatus());
        policiesToCleanup.add(adminGrantsReadAccessResponse.readEntity(AccessPolicy.class).getIdentifier());

        try {

            // When: user nobel re-queries /buckets
            final Bucket[] buckets2 = client
                    .target(createURL("buckets"))
                    .request()
                    .header("Authorization", "Bearer " + nobelAuthToken)
                    .get(Bucket[].class);

            // Then: the created bucket is now present
            assertNotNull(buckets2);
            assertEquals(1, buckets2.length);
            assertEquals(createdBucket.getIdentifier(), buckets2[0].getIdentifier());
            assertEquals(1, buckets2[0].getAuthorizedActions().size());
            assertTrue(buckets2[0].getAuthorizedActions().contains("read"));


            // When: nifiadmin grants write access on createdBucket to user 'nobel'
            AccessPolicy writePolicy = new AccessPolicy();
            writePolicy.setResource("/buckets/" + createdBucket.getIdentifier());
            writePolicy.setAction("write");
            writePolicy.addUsers(Arrays.asList(new Tenant(nobelId, "nobel")));
            Response adminGrantsWriteAccessResponse = client
                    .target(createURL("policies"))
                    .request()
                    .header("Authorization", "Bearer " + adminAuthToken)
                    .post(Entity.entity(writePolicy, MediaType.APPLICATION_JSON), Response.class);

            // Then: the server returns a 201 Created
            assertEquals(201, adminGrantsWriteAccessResponse.getStatus());
            policiesToCleanup.add(adminGrantsWriteAccessResponse.readEntity(AccessPolicy.class).getIdentifier());


            // When: user nobel re-queries /buckets
            final Bucket[] buckets3 = client
                    .target(createURL("buckets"))
                    .request()
                    .header("Authorization", "Bearer " + nobelAuthToken)
                    .get(Bucket[].class);

            // Then: the authorizedActions are updated
            assertNotNull(buckets3);
            assertEquals(1, buckets3.length);
            assertEquals(createdBucket.getIdentifier(), buckets3[0].getIdentifier());
            assertEquals(2, buckets3[0].getAuthorizedActions().size());
            assertTrue(buckets3[0].getAuthorizedActions().contains("read"));
            assertTrue(buckets3[0].getAuthorizedActions().contains("write"));

        } finally {
            // Teardown: delete the policy we made in case other tests assume they are starting with no policies
            for (String policyId : policiesToCleanup) {
                Response adminDeletesPolicyResponse = client
                        .target(createURL("policies/" + policyId))
                        .request()
                        .header("Authorization", "Bearer " + adminAuthToken)
                        .delete(Response.class);
                assertEquals(200, adminDeletesPolicyResponse.getStatus());
            }
        }

    }

    /** A helper method to lookup identifiers for tenant identities
     *
     * @param tenantIdentity - the identity to lookup
     * @return A string containing the identifier of the tenant, or null if the tenant identity is not found.
     */
    private String getTenantIdentifierByIdentity(String tenantIdentity) {

        final Tenant[] users = client
                .target(createURL("tenants/users"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(Tenant[].class);

        final Tenant[] groups = client
                .target(createURL("tenants/user-groups"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(Tenant[].class);


        final Tenant matchedTenant = Stream.concat(Arrays.stream(users), Arrays.stream(groups))
                .filter(tenant -> tenant.getIdentity().equalsIgnoreCase(tenantIdentity))
                .findFirst()
                .orElse(null);

        return matchedTenant != null ? matchedTenant.getIdentifier() : null;
    }

}
