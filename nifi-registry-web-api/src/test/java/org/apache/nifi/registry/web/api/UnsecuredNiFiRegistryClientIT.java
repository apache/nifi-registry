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

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.ItemsClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.client.UserClient;
import org.apache.nifi.registry.client.impl.JerseyNiFiRegistryClient;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.model.authorization.CurrentUser;
import org.apache.nifi.registry.params.SortOrder;
import org.apache.nifi.registry.params.SortParameter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test all basic functionality of JerseyNiFiRegistryClient.
 */
public class UnsecuredNiFiRegistryClientIT extends UnsecuredITBase {

    static final Logger LOGGER = LoggerFactory.getLogger(UnsecuredNiFiRegistryClientIT.class);

    private NiFiRegistryClient client;

    @Before
    public void setup() {
        final String baseUrl = createBaseURL();
        LOGGER.info("Using base url = " + baseUrl);

        final NiFiRegistryClientConfig clientConfig = new NiFiRegistryClientConfig.Builder()
                .baseUrl(baseUrl)
                .build();

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
        final CurrentUser status = userClient.getAccessStatus();
        Assert.assertEquals("anonymous", status.getIdentity());
        Assert.assertTrue(status.isAnonymous());
    }

    @Test
    public void testNiFiRegistryClient() throws IOException, NiFiRegistryException {
        // ---------------------- TEST BUCKETS --------------------------//

        final BucketClient bucketClient = client.getBucketClient();

        // create buckets
        final int numBuckets = 10;
        final List<Bucket> createdBuckets = new ArrayList<>();

        for (int i=0; i < numBuckets; i++) {
            final Bucket createdBucket = createBucket(bucketClient, i);
            LOGGER.info("Created bucket # " + i + " with id " + createdBucket.getIdentifier());
            createdBuckets.add(createdBucket);
        }

        // get each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket retrievedBucket = bucketClient.get(bucket.getIdentifier());
            Assert.assertNotNull(retrievedBucket);
            LOGGER.info("Retrieved bucket " + retrievedBucket.getIdentifier());
        }

        //final Bucket nonExistentBucket = bucketClient.get("does-not-exist");
        //Assert.assertNull(nonExistentBucket);

        // get bucket fields
        final Fields bucketFields = bucketClient.getFields();
        Assert.assertNotNull(bucketFields);
        LOGGER.info("Retrieved bucket fields, size = " + bucketFields.getFields().size());
        Assert.assertTrue(bucketFields.getFields().size() > 0);

        // get all buckets
        final List<Bucket> allBuckets = bucketClient.getAll();
        LOGGER.info("Retrieved buckets, size = " + allBuckets.size());
        Assert.assertEquals(numBuckets, allBuckets.size());
        allBuckets.stream().forEach(b -> System.out.println("Retrieve bucket " + b.getIdentifier()));

        // update each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket bucketUpdate = new Bucket();
            bucketUpdate.setIdentifier(bucket.getIdentifier());
            bucketUpdate.setDescription(bucket.getDescription() + " UPDATE");

            final Bucket updatedBucket = bucketClient.update(bucketUpdate);
            Assert.assertNotNull(updatedBucket);
            LOGGER.info("Updated bucket " + updatedBucket.getIdentifier());
        }

        // ---------------------- TEST FLOWS --------------------------//

        final FlowClient flowClient = client.getFlowClient();

        // create flows
        final Bucket flowsBucket = createdBuckets.get(0);

        final VersionedFlow flow1 = createFlow(flowClient, flowsBucket, 1);
        LOGGER.info("Created flow # 1 with id " + flow1.getIdentifier());

        final VersionedFlow flow2 = createFlow(flowClient, flowsBucket, 2);
        LOGGER.info("Created flow # 2 with id " + flow2.getIdentifier());

        // get flow
        final VersionedFlow retrievedFlow1 = flowClient.get(flowsBucket.getIdentifier(), flow1.getIdentifier());
        Assert.assertNotNull(retrievedFlow1);
        LOGGER.info("Retrieved flow # 1 with id " + retrievedFlow1.getIdentifier());

        final VersionedFlow retrievedFlow2 = flowClient.get(flowsBucket.getIdentifier(), flow2.getIdentifier());
        Assert.assertNotNull(retrievedFlow2);
        LOGGER.info("Retrieved flow # 2 with id " + retrievedFlow2.getIdentifier());

        // update flows
        final VersionedFlow flow1Update = new VersionedFlow();
        flow1Update.setIdentifier(flow1.getIdentifier());
        flow1Update.setName(flow1.getName() + " UPDATED");

        final VersionedFlow updatedFlow1 = flowClient.update(flowsBucket.getIdentifier(), flow1Update);
        Assert.assertNotNull(updatedFlow1);
        LOGGER.info("Updated flow # 1 with id " + updatedFlow1.getIdentifier());

        // get flow fields
        final Fields flowFields = flowClient.getFields();
        Assert.assertNotNull(flowFields);
        LOGGER.info("Retrieved flow fields, size = " + flowFields.getFields().size());
        Assert.assertTrue(flowFields.getFields().size() > 0);

        // get flows in bucket
        final List<VersionedFlow> flowsInBucket = flowClient.getByBucket(flowsBucket.getIdentifier());
        Assert.assertNotNull(flowsInBucket);
        Assert.assertEquals(2, flowsInBucket.size());
        flowsInBucket.stream().forEach(f -> LOGGER.info("Flow in bucket, flow id " + f.getIdentifier()));

        // ---------------------- TEST SNAPSHOTS --------------------------//

        final FlowSnapshotClient snapshotClient = client.getFlowSnapshotClient();

        // create snapshots
        final VersionedFlow snapshotFlow = flow1;

        final VersionedFlowSnapshot snapshot1 = createSnapshot(snapshotClient, snapshotFlow, 1);
        LOGGER.info("Created snapshot # 1 with version " + snapshot1.getSnapshotMetadata().getVersion());

        final VersionedFlowSnapshot snapshot2 = createSnapshot(snapshotClient, snapshotFlow, 2);
        LOGGER.info("Created snapshot # 2 with version " + snapshot2.getSnapshotMetadata().getVersion());

        // get snapshot
        final VersionedFlowSnapshot retrievedSnapshot1 = snapshotClient.get(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier(), 1);
        Assert.assertNotNull(retrievedSnapshot1);
        Assert.assertFalse(retrievedSnapshot1.isLatest());
        LOGGER.info("Retrieved snapshot # 1 with version " + retrievedSnapshot1.getSnapshotMetadata().getVersion());

        final VersionedFlowSnapshot retrievedSnapshot2 = snapshotClient.get(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier(), 2);
        Assert.assertNotNull(retrievedSnapshot2);
        Assert.assertTrue(retrievedSnapshot2.isLatest());
        LOGGER.info("Retrieved snapshot # 2 with version " + retrievedSnapshot2.getSnapshotMetadata().getVersion());

        // get latest
        final VersionedFlowSnapshot retrievedSnapshotLatest = snapshotClient.getLatest(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedSnapshotLatest);
        Assert.assertEquals(snapshot2.getSnapshotMetadata().getVersion(), retrievedSnapshotLatest.getSnapshotMetadata().getVersion());
        Assert.assertTrue(retrievedSnapshotLatest.isLatest());
        LOGGER.info("Retrieved latest snapshot with version " + retrievedSnapshotLatest.getSnapshotMetadata().getVersion());

        // get metadata
        final List<VersionedFlowSnapshotMetadata> retrievedMetadata = snapshotClient.getSnapshotMetadata(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedMetadata);
        Assert.assertEquals(2, retrievedMetadata.size());
        Assert.assertEquals(2, retrievedMetadata.get(0).getVersion());
        Assert.assertEquals(1, retrievedMetadata.get(1).getVersion());
        retrievedMetadata.stream().forEach(s -> LOGGER.info("Retrieved snapshot metadata " + s.getVersion()));

        // get latest metadata
        final VersionedFlowSnapshotMetadata latestMetadata = snapshotClient.getLatestMetadata(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(latestMetadata);
        Assert.assertEquals(2, latestMetadata.getVersion());

        // get latest metadata that doesn't exist
        try {
            snapshotClient.getLatestMetadata(snapshotFlow.getBucketIdentifier(), "DOES-NOT-EXIST");
            Assert.fail("Should have thrown exception");
        } catch (NiFiRegistryException nfe) {
            Assert.assertEquals("Error retrieving latest snapshot metadata: Versioned flow does not exist for identifier DOES-NOT-EXIST", nfe.getMessage());
        }

        // ---------------------- TEST ITEMS --------------------------//

        final ItemsClient itemsClient = client.getItemsClient();

        // get fields
        final Fields itemFields = itemsClient.getFields();
        Assert.assertNotNull(itemFields.getFields());
        Assert.assertTrue(itemFields.getFields().size() > 0);

        // get all items
        final List<BucketItem> allItems = itemsClient.getAll();
        Assert.assertEquals(2, allItems.size());
        allItems.stream().forEach(i -> LOGGER.info("All items, item " + i.getIdentifier()));

        // get all items with sorting
        final SortParameter itemsSortParam = new SortParameter("created", SortOrder.ASC);
        final List<BucketItem> allItemsSorted = itemsClient.getAll(Arrays.asList(itemsSortParam));
        Assert.assertEquals(2, allItemsSorted.size());

        // get items for bucket
        final List<BucketItem> bucketItems = itemsClient.getByBucket(flowsBucket.getIdentifier());
        Assert.assertEquals(2, bucketItems.size());
        bucketItems.stream().forEach(i -> LOGGER.info("Items in bucket, item " + i.getIdentifier()));

        // get items for bucket with sorting
        final List<BucketItem> bucketItemsSorted = itemsClient.getByBucket(flowsBucket.getIdentifier(), Arrays.asList(itemsSortParam));
        Assert.assertEquals(2, bucketItemsSorted.size());

        // ---------------------- DELETE DATA --------------------------//

        final VersionedFlow deletedFlow1 = flowClient.delete(flowsBucket.getIdentifier(), flow1.getIdentifier());
        Assert.assertNotNull(deletedFlow1);
        LOGGER.info("Deleted flow " + deletedFlow1.getIdentifier());

        final VersionedFlow deletedFlow2 = flowClient.delete(flowsBucket.getIdentifier(), flow2.getIdentifier());
        Assert.assertNotNull(deletedFlow2);
        LOGGER.info("Deleted flow " + deletedFlow2.getIdentifier());

        // delete each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket deletedBucket = bucketClient.delete(bucket.getIdentifier());
            Assert.assertNotNull(deletedBucket);
            LOGGER.info("Deleted bucket " + deletedBucket.getIdentifier());
        }
        Assert.assertEquals(0, bucketClient.getAll().size());

        LOGGER.info("!!! SUCCESS !!!");

    }

    private static Bucket createBucket(BucketClient bucketClient, int num) throws IOException, NiFiRegistryException {
        final Bucket bucket = new Bucket();
        bucket.setName("Bucket #" + num);
        bucket.setDescription("This is bucket #" + num);
        return bucketClient.create(bucket);
    }

    private static VersionedFlow createFlow(FlowClient client, Bucket bucket, int num) throws IOException, NiFiRegistryException {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName(bucket.getName() + " Flow #" + num);
        versionedFlow.setDescription("This is " + bucket.getName() + " flow #" + num);
        versionedFlow.setBucketIdentifier(bucket.getIdentifier());
        return client.create(versionedFlow);
    }

    private static VersionedFlowSnapshot createSnapshot(FlowSnapshotClient client, VersionedFlow flow, int num) throws IOException, NiFiRegistryException {
        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setBucketIdentifier(flow.getBucketIdentifier());
        snapshotMetadata.setFlowIdentifier(flow.getIdentifier());
        snapshotMetadata.setVersion(num);
        snapshotMetadata.setComments("This is snapshot #" + num);

        final VersionedProcessGroup rootProcessGroup = new VersionedProcessGroup();
        rootProcessGroup.setIdentifier("root-pg");
        rootProcessGroup.setName("Root Process Group");

        final VersionedProcessGroup subProcessGroup = new VersionedProcessGroup();
        subProcessGroup.setIdentifier("sub-pg");
        subProcessGroup.setName("Sub Process Group");
        rootProcessGroup.getProcessGroups().add(subProcessGroup);

        final Map<String,String> processorProperties = new HashMap<>();
        processorProperties.put("Prop 1", "Val 1");
        processorProperties.put("Prop 2", "Val 2");

        final VersionedProcessor processor1 = new VersionedProcessor();
        processor1.setIdentifier("p1");
        processor1.setName("Processor 1");
        processor1.setProperties(processorProperties);

        final VersionedProcessor processor2 = new VersionedProcessor();
        processor2.setIdentifier("p2");
        processor2.setName("Processor 2");
        processor2.setProperties(processorProperties);

        subProcessGroup.getProcessors().add(processor1);
        subProcessGroup.getProcessors().add(processor2);

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(rootProcessGroup);

        return client.create(snapshot);
    }

}
