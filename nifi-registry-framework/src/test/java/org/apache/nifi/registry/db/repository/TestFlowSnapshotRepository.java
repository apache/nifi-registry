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
package org.apache.nifi.registry.db.repository;

import org.apache.nifi.registry.db.DatabaseBaseTest;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotCount;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntityKey;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestFlowSnapshotRepository extends DatabaseBaseTest {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private FlowSnapshotRepository flowSnapshotRepository;


    @Test
    public void testCreateFlowSnapshot() {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey();
        key.setFlowId("2");
        key.setVersion(1);

        final FlowSnapshotEntity flowSnapshot = new FlowSnapshotEntity();
        flowSnapshot.setId(key);
        flowSnapshot.setComments("This is snapshot 1 for flow 2");
        flowSnapshot.setCreated(new Date());

        flowSnapshotRepository.save(flowSnapshot);

        final FlowSnapshotEntity createdFlowSnapshot = flowSnapshotRepository.findOne(key);
        assertNotNull(createdFlowSnapshot);
    }

    @Test
    public void testFindById() {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey();
        key.setFlowId("1");
        key.setVersion(1);

        final FlowSnapshotEntity flowSnapshot = flowSnapshotRepository.findOne(key);
        assertNotNull(flowSnapshot);
        assertEquals(key, flowSnapshot.getId());
        assertNotNull(flowSnapshot.getFlow());
        assertEquals("user1", flowSnapshot.getCreatedBy());
    }

    @Test
    public void testDeleteFlowSnapshot() {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey();
        key.setFlowId("1");
        key.setVersion(1);

        final FlowSnapshotEntity flowSnapshot = flowSnapshotRepository.findOne(key);
        assertNotNull(flowSnapshot);

        flowSnapshotRepository.delete(flowSnapshot);

        final FlowSnapshotEntity deletedFlowSnapshot = flowSnapshotRepository.findOne(key);
        assertNull(deletedFlowSnapshot);
    }

    @Test
    public void testDeleteBucketCascadesToSnapshots() {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey();
        key.setFlowId("1");
        key.setVersion(1);

        final FlowSnapshotEntity flowSnapshot = flowSnapshotRepository.findOne(key);
        assertNotNull(flowSnapshot);

        final FlowEntity flow = flowSnapshot.getFlow();
        assertNotNull(flow);

        final BucketEntity bucket = flow.getBucket();
        assertNotNull(bucket);

        bucketRepository.delete(bucket);

        assertNull(flowRepository.findOne(flow.getId()));
        assertNull(flowSnapshotRepository.findOne(key));
    }

    @Test
    public void testCountByFlow() {
        final List<FlowSnapshotCount> counts = flowSnapshotRepository.countByFlow();
        assertNotNull(counts);
        assertEquals(1, counts.size());

        final FlowSnapshotCount count = counts.get(0);
        assertEquals("1", count.getFlowIdentifier());
        assertEquals(3, count.getSnapshotCount());
    }

}
