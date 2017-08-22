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
package org.apache.nifi.registry.flow;

import org.apache.nifi.registry.provider.Provider;

/**
 * A service that can store and retrieve versioned flow snapshots.
 *
 * NOTE: Although this interface is intended to be an extension point, it is not yet considered stable and thus may
 * change across releases until the registry matures.
 */
public interface FlowPersistenceProvider extends Provider {

    /**
     * Persists a serialized versioned flow snapshot.
     *
     * @param context the context for the snapshot being persisted
     * @param content the serialized snapshot to persist
     * @throws FlowPersistenceException if the snapshot could not be persisted
     */
    void saveSnapshot(FlowSnapshotContext context, byte[] content) throws FlowPersistenceException;

    /**
     * Retrieves a versioned flow snapshot.
     *
     * @param bucketId the bucket id where the snapshot is located
     * @param flowId the id of the versioned flow the snapshot belongs to
     * @param version the version of the snapshot
     * @return the bytes for the requested snapshot, or null if not found
     * @throws FlowPersistenceException if the snapshot could not be retrieved due to an error in underlying provider
     */
    byte[] getSnapshot(String bucketId, String flowId, int version) throws FlowPersistenceException;

    /**
     * Deletes all snapshots for the versioned flow with the given id.
     *
     * @param bucketId the bucket the versioned flow belongs to
     * @param flowId the id of the versioned flow
     * @throws FlowPersistenceException if the snapshots could not be deleted due to an error in underlying provider
     */
    void deleteSnapshots(String bucketId, String flowId) throws FlowPersistenceException;

    /**
     * Deletes the given snapshot.
     *
     * @param bucketId the bucket id where the snapshot is located
     * @param flowId the id of the versioned flow the snapshot belongs to
     * @param version the version of the snapshot
     * @throws FlowPersistenceException if the snapshot could not be deleted due to an error in underlying provider
     */
    void deleteSnapshot(String bucketId, String flowId, int version) throws FlowPersistenceException;

}
