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
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntityKey;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility for mapping between Provider API and the registry data model.
 */
public class DataModelMapper {

    public static BucketEntity map(final Bucket bucket) {
        final BucketEntity bucketEntity = new BucketEntity();
        bucketEntity.setId(bucket.getIdentifier());
        bucketEntity.setName(bucket.getName());
        bucketEntity.setDescription(bucket.getDescription());
        bucketEntity.setCreated(new Date(bucket.getCreatedTimestamp()));

        // don't map items on the way in

        return bucketEntity;
    }

    public static Bucket map(final BucketEntity bucketEntity, final boolean mapChildren) {
        final Bucket bucket = new Bucket();
        bucket.setIdentifier(bucketEntity.getId());
        bucket.setName(bucketEntity.getName());
        bucket.setDescription(bucketEntity.getDescription());
        bucket.setCreatedTimestamp(bucketEntity.getCreated().getTime());

        if (mapChildren && bucketEntity.getItems() != null) {
            final Set<VersionedFlow> flows = new LinkedHashSet<>();
            for (final BucketItemEntity itemEntity : bucketEntity.getItems()) {
                if (BucketItemEntityType.FLOW == itemEntity.getType()) {
                    // we never return the snapshots when retrieving a bucket
                    flows.add(map((FlowEntity) itemEntity, false));
                }
            }
            bucket.setVersionedFlows(flows);
        }

        return bucket;
    }

    public static FlowEntity map(final VersionedFlow versionedFlow) {
        final FlowEntity flowEntity = new FlowEntity();
        flowEntity.setId(versionedFlow.getIdentifier());
        flowEntity.setName(versionedFlow.getName());
        flowEntity.setDescription(versionedFlow.getDescription());
        flowEntity.setCreated(new Date(versionedFlow.getCreatedTimestamp()));
        flowEntity.setModified(new Date(versionedFlow.getModifiedTimestamp()));
        flowEntity.setType(BucketItemEntityType.FLOW);

        // don't map snapshots on the way in

        return flowEntity;
    }

    public static VersionedFlow map(final FlowEntity flowEntity, boolean mapChildren) {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flowEntity.getId());
        versionedFlow.setBucketIdentifier(flowEntity.getBucket().getId());
        versionedFlow.setName(flowEntity.getName());
        versionedFlow.setDescription(flowEntity.getDescription());
        versionedFlow.setCreatedTimestamp(flowEntity.getCreated().getTime());
        versionedFlow.setModifiedTimestamp(flowEntity.getModified().getTime());

        if (mapChildren && flowEntity.getSnapshots() != null) {
            final SortedSet<VersionedFlowSnapshotMetadata> snapshots = new TreeSet<>();
            flowEntity.getSnapshots().stream().forEach(s -> snapshots.add(map(s)));
            versionedFlow.setSnapshotMetadata(snapshots);
        }

        return versionedFlow;
    }

    public static FlowSnapshotEntity map(final VersionedFlowSnapshotMetadata versionedFlowSnapshot) {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey();
        key.setFlowId(versionedFlowSnapshot.getFlowIdentifier());
        key.setVersion(versionedFlowSnapshot.getVersion());

        final FlowSnapshotEntity flowSnapshotEntity = new FlowSnapshotEntity();
        flowSnapshotEntity.setId(key);
        flowSnapshotEntity.setComments(versionedFlowSnapshot.getComments());
        flowSnapshotEntity.setCreated(new Date(versionedFlowSnapshot.getTimestamp()));
        return flowSnapshotEntity;
    }

    public static VersionedFlowSnapshotMetadata map(final FlowSnapshotEntity flowSnapshotEntity) {
        final VersionedFlowSnapshotMetadata metadata = new VersionedFlowSnapshotMetadata();
        metadata.setFlowIdentifier(flowSnapshotEntity.getId().getFlowId());
        metadata.setVersion(flowSnapshotEntity.getId().getVersion());
        metadata.setBucketIdentifier(flowSnapshotEntity.getFlow().getBucket().getId());
        metadata.setFlowName(flowSnapshotEntity.getFlow().getName());
        metadata.setComments(flowSnapshotEntity.getComments());
        metadata.setTimestamp(flowSnapshotEntity.getCreated().getTime());
        return metadata;
    }

}
