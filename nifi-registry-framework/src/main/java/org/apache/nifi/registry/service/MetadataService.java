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
import org.apache.nifi.registry.service.params.QueryParameters;

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
     * Retrieves all buckets known to this metadata provider.
     *
     * @param params the paging and sorting params, or null
     * @return the set of all buckets
     */
    List<BucketEntity> getBuckets(QueryParameters params);

    /**
     * Retrieves items across all buckets.
     *
     * @param queryParameters the parameters for retrieving the items, or null
     * @return the set of all items
     */
    List<BucketItemEntity> getBucketItems(QueryParameters queryParameters);

    /**
     * Retrieves items for the given bucket.
     *
     * @param bucket the bucket to retrieve items for
     * @param queryParameters the parameters for retrieving the items, or null
     * @return the set of items for the bucket
     */
    List<BucketItemEntity> getBucketItems(QueryParameters queryParameters, BucketEntity bucket);

    /**
     * Retrieves items for the given buckets.
     *
     * @param buckets the buckets to retrieve items for
     * @param queryParameters the parameters for retrieving the items, or null
     * @return the set of items for the bucket
     */
    List<BucketItemEntity> getBucketItems(QueryParameters queryParameters, Set<BucketEntity> buckets);

    /**
     * Creates a versioned flow in the given bucket.
     *
     * @param flow the versioned flow to create
     * @return the created versioned flow
     * @throws IllegalStateException if no bucket with the given identifier exists
     */
    FlowEntity createFlow(FlowEntity flow);

    /**
     * Retrieves the versioned flow with the given id.
     *
     * @param bucketIdentifier the identifier of the bucket storing the flow
     * @param flowIdentifier the identifier of the flow to retrieve
     * @return the versioned flow with the given id, or null if no flow with the given id exists
     */
    FlowEntity getFlowById(String bucketIdentifier, String flowIdentifier);

    /**
     * Retrieves the versioned flows with the given name. The name comparison must be case-insensitive.
     *
     * @param name the name of the flow to retrieve
     * @return the versioned flows with the given name, or empty list if no flows with the given name exists
     */
    List<FlowEntity> getFlowsByName(String name);

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

    /**
     * Retrieves all versioned flows known to this metadata provider.
     *
     * @param queryParameters the paging and sorting params, or null
     * @return the set of all versioned flows
     */
    List<FlowEntity> getFlows(QueryParameters queryParameters);

    /**
     * Retrieves items for the given buckets.
     *
     * @param bucketIds the ids of buckets to retrieve items for
     * @param queryParameters the parameters for retrieving the items, or null
     * @return the set of items for the bucket
     */
    List<FlowEntity> getFlows(QueryParameters queryParameters, Set<String> bucketIds);

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
     * @param bucketIdentifier the identifier of the bucket storign the flow
     * @param flowIdentifier the identifier of the flow the snapshot belongs to
     * @param version the version of the snapshot
     * @return the versioned flow snapshot for the given flow identifier and version, or null if none exists
     */
    FlowSnapshotEntity getFlowSnapshot(String bucketIdentifier, String flowIdentifier, Integer version);

    /**
     * Deletes the flow snapshot.
     *
     * @param flowSnapshot the flow snapshot to delete
     */
    void deleteFlowSnapshot(FlowSnapshotEntity flowSnapshot);

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