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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.flow.FlowSnapshotContext;
import org.apache.nifi.registry.flow.StandardFlowSnapshotContext;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.BucketMetadata;
import org.apache.nifi.registry.metadata.FlowMetadata;
import org.apache.nifi.registry.metadata.FlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.MetadataProvider;
import org.apache.nifi.registry.metadata.StandardBucketMetadata;
import org.apache.nifi.registry.metadata.StandardFlowMetadata;
import org.apache.nifi.registry.serialization.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class RegistryService {

    private final MetadataProvider metadataProvider;
    private final FlowPersistenceProvider flowPersistenceProvider;
    private final Serializer<VersionedFlowSnapshot> snapshotSerializer;
    private final Validator validator;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public RegistryService(@Autowired final MetadataProvider metadataProvider,
                           @Autowired final FlowPersistenceProvider flowPersistenceProvider,
                           @Autowired final Serializer<VersionedFlowSnapshot> snapshotSerializer,
                           @Autowired final Validator validator) {
        this.metadataProvider = metadataProvider;
        this.flowPersistenceProvider = flowPersistenceProvider;
        this.snapshotSerializer = snapshotSerializer;
        this.validator = validator;
        Objects.requireNonNull(this.metadataProvider);
        Objects.requireNonNull(this.flowPersistenceProvider);
        Objects.requireNonNull(this.snapshotSerializer);
        Objects.requireNonNull(this.validator);
    }

    private <T>  void validate(T t, String invalidMessage) {
        final Set<ConstraintViolation<T>> violations = validator.validate(t);
        if (violations.size() > 0) {
            throw new ConstraintViolationException(invalidMessage, violations);
        }
    }

    // ---------------------- Bucket methods ---------------------------------------------

    public Bucket createBucket(final Bucket bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        // set an id, the created time, and clear out the flows since its read-only
        bucket.setIdentifier(UUID.randomUUID().toString());
        bucket.setCreatedTimestamp(System.currentTimeMillis());
        bucket.setVersionedFlows(null);

        validate(bucket, "Bucket is not valid");

        writeLock.lock();
        try {
            final BucketMetadata existingBucketWithSameName = metadataProvider.getBucketByName(bucket.getName());
            if (existingBucketWithSameName != null) {
                throw new IllegalStateException("A bucket with the same name already exists: " + existingBucketWithSameName.getIdentifier());
            }

            final BucketMetadata createdBucket = metadataProvider.createBucket(DataModelMapper.map(bucket));
            return DataModelMapper.map(createdBucket);
        } finally {
            writeLock.unlock();
        }
    }

    public Bucket getBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketMetadata bucket = metadataProvider.getBucketById(bucketIdentifier);
            if (bucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            return DataModelMapper.map(bucket);
        } finally {
            readLock.unlock();
        }
    }

    public Set<Bucket> getBuckets() {
        readLock.lock();
        try {
            final Set<BucketMetadata> buckets = metadataProvider.getBuckets();
            return buckets.stream().map(b -> DataModelMapper.map(b)).collect(Collectors.toSet());
        } finally {
            readLock.unlock();
        }
    }

    public Bucket updateBucket(final Bucket bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        if (bucket.getIdentifier() == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        writeLock.lock();
        try {
            // ensure a bucket with the given id exists
            final BucketMetadata existingBucketById = metadataProvider.getBucketById(bucket.getIdentifier());
            if (existingBucketById == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucket.getIdentifier());
            }

            // ensure a different bucket with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(bucket.getName())) {
                final BucketMetadata existingBucketWithSameName = metadataProvider.getBucketByName(bucket.getName());
                if (existingBucketWithSameName != null && !existingBucketWithSameName.getIdentifier().equals(existingBucketById.getIdentifier())) {
                    throw new IllegalStateException("A bucket with the same name already exists: " + bucket.getName());
                }
            }

            final StandardBucketMetadata.Builder builder = new StandardBucketMetadata.Builder(existingBucketById);

            // transfer over the new values to the existing bucket
            if (StringUtils.isNotBlank(bucket.getName())) {
                builder.name(bucket.getName());
            }

            if (bucket.getDescription() != null) {
                builder.description(bucket.getDescription());
            }

            // perform the actual update
            final BucketMetadata updatedBucket = metadataProvider.updateBucket(builder.build());
            return DataModelMapper.map(updatedBucket);
        } finally {
            writeLock.unlock();
        }
    }

    public Bucket deleteBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketMetadata existingBucket = metadataProvider.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            // retrieve the versioned flows that are in this bucket
            final Set<FlowMetadata> bucketFlows = metadataProvider.getFlows(bucketIdentifier);

            // for each flow in the bucket, delete all snapshots from the flow persistence provider
            for (final FlowMetadata bucketFlow : bucketFlows) {
                flowPersistenceProvider.deleteSnapshots(bucketIdentifier, bucketFlow.getIdentifier());
            }

            // now delete the bucket from the metadata provider, which deletes all flows referencing it
            metadataProvider.deleteBucket(bucketIdentifier);

            return DataModelMapper.map(existingBucket);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- VersionedFlow methods ---------------------------------------------

    public VersionedFlow createFlow(final String bucketIdentifier, final VersionedFlow versionedFlow) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null or blank");
        }

        if (versionedFlow == null) {
            throw new IllegalArgumentException("VersionedFlow cannot be null");
        }

        if (versionedFlow.getBucketIdentifier() != null && !bucketIdentifier.equals(versionedFlow.getBucketIdentifier())) {
            throw new IllegalArgumentException("Bucket identifiers must match");
        }

        if (versionedFlow.getBucketIdentifier() == null) {
            versionedFlow.setBucketIdentifier(bucketIdentifier);
        }

        versionedFlow.setIdentifier(UUID.randomUUID().toString());

        final long timestamp = System.currentTimeMillis();
        versionedFlow.setCreatedTimestamp(timestamp);
        versionedFlow.setModifiedTimestamp(timestamp);

        // clear out the snapshots since they are read-only
        versionedFlow.setSnapshotMetadata(null);

        validate(versionedFlow, "VersionedFlow is not valid");

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketMetadata existingBucket = metadataProvider.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            final FlowMetadata existingFlowWithSameName = metadataProvider.getFlowByName(versionedFlow.getName());
            if (existingFlowWithSameName != null) {
                throw new IllegalStateException("A VersionedFlow with the same name already exists: " + existingFlowWithSameName.getIdentifier());
            }

            // create the flow
            final FlowMetadata createdFlow = metadataProvider.createFlow(bucketIdentifier, DataModelMapper.map(versionedFlow));
            return DataModelMapper.map(createdFlow);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow getFlow(final String flowIdentifier) {
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        readLock.lock();
        try {
            final FlowMetadata flowMetadata = metadataProvider.getFlowById(flowIdentifier);
            if (flowMetadata == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + flowIdentifier);
            }

            return DataModelMapper.map(flowMetadata);
        } finally {
            readLock.unlock();
        }
    }

    public Set<VersionedFlow> getFlows() {
        readLock.lock();
        try {
            final Set<FlowMetadata> flows = metadataProvider.getFlows();
            return flows.stream().map(f -> DataModelMapper.map(f)).collect(Collectors.toSet());
        } finally {
            readLock.unlock();
        }
    }

    public Set<VersionedFlow> getFlows(String bucketId) {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketMetadata existingBucket = metadataProvider.getBucketById(bucketId);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketId);
            }

            final Set<FlowMetadata> flows = metadataProvider.getFlows(bucketId);
            return flows.stream().map(f -> DataModelMapper.map(f)).collect(Collectors.toSet());
        } finally {
            readLock.unlock();
        }
    }

    public VersionedFlow updateFlow(final VersionedFlow versionedFlow) {
        if (versionedFlow == null) {
            throw new IllegalArgumentException("VersionedFlow cannot be null");
        }

        if (StringUtils.isBlank(versionedFlow.getIdentifier())) {
            throw new IllegalArgumentException("VersionedFlow identifier cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure a flow with the given id exists
            final FlowMetadata existingFlow = metadataProvider.getFlowById(versionedFlow.getIdentifier());
            if (existingFlow == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + versionedFlow.getIdentifier());
            }

            // ensure a different flow with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(versionedFlow.getName())) {
                final FlowMetadata existingFlowWithSameName = metadataProvider.getFlowByName(versionedFlow.getName());
                if (existingFlowWithSameName != null && !existingFlowWithSameName.getIdentifier().equals(existingFlow.getIdentifier())) {
                    throw new IllegalStateException("A VersionedFlow with the same name already exists: " + versionedFlow.getName());
                }
            }

            final StandardFlowMetadata.Builder builder = new StandardFlowMetadata.Builder(existingFlow);

            // transfer over the new values to the existing flow
            if (StringUtils.isNotBlank(versionedFlow.getName())) {
                builder.name(versionedFlow.getName());
            }

            if (versionedFlow.getDescription() != null) {
                builder.description(versionedFlow.getDescription());
            }

            builder.modified(System.currentTimeMillis());

            // perform the actual update
            final FlowMetadata updatedFlow = metadataProvider.updateFlow(builder.build());
            return DataModelMapper.map(updatedFlow);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow deleteFlow(final String flowIdentifier) {
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure the flow exists
            final FlowMetadata existingFlow = metadataProvider.getFlowById(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + flowIdentifier);
            }

            // delete all snapshots from the flow persistence provider
            flowPersistenceProvider.deleteSnapshots(existingFlow.getBucketIdentifier(), existingFlow.getIdentifier());

            // now delete the flow from the metadata provider
            metadataProvider.deleteFlow(flowIdentifier);

            return DataModelMapper.map(existingFlow);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- VersionedFlowSnapshot methods ---------------------------------------------

    public VersionedFlowSnapshot createFlowSnapshot(final VersionedFlowSnapshot flowSnapshot) {
        if (flowSnapshot == null) {
            throw new IllegalArgumentException("VersionedFlowSnapshot cannot be null");
        }

        // validation will ensure that the metadata and contents are not null
        if (flowSnapshot.getSnapshotMetadata() != null) {
            flowSnapshot.getSnapshotMetadata().setTimestamp(System.currentTimeMillis());
        }

        validate(flowSnapshot, "VersionedFlowSnapshot is not valid");

        writeLock.lock();
        try {
            final VersionedFlowSnapshotMetadata snapshotMetadata = flowSnapshot.getSnapshotMetadata();

            // ensure the bucket exists
            final BucketMetadata existingBucket = metadataProvider.getBucketById(snapshotMetadata.getBucketIdentifier());
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + snapshotMetadata.getBucketIdentifier());
            }

            // ensure the flow exists
            final FlowMetadata existingFlowMetadata = metadataProvider.getFlowById(snapshotMetadata.getFlowIdentifier());
            if (existingFlowMetadata == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + snapshotMetadata.getFlowIdentifier());
            }

            final VersionedFlow existingFlow = DataModelMapper.map(existingFlowMetadata);

            // if we already have snapshots we need to verify the new one has the correct version
            if (existingFlow.getSnapshotMetadata() != null && existingFlow.getSnapshotMetadata().size() > 0) {
                final VersionedFlowSnapshotMetadata lastSnapshot = existingFlow.getSnapshotMetadata().last();

                if (snapshotMetadata.getVersion() <= lastSnapshot.getVersion()) {
                    throw new IllegalStateException("A VersionedFlowSnapshot with the same version already exists: " + snapshotMetadata.getVersion());
                }

                if (snapshotMetadata.getVersion() > (lastSnapshot.getVersion() + 1)) {
                    throw new IllegalStateException("Version must be a one-up number, last version was "
                            + lastSnapshot.getVersion() + " and version for this snapshot was "
                            + snapshotMetadata.getVersion());
                }
            } else if (snapshotMetadata.getVersion() != 1) {
                throw new IllegalStateException("Version of first snapshot must be 1");
            }

            // serialize the snapshot
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            snapshotSerializer.serialize(flowSnapshot, out);

            // save the serialized snapshot to the persistence provider
            final Bucket bucket = DataModelMapper.map(existingBucket);
            final FlowSnapshotContext context = new StandardFlowSnapshotContext.Builder(bucket, snapshotMetadata).build();
            flowPersistenceProvider.saveSnapshot(context, out.toByteArray());

            // create snapshot in the metadata provider
            metadataProvider.createFlowSnapshot(DataModelMapper.map(snapshotMetadata));
            return flowSnapshot;
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlowSnapshot getFlowSnapshot(final String flowIdentifier, final Integer version) {
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        readLock.lock();
        try {
            // ensure the snapshot exists
            final FlowSnapshotMetadata snapshotMetadata = metadataProvider.getFlowSnapshot(flowIdentifier, version);
            if (snapshotMetadata == null) {
                throw new ResourceNotFoundException("VersionedFlowSnapshot does not exist for flow " + flowIdentifier + " and version " + version);
            }

            // get the serialized bytes of the snapshot
            final byte[] serializedSnapshot = flowPersistenceProvider.getSnapshot(
                    snapshotMetadata.getBucketIdentifier(),
                    snapshotMetadata.getFlowIdentifier(),
                    snapshotMetadata.getVersion()
            );

            if (serializedSnapshot == null || serializedSnapshot.length == 0) {
                throw new IllegalStateException("No serialized content found for snapshot with flow identifier "
                        + flowIdentifier + " and version " + version);
            }

            final InputStream input = new ByteArrayInputStream(serializedSnapshot);
            return snapshotSerializer.deserialize(input);
        } finally {
            readLock.unlock();
        }
    }

    public VersionedFlowSnapshotMetadata deleteFlowSnapshot(final String flowIdentifier, final Integer version) {
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure the snapshot exists
            final FlowSnapshotMetadata snapshotMetadata = metadataProvider.getFlowSnapshot(flowIdentifier, version);
            if (snapshotMetadata == null) {
                throw new ResourceNotFoundException("VersionedFlowSnapshot does not exist for flow "
                        + flowIdentifier + " and version " + version);
            }

            // delete the content of the snapshot
            flowPersistenceProvider.deleteSnapshot(
                    snapshotMetadata.getBucketIdentifier(),
                    snapshotMetadata.getFlowIdentifier(),
                    snapshotMetadata.getVersion());

            // delete the snapshot itself
            metadataProvider.deleteFlowSnapshot(flowIdentifier, version);
            return DataModelMapper.map(snapshotMetadata);
        } finally {
            writeLock.unlock();
        }
    }

}
