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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.nifi.registry.bucket.BucketObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Represents a versioned flow. A versioned flow is a named flow that is expected to change
 * over time. This flow is saved to the registry with information such as its name, a description,
 * and each version of the flow.
 * </p>
 *
 * @see VersionedFlowSnapshot
 */
@ApiModel(value = "versionedFlow")
public class VersionedFlow extends BucketObject {

    private String name;
    private long createdTimestamp;
    private long modifiedTimestamp;
    private String description;
    private int currentMaxVersion = 0;
    private ArrayList<VersionedFlowSnapshot> snapshots = new ArrayList<>();
    private Map<Integer, VersionedFlowSnapshot> snapshotsByVersion = new HashMap<>(); // TODO, could use a third-party collection type that supports primitive keys.

    @ApiModelProperty("The name of the flow.")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("The timestamp of when the flow was first created.")
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long timestamp) {
        this.createdTimestamp = timestamp;
    }

    @ApiModelProperty("The timestamp of when the flow was last modified, e.g., when a new version was saved.")
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @ApiModelProperty("A description of the flow.")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<VersionedFlowSnapshot> getSnapshots() {
        return snapshots;
    }

    public VersionedFlowSnapshot getSnapshot(int version) {
        return snapshotsByVersion.get(Integer.valueOf(version));
    }

    /**
     * Add a new snapshot version to this VersionedFlow.
     *
     * Note that this method has potential side effects.
     * If a snapshot version number is not a positive integer,
     * a new version number will be set as the current max version number + 1.
     *
     * @param snapshot  The snapshot to add to this versionedFlow
     */
    public void addVersionedFlowSnapshot(VersionedFlowSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        int snapshotVersion = snapshot.getVersion();
        if (snapshotVersion < 1) {
            snapshotVersion = ++currentMaxVersion;
            snapshot.setVersion(snapshotVersion);
        } else if (snapshotsByVersion.containsKey(Integer.valueOf(snapshotVersion))) {
            throw new IllegalStateException("Unable to add snapshot to VersionedFlow with duplicate version number '" + snapshotVersion + "'.");
        } else {
            currentMaxVersion = (snapshotVersion > currentMaxVersion) ? snapshotVersion : currentMaxVersion;
        }

        snapshots.add(snapshot);
        snapshotsByVersion.put(Integer.valueOf(snapshotVersion), snapshot);
    }

}
