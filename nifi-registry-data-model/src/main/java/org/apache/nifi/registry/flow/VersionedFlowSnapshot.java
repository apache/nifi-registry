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

import java.util.Objects;

/**
 * <p>
 * Represents a snapshot of a versioned flow. A versioned flow may change many times
 * over the course of its life. Each of these versions that is saved to the registry
 * is saved as a snapshot, representing information such as the name of the flow, the
 * version of the flow, the timestamp when it was saved, the contents of the flow, etc.
 * </p>
 */
@ApiModel(value = "versionedFlowSnapshot")
public class VersionedFlowSnapshot {

    private VersionedFlowSnapshotMetadata snapshotMetadata;
    private VersionedProcessGroup flowContents;

    @ApiModelProperty("The metadata for this snapshot")
    public VersionedFlowSnapshotMetadata getSnapshotMetadata() {
        return snapshotMetadata;
    }

    public void setSnapshotMetadata(VersionedFlowSnapshotMetadata snapshotMetadata) {
        this.snapshotMetadata = snapshotMetadata;
    }

    @ApiModelProperty("The contents of the versioned flow")
    public VersionedProcessGroup getFlowContents() {
        return flowContents;
    }

    public void setFlowContents(VersionedProcessGroup flowContents) {
        this.flowContents = flowContents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.snapshotMetadata);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final VersionedFlowSnapshot other = (VersionedFlowSnapshot) obj;
        return Objects.equals(this.snapshotMetadata, other.snapshotMetadata);
    }

}
