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
package org.apache.nifi.registry.metadata;

import org.apache.nifi.registry.provider.Provider;

import java.util.Set;

/**
 * A service for managing metadata about all objects stored by the registry.
 *
 * NOTE: Although this interface is intended to be an extension point, it is not yet considered stable and thus may
 * change across releases until the registry matures.
 */
public interface MetadataProvider extends Provider {

    /**
     * Creates the given bucket.
     *
     * @param bucket the bucket to create
     * @return the created bucket
     */
    BucketMetadata createBucket(BucketMetadata bucket);

    /**
     * Retrieves the bucket with the given id.
     *
     * @param bucketIdentifier the id of the bucket to retrieve
     * @return the bucket with the given id, or null if it does not exist
     */
    BucketMetadata getBucketById(String bucketIdentifier);

    /**
     * Retrieves the bucket with the given name. The name comparison must be case-insensitive.
     *
     * @param name the name of the bucket to retrieve
     * @return the bucket with the given name, or null if it does not exist
     */
    BucketMetadata getBucketByName(String name);
    /**
     * Updates the given bucket, only the name and description should be allowed to be updated.
     *
     * @param bucket the updated bucket to save
     * @return the updated bucket, or null if no bucket with the given id exists
     */
    BucketMetadata updateBucket(BucketMetadata bucket);

    /**
     * Deletes the bucket with the given identifier, as well as any objects that reference the bucket.
     *
     * @param bucketIdentifier the id of the bucket to delete
     */
    void deleteBucket(String bucketIdentifier);

    /**
     * Retrieves all buckets known to this metadata provider.
     *
     * @return the set of all buckets
     */
    Set<BucketMetadata> getBuckets();

    /**
     * Creates a versioned flow in the given bucket.
     *
     * @param bucketIdentifier the id of the bucket where the flow is being created
     * @param flow the versioned flow to create
     * @return the created versioned flow
     * @throws IllegalStateException if no bucket with the given identifier exists
     */
    FlowMetadata createFlow(String bucketIdentifier, FlowMetadata flow);

    /**
     * Retrieves the versioned flow with the given id.
     *
     * @param flowIdentifier the identifier of the flow to retrieve
     * @return the versioned flow with the given id, or null if no flow with the given id exists
     */
    FlowMetadata getFlowById(String flowIdentifier);

    /**
     * Retrieves the versioned flow with the given name. The name comparison must be case-insensitive.
     *
     * @param name the name of the flow to retrieve
     * @return the versioned flow with the given name, or null if no flow with the given name exists
     */
    FlowMetadata getFlowByName(String name);

    /**
     * Updates the given versioned flow, only the name and description should be allowed to be updated.
     *
     * @param versionedFlow the updated versioned flow to save
     * @return the updated versioned flow
     */
    FlowMetadata updateFlow(FlowMetadata versionedFlow);

    /**
     * Deletes the versioned flow with the given identifier if one exists.
     *
     * @param flowIdentifier the id of the versioned flow to delete
     */
    void deleteFlow(String flowIdentifier);

    /**
     * Retrieves all versioned flows known to this metadata provider.
     *
     * @return the set of all versioned flows
     */
    Set<FlowMetadata> getFlows();

    /**
     * Retrieves all the versioned flows for the given bucket.
     *
     * @param bucketId the id of the bucket to retrieve flow for
     * @return the set of versioned flows for the given bucket, or an empty set if none exist
     */
    Set<FlowMetadata> getFlows(String bucketId);

    /**
     * Creates a versioned flow snapshot.
     *
     * @param flowSnapshot the snapshot to create
     * @return the created snapshot
     * @throws IllegalStateException if the versioned flow specified by flowSnapshot.getFlowIdentifier() does not exist
     */
    FlowSnapshotMetadata createFlowSnapshot(FlowSnapshotMetadata flowSnapshot);

    /**
     * Retrieves the snapshot for the given flow identifier and snapshot version.
     *
     * @param flowIdentifier the identifier of the flow the snapshot belongs to
     * @param version the version of the snapshot
     * @return the versioned flow snapshot for the given flow identifier and version, or null if none exists
     */
    FlowSnapshotMetadata getFlowSnapshot(String flowIdentifier, Integer version);

    /**
     * Deletes the snapshot for the given flow identifier and version.
     *
     * @param flowIdentifier the identifier of the flow the snapshot belongs to
     * @param version the version of the snapshot
     */
    void deleteFlowSnapshot(String flowIdentifier, Integer version);

}
