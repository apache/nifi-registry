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

import java.util.Set;

/**
 * The metadata about a flow, including the metadata about any of it's snapshots.
 */
public interface FlowMetadata {

    /**
     * @return the identifier of this flow
     */
    String getIdentifier();

    /**
     * @return the name of this flow
     */
    String getName();

    /**
     * @return the identifier of the bucket this flow belongs to
     */
    String getBucketIdentifier();

    /**
     * @return the timestamp this flow was created
     */
    long getCreatedTimestamp();

    /**
     * @return the timestamp this flow was modified
     */
    long getModifiedTimestamp();

    /**
     * @return the description of this flow
     */
    String getDescription();

    /**
     * @return the metadata for the snapshots of this flow
     */
    Set<FlowSnapshotMetadata> getSnapshotMetadata();

    /**
     * Get the snapshot for the given version.
     *
     * @param version the version of a snapshot
     * @return the snapshot for the given version, or null if one doesn't exist
     */
    FlowSnapshotMetadata getSnapshot(int version);

}
