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

import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;

import java.util.List;
import java.util.Set;

/**
 * A service for managing metadata about all objects stored by the registry.
 *
 */
public interface MetadataService {

    /**
     * Creates the given bucket.
     *
     * @param bucket the bucket to create
     * @return the created bucket
     */
    BucketEntity createBucket(BucketEntity bucket);

    /**
     * Retrieves the bucket with the given id.
     *
     * @param bucketIdentifier the id of the bucket to retrieve
     * @return the bucket with the given id, or null if it does not exist
     */
    BucketEntity getBucketById(String bucketIdentifier);

    /**
     * Retrieves the buckets with the given name. The name comparison must be case-insensitive.
     *
     * @param name the name of the bucket to retrieve
     * @return the buckets with the given name, or empty list if none exist
     */
    List<BucketEntity> getBucketsByName(String name);

    /**
     * Updates the given bucket, only the name and description should be allowed to be updated.
     *
     * @param bucket the updated bucket to save
     * @return the updated bucket, or null if no bucket with the given id exists
     */
    BucketEntity updateBucket(BucketEntity bucket);

    /**
     * Deletes the bucket, as well as any objects that reference the bucket.
     *
     * @param bucket the bucket to delete
     */
    void deleteBucket(BucketEntity bucket);

    /**
     * Retrieves all buckets with the given ids.
     *
     * @param bucketIds the ids of the buckets to retrieve
     * @return the set of all buckets
     */
    List<BucketEntity> getBuckets(Set<String> bucketIds);

    /**
     * Retrieves all buckets.
     *
     * @return the set of all buckets
     */
    List<BucketEntity> getAllBuckets();

    // --------------------------------------------------------------------------------------------

    /**
     * Retrieves items for the given bucket.
     *
     * @param bucketId the id of bucket to retrieve items for
     * @return the set of items for the bucket
     */
    List<BucketItemEntity> getBucketItems(String bucketId);

    /**
     * Retrieves items for the given buckets.
     *
     * @param bucketIds the ids of buckets to retrieve items for
     * @return the set of items for the bucket
     */
    List<BucketItemEntity> getBucketItems(Set<String> bucketIds);

    // --------------------------------------------------------------------------------------------

    /**
     * Creates a versioned flow in the given bucket.
     *
     * @param flow the versioned flow to create
     * @return the created versioned flow
     * @throws IllegalStateException if no bucket with the given identifier exists
     */
    FlowEntity createFlow(FlowEntity flow);

    /**
     * Retrieves the versioned flow with the given id and DOES NOT populate the versionCount.
     *
     * @param flowIdentifier the identifier of the flow to retrieve
     * @return the versioned flow with the given id, or null if no flow with the given id exists
     */
    FlowEntity getFlowById(String flowIdentifier);

    /**
     * Retrieves the versioned flow with the given id and DOES populate the versionCount.
     *
     * @param flowIdentifier the identifier of the flow to retrieve
     * @return the versioned flow with the given id, or null if no flow with the given id exists
     */
    FlowEntity getFlowByIdWithSnapshotCounts(String flowIdentifier);

    /**
     * Retrieves the versioned flows with the given name. The name comparison must be case-insensitive.
     *
     * @param name the name of the flow to retrieve
     * @return the versioned flows with the given name, or empty list if no flows with the given name exists
     */
    List<FlowEntity> getFlowsByName(String name);

    /**
     * Retrieves the versioned flows for the given bucket.
     *
     * @param bucketIdentifier the bucket id to retrieve flows for
     * @return the flows in the given bucket
     */
    List<FlowEntity> getFlowsByBucket(String bucketIdentifier);

    /**
     * Updates the given versioned flow, only the name and description should be allowed to be updated.
     *
     * @param flow the updated versioned flow to save
     * @return the updated versioned flow
     */
    FlowEntity updateFlow(FlowEntity flow);

    /**
     * Deletes the flow if one exists.
     *
     * @param flow the flow to delete
     */
    void deleteFlow(FlowEntity flow);

    // --------------------------------------------------------------------------------------------

    /**
     * Creates a versioned flow snapshot.
     *
     * @param flowSnapshot the snapshot to create
     * @return the created snapshot
     * @throws IllegalStateException if the versioned flow specified by flowSnapshot.getFlowIdentifier() does not exist
     */
    FlowSnapshotEntity createFlowSnapshot(FlowSnapshotEntity flowSnapshot);

    /**
     * Retrieves the snapshot for the given flow identifier and snapshot version.
     *
     * @param flowIdentifier the identifier of the flow the snapshot belongs to
     * @param version the version of the snapshot
     * @return the versioned flow snapshot for the given flow identifier and version, or null if none exists
     */
    FlowSnapshotEntity getFlowSnapshot(String flowIdentifier, Integer version);

    /**
     * Retrieves the snapshot with the latest version number for the given flow in the given bucket.
     *
     * @param flowIdentifier the id of flow to retrieve the latest snapshot for
     * @return the latest snapshot for the flow, or null if one doesn't exist
     */
    FlowSnapshotEntity getLatestSnapshot(String flowIdentifier);

    /**
     * Retrieves the snapshots for the given flow in the given bucket.
     *
     * @param flowIdentifier the id of the flow
     * @return the snapshots
     */
    List<FlowSnapshotEntity> getSnapshots(String flowIdentifier);

    /**
     * Deletes the flow snapshot.
     *
     * @param flowSnapshot the flow snapshot to delete
     */
    void deleteFlowSnapshot(FlowSnapshotEntity flowSnapshot);

    // --------------------------------------------------------------------------------------------

    /**
     * @return the set of field names for Buckets
     */
    Set<String> getBucketFields();

    /**
     * @return the set of field names for BucketItems
     */
    Set<String> getBucketItemFields();

    /**
     * @return the set of field names for Flows
     */
    Set<String> getFlowFields();

}