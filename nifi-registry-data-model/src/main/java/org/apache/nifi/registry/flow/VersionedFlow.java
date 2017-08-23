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
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.bucket.BucketItemType;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.Collections;
import java.util.SortedSet;

/**
 * <p>
 * Represents a versioned flow. A versioned flow is a named flow that is expected to change
 * over time. This flow is saved to the registry with information such as its name, a description,
 * and each version of the flow.
 * </p>
 *
 * @see VersionedFlowSnapshot
 */
@XmlRootElement
@ApiModel(value = "versionedFlow")
public class VersionedFlow extends BucketItem {

    @Min(0)
    private long versionCount;

    @Valid
    private SortedSet<VersionedFlowSnapshotMetadata> snapshotMetadata = Collections.emptySortedSet();

    public VersionedFlow() {
        super(BucketItemType.FLOW);
    }

    @ApiModelProperty(value = "The number of versions of this flow.", readOnly = true)
    public long getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(long versionCount) {
        this.versionCount = versionCount;
    }

    @ApiModelProperty(value = "The metadata for each snapshot of this flow.", readOnly = true)
    public SortedSet<VersionedFlowSnapshotMetadata> getSnapshotMetadata() {
        return snapshotMetadata;
    }

    public void setSnapshotMetadata(SortedSet<VersionedFlowSnapshotMetadata> snapshotMetadata) {
        this.snapshotMetadata = snapshotMetadata;
    }

}
