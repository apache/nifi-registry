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

import java.util.Date;

public class ExtensionBundleVersionEntity {

    // Database id for this specific version of an extension bundle
    private String id;

    // Foreign key to the extension bundle this version goes with
    private String extensionBundleId;

    // The version of this bundle
    private String version;

    // General info about this version of the bundle
    private Date created;
    private String createdBy;
    private String description;

    // The hex representation of the SHA-256 digest for the binary content of this version
    private String sha256Hex;

    // Indicates whether the SHA-256 was supplied by the client, which means it matched the server's calculation, or was not supplied by the client
    private boolean sha256Supplied;

    // The size of binary content in bytes
    private long contentSize;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExtensionBundleId() {
        return extensionBundleId;
    }

    public void setExtensionBundleId(String extensionBundleId) {
        this.extensionBundleId = extensionBundleId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSha256Hex() {
        return sha256Hex;
    }

    public void setSha256Hex(String sha256Hex) {
        this.sha256Hex = sha256Hex;
    }

    public boolean getSha256Supplied() {
        return sha256Supplied;
    }

    public void setSha256Supplied(boolean sha256Supplied) {
        this.sha256Supplied = sha256Supplied;
    }

    public long getContentSize() {
        return contentSize;
    }

    public void setContentSize(long contentSize) {
        this.contentSize = contentSize;
    }

}
