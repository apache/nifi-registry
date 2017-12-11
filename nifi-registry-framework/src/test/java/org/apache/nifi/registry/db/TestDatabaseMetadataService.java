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
package org.apache.nifi.registry.db;

import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.service.MetadataService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestDatabaseMetadataService extends DatabaseBaseTest {

    @Autowired
    private MetadataService metadataService;

    //----------------- Buckets ---------------------------------

    @Test
    public void testCreateAndGetBucket() {
        final BucketEntity b = new BucketEntity();
        b.setId("testBucketId");
        b.setName("testBucketName");
        b.setDescription("testBucketDesc");
        b.setCreated(new Date());

        metadataService.createBucket(b);

        final BucketEntity createdBucket = metadataService.getBucketById(b.getId());
        assertNotNull(createdBucket);
        assertEquals(b.getId(), createdBucket.getId());
        assertEquals(b.getName(), createdBucket.getName());
        assertEquals(b.getDescription(), createdBucket.getDescription());
        assertEquals(b.getCreated(), createdBucket.getCreated());
    }

    @Test
    public void testGetBucketDoesNotExist() {
        final BucketEntity bucket = metadataService.getBucketById("does-not-exist");
        assertNull(bucket);
    }

    @Test
    public void testGetBucketsByName() {
        final List<BucketEntity> buckets = metadataService.getBucketsByName("Bucket 1");
        assertNotNull(buckets);
        assertEquals(1, buckets.size());
        assertEquals("Bucket 1", buckets.get(0).getName());
    }

    @Test
    public void testGetBucketsByNameNoneFound() {
        final List<BucketEntity> buckets = metadataService.getBucketsByName("Bucket XYZ");
        assertNotNull(buckets);
        assertEquals(0, buckets.size());
    }

    @Test
    public void testUpdateBucket() {
        final BucketEntity bucket = metadataService.getBucketById("1");
        assertNotNull(bucket);

        final String updatedName = bucket.getName() + " UPDATED";
        final String updatedDesc = bucket.getDescription() + "DESC";

        bucket.setName(updatedName);
        bucket.setDescription(updatedDesc);

        metadataService.updateBucket(bucket);

        final BucketEntity updatedBucket = metadataService.getBucketById(bucket.getId());
        assertNotNull(updatedName);
        assertEquals(updatedName, updatedBucket.getName());
        assertEquals(updatedDesc, updatedBucket.getDescription());
    }

    @Test
    public void testDeleteBucketNoChildren() {
        final BucketEntity bucket = metadataService.getBucketById("6");
        assertNotNull(bucket);

        metadataService.deleteBucket(bucket);

        final BucketEntity deletedBucket = metadataService.getBucketById("6");
        assertNull(deletedBucket);
    }

    @Test
    public void testDeleteBucketWithChildren() {
        final BucketEntity bucket = metadataService.getBucketById("1");
        assertNotNull(bucket);

        metadataService.deleteBucket(bucket);

        final BucketEntity deletedBucket = metadataService.getBucketById("1");
        assertNull(deletedBucket);
    }

    @Test
    public void testGetBucketsForIds() {
        final List<BucketEntity> buckets = metadataService.getBuckets(new HashSet<>(Arrays.asList("1", "2")));
        assertNotNull(buckets);
        assertEquals(2, buckets.size());
        assertEquals("1", buckets.get(0).getId());
        assertEquals("2", buckets.get(1).getId());
    }

    @Test
    public void testGetAllBuckets() {
        final List<BucketEntity> buckets = metadataService.getAllBuckets();
        assertNotNull(buckets);
        assertEquals(6, buckets.size());
    }

    //----------------- BucketItems ---------------------------------

    @Test
    public void testGetBucketItemsForBucket() {
        final BucketEntity bucket = metadataService.getBucketById("1");
        assertNotNull(bucket);

        final List<BucketItemEntity> items = metadataService.getBucketItems(bucket.getId());
        assertNotNull(items);
        assertEquals(2, items.size());
    }

    @Test
    public void testGetBucketItemsForBuckets() {
        final List<BucketItemEntity> items = metadataService.getBucketItems(new HashSet<>(Arrays.asList("1", "2")));
        assertNotNull(items);
        assertEquals(3, items.size());
    }

    @Test
    public void testGetItemsWithCounts() {
        final List<BucketItemEntity> items = metadataService.getBucketItems(new HashSet<>(Arrays.asList("1", "2")));
        assertNotNull(items);

        // 3 items across all buckets
        assertEquals(3, items.size());

        final BucketItemEntity item1 = items.stream().filter(i -> i.getId().equals("1")).findFirst().orElse(null);
        assertNotNull(item1);
        assertEquals(BucketItemEntityType.FLOW, item1.getType());

        final FlowEntity flowEntity = (FlowEntity) item1;
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    @Test
    public void testGetItemsWithCountsFilteredByBuckets() {
        final List<BucketItemEntity> items = metadataService.getBucketItems(Collections.singleton("1"));
        assertNotNull(items);

        // only 2 items in bucket 1
        assertEquals(2, items.size());

        final BucketItemEntity item1 = items.stream().filter(i -> i.getId().equals("1")).findFirst().orElse(null);
        assertNotNull(item1);
        assertEquals(BucketItemEntityType.FLOW, item1.getType());

        final FlowEntity flowEntity = (FlowEntity) item1;
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    //----------------- Flows ---------------------------------

    @Test
    public void testGetFlowByIdWhenExists() {
        final FlowEntity flow = metadataService.getFlowById("1");
        assertNotNull(flow);
        assertEquals("1", flow.getId());
        assertEquals("1", flow.getBucketId());
    }

    @Test
    public void testGetFlowByIdWhenDoesNotExist() {
        final FlowEntity flow = metadataService.getFlowById("does-not-exist");
        assertNull(flow);
    }

    @Test
    public void testCreateFlow() {
        final String bucketId = "1";

        final FlowEntity flow = new FlowEntity();
        flow.setId(UUID.randomUUID().toString());
        flow.setBucketId(bucketId);
        flow.setName("Test Flow 1");
        flow.setDescription("Description for Test Flow 1");
        flow.setCreated(new Date());
        flow.setModified(new Date());
        flow.setType(BucketItemEntityType.FLOW);

        metadataService.createFlow(flow);

        final FlowEntity createdFlow = metadataService.getFlowById(flow.getId());
        assertNotNull(flow);
        assertEquals(flow.getId(), createdFlow.getId());
        assertEquals(flow.getBucketId(), createdFlow.getBucketId());
        assertEquals(flow.getName(), createdFlow.getName());
        assertEquals(flow.getDescription(), createdFlow.getDescription());
        assertEquals(flow.getCreated(), createdFlow.getCreated());
        assertEquals(flow.getModified(), createdFlow.getModified());
        assertEquals(flow.getType(), createdFlow.getType());
    }

    @Test
    public void testGetFlowByIdWithSnapshotCount() {
       final FlowEntity flowEntity = metadataService.getFlowByIdWithSnapshotCounts("1");
        assertNotNull(flowEntity);
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    @Test
    public void testGetFlowsByBucket() {
        final BucketEntity bucketEntity = metadataService.getBucketById("1");
        final List<FlowEntity> flows = metadataService.getFlowsByBucket(bucketEntity.getId());
        assertEquals(2, flows.size());

        final FlowEntity flowEntity = flows.stream().filter(f -> f.getId().equals("1")).findFirst().orElse(null);
        assertNotNull(flowEntity);
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    @Test
    public void testGetFlowsByName() {
        final List<FlowEntity> flows = metadataService.getFlowsByName("Flow 1");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        assertEquals("Flow 1", flows.get(0).getName());
    }

    @Test
    public void testUpdateFlow() {
        final FlowEntity flow = metadataService.getFlowById("1");
        assertNotNull(flow);

        final Date originalModified = flow.getModified();

        flow.setName(flow.getName() + " UPDATED");
        flow.setDescription(flow.getDescription() + " UPDATED");

        metadataService.updateFlow(flow);

        final FlowEntity updatedFlow = metadataService.getFlowById( "1");
        assertNotNull(flow);
        assertEquals(flow.getName(), updatedFlow.getName());
        assertEquals(flow.getDescription(), updatedFlow.getDescription());
        assertEquals(flow.getModified().getTime(), updatedFlow.getModified().getTime());
        assertTrue(updatedFlow.getModified().getTime() > originalModified.getTime());
    }

    @Test
    public void testDeleteFlowWithSnapshots() {
        final FlowEntity flow = metadataService.getFlowById( "1");
        assertNotNull(flow);

        metadataService.deleteFlow(flow);

        final FlowEntity deletedFlow = metadataService.getFlowById("1");
        assertNull(deletedFlow);
    }

    //----------------- FlowSnapshots ---------------------------------

    @Test
    public void testGetFlowSnapshot() {
        final FlowSnapshotEntity entity = metadataService.getFlowSnapshot( "1", 1);
        assertNotNull(entity);
        assertEquals("1", entity.getFlowId());
        assertEquals(1, entity.getVersion().intValue());
    }

    @Test
    public void testGetFlowSnapshotDoesNotExist() {
        final FlowSnapshotEntity entity = metadataService.getFlowSnapshot( "DOES-NOT-EXIST", 1);
        assertNull(entity);
    }

    @Test
    public void testCreateFlowSnapshot() {
        final FlowSnapshotEntity flowSnapshot = new FlowSnapshotEntity();
        flowSnapshot.setFlowId("1");
        flowSnapshot.setVersion(4);
        flowSnapshot.setCreated(new Date());
        flowSnapshot.setCreatedBy("test-user");
        flowSnapshot.setComments("Comments");

        metadataService.createFlowSnapshot(flowSnapshot);

        final FlowSnapshotEntity createdFlowSnapshot = metadataService.getFlowSnapshot(flowSnapshot.getFlowId(), flowSnapshot.getVersion());
        assertNotNull(createdFlowSnapshot);
        assertEquals(flowSnapshot.getFlowId(), createdFlowSnapshot.getFlowId());
        assertEquals(flowSnapshot.getVersion(), createdFlowSnapshot.getVersion());
        assertEquals(flowSnapshot.getComments(), createdFlowSnapshot.getComments());
        assertEquals(flowSnapshot.getCreated(), createdFlowSnapshot.getCreated());
        assertEquals(flowSnapshot.getCreatedBy(), createdFlowSnapshot.getCreatedBy());
    }

    @Test
    public void testGetLatestSnapshot() {
        final FlowSnapshotEntity latest = metadataService.getLatestSnapshot("1");
        assertNotNull(latest);
        assertEquals("1", latest.getFlowId());
        assertEquals(3, latest.getVersion().intValue());
    }

    @Test
    public void testGetLatestSnapshotDoesNotExist() {
        final FlowSnapshotEntity latest = metadataService.getLatestSnapshot("DOES-NOT-EXIST");
        assertNull(latest);
    }

    @Test
    public void testGetFlowSnapshots() {
        final List<FlowSnapshotEntity> flowSnapshots = metadataService.getSnapshots( "1");
        assertNotNull(flowSnapshots);
        assertEquals(3, flowSnapshots.size());
    }

    @Test
    public void testGetFlowSnapshotsNoneFound() {
        final List<FlowSnapshotEntity> flowSnapshots = metadataService.getSnapshots( "2");
        assertNotNull(flowSnapshots);
        assertEquals(0, flowSnapshots.size());
    }

    @Test
    public void testDeleteFlowSnapshot() {
        final FlowSnapshotEntity entity = metadataService.getFlowSnapshot( "1", 1);
        assertNotNull(entity);

        metadataService.deleteFlowSnapshot(entity);

        final FlowSnapshotEntity deletedEntity = metadataService.getFlowSnapshot( "1", 1);
        assertNull(deletedEntity);
    }

}
