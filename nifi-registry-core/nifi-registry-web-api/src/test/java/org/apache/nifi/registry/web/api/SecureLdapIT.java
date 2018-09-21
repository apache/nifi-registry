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
import org.apache.nifi.registry.authorization.AccessPolicy;
import org.apache.nifi.registry.authorization.AccessPolicySummary;
import org.apache.nifi.registry.authorization.CurrentUser;
import org.apache.nifi.registry.authorization.Permissions;
import org.apache.nifi.registry.authorization.Tenant;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.extension.ExtensionManager;
import org.apache.nifi.registry.properties.AESSensitivePropertyProvider;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.properties.SensitivePropertyProvider;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.AuthorizerFactory;
import org.apache.nifi.registry.security.crypto.BootstrapFileCryptoKeyProvider;
import org.apache.nifi.registry.security.crypto.CryptoKeyProvider;
import org.junit.After;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Deploy the Web API Application using an embedded Jetty Server for local integration testing, with the follow characteristics:
 *
 * - A NiFiRegistryProperties has to be explicitly provided to the ApplicationContext using a profile unique to this test suite.
 * - A NiFiRegistryClientConfig has been configured to create a client capable of completing one-way TLS
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

    private static final String tokenLoginPath = "access/token/login";
    private static final String tokenIdentityProviderPath = "access/token/identity-provider";

    @TestConfiguration
    @Profile("ITSecureLdap")
    public static class LdapTestConfiguration {

        static AuthorizerFactory authorizerFactory;

        @Primary
        @Bean
        @DependsOn({"directoryServer"}) // Can't load LdapUserGroupProvider until the embedded LDAP server, which creates the "directoryServer" bean, is running
        public static Authorizer getAuthorizer(@Autowired NiFiRegistryProperties properties, ExtensionManager extensionManager) throws Exception {
            if (authorizerFactory == null) {
                authorizerFactory = new AuthorizerFactory(properties, extensionManager, sensitivePropertyProvider());
            }
            return authorizerFactory.getAuthorizer();
        }

        @Primary
        @Bean
        public static SensitivePropertyProvider sensitivePropertyProvider() throws Exception {
            return new AESSensitivePropertyProvider(getNiFiRegistryMasterKeyProvider().getKey());
        }

        private static CryptoKeyProvider getNiFiRegistryMasterKeyProvider() {
            return new BootstrapFileCryptoKeyProvider("src/test/resources/conf/secure-ldap/bootstrap.conf");
        }

    }

    private String adminAuthToken;
    private List<AccessPolicy> beforeTestAccessPoliciesSnapshot;

    @Before
    public void setup() {
        final String basicAuthCredentials = encodeCredentialsForBasicAuth("nifiadmin", "password");
        final String token = client
                .target(createURL(tokenIdentityProviderPath))
                .request()
                .header("Authorization", "Basic " + basicAuthCredentials)
                .post(null, String.class);
        adminAuthToken = token;

        beforeTestAccessPoliciesSnapshot = createAccessPoliciesSnapshot();
    }

    @After
    public void cleanup() {
        restoreAccessPoliciesSnapshot(beforeTestAccessPoliciesSnapshot);
    }

    @Test
    public void testTokenGenerationAndAccessStatus() throws Exception {

        // Note: this test intentionally does not use the token generated
        // for nifiadmin by the @Before method

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJwtPayloadJson = "{" +
                "\"sub\":\"nobel\"," +
                "\"preferred_username\":\"nobel\"," +
                "\"iss\":\"LdapIdentityProvider\"" +
                "}";
        String expectedAccessStatusJson = "{" +
                "\"identity\":\"nobel\"," +
                "\"anonymous\":false" +
                "}";

        // When: the /access/token/login endpoint is queried
        final String basicAuthCredentials = encodeCredentialsForBasicAuth("nobel", "password");
        final Response tokenResponse = client
                .target(createURL(tokenIdentityProviderPath))
                .request()
                .header("Authorization", "Basic " + basicAuthCredentials)
                .post(null, Response.class);

        // Then: the server returns 200 OK with an access token
        assertEquals(201, tokenResponse.getStatus());
        String token = tokenResponse.readEntity(String.class);
        assertTrue(StringUtils.isNotEmpty(token));
        String[] jwtParts = token.split("\\.");
        assertEquals(3, jwtParts.length);
        String jwtPayload = new String(Base64.getDecoder().decode(jwtParts[1]), "UTF-8");
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
    public void testTokenGenerationWithIdentityProvider() throws Exception {

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJwtPayloadJson = "{" +
                "\"sub\":\"nobel\"," +
                "\"preferred_username\":\"nobel\"," +
                "\"iss\":\"LdapIdentityProvider\"," +
                "\"aud\":\"LdapIdentityProvider\"" +
                "}";
        String expectedAccessStatusJson = "{" +
                "\"identity\":\"nobel\"," +
                "\"anonymous\":false" +
                "}";

        // When: the /access/token/identity-provider endpoint is queried
        final String basicAuthCredentials = encodeCredentialsForBasicAuth("nobel", "password");
        final Response tokenResponse = client
                .target(createURL(tokenIdentityProviderPath))
                .request()
                .header("Authorization", "Basic " + basicAuthCredentials)
                .post(null, Response.class);

        // Then: the server returns 200 OK with an access token
        assertEquals(201, tokenResponse.getStatus());
        String token = tokenResponse.readEntity(String.class);
        assertTrue(StringUtils.isNotEmpty(token));
        String[] jwtParts = token.split("\\.");
        assertEquals(3, jwtParts.length);
        String jwtPayload = new String(Base64.getDecoder().decode(jwtParts[1]), "UTF-8");
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
    public void testGetCurrentUserFailsForAnonymous() throws Exception {

        // Given: the client is connected to an unsecured NiFi Registry

        // When: the /access endpoint is queried with no credentials
        final Response response = client
                .target(createURL("/access"))
                .request()
                .get(Response.class);

        // Then: the server returns a 200 OK with the expected current user
        assertEquals(401, response.getStatus());

    }

    @Test
    public void testGetCurrentUser() throws Exception {

        // Given: the client is connected to an unsecured NiFi Registry
        String expectedJson = "{" +
                "\"identity\":\"nifiadmin\"," +
                "\"anonymous\":false," +
                "\"resourcePermissions\":{" +
                "\"anyTopLevelResource\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"buckets\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"tenants\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"policies\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"proxy\":{\"canRead\":false,\"canWrite\":true,\"canDelete\":false}}" +
                "}";

        // When: the /access endpoint is queried using a JWT for the nifiadmin LDAP user
        final Response response = client
                .target(createURL("/access"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(Response.class);

        // Then: the server returns a 200 OK with the expected current user
        assertEquals(200, response.getStatus());
        String actualJson = response.readEntity(String.class);
        JSONAssert.assertEquals(expectedJson, actualJson, false);

    }

    @Test
    public void testUsers() throws Exception {

        // Given: the client and server have been configured correctly for LDAP authentication
        String expectedJson = "[" +
                "{\"identity\":\"nifiadmin\",\"userGroups\":[],\"configurable\":false," +
                    "\"resourcePermissions\":{" +
                    "\"anyTopLevelResource\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                    "\"buckets\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                    "\"tenants\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                    "\"policies\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                    "\"proxy\":{\"canRead\":false,\"canWrite\":true,\"canDelete\":false}}}," +
                "{\"identity\":\"euler\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"euclid\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"boyle\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"newton\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"riemann\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"gauss\",\"userGroups\":[{\"identity\":\"mathematicians\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"galileo\",\"userGroups\":[{\"identity\":\"scientists\"},{\"identity\":\"italians\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"nobel\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"pasteur\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"tesla\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"nogroup\",\"userGroups\":[],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"einstein\",\"userGroups\":[{\"identity\":\"scientists\"}],\"accessPolicies\":[],\"configurable\":false}," +
                "{\"identity\":\"curie\",\"userGroups\":[{\"identity\":\"chemists\"}],\"accessPolicies\":[],\"configurable\":false}]";

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
                "{" +
                    "\"identity\":\"chemists\"," +
                    "\"users\":[{\"identity\":\"pasteur\"},{\"identity\":\"boyle\"},{\"identity\":\"curie\"},{\"identity\":\"nobel\"}]," +
                    "\"accessPolicies\":[]," +
                    "\"configurable\":false" +
                "}," +
                "{" +
                    "\"identity\":\"mathematicians\"," +
                    "\"users\":[{\"identity\":\"gauss\"},{\"identity\":\"euclid\"},{\"identity\":\"riemann\"},{\"identity\":\"euler\"}]," +
                    "\"accessPolicies\":[]," +
                    "\"configurable\":false" +
                "}," +
                "{" +
                    "\"identity\":\"scientists\"," +
                    "\"users\":[{\"identity\":\"einstein\"},{\"identity\":\"tesla\"},{\"identity\":\"newton\"},{\"identity\":\"galileo\"}]," +
                    "\"accessPolicies\":[]," +
                    "\"configurable\":false" +
                "}," +
                "{" +
                    "\"identity\":\"italians\"," +
                    "\"users\":[{\"identity\":\"galileo\"}]," +
                    "\"accessPolicies\":[]," +
                    "\"configurable\":false" +
                "}]";

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

        final String basicAuthCredentials = encodeCredentialsForBasicAuth("nobel", "password");
        final String nobelAuthToken = client
                .target(createURL(tokenIdentityProviderPath))
                .request()
                .header("Authorization", "Basic " + basicAuthCredentials)
                .post(null, String.class);

        // When: user nobel re-checks top-level permissions
        final CurrentUser currentUser = client
                .target(createURL("/access"))
                .request()
                .header("Authorization", "Bearer " + nobelAuthToken)
                .get(CurrentUser.class);

        // Then: 200 OK is returned indicating user has access to no top-level resources
        assertEquals(new Permissions(), currentUser.getResourcePermissions().getBuckets());
        assertEquals(new Permissions(), currentUser.getResourcePermissions().getTenants());
        assertEquals(new Permissions(), currentUser.getResourcePermissions().getPolicies());
        assertEquals(new Permissions(), currentUser.getResourcePermissions().getProxy());

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


        // When: nifiadmin tries to list all buckets
        final Bucket[] adminBuckets = client
                .target(createURL("buckets"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(Bucket[].class);

        // Then: the full list is returned (verifies that per-bucket access policies are additive to base /buckets policy)
        assertNotNull(adminBuckets);
        assertEquals(1, adminBuckets.length);
        assertEquals(createdBucket.getIdentifier(), adminBuckets[0].getIdentifier());
        assertEquals(new Permissions().withCanRead(true).withCanWrite(true).withCanDelete(true), adminBuckets[0].getPermissions());


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
        assertEquals(new Permissions().withCanRead(true), buckets2[0].getPermissions());


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
        assertEquals(new Permissions().withCanRead(true).withCanWrite(true), buckets3[0].getPermissions());

    }

    /** A helper method to lookup identifiers for tenant identities using the REST API
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

    /** A helper method to lookup access policies
     *
     * @return A string containing the identifier of the policy, or null if the policy identity is not found.
     */
    private AccessPolicy getPolicyByResourceAction(String action, String resource) {

        final AccessPolicySummary[] policies = client
                .target(createURL("policies"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(AccessPolicySummary[].class);

        final AccessPolicySummary matchedPolicy = Arrays.stream(policies)
                .filter(p -> p.getAction().equalsIgnoreCase(action) && p.getResource().equalsIgnoreCase(resource))
                .findFirst()
                .orElse(null);

        if (matchedPolicy == null) {
            return null;
        }

        String policyId = matchedPolicy.getIdentifier();

        final AccessPolicy policy = client
                .target(createURL("policies/" + policyId))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(AccessPolicy.class);

        return policy;
    }

    private List<AccessPolicy> createAccessPoliciesSnapshot() {

        final AccessPolicySummary[] policySummaries = client
                .target(createURL("policies"))
                .request()
                .header("Authorization", "Bearer " + adminAuthToken)
                .get(AccessPolicySummary[].class);

        final List<AccessPolicy> policies = new ArrayList<>(policySummaries.length);
        for (AccessPolicySummary s : policySummaries) {
            AccessPolicy policy = client
                    .target(createURL("policies/" + s.getIdentifier()))
                    .request()
                    .header("Authorization", "Bearer " + adminAuthToken)
                    .get(AccessPolicy.class);
            policies.add(policy);
        }

        return policies;
    }

    private void restoreAccessPoliciesSnapshot(List<AccessPolicy> accessPoliciesSnapshot) {

        List<AccessPolicy> currentAccessPolicies = createAccessPoliciesSnapshot();

        Set<String> policiesToRestore = accessPoliciesSnapshot.stream()
                .map(AccessPolicy::getIdentifier)
                .collect(Collectors.toSet());

        Set<String> policiesToDelete = currentAccessPolicies.stream()
                .filter(p -> !policiesToRestore.contains(p.getIdentifier()))
                .map(AccessPolicy::getIdentifier)
                .collect(Collectors.toSet());

        for (AccessPolicy originalPolicy : accessPoliciesSnapshot) {

            Response getCurrentPolicy = client
                    .target(createURL("policies/" + originalPolicy.getIdentifier()))
                    .request()
                    .header("Authorization", "Bearer " + adminAuthToken)
                    .get(Response.class);

            if (getCurrentPolicy.getStatus() == 200) {
                // update policy to match original
                client.target(createURL("policies/" + originalPolicy.getIdentifier()))
                        .request()
                        .header("Authorization", "Bearer " + adminAuthToken)
                        .put(Entity.entity(originalPolicy, MediaType.APPLICATION_JSON));
            } else {
                // post the original policy
                client.target(createURL("policies"))
                        .request()
                        .header("Authorization", "Bearer " + adminAuthToken)
                        .post(Entity.entity(originalPolicy, MediaType.APPLICATION_JSON));
            }

        }

        for (String id : policiesToDelete) {
            try {
                client.target(createURL("policies/" + id))
                        .request()
                        .header("Authorization", "Bearer " + adminAuthToken)
                        .delete();
            } catch (Exception e) {
                // do nothing
            }
        }

    }

    private static Form encodeCredentialsForURLFormParams(String username, String password) {
        return new Form()
                .param("username", username)
                .param("password", password);
    }

    private static String encodeCredentialsForBasicAuth(String username, String password) {
        final String credentials = username + ":" + password;
        final String base64credentials =  new String(Base64.getEncoder().encode(credentials.getBytes(Charset.forName("UTF-8"))));
        return base64credentials;
    }
}
