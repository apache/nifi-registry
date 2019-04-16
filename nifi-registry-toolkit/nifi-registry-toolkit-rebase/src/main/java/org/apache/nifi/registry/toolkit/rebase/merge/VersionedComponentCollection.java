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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedLabel;
import org.apache.nifi.registry.flow.VersionedPort;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.VersionedFlowSnapshotReconciliation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionedComponentCollection<T extends VersionedComponent> {
    protected static final Map<ComponentType, Function<VersionedProcessGroup, VersionedComponentCollection<?>>> componentTypeMap = Collections.unmodifiableMap(
        Stream.<Pair<ComponentType, Function<VersionedProcessGroup, VersionedComponentCollection<?>>>>of(
            Pair.of(ComponentType.CONNECTION, p -> new VersionedComponentCollection<>(VersionedConnection.class, p.getConnections())),
            Pair.of(ComponentType.PROCESSOR, p -> new VersionedComponentCollection<>(VersionedProcessor.class, p.getProcessors())),
            Pair.of(ComponentType.PROCESS_GROUP, p -> new VersionedComponentCollection<>(VersionedProcessGroup.class, p.getProcessGroups())),
            Pair.of(ComponentType.REMOTE_PROCESS_GROUP, p -> new VersionedComponentCollection<>(VersionedRemoteProcessGroup.class, p.getRemoteProcessGroups())),
            Pair.of(ComponentType.REMOTE_INPUT_PORT, p -> new VersionedComponentCollection<>(VersionedRemoteGroupPort.class,
                    Collections.unmodifiableSet(p.getRemoteProcessGroups().stream().flatMap(r -> r.getInputPorts().stream()).collect(Collectors.toSet())))),
            Pair.of(ComponentType.REMOTE_OUTPUT_PORT, p -> new VersionedComponentCollection<>(VersionedRemoteGroupPort.class,
                    Collections.unmodifiableSet(p.getRemoteProcessGroups().stream().flatMap(r -> r.getOutputPorts().stream()).collect(Collectors.toSet())))),
            Pair.of(ComponentType.INPUT_PORT, p -> new VersionedComponentCollection<>(VersionedPort.class, p.getInputPorts())),
            Pair.of(ComponentType.OUTPUT_PORT, p -> new VersionedComponentCollection<>(VersionedPort.class, p.getOutputPorts())),
            Pair.of(ComponentType.FUNNEL, p -> new VersionedComponentCollection<>(VersionedFunnel.class, p.getFunnels())),
            Pair.of(ComponentType.LABEL, p -> new VersionedComponentCollection<>(VersionedLabel.class, p.getLabels())),
            Pair.of(ComponentType.CONTROLLER_SERVICE, p -> new VersionedComponentCollection<>(VersionedControllerService.class, p.getControllerServices()))
    ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

    public static VersionedComponentCollection<? extends VersionedComponent> get(VersionedProcessGroup processGroup, String groupId, ComponentType componentType) {
        Function<VersionedProcessGroup, VersionedComponentCollection<?>> collectionFunction = componentTypeMap.get(componentType);
        Optional<VersionedProcessGroup> pgWithId = findById(processGroup, groupId);
        return collectionFunction.apply(pgWithId.orElseThrow(() -> new IllegalStateException("Couldn't find process group with id " + groupId + " in " + processGroup)));
    }

    public static Optional<VersionedProcessGroup> findById(VersionedProcessGroup group, String groupId) {
        if (group.getGroupIdentifier() == null && VersionedFlowSnapshotReconciliation.ROOT_PG_ID.equals(groupId)) {
            return Optional.of(group);
        }
        if (groupId.equals(group.getIdentifier())) {
            return Optional.of(group);
        }
        for (VersionedProcessGroup processGroup : group.getProcessGroups()) {
            Optional<VersionedProcessGroup> child = findById(processGroup, groupId);
            if (child.isPresent()) {
                return child;
            }
        }
        return Optional.empty();
    }

    private final Class<T> clazz;
    private final Set<T> collection;

    public VersionedComponentCollection(Class<T> clazz, Set<T> collection) {
        this.clazz = clazz;
        this.collection = collection;
    }

    public <U extends VersionedComponent> boolean add(U component) {
        return this.collection.add(clazz.cast(component));
    }

    public boolean removeById(String id) {
        return this.collection.removeIf(c -> id.equals(c.getIdentifier()));
    }

    public Optional<T> getById(String id) {
        return this.collection.stream().filter(c -> id.equals(c.getIdentifier())).findFirst();
    }
}
