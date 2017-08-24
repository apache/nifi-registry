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

import java.util.Objects;

/**
 * Standard immutable implementation of FlowSnapshotMetadata.
 */
public class StandardFlowSnapshotMetadata implements FlowSnapshotMetadata {

    private final String bucketIdentifier;
    private final String flowIdentifier;
    private final String flowName;
    private final int version;
    private final long createdTimestamp;
    private final String comments;

    private StandardFlowSnapshotMetadata(final Builder builder) {
        this.bucketIdentifier = builder.bucketIdentifier;
        this.flowIdentifier = builder.flowIdentifier;
        this.flowName = builder.flowName;
        this.version = builder.version;
        this.createdTimestamp = builder.createdTimestamp;
        this.comments = builder.comments;
    }

    @Override
    public String getBucketIdentifier() {
        return bucketIdentifier;
    }

    @Override
    public String getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String getFlowName() {
        return flowName;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    @Override
    public String getComments() {
        return comments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bucketIdentifier, this.flowIdentifier, Integer.valueOf(this.version));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final StandardFlowSnapshotMetadata other = (StandardFlowSnapshotMetadata) obj;

        return Objects.equals(this.bucketIdentifier, other.bucketIdentifier)
                && Objects.equals(this.flowIdentifier, other.flowIdentifier)
                && Objects.equals(this.version, other.version);
    }

    public static class Builder {

        private String bucketIdentifier;
        private String flowIdentifier;
        private String flowName;
        private int version;
        private long createdTimestamp;
        private String comments;

        public Builder bucketIdentifier(final String bucketIdentifier) {
            this.bucketIdentifier = bucketIdentifier;
            return this;
        }

        public Builder flowIdentifier(final String flowIdentifier) {
            this.flowIdentifier = flowIdentifier;
            return this;
        }

        public Builder flowName(final String flowName) {
            this.flowName = flowName;
            return this;
        }

        public Builder version(final int version) {
            this.version = version;
            return this;
        }

        public Builder created(long createdTimestamp) {
            this.createdTimestamp = createdTimestamp;
            return this;
        }

        public Builder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public StandardFlowSnapshotMetadata build() {
            return new StandardFlowSnapshotMetadata(this);
        }
    }
}
