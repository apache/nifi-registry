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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility for mapping between Provider API and the registry data model.
 */
public class DataModelMapper {

    public static Bucket map(final BucketMetadata bucketMetadata) {
        final Bucket bucket = new Bucket();
        bucket.setIdentifier(bucketMetadata.getIdentifier());
        bucket.setName(bucketMetadata.getName());
        bucket.setDescription(bucketMetadata.getDescription());
        bucket.setCreatedTimestamp(bucketMetadata.getCreatedTimestamp());

        if (bucketMetadata.getFlowMetadata() != null) {
            final Set<VersionedFlow> flows = new LinkedHashSet<>();
            bucketMetadata.getFlowMetadata().stream().forEach(f -> flows.add(map(f)));
            bucket.setVersionedFlows(flows);
        }

        return bucket;
    }

    public static BucketMetadata map(final Bucket bucket) {
        final StandardBucketMetadata.Builder builder = new StandardBucketMetadata.Builder()
                .identifier(bucket.getIdentifier())
                .name(bucket.getName())
                .description(bucket.getDescription())
                .created(bucket.getCreatedTimestamp());

        if (bucket.getVersionedFlows() != null) {
            bucket.getVersionedFlows().stream().forEach(f -> builder.addFlow(map(f)));
        }

        return builder.build();
    }

    public static VersionedFlow map(final FlowMetadata flowMetadata) {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flowMetadata.getIdentifier());
        versionedFlow.setName(flowMetadata.getName());
        versionedFlow.setBucketIdentifier(flowMetadata.getBucketIdentifier());
        versionedFlow.setDescription(flowMetadata.getDescription());
        versionedFlow.setCreatedTimestamp(flowMetadata.getCreatedTimestamp());
        versionedFlow.setModifiedTimestamp(flowMetadata.getModifiedTimestamp());

        if (flowMetadata.getSnapshotMetadata() != null) {
            final SortedSet<VersionedFlowSnapshotMetadata> snapshots = new TreeSet<>();
            flowMetadata.getSnapshotMetadata().stream().forEach(s -> snapshots.add(map(s)));
            versionedFlow.setSnapshotMetadata(snapshots);
        }

        return versionedFlow;
    }

    public static FlowMetadata map(final VersionedFlow versionedFlow) {
        final StandardFlowMetadata.Builder builder = new StandardFlowMetadata.Builder()
                .identifier(versionedFlow.getIdentifier())
                .name(versionedFlow.getName())
                .bucketIdentifier(versionedFlow.getBucketIdentifier())
                .description(versionedFlow.getDescription())
                .created(versionedFlow.getCreatedTimestamp())
                .modified(versionedFlow.getModifiedTimestamp());

        if (versionedFlow.getSnapshotMetadata() != null) {
            versionedFlow.getSnapshotMetadata().stream().forEach(s -> builder.addSnapshot(map(s)));
        }

        return builder.build();
    }

    public static VersionedFlowSnapshotMetadata map(final FlowSnapshotMetadata flowSnapshotMetadata) {
        final VersionedFlowSnapshotMetadata metadata = new VersionedFlowSnapshotMetadata();
        metadata.setBucketIdentifier(flowSnapshotMetadata.getBucketIdentifier());
        metadata.setFlowIdentifier(flowSnapshotMetadata.getFlowIdentifier());
        metadata.setFlowName(flowSnapshotMetadata.getFlowName());
        metadata.setComments(flowSnapshotMetadata.getComments());
        metadata.setTimestamp(flowSnapshotMetadata.getCreatedTimestamp());
        metadata.setVersion(flowSnapshotMetadata.getVersion());
        return metadata;
    }

    public static FlowSnapshotMetadata map(final VersionedFlowSnapshotMetadata metadata) {
        return new StandardFlowSnapshotMetadata.Builder()
                .bucketIdentifier(metadata.getBucketIdentifier())
                .flowIdentifier(metadata.getFlowIdentifier())
                .flowName(metadata.getFlowName())
                .comments(metadata.getComments())
                .created(metadata.getTimestamp())
                .version(metadata.getVersion())
                .build();
    }

}
