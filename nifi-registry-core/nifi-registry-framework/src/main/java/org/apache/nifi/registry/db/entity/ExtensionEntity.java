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

import java.util.Set;

public class ExtensionEntity {

    private String id;

    private String extensionBundleVersionId;

    private String name;

    private String description;

    private String generalRestriction;

    private ExtensionEntityCategory category;

    // populated during creation if provided, but typically won't be populated on retrieval
    private String additionalDetails;

    // Comma separated list of tags so we don't have to query tag table for each extension
    private String tags;

    private Set<ExtensionProvidedServiceApiEntity> providedServiceApis;

    private Set<ExtensionRestrictionEntity> restrictions;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExtensionBundleVersionId() {
        return extensionBundleVersionId;
    }

    public void setExtensionBundleVersionId(String extensionBundleVersionId) {
        this.extensionBundleVersionId = extensionBundleVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGeneralRestriction() {
        return this.generalRestriction;
    }

    public void setGeneralRestriction(String generalRestriction) {
        this.generalRestriction = generalRestriction;
    }

    public ExtensionEntityCategory getCategory() {
        return category;
    }

    public void setCategory(ExtensionEntityCategory category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Set<ExtensionProvidedServiceApiEntity> getProvidedServiceApis() {
        return providedServiceApis;
    }

    public void setProvidedServiceApis(Set<ExtensionProvidedServiceApiEntity> providedServiceApis) {
        this.providedServiceApis = providedServiceApis;
    }

    public Set<ExtensionRestrictionEntity> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Set<ExtensionRestrictionEntity> restrictions) {
        this.restrictions = restrictions;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

}
