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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

@ApiModel
public class ExtensionMetadata {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotNull
    private String description;

    private String generalRestriction;

    @NotNull
    private ExtensionCategory category;

    private Set<String> tags;

    @Valid
    private Set<ExtensionProvidedServiceApi> providedServiceApis;

    @Valid
    private Set<ExtensionRestriction> restrictions;


    @ApiModelProperty(value = "The id of the extension", readOnly = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The fully qualified name of the extension")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "The description of the extension")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "The general restriction statement for this extension, or null if it is not restricted")
    public String getGeneralRestriction() {
        return generalRestriction;
    }

    public void setGeneralRestriction(String generalRestriction) {
        this.generalRestriction = generalRestriction;
    }

    @ApiModelProperty(value = "The category of the extension, such a processor, service, reporting task")
    public ExtensionCategory getCategory() {
        return category;
    }

    public void setCategory(ExtensionCategory category) {
        this.category = category;
    }

    @ApiModelProperty(value = "The tags for the extension")
    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @ApiModelProperty(value = "The service APIs that this extension implements")
    public Set<ExtensionProvidedServiceApi> getProvidedServiceApis() {
        return providedServiceApis;
    }

    public void setProvidedServiceApis(Set<ExtensionProvidedServiceApi> providedServiceApis) {
        this.providedServiceApis = providedServiceApis;
    }

    @ApiModelProperty(value = "The restrictions for this extension")
    public Set<ExtensionRestriction> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Set<ExtensionRestriction> restrictions) {
        this.restrictions = restrictions;
    }
}
