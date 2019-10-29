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
package org.apache.nifi.registry.sync;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
@ApiModel(value = "RepositorySyncStatus")
public class RepositorySyncStatus {
    private boolean isClean;
    private boolean hasChanges;
    private Collection<String> changes;

    public RepositorySyncStatus(boolean isClean, boolean hasChanges, Collection<String> changes) {
        this.isClean = isClean;
        this.hasChanges = hasChanges;
        this.changes = changes;
    }

    public RepositorySyncStatus(){}

    @ApiModelProperty(value = "Repository is in sync with registry.", required = true)
    public boolean getIsClean() {
        return isClean;
    }
    public void setIsClean(boolean isClean) {
        this.isClean = isClean;
    }

    @ApiModelProperty(value = "The repository contains changes not reflected in registry.", required = true)
    public boolean getHasChanges() {
        return this.hasChanges;
    }
    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
    }

    @ApiModelProperty(value = "List of changes in the repository which should be synchronized with registry.", required = true)
    public Collection<String> getChanges() {
        return this.changes;
    }
    public void setChanges(Collection<String> changes) {
        this.changes = changes;
    }
}
