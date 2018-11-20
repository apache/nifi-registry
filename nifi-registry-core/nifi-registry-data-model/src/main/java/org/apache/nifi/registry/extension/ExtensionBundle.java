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
package org.apache.nifi.registry.extension;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.bucket.BucketItemType;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents an extension bundle identified by a group and artifact id with in a bucket.
 *
 * Each bundle may then have one or more versions associated with it by creating an {@link ExtensionBundleVersion}.
 *
 * The {@link ExtensionBundleVersion} represents the actually binary bundle which may contain one or more extensions.
 */
@ApiModel
@XmlRootElement
public class ExtensionBundle extends BucketItem {

    @NotNull
    private ExtensionBundleType bundleType;

    @NotBlank
    private String groupId;

    @NotBlank
    private String artifactId;

    @Min(0)
    private long versionCount;

    public ExtensionBundle() {
        super(BucketItemType.Extension_Bundle);
    }

    @ApiModelProperty(value = "The type of the extension bundle")
    public ExtensionBundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(ExtensionBundleType bundleType) {
        this.bundleType = bundleType;
    }

    @ApiModelProperty(value = "The group id of the extension bundle")
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @ApiModelProperty(value = "The artifact id of the extension bundle")
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @ApiModelProperty(value = "The number of versions of this extension bundle.", readOnly = true)
    public long getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(long versionCount) {
        this.versionCount = versionCount;
    }

}
