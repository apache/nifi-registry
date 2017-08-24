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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Standard immutable implementation of FlowMetadata.
 */
public class StandardFlowMetadata implements FlowMetadata {

    private final String identifier;
    private final String name;
    private final String bucketIdentifier;
    private final long createdTimestamp;
    private final long modifiedTimestamp;
    private final String description;
    private final Set<FlowSnapshotMetadata> snapshotMetadata;
    private final Map<String,FlowSnapshotMetadata> snapshotMetadataByVersion;

    private StandardFlowMetadata(final Builder builder) {
        this.identifier = builder.identifier;
        this.name = builder.name;
        this.bucketIdentifier = builder.bucketIdentifier;
        this.createdTimestamp = builder.createdTimestamp;
        this.modifiedTimestamp = builder.modifiedTimestamp;
        this.description = builder.description;
        this.snapshotMetadata = Collections.unmodifiableSet(
                builder.snapshotMetadata == null
                        ? Collections.emptySet() : new LinkedHashSet<>(builder.snapshotMetadata));

        final Map<String,FlowSnapshotMetadata> tempMetadataMap = new HashMap<>();
        this.snapshotMetadata.stream().forEach(s -> tempMetadataMap.put(String.valueOf(s.getVersion()), s));
        this.snapshotMetadataByVersion = Collections.unmodifiableMap(tempMetadataMap);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBucketIdentifier() {
        return bucketIdentifier;
    }

    @Override
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Set<FlowSnapshotMetadata> getSnapshotMetadata() {
        return snapshotMetadata;
    }

    @Override
    public FlowSnapshotMetadata getSnapshot(int version) {
        return snapshotMetadataByVersion.get(String.valueOf(version));
    }

    public static class Builder {

        private String identifier;
        private String name;
        private String bucketIdentifier;
        private long createdTimestamp;
        private long modifiedTimestamp;
        private String description;
        private Set<FlowSnapshotMetadata> snapshotMetadata = new LinkedHashSet<>();

        public Builder() {

        }

        public Builder(FlowMetadata flowMetadata) {
            identifier(flowMetadata.getIdentifier());
            name(flowMetadata.getName());
            bucketIdentifier(flowMetadata.getBucketIdentifier());
            created(flowMetadata.getCreatedTimestamp());
            modified(flowMetadata.getModifiedTimestamp());
            description(flowMetadata.getDescription());
            addSnapshots(flowMetadata.getSnapshotMetadata());
        }

        public Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder bucketIdentifier(final String bucketIdentifier) {
            this.bucketIdentifier = bucketIdentifier;
            return this;
        }

        public Builder created(final long createdTimestamp) {
            this.createdTimestamp = createdTimestamp;
            return this;
        }

        public Builder modified(final long modifiedTimestamp) {
            this.modifiedTimestamp = modifiedTimestamp;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder addSnapshot(final FlowSnapshotMetadata snapshotMetadata) {
            if (snapshotMetadata != null) {
                this.snapshotMetadata.add(snapshotMetadata);
            }
            return this;
        }

        public Builder addSnapshots(final Collection<FlowSnapshotMetadata> snapshotMetadata) {
            if (snapshotMetadata != null) {
                this.snapshotMetadata.addAll(snapshotMetadata);
            }
            return this;
        }

        public Builder clearSnapshots() {
            this.snapshotMetadata.clear();
            return this;
        }

        public StandardFlowMetadata build() {
            return new StandardFlowMetadata(this);
        }

    }
}
