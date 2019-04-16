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
package org.apache.nifi.registry.toolkit.rebase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlowCoordinates;
import org.apache.nifi.registry.flow.diff.DifferenceType;
import org.apache.nifi.registry.flow.diff.FlowDifference;
import org.apache.nifi.registry.toolkit.rebase.merge.ComponentAddOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.ComponentRemoveOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeConflict;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.PropertyOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.SingleDifferenceTypeOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class VersionedComponentReconciliation {
    private final Optional<String> groupId;
    private final String id;
    private final String name;
    private final List<MergeOperation> mergeOperations;
    private final List<MergeConflict> mergeConflicts;

    public VersionedComponentReconciliation(RebaseCalculationContext rebaseCalculationContext, Collection<FlowDifference> branchFlowDifferences,
                                            Collection<FlowDifference> upstreamFlowDifferences) throws IOException, NiFiRegistryException {
        FlowDifference firstDiff = branchFlowDifferences.iterator().next();
        VersionedComponent component = firstDiff.getComponentA();
        if (component == null) {
            component = firstDiff.getComponentB();
        }

        String groupIdentifier = component.getGroupIdentifier();
        this.groupId = groupIdentifier == null || VersionedFlowSnapshotReconciliation.ROOT_PG_ID.equals(groupIdentifier) ? Optional.empty() : Optional.of(groupIdentifier);
        this.id = component.getIdentifier();
        this.name = component.getName();

        Map<DifferenceType, List<FlowDifference>> upstreamDifferencesByType = upstreamFlowDifferences.stream().collect(Collectors.groupingBy(FlowDifference::getDifferenceType));
        Map<DifferenceType, List<FlowDifference>> branchDifferencesByType = branchFlowDifferences.stream().collect(Collectors.groupingBy(FlowDifference::getDifferenceType));

        this.mergeOperations = new ArrayList<>();
        this.mergeConflicts = new ArrayList<>();

        Optional<MergeOperation> componentAddMergeOperation = maybeAddComponent(branchDifferencesByType, upstreamDifferencesByType);
        if (componentAddMergeOperation.isPresent()) {
            this.mergeOperations.add(componentAddMergeOperation.get());
            return;
        }

        Pair<Optional<MergeOperation>, Optional<MergeConflict>> removeComponent = maybeRemoveComponent(branchDifferencesByType, upstreamDifferencesByType);
        if (removeComponent.getLeft().isPresent() || removeComponent.getRight().isPresent()) {
            removeComponent.getLeft().ifPresent(this.mergeOperations::add);
            removeComponent.getRight().ifPresent(this.mergeConflicts::add);
            return;
        }

        List<FlowDifference> versionChanges = Optional.ofNullable(branchDifferencesByType.remove(DifferenceType.VERSIONED_FLOW_COORDINATES_CHANGED)).orElse(Collections.emptyList());
        if (versionChanges.size() > 0) {
            if (versionChanges.size() > 1) {
                throw new IllegalStateException("Expected max of 1 version coordinate change per component.");
            }
            List<FlowDifference> upstreamVersionChanges = Optional.ofNullable(upstreamDifferencesByType.remove(DifferenceType.VERSIONED_FLOW_COORDINATES_CHANGED)).orElse(Collections.emptyList());
            if (upstreamVersionChanges.size() > 1) {
                throw new IllegalStateException("Expected max of 1 upstream version coordinate change per component.");
            }
            Pair<Optional<MergeOperation>, Optional<MergeConflict>> versionDiff = rebaseCalculationContext.resolve((VersionedFlowCoordinates) versionChanges.get(0).getValueB(),
                    upstreamVersionChanges.size() == 0 ? Optional.empty() : Optional.of((VersionedFlowCoordinates) upstreamVersionChanges.get(0).getValueB()));
            versionDiff.getLeft().ifPresent(this.mergeOperations::add);
            versionDiff.getRight().ifPresent(this.mergeConflicts::add);
        }

        Pair<List<MergeOperation>, List<MergeConflict>> remainingDifferences = handleRemainingDifferences(branchDifferencesByType, upstreamDifferencesByType);
        this.mergeOperations.addAll(remainingDifferences.getLeft());
        this.mergeConflicts.addAll(remainingDifferences.getRight());
    }

    @JsonCreator
    public VersionedComponentReconciliation(@JsonProperty("groupId") Optional<String> groupId, @JsonProperty("id") String id, @JsonProperty("name") String name,
                                            @JsonProperty("mergeOperations") List<MergeOperation> mergeOperations, @JsonProperty("mergeConflicts") List<MergeConflict> mergeConflicts) {
        this.groupId = groupId;
        this.id = id;
        this.name = name;
        this.mergeOperations = mergeOperations;
        this.mergeConflicts = mergeConflicts == null ? new ArrayList<>() : mergeConflicts;
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public Optional<String> getGroupId() {
        return groupId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MergeOperation> getMergeOperations() {
        return mergeOperations;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<MergeConflict> getMergeConflicts() {
        return mergeConflicts;
    }

    private static Pair<Optional<MergeOperation>, Optional<MergeConflict>> maybeRemoveComponent(Map<DifferenceType, List<FlowDifference>> branchDifferencesByType,
                                                                                                Map<DifferenceType, List<FlowDifference>> upstreamDifferencesByType) {
        List<FlowDifference> branchRemovedDifferences = Optional.ofNullable(branchDifferencesByType.remove(DifferenceType.COMPONENT_REMOVED)).orElseGet(Collections::emptyList);
        if (branchRemovedDifferences.size() > 0) {
            if (branchRemovedDifferences.size() != 1 || branchDifferencesByType.size() != 0) {
                throw new IllegalStateException("When a component is removed by a branch, expect that to be the only change to it on the branch.");
            }
            MergeOperation removeOperation = new ComponentRemoveOperation();
            if (upstreamDifferencesByType.size() == 0) {
                // Removed in branch, need to apply it upstream
                return Pair.of(Optional.of(removeOperation), Optional.empty());
            }
            List<FlowDifference> upstreamRemovedDifferences = Optional.ofNullable(upstreamDifferencesByType.remove(DifferenceType.COMPONENT_REMOVED)).orElseGet(Collections::emptyList);
            if (upstreamRemovedDifferences.size() > 0) {
                if (upstreamRemovedDifferences.size() != 1 || upstreamDifferencesByType.size() != 0) {
                    throw new IllegalStateException("When a component is removed by the upstream, expect that to be the only change to it upstream.");
                }
                // Removed upstream too, we don't need to do anything
                return Pair.of(Optional.empty(), Optional.empty());
            }
            // Upstream changes conflict with removal
            return Pair.of(Optional.empty(), Optional.of(new MergeConflict(Collections.singletonList(removeOperation),
                    handleRemainingDifferences(upstreamDifferencesByType, Collections.emptyMap()).getLeft())));
        }
        List<FlowDifference> upstreamRemovedDifferences = Optional.ofNullable(upstreamDifferencesByType.remove(DifferenceType.COMPONENT_REMOVED)).orElseGet(Collections::emptyList);
        if (upstreamRemovedDifferences.size() > 0) {
            if (upstreamRemovedDifferences.size() != 1 || upstreamDifferencesByType.size() != 1) {
                throw new IllegalStateException("When a component is removed by the upstream, expect that to be the only change to it upstream.");
            }
            // We know we have local changes or we wouldn't be here, upstream removal conflicts with them.
            return Pair.of(Optional.empty(), Optional.of(new MergeConflict(handleRemainingDifferences(branchDifferencesByType, Collections.emptyMap()).getLeft(),
                    Collections.singletonList(new ComponentRemoveOperation()))));
        }
        return Pair.of(Optional.empty(), Optional.empty());
    }

    private static Optional<MergeOperation> maybeAddComponent(Map<DifferenceType, List<FlowDifference>> branchDifferencesByType, Map<DifferenceType, List<FlowDifference>> upstreamDifferencesByType) {
        List<FlowDifference> addedDifferences = Optional.ofNullable(branchDifferencesByType.remove(DifferenceType.COMPONENT_ADDED)).orElseGet(Collections::emptyList);
        if (addedDifferences.size() > 0) {
            if (addedDifferences.size() != 1 || branchDifferencesByType.size() != 0) {
                throw new IllegalStateException("When component is added by branch, expect that to be the only change to it on the branch.");
            }
            if (upstreamDifferencesByType.size() > 0) {
                throw new IllegalStateException("When component is added by branch, don't expect it to exist in upstream.");
            }
            return Optional.of(new ComponentAddOperation(addedDifferences.get(0).getComponentB()));
        }
        return Optional.empty();
    }

    private static Pair<List<MergeOperation>, List<MergeConflict>> handleRemainingDifferences(Map<DifferenceType, List<FlowDifference>> branchDifferencesByType,
                                                                                              Map<DifferenceType, List<FlowDifference>> upstreamDifferencesByType) {
        List<MergeOperation> mergeOperations = new ArrayList<>();
        List<MergeConflict> mergeConflicts = new ArrayList<>();

        // Treat all property changes the same
        Optional<PropertyOperation> branchPropertyOperation = convertToProperyChanged(branchDifferencesByType);
        Optional<PropertyOperation> upstreamPropertyOperation = convertToProperyChanged(upstreamDifferencesByType);

        if (branchPropertyOperation.isPresent()) {
            if (upstreamPropertyOperation.isPresent()) {
                Pair<Optional<MergeOperation>, Optional<MergeConflict>> merge = branchPropertyOperation.get().merge(upstreamPropertyOperation.get());
                merge.getLeft().ifPresent(mergeOperations::add);
                merge.getRight().ifPresent(mergeConflicts::add);
            } else {
                mergeOperations.add(branchPropertyOperation.get());
            }
        }

        for (DifferenceType supportedDifferenceType : SingleDifferenceTypeOperation.getSupportedDifferenceTypes()) {
            List<FlowDifference> branchDifferences = Optional.ofNullable(branchDifferencesByType.remove(supportedDifferenceType)).orElseGet(Collections::emptyList);
            List<FlowDifference> upstreamDifferences = Optional.ofNullable(upstreamDifferencesByType.remove(supportedDifferenceType)).orElseGet(Collections::emptyList);
            if (branchDifferences.size() == 0) {
                continue;
            }
            if (branchDifferences.size() > 1) {
                throw new RuntimeException("Expected only one change in branch of type " + supportedDifferenceType);
            }
            if (upstreamDifferences.size() > 1) {
                throw new RuntimeException("Expected only one change in upstream of type " + supportedDifferenceType);
            }
            FlowDifference branchDifference = branchDifferences.get(0);
            SingleDifferenceTypeOperation branchMergeOperation = new SingleDifferenceTypeOperation(branchDifference);
            if (upstreamDifferences.size() == 0) {
                mergeOperations.add(branchMergeOperation);
            } else {
                mergeConflicts.add(new MergeConflict(branchMergeOperation, new SingleDifferenceTypeOperation(upstreamDifferences.get(0))));
            }
        }

        if (branchDifferencesByType.size() > 0) {
            throw new RuntimeException("Unable to handle differences of type(s) " + branchDifferencesByType.keySet().stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")));
        }

        return Pair.of(mergeOperations, mergeConflicts);
    }

    private static Optional<PropertyOperation> convertToProperyChanged(Map<DifferenceType, List<FlowDifference>> differencesByType) {
        List<FlowDifference> propertyDifferences = new ArrayList<>();
        propertyDifferences.addAll(Optional.ofNullable(differencesByType.remove(DifferenceType.PROPERTY_ADDED)).orElseGet(Collections::emptyList));
        propertyDifferences.addAll(Optional.ofNullable(differencesByType.remove(DifferenceType.PROPERTY_REMOVED)).orElseGet(Collections::emptyList));
        propertyDifferences.addAll(Optional.ofNullable(differencesByType.remove(DifferenceType.PROPERTY_CHANGED)).orElseGet(Collections::emptyList));

        if (propertyDifferences.size() == 0) {
            return Optional.empty();
        }

        FlowDifference difference = propertyDifferences.get(0);

        return Optional.of(new PropertyOperation(PropertyOperation.getProperties(difference.getComponentA()), PropertyOperation.getProperties(difference.getComponentB())));
    }
}
