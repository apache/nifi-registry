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

package org.apache.nifi.registry.authorization;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Access policy summary of which actions ("read', "write") are allowable for a specified web resource.
 */
@ApiModel("accessPolicySummary")
public class AccessPolicySummary {

    private String identifier;
    private String resource;
    private String action;
    private Boolean configurable;

    @ApiModelProperty("The id of the policy. Set by server at creation time.")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty("The resource for this access policy.")
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @ApiModelProperty(
            value = "The action associated with this access policy.",
            allowableValues = "READ, WRITE"
    )
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ApiModelProperty(value = "Indicates if this access policy is configurable, based on which Authorizer has been configured to manage it.", readOnly = true)
    public Boolean getConfigurable() {
        return configurable;
    }

    public void setConfigurable(Boolean configurable) {
        this.configurable = configurable;
    }
}
