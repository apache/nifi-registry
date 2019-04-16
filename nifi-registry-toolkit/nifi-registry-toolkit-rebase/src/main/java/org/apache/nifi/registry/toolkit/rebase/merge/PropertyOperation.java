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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PropertyOperation implements MergeOperation {
    private final Map<String, PropertyChange> changes;

    public PropertyOperation(Map<String, String> valueA, Map<String, String> valueB) {
        Set<String> keys = new HashSet<>(valueA.keySet());
        keys.addAll(valueB.keySet());

        this.changes = new TreeMap<>();

        for (String key : keys) {
            String oldValue = valueA.get(key);
            String newValue = valueB.get(key);

            if (StringUtils.equals(oldValue, newValue)) {
                continue;
            }
            this.changes.put(key, new PropertyChange(oldValue, newValue));
        }
    }

    public PropertyOperation(@JsonProperty("changes") Map<String, PropertyChange> changes) {
        this.changes = new TreeMap<>(changes == null ? Collections.emptyMap() : changes);
    }

    public Map<String, PropertyChange> getChanges() {
        return changes;
    }

    public Pair<Optional<MergeOperation>, Optional<MergeConflict>> merge(PropertyOperation upstream) {
        Set<String> conflicted = new HashSet<>(changes.keySet());
        conflicted.retainAll(upstream.changes.keySet());

        Set<String> unconflicted = new HashSet<>(changes.keySet());
        unconflicted.removeAll(conflicted);

        Optional<MergeOperation> mergeOperation = Optional.empty();
        if (unconflicted.size() > 0) {
            mergeOperation = Optional.of(new PropertyOperation(getValues(unconflicted, changes)));
        }

        // Don't care about changes if they were made upstream too
        Set<String> sameChanges = new HashSet<>();
        for (String s : conflicted) {
            if (StringUtils.equals(changes.get(s).newValue, upstream.changes.get(s).newValue)) {
                sameChanges.add(s);
            }
        }
        conflicted.removeAll(sameChanges);

        Optional<MergeConflict> mergeConflict = Optional.empty();
        if (conflicted.size() != 0) {
            mergeConflict = Optional.of(new MergeConflict(new PropertyOperation(getValues(conflicted, changes)), new PropertyOperation(getValues(conflicted, upstream.changes))));
        }

        return Pair.of(mergeOperation, mergeConflict);
    }

    private static Map<String, PropertyChange> getValues(Collection<String> keys, Map<String, PropertyChange> lookup) {
        return keys.stream().map(k -> Pair.of(k, lookup.get(k))).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    @Override
    public void apply(RebaseApplicationContext rebaseApplicationContext, VersionedProcessGroup processGroup, String groupId, ComponentType componentType, String componentId) {
        apply(getProperties(VersionedComponentCollection.get(processGroup, groupId, componentType).getById(componentId).orElseThrow(() ->
                new IllegalStateException("Can't find component " + componentId + " with type " + componentType))));
    }

    public static Map<String, String> getProperties(VersionedComponent versionedComponent) {
        switch (versionedComponent.getComponentType()) {
            case PROCESSOR:
                return ((VersionedProcessor) versionedComponent).getProperties();
            case CONTROLLER_SERVICE:
                return ((VersionedControllerService) versionedComponent).getProperties();
            default:
                throw new IllegalArgumentException("Unsupported operation for component type " + versionedComponent.getComponentType());
        }
    }

    protected void apply(Map<String, String> properties) {
        for (Map.Entry<String, PropertyChange> entry : changes.entrySet()) {
            String key = entry.getKey();
            String newVal = entry.getValue().getNew();

            if (newVal == null) {
                properties.remove(key);
            } else {
                properties.put(key, newVal);
            }
        }
    }

    public static class PropertyChange {
        private final String oldValue;
        private final String newValue;

        @JsonCreator
        public PropertyChange(@JsonProperty("old") String oldValue, @JsonProperty("new") String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getOld() {
            return oldValue;
        }

        public String getNew() {
            return newValue;
        }
    }
}
