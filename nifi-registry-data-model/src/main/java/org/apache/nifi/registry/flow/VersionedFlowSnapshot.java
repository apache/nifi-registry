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
    private String flowIdentifier;
    private String flowName;
    private int version;
    private long timestamp;
    private String comments;
    private VersionedProcessGroup flowContents;

    @ApiModelProperty("The identifier of the flow this snapshot belongs to")
    public String getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(String flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @ApiModelProperty("The name of the flow this snapshot belongs to")
    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    @ApiModelProperty("The version of this snapshot of the flow")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @ApiModelProperty("The timestamp when the flow was saved")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ApiModelProperty("The comments provided by the user when creating the snapshot")
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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
        return Objects.hash(this.flowIdentifier, Integer.valueOf(this.version));
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
        return Objects.equals(this.flowIdentifier, other.flowIdentifier) && Objects.equals(this.version, other.version);
    }
}
