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
import org.apache.nifi.registry.authorization.Permissions;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.client.UserClient;
import org.apache.nifi.registry.client.impl.JerseyNiFiRegistryClient;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.authorization.CurrentUser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = NiFiRegistryTestApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.include=ITSecureFile")
@Import(SecureITClientConfiguration.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:db/clearDB.sql", "classpath:db/FlowsIT.sql"})
public class SecureNiFiRegistryClientIT extends IntegrationTestBase {

    static final Logger LOGGER = LoggerFactory.getLogger(SecureNiFiRegistryClientIT.class);

    private NiFiRegistryClient client;

    @Before
    public void setup() {
        final String baseUrl = createBaseURL();
        LOGGER.info("Using base url = " + baseUrl);

        final NiFiRegistryClientConfig clientConfig = createClientConfig(baseUrl);
        Assert.assertNotNull(clientConfig);

        final NiFiRegistryClient client = new JerseyNiFiRegistryClient.Builder()
                .config(clientConfig)
                .build();
        Assert.assertNotNull(client);
        this.client = client;
    }

    @After
    public void teardown() {
        try {
            client.close();
        } catch (Exception e) {

        }
    }

    @Test
    public void testGetAccessStatus() throws IOException, NiFiRegistryException {
        final UserClient userClient = client.getUserClient();
        final CurrentUser currentUser = userClient.getAccessStatus();
        Assert.assertEquals("CN=user1, OU=nifi", currentUser.getIdentity());
        Assert.assertFalse(currentUser.isAnonymous());
        Assert.assertNotNull(currentUser.getResourcePermissions());
        Permissions fullAccess = new Permissions().withCanRead(true).withCanWrite(true).withCanDelete(true);
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getAnyTopLevelResource());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getBuckets());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getTenants());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getPolicies());
        Assert.assertEquals(new Permissions().withCanWrite(true), currentUser.getResourcePermissions().getProxy());
    }

    @Test
    public void testCrudOperations() throws IOException, NiFiRegistryException {
        final Bucket bucket = new Bucket();
        bucket.setName("Bucket 1 " + System.currentTimeMillis());
        bucket.setDescription("This is bucket 1");

        final BucketClient bucketClient = client.getBucketClient();
        final Bucket createdBucket = bucketClient.create(bucket);
        Assert.assertNotNull(createdBucket);
        Assert.assertNotNull(createdBucket.getIdentifier());

        final List<Bucket> buckets = bucketClient.getAll();
        Assert.assertEquals(4, buckets.size());

        final VersionedFlow flow = new VersionedFlow();
        flow.setBucketIdentifier(createdBucket.getIdentifier());
        flow.setName("Flow 1 - " + System.currentTimeMillis());

        final FlowClient flowClient = client.getFlowClient();
        final VersionedFlow createdFlow = flowClient.create(flow);
        Assert.assertNotNull(createdFlow);
        Assert.assertNotNull(createdFlow.getIdentifier());

        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setBucketIdentifier(createdFlow.getBucketIdentifier());
        snapshotMetadata.setFlowIdentifier(createdFlow.getIdentifier());
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setComments("This is snapshot #1");

        final VersionedProcessGroup rootProcessGroup = new VersionedProcessGroup();
        rootProcessGroup.setIdentifier("root-pg");
        rootProcessGroup.setName("Root Process Group");

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(rootProcessGroup);

        final FlowSnapshotClient snapshotClient = client.getFlowSnapshotClient();
        final VersionedFlowSnapshot createdSnapshot = snapshotClient.create(snapshot);
        Assert.assertNotNull(createdSnapshot);
        Assert.assertEquals("CN=user1, OU=nifi", createdSnapshot.getSnapshotMetadata().getAuthor());
    }

    @Test
    public void testGetAccessStatusWithProxiedEntity() throws IOException, NiFiRegistryException {
        final String proxiedEntity = "user2";
        final UserClient userClient = client.getUserClient(proxiedEntity);
        final CurrentUser status = userClient.getAccessStatus();
        Assert.assertEquals("user2", status.getIdentity());
        Assert.assertFalse(status.isAnonymous());
    }

    @Test
    public void testCreatedBucketWithProxiedEntity() throws IOException, NiFiRegistryException {
        final String proxiedEntity = "user2";
        final BucketClient bucketClient = client.getBucketClient(proxiedEntity);

        final Bucket bucket = new Bucket();
        bucket.setName("Bucket 1");
        bucket.setDescription("This is bucket 1");

        try {
            bucketClient.create(bucket);
            Assert.fail("Shouldn't have been able to create a bucket");
        } catch (Exception e) {

        }
    }

    @Test
    public void testDirectFlowAccess() throws IOException {
        // this user shouldn't have access to anything
        final String proxiedEntity = "CN=no-access, OU=nifi";

        final FlowClient proxiedFlowClient = client.getFlowClient(proxiedEntity);
        final FlowSnapshotClient proxiedFlowSnapshotClient = client.getFlowSnapshotClient(proxiedEntity);

        try {
            proxiedFlowClient.get("1");
            Assert.fail("Shouldn't have been able to retrieve flow");
        } catch (NiFiRegistryException e) {
            Assert.assertTrue(e.getCause()  instanceof ForbiddenException);
        }

        try {
            proxiedFlowSnapshotClient.getLatest("1");
            Assert.fail("Shouldn't have been able to retrieve flow");
        } catch (NiFiRegistryException e) {
            Assert.assertTrue(e.getCause()  instanceof ForbiddenException);
        }

        try {
            proxiedFlowSnapshotClient.getLatestMetadata("1");
            Assert.fail("Shouldn't have been able to retrieve flow");
        } catch (NiFiRegistryException e) {
            Assert.assertTrue(e.getCause()  instanceof ForbiddenException);
        }

        try {
            proxiedFlowSnapshotClient.get("1", 1);
            Assert.fail("Shouldn't have been able to retrieve flow");
        } catch (NiFiRegistryException e) {
            Assert.assertTrue(e.getCause()  instanceof ForbiddenException);
        }

        try {
            proxiedFlowSnapshotClient.getSnapshotMetadata("1");
            Assert.fail("Shouldn't have been able to retrieve flow");
        } catch (NiFiRegistryException e) {
            Assert.assertTrue(e.getCause()  instanceof ForbiddenException);
        }

    }

}
