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
import org.apache.nifi.registry.flow.VersionedProcessGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
    private final Serializer<VersionedProcessGroup> processGroupSerializer;
    private final Validator validator;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    @Autowired
    public RegistryService(final MetadataService metadataService,
                           final FlowPersistenceProvider flowPersistenceProvider,
                           final Serializer<VersionedProcessGroup> processGroupSerializer,
                           final Validator validator) {
        this.metadataService = metadataService;
        this.flowPersistenceProvider = flowPersistenceProvider;
        this.processGroupSerializer = processGroupSerializer;
        this.validator = validator;
        Validate.notNull(this.metadataService);
        Validate.notNull(this.flowPersistenceProvider);
        Validate.notNull(this.processGroupSerializer);
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

        validate(bucket, "Cannot create Bucket");

        writeLock.lock();
        try {
            final List<BucketEntity> bucketsWithSameName = metadataService.getBucketsByName(bucket.getName());
            if (bucketsWithSameName.size() > 0) {
                throw new IllegalStateException("A bucket with the same name already exists");
            }

            final BucketEntity createdBucket = metadataService.createBucket(DataModelMapper.map(bucket));
            return DataModelMapper.map(createdBucket);
        } finally {
            writeLock.unlock();
        }
    }

    public Bucket getBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketEntity bucket = metadataService.getBucketById(bucketIdentifier);
            if (bucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            return DataModelMapper.map(bucket);
        } finally {
            readLock.unlock();
        }
    }

    public List<Bucket> getBuckets() {
        readLock.lock();
        try {
            final List<BucketEntity> buckets = metadataService.getAllBuckets();
            return buckets.stream().map(b -> DataModelMapper.map(b)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<Bucket> getBuckets(final Set<String> bucketIds) {
        readLock.lock();
        try {
            final List<BucketEntity> buckets = metadataService.getBuckets(bucketIds);
            return buckets.stream().map(b -> DataModelMapper.map(b)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public Bucket updateBucket(final Bucket bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        if (bucket.getIdentifier() == null) {
            throw new IllegalArgumentException("Bucket identifier cannot be null");
        }

        if (bucket.getName() != null && StringUtils.isBlank(bucket.getName())) {
            throw new IllegalArgumentException("Bucket name cannot be blank");
        }

        writeLock.lock();
        try {
            // ensure a bucket with the given id exists
            final BucketEntity existingBucketById = metadataService.getBucketById(bucket.getIdentifier());
            if (existingBucketById == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucket.getIdentifier());
            }

            // ensure a different bucket with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(bucket.getName())) {
                final List<BucketEntity> bucketsWithSameName = metadataService.getBucketsByName(bucket.getName());
                if (bucketsWithSameName != null) {
                    for (final BucketEntity bucketWithSameName : bucketsWithSameName) {
                        if (!bucketWithSameName.getId().equals(existingBucketById.getId())){
                            throw new IllegalStateException("A bucket with the same name already exists - " + bucket.getName());
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
            return DataModelMapper.map(updatedBucket);
        } finally {
            writeLock.unlock();
        }
    }

    public Bucket deleteBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket identifier cannot be null");
        }

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // for each flow in the bucket, delete all snapshots from the flow persistence provider
            for (final FlowEntity flowEntity : metadataService.getFlowsByBucket(existingBucket.getId())) {
                flowPersistenceProvider.deleteAllFlowContent(bucketIdentifier, flowEntity.getId());
            }

            // now delete the bucket from the metadata provider, which deletes all flows referencing it
            metadataService.deleteBucket(existingBucket);

            return DataModelMapper.map(existingBucket);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- BucketItem methods ---------------------------------------------

    public List<BucketItem> getBucketItems(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketEntity bucket = metadataService.getBucketById(bucketIdentifier);
            if (bucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            final List<BucketItem> bucketItems = new ArrayList<>();
            metadataService.getBucketItems(bucket.getId()).stream().forEach(b -> addBucketItem(bucketItems, b));
            return bucketItems;
        } finally {
            readLock.unlock();
        }
    }

    public List<BucketItem> getBucketItems(final Set<String> bucketIdentifiers) {
        if (bucketIdentifiers == null || bucketIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("Bucket identifiers cannot be null or empty");
        }

        readLock.lock();
        try {
            final List<BucketItem> bucketItems = new ArrayList<>();
            metadataService.getBucketItems(bucketIdentifiers).stream().forEach(b -> addBucketItem(bucketItems, b));
            return bucketItems;
        } finally {
            readLock.unlock();
        }
    }

    private void addBucketItem(final List<BucketItem> bucketItems, final BucketItemEntity itemEntity) {
        if (itemEntity instanceof FlowEntity) {
            final FlowEntity flowEntity = (FlowEntity) itemEntity;

            // Currently we don't populate the bucket name for items
            bucketItems.add(DataModelMapper.map(null, flowEntity));
        } else {
            LOGGER.error("Unknown type of BucketItemEntity: " + itemEntity.getClass().getCanonicalName());
        }
    }

    // ---------------------- VersionedFlow methods ---------------------------------------------

    public VersionedFlow createFlow(final String bucketIdentifier, final VersionedFlow versionedFlow) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (versionedFlow == null) {
            throw new IllegalArgumentException("Versioned flow cannot be null");
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

        validate(versionedFlow, "Cannot create versioned flow");

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // ensure another flow with the same name doesn't exist
            final List<FlowEntity> flowsWithSameName = metadataService.getFlowsByName(existingBucket.getId(), versionedFlow.getName());
            if (flowsWithSameName != null && flowsWithSameName.size() > 0) {
                throw new IllegalStateException("A versioned flow with the same name already exists in the selected bucket");
            }

            // convert from dto to entity and set the bucket relationship
            final FlowEntity flowEntity = DataModelMapper.map(versionedFlow);
            flowEntity.setBucketId(existingBucket.getId());

            // persist the flow and return the created entity
            final FlowEntity createdFlow = metadataService.createFlow(flowEntity);
            return DataModelMapper.map(existingBucket, createdFlow);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow getFlow(final String bucketIdentifier, final String flowIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Versioned flow identifier cannot be null or blank");
        }

        readLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            final FlowEntity existingFlow = metadataService.getFlowByIdWithSnapshotCounts(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            return DataModelMapper.map(existingBucket, existingFlow);
        } finally {
            readLock.unlock();
        }
    }

    public List<VersionedFlow> getFlows(final String bucketId) {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null");
        }

        readLock.lock();
        try {
            final BucketEntity existingBucket = metadataService.getBucketById(bucketId);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketId);
            }

            // return non-verbose set of flows for the given bucket
            final List<FlowEntity> flows = metadataService.getFlowsByBucket(existingBucket.getId());
            return flows.stream().map(f -> DataModelMapper.map(existingBucket, f)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public VersionedFlow updateFlow(final VersionedFlow versionedFlow) {
        if (versionedFlow == null) {
            throw new IllegalArgumentException("Versioned flow cannot be null");
        }

        if (StringUtils.isBlank(versionedFlow.getIdentifier())) {
            throw new IllegalArgumentException("Versioned flow identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(versionedFlow.getBucketIdentifier())) {
            throw new IllegalArgumentException("Versioned flow bucket identifier cannot be null or blank");
        }

        if (versionedFlow.getName() != null && StringUtils.isBlank(versionedFlow.getName())) {
            throw new IllegalArgumentException("Versioned flow name cannot be blank");
        }

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(versionedFlow.getBucketIdentifier());
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + versionedFlow.getBucketIdentifier());
            }

            final FlowEntity existingFlow = metadataService.getFlowByIdWithSnapshotCounts(versionedFlow.getIdentifier());
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + versionedFlow.getIdentifier());
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // ensure a different flow with the same name does not exist
            // since we're allowing partial updates here, only check this if a non-null name is provided
            if (StringUtils.isNotBlank(versionedFlow.getName())) {
                final List<FlowEntity> flowsWithSameName = metadataService.getFlowsByName(existingBucket.getId(), versionedFlow.getName());
                if (flowsWithSameName != null) {
                    for (final FlowEntity flowWithSameName : flowsWithSameName) {
                         if(!flowWithSameName.getId().equals(existingFlow.getId())) {
                            throw new IllegalStateException("A versioned flow with the same name already exists in the selected bucket");
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
            return DataModelMapper.map(existingBucket, updatedFlow);
        } finally {
            writeLock.unlock();
        }
    }

    public VersionedFlow deleteFlow(final String bucketIdentifier, final String flowIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }
        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be null or blank");
        }

        writeLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // delete all snapshots from the flow persistence provider
            flowPersistenceProvider.deleteAllFlowContent(existingFlow.getBucketId(), existingFlow.getId());

            // now delete the flow from the metadata provider
            metadataService.deleteFlow(existingFlow);

            return DataModelMapper.map(existingBucket, existingFlow);
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------- VersionedFlowSnapshot methods ---------------------------------------------

    public VersionedFlowSnapshot createFlowSnapshot(final VersionedFlowSnapshot flowSnapshot) {
        if (flowSnapshot == null) {
            throw new IllegalArgumentException("Versioned flow snapshot cannot be null");
        }

        // validation will ensure that the metadata and contents are not null
        if (flowSnapshot.getSnapshotMetadata() != null) {
            flowSnapshot.getSnapshotMetadata().setTimestamp(System.currentTimeMillis());
        }

        // these fields aren't used for creation
        flowSnapshot.setFlow(null);
        flowSnapshot.setBucket(null);

        validate(flowSnapshot, "Cannot create versioned flow snapshot");

        writeLock.lock();
        try {
            final VersionedFlowSnapshotMetadata snapshotMetadata = flowSnapshot.getSnapshotMetadata();

            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(snapshotMetadata.getBucketIdentifier());
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + snapshotMetadata.getBucketIdentifier());
            }

            // ensure the flow exists, we need to use "with counts" here so we can return this is a part of the response
            final FlowEntity existingFlow = metadataService.getFlowByIdWithSnapshotCounts(snapshotMetadata.getFlowIdentifier());
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + snapshotMetadata.getFlowIdentifier());
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // convert the set of FlowSnapshotEntity to set of VersionedFlowSnapshotMetadata
            final SortedSet<VersionedFlowSnapshotMetadata> sortedSnapshots = new TreeSet<>();
            final List<FlowSnapshotEntity> existingFlowSnapshots = metadataService.getSnapshots(existingFlow.getId());
            if (existingFlowSnapshots != null) {
                existingFlowSnapshots.stream().forEach(s -> sortedSnapshots.add(DataModelMapper.map(existingBucket, s)));
            }

            // if we already have snapshots we need to verify the new one has the correct version
            if (sortedSnapshots != null && sortedSnapshots.size() > 0) {
                final VersionedFlowSnapshotMetadata lastSnapshot = sortedSnapshots.last();

                if (snapshotMetadata.getVersion() <= lastSnapshot.getVersion()) {
                    throw new IllegalStateException("A Versioned flow snapshot with the same version already exists: " + snapshotMetadata.getVersion());
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
            processGroupSerializer.serialize(flowSnapshot.getFlowContents(), out);

            // save the serialized snapshot to the persistence provider
            final Bucket bucket = DataModelMapper.map(existingBucket);
            final VersionedFlow versionedFlow = DataModelMapper.map(existingBucket, existingFlow);
            final FlowSnapshotContext context = new StandardFlowSnapshotContext.Builder(bucket, versionedFlow, snapshotMetadata).build();
            flowPersistenceProvider.saveFlowContent(context, out.toByteArray());

            // create snapshot in the metadata provider
            metadataService.createFlowSnapshot(DataModelMapper.map(snapshotMetadata));

            // update the modified date on the flow
            metadataService.updateFlow(existingFlow);

            flowSnapshot.setBucket(bucket);
            flowSnapshot.setFlow(versionedFlow);
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
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // we need to populate the version count here so we have to do this retrieval instead of snapshotEntity.getFlow()
            final FlowEntity flowEntityWithCount = metadataService.getFlowByIdWithSnapshotCounts(flowIdentifier);
            if (flowEntityWithCount == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist with identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(flowEntityWithCount.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // ensure the snapshot exists
            final FlowSnapshotEntity snapshotEntity = metadataService.getFlowSnapshot(flowIdentifier, version);
            if (snapshotEntity == null) {
                throw new ResourceNotFoundException("Versioned flow snapshot does not exist for flow " + flowIdentifier + " and version " + version);
            }

            // get the serialized bytes of the snapshot
            final byte[] serializedSnapshot = flowPersistenceProvider.getFlowContent(bucketIdentifier, flowIdentifier, version);

            if (serializedSnapshot == null || serializedSnapshot.length == 0) {
                throw new IllegalStateException("No serialized content found for snapshot with flow identifier "
                        + flowIdentifier + " and version " + version);
            }

            // deserialize the contents
            final InputStream input = new ByteArrayInputStream(serializedSnapshot);
            final VersionedProcessGroup flowContents = processGroupSerializer.deserialize(input);

            // map entities to data model
            final Bucket bucket = DataModelMapper.map(existingBucket);
            final VersionedFlow versionedFlow = DataModelMapper.map(existingBucket, flowEntityWithCount);
            final VersionedFlowSnapshotMetadata snapshotMetadata = DataModelMapper.map(existingBucket, snapshotEntity);

            // create the snapshot to return
            final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
            snapshot.setFlowContents(flowContents);
            snapshot.setSnapshotMetadata(snapshotMetadata);
            snapshot.setFlow(versionedFlow);
            snapshot.setBucket(bucket);
            return snapshot;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns all versions of a flow, sorted newest to oldest.
     *
     * @param bucketIdentifier the id of the bucket to search for the flowIdentifier
     * @param flowIdentifier the id of the flow to retrieve from the specified bucket
     * @return all versions of the specified flow, sorted newest to oldest
     */
    public SortedSet<VersionedFlowSnapshotMetadata> getFlowSnapshots(final String bucketIdentifier, final String flowIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be null or blank");
        }

        readLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // convert the set of FlowSnapshotEntity to set of VersionedFlowSnapshotMetadata, ordered by version descending
            final SortedSet<VersionedFlowSnapshotMetadata> sortedSnapshots = new TreeSet<>(Collections.reverseOrder());
            final List<FlowSnapshotEntity> existingFlowSnapshots = metadataService.getSnapshots(existingFlow.getId());
            if (existingFlowSnapshots != null) {
                existingFlowSnapshots.stream().forEach(s -> sortedSnapshots.add(DataModelMapper.map(existingBucket, s)));
            }

            return sortedSnapshots;

        } finally {
            readLock.unlock();
        }
    }

    public VersionedFlowSnapshotMetadata getLatestFlowSnapshotMetadata(final String bucketIdentifier, final String flowIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (StringUtils.isBlank(flowIdentifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be null or blank");
        }

        readLock.lock();
        try {
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // get latest snapshot for the flow
            final FlowSnapshotEntity latestSnapshot = metadataService.getLatestSnapshot(existingFlow.getId());
            return DataModelMapper.map(existingBucket, latestSnapshot);
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
            // ensure the bucket exists
            final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
            if (existingBucket == null) {
                throw new ResourceNotFoundException("Bucket does not exist for identifier " + bucketIdentifier);
            }

            // ensure the flow exists
            final FlowEntity existingFlow = metadataService.getFlowById(flowIdentifier);
            if (existingFlow == null) {
                throw new ResourceNotFoundException("Versioned flow does not exist for identifier " + flowIdentifier);
            }

            if (!existingBucket.getId().equals(existingFlow.getBucketId())) {
                throw new IllegalStateException("The requested flow is not located in the given bucket");
            }

            // ensure the snapshot exists
            final FlowSnapshotEntity snapshotEntity = metadataService.getFlowSnapshot(flowIdentifier, version);
            if (snapshotEntity == null) {
                throw new ResourceNotFoundException("Versioned flow snapshot does not exist for flow "
                        + flowIdentifier + " and version " + version);
            }

            // delete the content of the snapshot
            flowPersistenceProvider.deleteFlowContent(bucketIdentifier, flowIdentifier, version);

            // delete the snapshot itself
            metadataService.deleteFlowSnapshot(snapshotEntity);
            return DataModelMapper.map(existingBucket, snapshotEntity);
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
