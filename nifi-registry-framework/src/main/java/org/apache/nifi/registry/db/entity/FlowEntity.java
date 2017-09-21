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
package org.apache.nifi.registry.db.entity;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

@Entity
@Table(name = "FLOW")
@DiscriminatorValue(value = BucketItemEntityType.Values.FLOW)
public class FlowEntity extends BucketItemEntity {

    @OneToMany(
            mappedBy = "flow",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<FlowSnapshotEntity> snapshots;

    @Transient
    private long snapshotCount;

    public FlowEntity() {
        setType(BucketItemEntityType.FLOW);
    }

    public Set<FlowSnapshotEntity> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(Set<FlowSnapshotEntity> snapshots) {
        this.snapshots = snapshots;
    }

    public long getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(long snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    @Override
    public void setType(BucketItemEntityType type) {
        if (BucketItemEntityType.FLOW != type) {
            throw new IllegalStateException("Must set type to FLOW");
        }
        super.setType(type);
    }
}
