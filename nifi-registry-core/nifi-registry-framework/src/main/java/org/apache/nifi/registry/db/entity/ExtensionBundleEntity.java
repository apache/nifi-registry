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

public class ExtensionBundleEntity extends BucketItemEntity {

    private String groupId;

    private String artifactId;

    private ExtensionBundleEntityType bundleType;

    private long versionCount;

    public ExtensionBundleEntity() {
        setType(BucketItemEntityType.EXTENSION_BUNDLE);
    }

    public ExtensionBundleEntityType getBundleType() {
        return bundleType;
    }

    public void setBundleType(ExtensionBundleEntityType bundleType) {
        this.bundleType = bundleType;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public long getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(long versionCount) {
        this.versionCount = versionCount;
    }

    @Override
    public void setType(BucketItemEntityType type) {
        if (BucketItemEntityType.EXTENSION_BUNDLE != type) {
            throw new IllegalStateException("Must set type to " + BucketItemEntityType.Values.EXTENSION_BUNDLE);
        }
        super.setType(type);
    }

}
