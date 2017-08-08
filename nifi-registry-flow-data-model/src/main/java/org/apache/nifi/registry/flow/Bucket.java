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
package org.apache.nifi.registry.flow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApiModel(value = "bucket")
public class Bucket {

    private String identifier;
    private String name;
    private long createdTimestamp;
    private String description;
    private Map<String, BucketObject> bucketObjectMap = new HashMap<>();

    @ApiModelProperty("The id of the bucket. This is set by the server at creation time.")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty("The name of the bucket.")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("The timestamp of when the bucket was first created. This is set by the server at creation time.")
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @ApiModelProperty("A description of the bucket.")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Add a new object version to this bucket.
     *
     * Note that this method has a potential side effect.
     * If a BucketObject ID is not set, a random UUID string will be set.
     *
     * @param object  The object to add to this bucket.
     */
    public void addObject(BucketObject object) {
        if(object.getIdentifier() == null) {
            object.setIdentifier(UUID.randomUUID().toString());
        }

        this.bucketObjectMap.put(object.getIdentifier(), object);
    }

    protected Map<String, BucketObject> getBucketObjectMap() {
        return bucketObjectMap;
    }

    protected void setBucketObjectMap(Map<String, BucketObject> bucketObjectMap) {
        this.bucketObjectMap = bucketObjectMap;
    }


}
