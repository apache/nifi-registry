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
import org.apache.nifi.registry.link.LinkableEntity;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * The metadata information about a VersionedFlowSnapshot. This class implements Comparable in order
 * to sort based on the snapshot version in ascending order.
 */
@ApiModel(value = "versionedFlowSnapshot")
public class VersionedFlowSnapshotMetadata extends LinkableEntity implements Comparable<VersionedFlowSnapshotMetadata> {

    @NotBlank
    private String bucketIdentifier;

    // read-only
    private String bucketName;

    @NotBlank
    private String flowIdentifier;

    // read-only
    private String flowName;

    // read-only
    private String flowDescription;

    @Min(1)
    private int version;

    @Min(1)
    private long timestamp;

    private String comments;


    @ApiModelProperty("The identifier of the bucket this snapshot belongs to.")
    public String getBucketIdentifier() {
        return bucketIdentifier;
    }

    public void setBucketIdentifier(String bucketIdentifier) {
        this.bucketIdentifier = bucketIdentifier;
    }

    @ApiModelProperty(value = "The name of the bucket this snapshot belongs to.", readOnly = true)
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @ApiModelProperty("The identifier of the flow this snapshot belongs to.")
    public String getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(String flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @ApiModelProperty(value = "The name of the flow this snapshot belongs to.", readOnly = true)
    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    @ApiModelProperty(value = "The description of the flow this snapshot belongs to.", readOnly = true)
    public String getFlowDescription() {
        return flowDescription;
    }

    public void setFlowDescription(String flowDescription) {
        this.flowDescription = flowDescription;
    }

    @ApiModelProperty("The version of this snapshot of the flow.")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @ApiModelProperty("The timestamp when the flow was saved.")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ApiModelProperty("The comments provided by the user when creating the snapshot.")
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public int compareTo(final VersionedFlowSnapshotMetadata o) {
        return o == null ? -1 : Integer.compare(version, o.version);
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

        final VersionedFlowSnapshotMetadata other = (VersionedFlowSnapshotMetadata) obj;

        return Objects.equals(this.flowIdentifier, other.flowIdentifier)
                && Objects.equals(this.version, other.version);
    }
}
