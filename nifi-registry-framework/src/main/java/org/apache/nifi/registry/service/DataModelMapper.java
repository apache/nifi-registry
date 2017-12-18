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
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.KeyEntity;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.security.key.Key;

import java.util.Date;

/**
 * Utility for mapping between Provider API and the registry data model.
 */
public class DataModelMapper {

    // --- Map buckets

    public static BucketEntity map(final Bucket bucket) {
        final BucketEntity bucketEntity = new BucketEntity();
        bucketEntity.setId(bucket.getIdentifier());
        bucketEntity.setName(bucket.getName());
        bucketEntity.setDescription(bucket.getDescription());
        bucketEntity.setCreated(new Date(bucket.getCreatedTimestamp()));
        return bucketEntity;
    }

    public static Bucket map(final BucketEntity bucketEntity) {
        final Bucket bucket = new Bucket();
        bucket.setIdentifier(bucketEntity.getId());
        bucket.setName(bucketEntity.getName());
        bucket.setDescription(bucketEntity.getDescription());
        bucket.setCreatedTimestamp(bucketEntity.getCreated().getTime());
        return bucket;
    }

    // --- Map flows

    public static FlowEntity map(final VersionedFlow versionedFlow) {
        final FlowEntity flowEntity = new FlowEntity();
        flowEntity.setId(versionedFlow.getIdentifier());
        flowEntity.setName(versionedFlow.getName());
        flowEntity.setDescription(versionedFlow.getDescription());
        flowEntity.setCreated(new Date(versionedFlow.getCreatedTimestamp()));
        flowEntity.setModified(new Date(versionedFlow.getModifiedTimestamp()));
        flowEntity.setType(BucketItemEntityType.FLOW);
        return flowEntity;
    }

    public static VersionedFlow map(final BucketEntity bucketEntity, final FlowEntity flowEntity) {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flowEntity.getId());
        versionedFlow.setBucketIdentifier(flowEntity.getBucketId());
        versionedFlow.setName(flowEntity.getName());
        versionedFlow.setDescription(flowEntity.getDescription());
        versionedFlow.setCreatedTimestamp(flowEntity.getCreated().getTime());
        versionedFlow.setModifiedTimestamp(flowEntity.getModified().getTime());
        versionedFlow.setVersionCount(flowEntity.getSnapshotCount());

        if (bucketEntity != null) {
            versionedFlow.setBucketName(bucketEntity.getName());
        } else {
            versionedFlow.setBucketName(flowEntity.getBucketName());
        }

        return versionedFlow;
    }

    // --- Map snapshots

    public static FlowSnapshotEntity map(final VersionedFlowSnapshotMetadata versionedFlowSnapshot) {
        final FlowSnapshotEntity flowSnapshotEntity = new FlowSnapshotEntity();
        flowSnapshotEntity.setFlowId(versionedFlowSnapshot.getFlowIdentifier());
        flowSnapshotEntity.setVersion(versionedFlowSnapshot.getVersion());
        flowSnapshotEntity.setComments(versionedFlowSnapshot.getComments());
        flowSnapshotEntity.setCreated(new Date(versionedFlowSnapshot.getTimestamp()));
        flowSnapshotEntity.setCreatedBy(versionedFlowSnapshot.getAuthor());
        return flowSnapshotEntity;
    }

    public static VersionedFlowSnapshotMetadata map(final BucketEntity bucketEntity, final FlowSnapshotEntity flowSnapshotEntity) {
        final VersionedFlowSnapshotMetadata metadata = new VersionedFlowSnapshotMetadata();
        metadata.setFlowIdentifier(flowSnapshotEntity.getFlowId());
        metadata.setVersion(flowSnapshotEntity.getVersion());
        metadata.setComments(flowSnapshotEntity.getComments());
        metadata.setTimestamp(flowSnapshotEntity.getCreated().getTime());
        metadata.setAuthor(flowSnapshotEntity.getCreatedBy());

        if (bucketEntity != null) {
            metadata.setBucketIdentifier(bucketEntity.getId());
        }

        return metadata;
    }

    // --- Map keys

    public static Key map(final KeyEntity keyEntity) {
        final Key key = new Key();
        key.setId(keyEntity.getId());
        key.setIdentity(keyEntity.getTenantIdentity());
        key.setKey(keyEntity.getKeyValue());
        return key;
    }

    public static KeyEntity map(final Key key) {
        final KeyEntity keyEntity = new KeyEntity();
        keyEntity.setId(key.getId());
        keyEntity.setTenantIdentity(key.getIdentity());
        keyEntity.setKeyValue(key.getKey());
        return keyEntity;
    }

}
