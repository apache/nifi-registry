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
import org.apache.nifi.registry.db.entity.ExtensionBundleEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionDependencyEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.KeyEntity;
import org.apache.nifi.registry.diff.ComponentDifference;
import org.apache.nifi.registry.diff.ComponentDifferenceGroup;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersionDependency;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.diff.FlowDifference;
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

    public static ComponentDifference map(final FlowDifference flowDifference){
        ComponentDifference diff = new ComponentDifference();
        diff.setChangeDescription(flowDifference.getDescription());
        diff.setDifferenceType(flowDifference.getDifferenceType().toString());
        diff.setDifferenceTypeDescription(flowDifference.getDifferenceType().getDescription());
        diff.setValueA(getValueDescription(flowDifference.getValueA()));
        diff.setValueB(getValueDescription(flowDifference.getValueB()));
        return diff;
    }

    public static ComponentDifferenceGroup map(VersionedComponent versionedComponent){
        ComponentDifferenceGroup grouping = new ComponentDifferenceGroup();
        grouping.setComponentId(versionedComponent.getIdentifier());
        grouping.setComponentName(versionedComponent.getName());
        grouping.setProcessGroupId(versionedComponent.getGroupIdentifier());
        grouping.setComponentType(versionedComponent.getComponentType().getTypeName());
        return grouping;
    }

    private static String getValueDescription(Object valueA){
        if(valueA instanceof VersionedComponent){
            return ((VersionedComponent) valueA).getIdentifier();
        }
        if(valueA!= null){
            return valueA.toString();
        }
        return null;
    }

    // -- Map ExtensionBundleType

    public static ExtensionBundleEntityType map(final ExtensionBundleType bundleType) {
        switch (bundleType) {
            case NIFI_NAR:
                return ExtensionBundleEntityType.NIFI_NAR;

            case MINIFI_CPP:
                return ExtensionBundleEntityType.MINIFI_CPP;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
        }
    }

    public static ExtensionBundleType map(final ExtensionBundleEntityType bundleEntityType) {
        switch (bundleEntityType) {
            case NIFI_NAR:
                return ExtensionBundleType.NIFI_NAR;
            case MINIFI_CPP:
                return ExtensionBundleType.MINIFI_CPP;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleEntityType);
        }
    }

    // -- Map ExtensionBundle

    public static ExtensionBundleEntity map(final ExtensionBundle bundle) {
        final ExtensionBundleEntity entity = new ExtensionBundleEntity();
        entity.setId(bundle.getIdentifier());
        entity.setName(bundle.getName());
        entity.setDescription(bundle.getDescription());
        entity.setCreated(new Date(bundle.getCreatedTimestamp()));
        entity.setModified(new Date(bundle.getModifiedTimestamp()));
        entity.setType(BucketItemEntityType.EXTENSION_BUNDLE);
        entity.setBucketId(bundle.getBucketIdentifier());

        entity.setGroupId(bundle.getGroupId());
        entity.setArtifactId(bundle.getArtifactId());
        entity.setBundleType(map(bundle.getBundleType()));
        return entity;
    }

    public static ExtensionBundle map(final BucketEntity bucketEntity, final ExtensionBundleEntity bundleEntity) {
        final ExtensionBundle bundle = new ExtensionBundle();
        bundle.setIdentifier(bundleEntity.getId());
        bundle.setName(bundleEntity.getName());
        bundle.setDescription(bundleEntity.getDescription());
        bundle.setCreatedTimestamp(bundleEntity.getCreated().getTime());
        bundle.setModifiedTimestamp(bundleEntity.getModified().getTime());
        bundle.setBucketIdentifier(bundleEntity.getBucketId());

        if (bucketEntity != null) {
            bundle.setBucketName(bucketEntity.getName());
        } else {
            bundle.setBucketName(bundleEntity.getBucketName());
        }

        bundle.setGroupId(bundleEntity.getGroupId());
        bundle.setArtifactId(bundleEntity.getArtifactId());
        bundle.setBundleType(map(bundleEntity.getBundleType()));
        bundle.setVersionCount(bundleEntity.getVersionCount());
        return bundle;
    }

    // -- Map ExtensionBundleVersion

    public static ExtensionBundleVersionEntity map(final ExtensionBundleVersionMetadata bundleVersionMetadata) {
        final ExtensionBundleVersionEntity entity = new ExtensionBundleVersionEntity();
        entity.setId(bundleVersionMetadata.getId());
        entity.setExtensionBundleId(bundleVersionMetadata.getExtensionBundleId());
        entity.setVersion(bundleVersionMetadata.getVersion());
        entity.setCreated(new Date(bundleVersionMetadata.getTimestamp()));
        entity.setCreatedBy(bundleVersionMetadata.getAuthor());
        entity.setDescription(bundleVersionMetadata.getDescription());
        entity.setSha256Hex(bundleVersionMetadata.getSha256());
        return entity;
    }

    public static ExtensionBundleVersionMetadata map(final BucketEntity bucketEntity, final ExtensionBundleVersionEntity bundleVersionEntity) {
        final ExtensionBundleVersionMetadata bundleVersionMetadata = new ExtensionBundleVersionMetadata();
        bundleVersionMetadata.setId(bundleVersionEntity.getId());
        bundleVersionMetadata.setExtensionBundleId(bundleVersionEntity.getExtensionBundleId());
        bundleVersionMetadata.setVersion(bundleVersionEntity.getVersion());
        bundleVersionMetadata.setTimestamp(bundleVersionEntity.getCreated().getTime());
        bundleVersionMetadata.setAuthor(bundleVersionEntity.getCreatedBy());
        bundleVersionMetadata.setDescription(bundleVersionEntity.getDescription());
        bundleVersionMetadata.setSha256(bundleVersionEntity.getSha256Hex());

        if (bucketEntity != null) {
            bundleVersionMetadata.setBucketId(bucketEntity.getId());
        }

        return bundleVersionMetadata;
    }

    // -- Map ExtensionBundleVersionDependency

    public static ExtensionBundleVersionDependencyEntity map(final ExtensionBundleVersionDependency bundleVersionDependency) {
        final ExtensionBundleVersionDependencyEntity entity = new ExtensionBundleVersionDependencyEntity();
        entity.setGroupId(bundleVersionDependency.getGroupId());
        entity.setArtifactId(bundleVersionDependency.getArtifactId());
        entity.setVersion(bundleVersionDependency.getVersion());
        return entity;
    }

    public static ExtensionBundleVersionDependency map(final ExtensionBundleVersionDependencyEntity dependencyEntity) {
        final ExtensionBundleVersionDependency dependency = new ExtensionBundleVersionDependency();
        dependency.setGroupId(dependencyEntity.getGroupId());
        dependency.setArtifactId(dependencyEntity.getArtifactId());
        dependency.setVersion(dependencyEntity.getVersion());
        return dependency;
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

    // map

}
