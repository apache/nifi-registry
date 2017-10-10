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
import org.apache.nifi.registry.service.params.QueryParameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDatabaseMetadataService extends DatabaseBaseTest {

    @Autowired
    private DatabaseMetadataService metadataService;

    @Test
    public void testGetAllBuckets() {
        final List<BucketEntity> buckets = metadataService.getAllBuckets();
        assertNotNull(buckets);
        assertEquals(6, buckets.size());
    }

    @Test
    public void testGetBuckets() {
        final List<BucketEntity> buckets = metadataService.getBuckets(QueryParameters.EMPTY_PARAMETERS, new HashSet<>(Arrays.asList("1", "2")));
        assertNotNull(buckets);
        assertEquals(2, buckets.size());
    }

    @Test
    public void testGetItemsWithCounts() {
        final List<BucketItemEntity> items = metadataService.getBucketItems(null, new HashSet<>(Arrays.asList("1", "2")));
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
        final List<BucketItemEntity> items = metadataService.getBucketItems(null, Collections.singleton("1"));
        assertNotNull(items);

        // only 2 items in bucket 1
        assertEquals(2, items.size());

        final BucketItemEntity item1 = items.stream().filter(i -> i.getId().equals("1")).findFirst().orElse(null);
        assertNotNull(item1);
        assertEquals(BucketItemEntityType.FLOW, item1.getType());

        final FlowEntity flowEntity = (FlowEntity) item1;
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    @Test
    public void testGetFlowsByBucketWithCounts() {
        final BucketEntity bucketEntity = metadataService.getBucketById("1");
        final List<FlowEntity> flows = metadataService.getFlowsByBucket(bucketEntity);
        assertEquals(2, flows.size());

        final FlowEntity flowEntity = flows.stream().filter(f -> f.getId().equals("1")).findFirst().orElse(null);
        assertNotNull(flowEntity);
        assertEquals(3, flowEntity.getSnapshotCount());
    }

    @Test
    public void testGetFlowByIdWithVersionCount() {
        final FlowEntity flowEntity = metadataService.getFlowByIdWithSnapshotCounts("1", "1");
        assertNotNull(flowEntity);
        assertEquals(3, flowEntity.getSnapshotCount());
    }

}
