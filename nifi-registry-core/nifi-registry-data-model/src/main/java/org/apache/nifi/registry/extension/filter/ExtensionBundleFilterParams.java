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
package org.apache.nifi.registry.extension.filter;

/**
 * Filter parameters for retrieving extension bundles.
 */
public class ExtensionBundleFilterParams {

    private static final ExtensionBundleFilterParams EMPTY_PARAMS = new Builder().build();

    private final String bucketName;
    private final String groupId;
    private final String artifactId;

    private ExtensionBundleFilterParams(final Builder builder) {
        this.bucketName = builder.bucketName;
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public static ExtensionBundleFilterParams of(final String bucketName, final String groupId, final String artifactId) {
        return new Builder().bucket(bucketName).group(groupId).artifact(artifactId).build();
    }

    public static ExtensionBundleFilterParams of(final String groupId, final String artifactId) {
        return new Builder().group(groupId).artifact(artifactId).build();
    }

    public static ExtensionBundleFilterParams empty() {
        return EMPTY_PARAMS;
    }

    public static class Builder {

        private String bucketName;
        private String groupId;
        private String artifactId;

        public Builder bucket(final String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder group(final String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifact(final String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public ExtensionBundleFilterParams build() {
            return new ExtensionBundleFilterParams(this);
        }
    }

}
