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
package org.apache.nifi.registry.toolkit.rebase.merge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedFlowCoordinates;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;

public class VersionFlowCoordinatesOperation implements MergeOperation {
    private final VersionedFlowCoordinates versionedFlowCoordinates;

    @JsonCreator
    public VersionFlowCoordinatesOperation(@JsonProperty("versionedFlowCoordinates") VersionedFlowCoordinates versionedFlowCoordinates) {
        this.versionedFlowCoordinates = versionedFlowCoordinates;
    }

    public VersionedFlowCoordinates getVersionedFlowCoordinates() {
        return versionedFlowCoordinates;
    }

    @Override
    public void apply(RebaseApplicationContext rebaseApplicationContext, VersionedProcessGroup processGroup, String groupId, ComponentType componentType, String componentId) {
        VersionedProcessGroup component = (VersionedProcessGroup) VersionedComponentCollection.get(processGroup, groupId, componentType).getById(componentId).orElseThrow(() ->
                new IllegalStateException("Couldn't find " + componentId));
        component.setVersionedFlowCoordinates(versionedFlowCoordinates);
    }
}
