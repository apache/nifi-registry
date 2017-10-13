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
import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.flow.FlowSnapshotContext;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.provider.flow.StandardFlowSnapshotContext;
import org.apache.nifi.registry.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Main service for all back-end operations, REST resources should only interact with this service.
 *
 * This service is marked as @Transactional so that Spring will automatically start a transaction upon entering
 * any method, and will rollback the transaction if any Exception is thrown out of a method.
 *
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryService.class);

    private final MetadataService metadataService;
    private final FlowPersistenceProvider flowPersistenceProvider;
    private final Serializer<VersionedFlowSnapshot> snapshotSerializer;
    private final Validator validator;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    @Autowired
    public RegistryService(final MetadataService metadataService,
                           final FlowPersistenceProvider flowPersistenceProvider,
                           final Serializer<VersionedFlowSnapshot> snapshotSerializer,
                           final Validator validator) {
        this.metadataService = metadataService;
        this.flowPersistenceProvider = flowPersistenceProvider;
        this.snapshotSerializer = snapshotSerializer;
        this.validator = validator;
        Validate.notNull(this.metadataService);
        Validate.notNull(this.flowPersistenceProvider);
        Validate.notNull(this.snapshotSerializer);
        Validate.notNull(this.validator);
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
            final List<BucketEntity> bucketsWithSameName = metadataService.getBucketsByName(bucket.getName());
            if (bucketsWithSameName.size() > 0) {
                throw new IllegalStateException("A bucket with the same name already exists");
            }

            final BucketEntity createdBucket = metadataService.createBucket(DataModelMapper.map(bucket));
            return DataModelMapper.map(createdBucket, false);
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
            final BucketEntity bucket = metadataService.getBucketById(bucketIdentifier);
            if (bucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            return DataModelMapper.map(bucket, false);
        } finally {
            readLock.unlock();
        }
    }

    public List<Bucket> getBuckets() {
        readLock.lock();
        try {
            final List<BucketEntity> buckets = metadataService.getAllBuckets();
            return buckets.stream().map(b -> DataModelMapper.map(b, false)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<Bucket> getBuckets(final QueryParameters queryParameters, final Set<String> bucketIds) {
        readLock.lock();
        try {
            final List<BucketEntity> buckets = metadataService.getBuckets(queryParameters, bucketIds);
            return buckets.stream().map(b -> DataModelMapper.map(b, false)).collect(Collectors.toList());
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
            final BucketEntity existingBucketById = metadataService.getBucketById(bucket.getIdentifier());
            if (existingBucketById == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucket.getIdentifier());
            }

            // ensure a different bucket with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(bucket.getName())) {
                final List<BucketEntity> bucketsWithSameName = metadataService.getBucketsByName(bucket.getName());
                if (bucketsWithSameName != null) {
                    for (final BucketEntity bucketWithSameName : bucketsWithSameName) {
                        if (!bucketWithSameName.getId().equals(existingBucketById.getId())){
                            throw new IllegalStateException("A bucket with the same name already exists: " + bucket.getName());
                        }
                    }
                }
            }

            // transfer over the new values to the existing bucket
            if (StringUtils.isNotBlank(bucket.getName())) {
                existingBucketById.setName(bucket.getName());
            }

            if (bucket.getDescription() != null) {
                existingBucketById.setDescription(bucket.getDescription());
            }

            // perform the actual update
            final BucketEntity updatedBucket = metadataService.updateBucket(existingBucketById);
            return DataModelMapper.map(updatedBucket, false);
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
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            // for each flow in the bucket, delete all snapshots from the flow persistence provider
            for (final FlowEntity flowEntity : existingBucket.getFlows()) {
                flowPersistenceProvider.deleteSnapshots(bucketIdentifier, flowEntity.getId());
            }

            // now delete the bucket from the metadata provider, which deletes all flows referencing it
            metadataService.deleteBucket(existingBucket);

            return DataModelMapper.map(existingBucket, false);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- BucketItem methods ---------------------------------------------

    public List<BucketItem> getBucketItems(final QueryParameters queryParameters, final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketEntity bucket = metadataService.getBucketById(bucketIdentifier);
            if (bucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            final List<BucketItem> bucketItems = new ArrayList<>();
            metadataService.getBucketItems(queryParameters, bucket).stream().forEach(b -> addBucketItem(bucketItems, b));
            return bucketItems;
        } finally {
            readLock.unlock();
        }
    }

    public List<BucketItem> getBucketItems(final QueryParameters queryParameters, final Set<String> bucketIdentifiers) {
        if (bucketIdentifiers == null || bucketIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("Bucket Identifiers cannot be null or empty");
        }

        readLock.lock();
        try {
            final List<BucketItem> bucketItems = new ArrayList<>();
            metadataService.getBucketItems(queryParameters, bucketIdentifiers).stream().forEach(b -> addBucketItem(bucketItems, b));
            return bucketItems;
        } finally {
            readLock.unlock();
        }
    }

    private void addBucketItem(final List<BucketItem> bucketItems, final BucketItemEntity itemEntity) {
        if (itemEntity instanceof FlowEntity) {
            final FlowEntity flowEntity = (FlowEntity) itemEntity;
            bucketItems.add(DataModelMapper.map(flowEntity, false));
        } else {
            LOGGER.error("Unknown type of BucketItemEntity: " + itemEntity.getClass().getCanonicalName());
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
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketIdentifier);
            }

            // ensure another flow with the same name doesn't exist
            final List<FlowEntity> flowsWithSameName = metadataService.getFlowsByName(versionedFlow.getName());
            if (flowsWithSameName != null && flowsWithSameName.size() > 0) {
                throw new IllegalStateException("A VersionedFlow with the same name already exists");
            }

            // convert from dto to entity and set the bucket relationship
            final FlowEntity flowEntity = DataModelMapper.map(versionedFlow);
            flowEntity.setBucket(existingBucket);

            // persist the flow and return the created entity
            final FlowEntity createdFlow = metadataService.createFlow(flowEntity);
            return DataModelMapper.map(createdFlow, false);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow getFlow(final String bucketIdentifier, final String flowIdentifier, final boolean verbose) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket Identifier Identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        readLock.lock();
        try {
            final FlowEntity flowEntity = metadataService.getFlowByIdWithSnapshotCounts(bucketIdentifier, flowIdentifier);
            if (flowEntity == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + flowIdentifier);
            }

            return DataModelMapper.map(flowEntity, verbose);
        } finally {
            readLock.unlock();
        }
    }

    public List<VersionedFlow> getFlows(final String bucketId) {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketEntity existingBucket = metadataService.getBucketById(bucketId);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + bucketId);
            }

            // return non-verbose set of flows for the given bucket
            final List<FlowEntity> flows = metadataService.getFlowsByBucket(existingBucket);
            return flows.stream().map(f -> DataModelMapper.map(f, false)).collect(Collectors.toList());
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

        if (StringUtils.isBlank(versionedFlow.getBucketIdentifier())) {
            throw new IllegalArgumentException("VersionedFlow bucket identifier cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure a flow with the given id exists
            final FlowEntity existingFlow = metadataService.getFlowById(versionedFlow.getBucketIdentifier(), versionedFlow.getIdentifier());
            if (existingFlow == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + versionedFlow.getIdentifier());
            }

            // ensure a different flow with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(versionedFlow.getName())) {
                final List<FlowEntity> flowsWithSameName = metadataService.getFlowsByName(versionedFlow.getName());
                if (flowsWithSameName != null) {
                    for (final FlowEntity flowWithSameName : flowsWithSameName) {
                         if(!flowWithSameName.getId().equals(existingFlow.getId())) {
                            throw new IllegalStateException("A VersionedFlow with the same name already exists: " + versionedFlow.getName());
                        }
                    }
                }
            }

            // transfer over the new values to the existing flow
            if (StringUtils.isNotBlank(versionedFlow.getName())) {
                existingFlow.setName(versionedFlow.getName());
            }

            if (versionedFlow.getDescription() != null) {
                existingFlow.setDescription(versionedFlow.getDescription());
            }

            // perform the actual update
            final FlowEntity updatedFlow = metadataService.updateFlow(existingFlow);
            return DataModelMapper.map(updatedFlow, false);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow deleteFlow(final String bucketIdentifier, final String flowIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null or blank");
        }
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow Identifier cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(bucketIdentifier, flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + flowIdentifier);
            }

            // delete all snapshots from the flow persistence provider
            flowPersistenceProvider.deleteSnapshots(existingFlow.getBucket().getId(), existingFlow.getId());

            // now delete the flow from the metadata provider
            metadataService.deleteFlow(existingFlow);

            return DataModelMapper.map(existingFlow, false);
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
            final BucketEntity existingBucket = metadataService.getBucketById(snapshotMetadata.getBucketIdentifier());
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier: " + snapshotMetadata.getBucketIdentifier());
            }

            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(snapshotMetadata.getBucketIdentifier(), snapshotMetadata.getFlowIdentifier());
            if (existingFlow == null) {
                throw new ResourceNotFoundException("VersionedFlow does not exist for identifier: " + snapshotMetadata.getFlowIdentifier());
            }

            final VersionedFlow versionedFlow = DataModelMapper.map(existingFlow, true);

            // if we already have snapshots we need to verify the new one has the correct version
            if (versionedFlow.getSnapshotMetadata() != null && versionedFlow.getSnapshotMetadata().size() > 0) {
                final VersionedFlowSnapshotMetadata lastSnapshot = versionedFlow.getSnapshotMetadata().last();

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
            final Bucket bucket = DataModelMapper.map(existingBucket, false);
            final FlowSnapshotContext context = new StandardFlowSnapshotContext.Builder(bucket, snapshotMetadata).build();
            flowPersistenceProvider.saveSnapshot(context, out.toByteArray());

            // create snapshot in the metadata provider
            metadataService.createFlowSnapshot(DataModelMapper.map(snapshotMetadata));

            // update the modified date on the flow
            existingFlow.setModified(new Date());
            metadataService.updateFlow(existingFlow);

            return flowSnapshot;
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlowSnapshot getFlowSnapshot(final String bucketIdentifier, final String flowIdentifier, final Integer version) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be null or blank");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        readLock.lock();
        try {
            // ensure the snapshot exists
            final FlowSnapshotEntity snapshotEntity = metadataService.getFlowSnapshot(bucketIdentifier, flowIdentifier, version);
            if (snapshotEntity == null) {
                throw new ResourceNotFoundException("VersionedFlowSnapshot does not exist for flow " + flowIdentifier + " and version " + version);
            }

            // get the serialized bytes of the snapshot
            final byte[] serializedSnapshot = flowPersistenceProvider.getSnapshot(bucketIdentifier, flowIdentifier, version);

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

    public VersionedFlowSnapshotMetadata deleteFlowSnapshot(final String bucketIdentifier, final String flowIdentifier, final Integer version) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be null or blank");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure the snapshot exists
            final FlowSnapshotEntity snapshotEntity = metadataService.getFlowSnapshot(bucketIdentifier, flowIdentifier, version);
            if (snapshotEntity == null) {
                throw new ResourceNotFoundException("VersionedFlowSnapshot does not exist for flow "
                        + flowIdentifier + " and version " + version);
            }

            // delete the content of the snapshot
            flowPersistenceProvider.deleteSnapshot(bucketIdentifier, flowIdentifier, version);

            // delete the snapshot itself
            metadataService.deleteFlowSnapshot(snapshotEntity);
            return DataModelMapper.map(snapshotEntity);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- Field methods ---------------------------------------------

    public Set<String> getBucketFields() {
        return metadataService.getBucketFields();
    }

    public Set<String> getBucketItemFields() {
        return metadataService.getBucketItemFields();
    }

    public Set<String> getFlowFields() {
        return metadataService.getFlowFields();
    }

}
