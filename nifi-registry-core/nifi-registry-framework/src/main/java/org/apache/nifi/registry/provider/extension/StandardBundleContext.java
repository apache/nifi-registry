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
package org.apache.nifi.registry.provider.extension;

import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.extension.BundleContext;

public class StandardBundleContext implements BundleContext {

    private final BundleType bundleType;
    private final String bucketId;
    private final String bucketName;
    private final String bundleId;
    private final String bundleGroupId;
    private final String bundleArtifactId;
    private final String bundleVersion;
    private final String description;
    private final String author;
    private final long timestamp;
    private final long bundleSize;

    private StandardBundleContext(final Builder builder) {
        this.bundleType = builder.bundleType;
        this.bucketId = builder.bucketId;
        this.bucketName = builder.bucketName;
        this.bundleId = builder.bundleId;
        this.bundleGroupId = builder.bundleGroupId;
        this.bundleArtifactId = builder.bundleArtifactId;
        this.bundleVersion = builder.bundleVersion;
        this.bundleSize = builder.bundleSize;
        this.description = builder.description;
        this.author = builder.author;
        this.timestamp = builder.timestamp;
        Validate.notNull(this.bundleType);
        Validate.notBlank(this.bucketId);
        Validate.notBlank(this.bucketName);
        Validate.notBlank(this.bundleId);
        Validate.notBlank(this.bundleGroupId);
        Validate.notBlank(this.bundleArtifactId);
        Validate.notBlank(this.bundleVersion);
        Validate.notBlank(this.author);
    }


    @Override
    public BundleType getBundleType() {
        return bundleType;
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
    public String getBundleId() {
        return bundleId;
    }

    @Override
    public String getBundleGroupId() {
        return bundleGroupId;
    }

    @Override
    public String getBundleArtifactId() {
        return bundleArtifactId;
    }

    @Override
    public String getBundleVersion() {
        return bundleVersion;
    }

    @Override
    public long getBundleSize() {
        return bundleSize;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    public static class Builder {

        private BundleType bundleType;
        private String bucketId;
        private String bucketName;
        private String bundleId;
        private String bundleGroupId;
        private String bundleArtifactId;
        private String bundleVersion;
        private String description;
        private String author;
        private long timestamp;
        private long bundleSize;

        public Builder bundleType(final BundleType bundleType) {
            this.bundleType = bundleType;
            return this;
        }

        public Builder bucketId(final String bucketId) {
            this.bucketId = bucketId;
            return this;
        }

        public Builder bucketName(final String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder bundleId(final String bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public Builder bundleGroupId(final String bundleGroupId) {
            this.bundleGroupId = bundleGroupId;
            return this;
        }

        public Builder bundleArtifactId(final String bundleArtifactId) {
            this.bundleArtifactId = bundleArtifactId;
            return this;
        }

        public Builder bundleVersion(final String bundleVersion) {
            this.bundleVersion = bundleVersion;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder author(final String author) {
            this.author = author;
            return this;
        }

        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder bundleSize(final long size) {
            this.bundleSize = size;
            return this;
        }

        public StandardBundleContext build() {
            return new StandardBundleContext(this);
        }

    }

}
