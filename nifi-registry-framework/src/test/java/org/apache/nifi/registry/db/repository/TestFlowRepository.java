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
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestFlowRepository extends DatabaseBaseTest {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private FlowRepository flowRepository;


    @Test
    public void testCreateFlow() {
        final String bucketId = "1";

        final BucketEntity existingBucket = bucketRepository.findOne(bucketId);
        assertNotNull(existingBucket);
        assertNotNull(existingBucket.getItems());
        assertEquals(2, existingBucket.getItems().size());

        // created, and modified should get set automatically
        final FlowEntity flow = new FlowEntity();
        flow.setId(UUID.randomUUID().toString());
        flow.setName("Flow 4");
        flow.setDescription("This is flow 4");
        flow.setBucket(existingBucket);
        flow.setCreated(new Date());
        flow.setModified(new Date());

        final FlowEntity createdFlow = flowRepository.save(flow);
        assertEquals(flow.getId(), createdFlow.getId());
        assertEquals(flow.getName(), createdFlow.getName());
        assertEquals(flow.getDescription(), createdFlow.getDescription());
        assertEquals(flow.getCreated(), createdFlow.getCreated());
        assertEquals(flow.getModified(), createdFlow.getModified());
        assertEquals(BucketItemEntityType.FLOW, flow.getType());
    }

    @Test
    public void testUpdateFlow() {
        final String flowId = "1";

        final FlowEntity flow = flowRepository.findOne(flowId);
        assertNotNull(flow);
        assertEquals(flowId, flow.getId());

        flow.setName(flow.getName() + " UPDATED");
        flow.setDescription(flow.getDescription() + " UPDATED");

        flowRepository.save(flow);

        final FlowEntity updatedFlow = flowRepository.findOne(flowId);
        assertEquals(flow.getName(), updatedFlow.getName());
        assertEquals(flow.getDescription(), updatedFlow.getDescription());
        assertEquals(flow.getCreated(), updatedFlow.getCreated());
        assertEquals(flow.getModified(), updatedFlow.getModified());
        assertEquals(BucketItemEntityType.FLOW, updatedFlow.getType());
    }

    @Test
    public void testDeleteFlow() {
        final String flowId = "1";

        final FlowEntity flow = flowRepository.findOne(flowId);
        assertNotNull(flow);

        flowRepository.delete(flow);

        final FlowEntity deletedFlow = flowRepository.findOne(flowId);
        assertNull(deletedFlow);
    }

    @Test
    public void testOneToManyWithFlowSnapshots() {
        final String flowId = "1";

        final FlowEntity flow = flowRepository.findOne(flowId);
        assertNotNull(flow);
        assertNotNull(flow.getSnapshots());
        assertEquals(3, flow.getSnapshots().size());
    }

    @Test
    public void testFindFlowByNameCaseInsensitive() {
        final String flowName = "fLoW 1";

        final List<FlowEntity> flows = flowRepository.findByNameIgnoreCase(flowName);
        assertNotNull(flows);
        assertEquals(1, flows.size());

        final FlowEntity flow = flows.get(0);
        assertEquals(flowName.toLowerCase(), flow.getName().toLowerCase());
    }

}
