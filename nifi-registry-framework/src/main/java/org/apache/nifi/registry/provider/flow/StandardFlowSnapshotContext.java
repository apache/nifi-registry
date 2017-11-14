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
package org.apache.nifi.registry.provider.flow;

import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.FlowSnapshotContext;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

/**
 * Standard implementation of FlowSnapshotContext.
 */
public class StandardFlowSnapshotContext implements FlowSnapshotContext {

    private final String bucketId;
    private final String bucketName;
    private final String flowId;
    private final String flowName;
    private final int version;
    private final String comments;
    private final long snapshotTimestamp;

    private StandardFlowSnapshotContext(final Builder builder) {
        this.bucketId = builder.bucketId;
        this.bucketName = builder.bucketName;
        this.flowId = builder.flowId;
        this.flowName = builder.flowName;
        this.version = builder.version;
        this.comments = builder.comments;
        this.snapshotTimestamp = builder.snapshotTimestamp;

        Validate.notBlank(bucketId);
        Validate.notBlank(bucketName);
        Validate.notBlank(flowId);
        Validate.notBlank(flowName);
        Validate.isTrue(version > 0);
        Validate.isTrue(snapshotTimestamp > 0);
    }

    @Override
    public String getBucketId() {
        return bucketId;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getFlowId() {
        return flowId;
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
    public String getComments() {
        return comments;
    }

    @Override
    public long getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    /**
     * Builder for creating instances of StandardFlowSnapshotContext.
     */
    public static class Builder {

        private String bucketId;
        private String bucketName;
        private String flowId;
        private String flowName;
        private int version;
        private String comments;
        private long snapshotTimestamp;

        public Builder() {

        }

        public Builder(final Bucket bucket, final VersionedFlow versionedFlow, final VersionedFlowSnapshotMetadata snapshotMetadata) {
            bucketId(bucket.getIdentifier());
            bucketName(bucket.getName());
            flowId(snapshotMetadata.getFlowIdentifier());
            flowName(versionedFlow.getName());
            version(snapshotMetadata.getVersion());
            comments(snapshotMetadata.getComments());
            snapshotTimestamp(snapshotMetadata.getTimestamp());
        }

        public Builder bucketId(final String bucketId) {
            this.bucketId = bucketId;
            return this;
        }

        public Builder bucketName(final String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder flowId(final String flowId) {
            this.flowId = flowId;
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

        public Builder comments(final String comments) {
            this.comments = comments;
            return this;
        }

        public Builder snapshotTimestamp(final long snapshotTimestamp) {
            this.snapshotTimestamp = snapshotTimestamp;
            return this;
        }

        public StandardFlowSnapshotContext build() {
            return new StandardFlowSnapshotContext(this);
        }

    }

}
