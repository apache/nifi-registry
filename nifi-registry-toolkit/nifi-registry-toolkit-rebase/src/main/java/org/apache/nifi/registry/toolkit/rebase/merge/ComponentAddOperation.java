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
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.apache.nifi.registry.toolkit.rebase.VersionedFlowSnapshotReconciliation;

public class ComponentAddOperation implements MergeOperation {
    private final VersionedComponent versionedComponent;

    @JsonCreator
    public ComponentAddOperation(@JsonProperty("versionedComponent") VersionedComponent versionedComponent) {
        this.versionedComponent = versionedComponent;
    }

    @Override
    public void apply(RebaseApplicationContext rebaseApplicationContext, VersionedProcessGroup processGroup, String groupId, ComponentType componentType, String componentId) {
        VersionedComponentCollection<? extends VersionedComponent> collection = getVersionedComponentCollection(processGroup, componentType);
        if (VersionedFlowSnapshotReconciliation.ROOT_PG_ID.equals(versionedComponent.getGroupIdentifier())) {
            versionedComponent.setGroupIdentifier(groupId);
        }
        collection.add(versionedComponent);
    }

    private VersionedComponentCollection<? extends VersionedComponent> getVersionedComponentCollection(VersionedProcessGroup processGroup, ComponentType componentType) {
        VersionedComponentCollection<? extends VersionedComponent> collection;
        // Remote ports are stored within the RPG
        if (componentType == ComponentType.REMOTE_INPUT_PORT || componentType == ComponentType.REMOTE_OUTPUT_PORT) {
            collection = VersionedComponentCollection.get(processGroup, versionedComponent.getGroupIdentifier(), ComponentType.REMOTE_PROCESS_GROUP)
                    .getById(((VersionedRemoteGroupPort) versionedComponent).getRemoteGroupId())
                    .map(VersionedRemoteProcessGroup.class::cast)
                    // Get correct collection off of it
                    .map(rpg -> {
                        if (componentType == ComponentType.REMOTE_INPUT_PORT) {
                            return rpg.getInputPorts();
                        } else {
                            return rpg.getOutputPorts();
                        }
                    })
                    .map(ports -> new VersionedComponentCollection<>(VersionedRemoteGroupPort.class, ports))
                    .orElseThrow(() -> new IllegalStateException("Couldn't find remote process group for remote group port " + versionedComponent));
        } else {
            collection = VersionedComponentCollection.get(processGroup, versionedComponent.getGroupIdentifier(), componentType);
        }
        return collection;
    }

    public VersionedComponent getVersionedComponent() {
        return versionedComponent;
    }
}
