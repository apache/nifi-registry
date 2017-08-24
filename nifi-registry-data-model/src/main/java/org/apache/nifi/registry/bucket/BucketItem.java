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
package org.apache.nifi.registry.bucket;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel("bucketItem")
public class BucketItem {

    private String identifier;
    private String name;
    private String bucketIdentifier;
    private long createdTimestamp;
    private long modifiedTimestamp;
    private BucketItemType type;

    @ApiModelProperty("An ID to uniquely identify this object.")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty("The name of the item.")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("The identifier of the bucket this items belongs to.")
    public String getBucketIdentifier() {
        return bucketIdentifier;
    }

    public void setBucketIdentifier(String bucketIdentifier) {
        this.bucketIdentifier = bucketIdentifier;
    }

    @ApiModelProperty("The timestamp of when the item was created.")
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @ApiModelProperty("The timestamp of when the item was last modified.")
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @ApiModelProperty("The type of item.")
    public BucketItemType getType() {
        return type;
    }

    public void setType(BucketItemType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.identifier);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final BucketItem other = (BucketItem) obj;
        return Objects.equals(this.identifier, other.identifier);
    }
}
