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
import org.apache.nifi.registry.link.LinkableEntity;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@ApiModel
@XmlRootElement
public class ExtensionBundleVersionMetadata extends LinkableEntity implements Comparable<ExtensionBundleVersionMetadata> {

    @NotBlank
    private String id;

    @NotBlank
    private String extensionBundleId;

    @NotBlank
    private String bucketId;

    @NotBlank
    private String version;

    private ExtensionBundleVersionDependency dependency;

    @Min(1)
    private long timestamp;

    @NotBlank
    private String author;

    private String description;

    @NotBlank
    private String sha256Hex;


    @ApiModelProperty(value = "The id of this version of the extension bundle")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The id of the extension bundle this version is for")
    public String getExtensionBundleId() {
        return extensionBundleId;
    }

    public void setExtensionBundleId(String extensionBundleId) {
        this.extensionBundleId = extensionBundleId;
    }

    @ApiModelProperty(value = "The id of the bucket the extension bundle belongs to", required = true)
    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    @ApiModelProperty(value = "The version of the extension bundle")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @ApiModelProperty(value = "The optional bundle dependency (i.e. another bundle this bundle is dependent on)")
    public ExtensionBundleVersionDependency getDependency() {
        return dependency;
    }

    public void setDependency(ExtensionBundleVersionDependency dependency) {
        this.dependency = dependency;
    }

    @ApiModelProperty(value = "The timestamp of the create date of this version")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ApiModelProperty(value = "The identity that created this version")
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @ApiModelProperty(value = "The description for this version")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "The hex representation of the SHA-256 digest of the binary content for this version")
    public String getSha256Hex() {
        return sha256Hex;
    }

    public void setSha256Hex(String sha256Hex) {
        this.sha256Hex = sha256Hex;
    }

    @Override
    public int compareTo(final ExtensionBundleVersionMetadata o) {
        return o == null ? -1 : version.compareTo(o.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final ExtensionBundleVersionMetadata other = (ExtensionBundleVersionMetadata) obj;
        return Objects.equals(this.id, other.id);
    }
}
