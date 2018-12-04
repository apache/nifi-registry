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
 * Filter parameters for extension bundle versions.
 */
public class ExtensionBundleVersionFilterParams {

    private static final ExtensionBundleVersionFilterParams EMPTY_PARAMS = new Builder().build();

    private final String groupId;
    private final String artifactId;
    private final String version;

    private ExtensionBundleVersionFilterParams(final Builder builder) {
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.version = builder.version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public static ExtensionBundleVersionFilterParams of(final String groupId, final String artifactId, final String version) {
        return new Builder().group(groupId).artifact(artifactId).version(version).build();
    }

    public static ExtensionBundleVersionFilterParams empty() {
        return EMPTY_PARAMS;
    }

    public static class Builder {

        private String groupId;
        private String artifactId;
        private String version;

        public Builder group(final String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifact(final String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public ExtensionBundleVersionFilterParams build() {
            return new ExtensionBundleVersionFilterParams(this);
        }
    }

}
