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
package org.apache.nifi.registry.service;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.BucketMetadata;
import org.apache.nifi.registry.metadata.FlowMetadata;
import org.apache.nifi.registry.metadata.FlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.StandardBucketMetadata;
import org.apache.nifi.registry.metadata.StandardFlowMetadata;
import org.apache.nifi.registry.metadata.StandardFlowSnapshotMetadata;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDataModelMapper {

    @Test
    public void testMapBucketToBucketMetadata() {
        // create a bucket
        final Bucket bucket = new Bucket();
        bucket.setIdentifier("bucket1");
        bucket.setName("Bucket 1");
        bucket.setDescription("This is bucket 1.");
        bucket.setCreatedTimestamp(System.currentTimeMillis());

        // create a flow
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier("flow1");
        versionedFlow.setName("Flow 1");
        versionedFlow.setDescription("This is flow 1");
        versionedFlow.setBucketIdentifier(bucket.getIdentifier());
        versionedFlow.setCreatedTimestamp(System.currentTimeMillis());
        versionedFlow.setModifiedTimestamp(System.currentTimeMillis());

        // create a snapshot for the flow
        final VersionedFlowSnapshotMetadata versionedFlowSnapshotMetadata = new VersionedFlowSnapshotMetadata();
        versionedFlowSnapshotMetadata.setBucketIdentifier(bucket.getIdentifier());
        versionedFlowSnapshotMetadata.setFlowIdentifier(versionedFlow.getIdentifier());
        versionedFlowSnapshotMetadata.setFlowName(versionedFlow.getName());
        versionedFlowSnapshotMetadata.setVersion(1);
        versionedFlowSnapshotMetadata.setTimestamp(System.currentTimeMillis());
        versionedFlowSnapshotMetadata.setComments("This is snapshot 1 of flow 1");

        // add the snapshot to the flow
        final SortedSet<VersionedFlowSnapshotMetadata> versionedFlowSnapshotMetadataSet = new TreeSet<>();
        versionedFlowSnapshotMetadataSet.add(versionedFlowSnapshotMetadata);
        versionedFlow.setSnapshotMetadata(versionedFlowSnapshotMetadataSet);

        // add the flow to the bucket
        final Set<VersionedFlow> versionedFlows = new LinkedHashSet<>();
        versionedFlows.add(versionedFlow);
        bucket.setVersionedFlows(versionedFlows);

        // test the mapping from bucket to bucket metadata

        final BucketMetadata bucketMetadata = DataModelMapper.map(bucket);
        assertEquals(bucket.getIdentifier(), bucketMetadata.getIdentifier());
        assertEquals(bucket.getName(), bucketMetadata.getName());
        assertEquals(bucket.getDescription(), bucketMetadata.getDescription());
        assertEquals(bucket.getCreatedTimestamp(), bucketMetadata.getCreatedTimestamp());

        assertNotNull(bucketMetadata.getFlowMetadata());
        assertEquals(1, bucketMetadata.getFlowMetadata().size());

        final FlowMetadata flowMetadata = bucketMetadata.getFlowMetadata().iterator().next();
        assertNotNull(flowMetadata);
        assertEquals(versionedFlow.getIdentifier(), flowMetadata.getIdentifier());
        assertEquals(versionedFlow.getName(), flowMetadata.getName());
        assertEquals(versionedFlow.getDescription(), flowMetadata.getDescription());
        assertEquals(versionedFlow.getBucketIdentifier(), flowMetadata.getBucketIdentifier());
        assertEquals(versionedFlow.getCreatedTimestamp(), flowMetadata.getCreatedTimestamp());
        assertEquals(versionedFlow.getModifiedTimestamp(), flowMetadata.getModifiedTimestamp());

        assertNotNull(flowMetadata.getSnapshotMetadata());
        assertEquals(1, flowMetadata.getSnapshotMetadata().size());

        final FlowSnapshotMetadata flowSnapshotMetadata = flowMetadata.getSnapshotMetadata().iterator().next();
        assertNotNull(flowSnapshotMetadata);
        assertEquals(versionedFlowSnapshotMetadata.getFlowIdentifier(), flowSnapshotMetadata.getFlowIdentifier());
        assertEquals(versionedFlowSnapshotMetadata.getFlowName(), flowSnapshotMetadata.getFlowName());
        assertEquals(versionedFlowSnapshotMetadata.getBucketIdentifier(), flowSnapshotMetadata.getBucketIdentifier());
        assertEquals(versionedFlowSnapshotMetadata.getVersion(), flowSnapshotMetadata.getVersion());
        assertEquals(versionedFlowSnapshotMetadata.getComments(), flowSnapshotMetadata.getComments());
        assertEquals(versionedFlowSnapshotMetadata.getTimestamp(), flowSnapshotMetadata.getCreatedTimestamp());
    }

    @Test
    public void testMapBucketMetadataToBucket() {
        // create snapshot metadata
        final FlowSnapshotMetadata snapshotMetadata = new StandardFlowSnapshotMetadata.Builder()
                .flowIdentifier("flow1")
                .flowName("Flow 1")
                .bucketIdentifier("bucket1")
                .version(1)
                .comments("This is snapshot 1 of flow 1.")
                .created(System.currentTimeMillis())
                .build();

        // create flow metadata
        final FlowMetadata flowMetadata = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("Flow 1")
                .bucketIdentifier("bucket1")
                .description("This flow 1.")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .addSnapshot(snapshotMetadata)
                .build();

        // create bucket metadata
        final BucketMetadata bucketMetadata = new StandardBucketMetadata.Builder()
                .identifier("bucket1")
                .name("Bucket 1")
                .description("This is bucket 1.")
                .created(System.currentTimeMillis())
                .addFlow(flowMetadata)
                .build();

        // test the mapping from bucket metadata to bucket

        final Bucket bucket = DataModelMapper.map(bucketMetadata);
        assertEquals(bucketMetadata.getIdentifier(), bucket.getIdentifier());
        assertEquals(bucketMetadata.getName(), bucket.getName());
        assertEquals(bucketMetadata.getDescription(), bucket.getDescription());
        assertEquals(bucketMetadata.getCreatedTimestamp(), bucket.getCreatedTimestamp());

        assertNotNull(bucket.getVersionedFlows());
        assertEquals(1, bucket.getVersionedFlows().size());

        final VersionedFlow versionedFlow = bucket.getVersionedFlows().iterator().next();
        assertNotNull(versionedFlow);
        assertEquals(flowMetadata.getIdentifier(), versionedFlow.getIdentifier());
        assertEquals(flowMetadata.getName(), versionedFlow.getName());
        assertEquals(flowMetadata.getBucketIdentifier(), versionedFlow.getBucketIdentifier());
        assertEquals(flowMetadata.getDescription(), versionedFlow.getDescription());
        assertEquals(flowMetadata.getCreatedTimestamp(), versionedFlow.getCreatedTimestamp());
        assertEquals(flowMetadata.getModifiedTimestamp(), versionedFlow.getModifiedTimestamp());

        assertNotNull(versionedFlow.getSnapshotMetadata());
        assertEquals(1, versionedFlow.getSnapshotMetadata().size());

        final VersionedFlowSnapshotMetadata versionedFlowSnapshotMetadata = versionedFlow.getSnapshotMetadata().first();
        assertEquals(snapshotMetadata.getFlowIdentifier(), versionedFlowSnapshotMetadata.getFlowIdentifier());
        assertEquals(snapshotMetadata.getFlowName(), versionedFlowSnapshotMetadata.getFlowName());
        assertEquals(snapshotMetadata.getBucketIdentifier(), versionedFlowSnapshotMetadata.getBucketIdentifier());
        assertEquals(snapshotMetadata.getVersion(), versionedFlowSnapshotMetadata.getVersion());
        assertEquals(snapshotMetadata.getComments(), versionedFlowSnapshotMetadata.getComments());
        assertEquals(snapshotMetadata.getCreatedTimestamp(), versionedFlowSnapshotMetadata.getTimestamp());
    }

}
