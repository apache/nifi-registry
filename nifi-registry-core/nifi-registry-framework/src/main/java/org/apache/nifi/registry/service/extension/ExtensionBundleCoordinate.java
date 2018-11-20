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
package org.apache.nifi.registry.service.extension;

import org.apache.commons.lang3.Validate;

/**
 * The unique coordinate for an extension bundle.
 *
 * This is an alternative to using the single uuid identifier for the bundle.
 */
public class ExtensionBundleCoordinate {

    private final String bucketId;
    private final String groupId;
    private final String artifactId;

    public ExtensionBundleCoordinate(final String bucketId, final String groupId, final String artifactId) {
        this.bucketId = bucketId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        Validate.notBlank(this.bucketId, "Bucket id cannot be null or blank");
        Validate.notBlank(this.groupId, "Group id cannot be null or blank");
        Validate.notBlank(this.artifactId, "Artifact id cannot be null or blank");
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String toString() {
        return bucketId + ":" + groupId + ":" + artifactId;
    }
}
