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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.flow.diff.ConciseEvolvingDifferenceDescriptor;
import org.apache.nifi.registry.flow.diff.FlowComparison;
import org.apache.nifi.registry.flow.diff.FlowDifference;
import org.apache.nifi.registry.flow.diff.StandardComparableDataFlow;
import org.apache.nifi.registry.flow.diff.StandardFlowComparator;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeConflict;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VersionedFlowSnapshotReconciliation {
    public static final String ROOT_PG_ID = "ROOT_PG";

    private final String bucketId;
    private final String flowId;
    private final int branchVersion;
    private final List<String> branchComments;
    private final int upstreamVersion;
    private final Map<ComponentType, List<VersionedComponentReconciliation>> differences;

    public VersionedFlowSnapshotReconciliation(RebaseCalculationContext rebaseCalculationContext, String bucketId, String flowId) throws IOException, NiFiRegistryException {
        this(rebaseCalculationContext, bucketId, flowId, Optional.empty());
    }

    public VersionedFlowSnapshotReconciliation(RebaseCalculationContext rebaseCalculationContext, String bucketId, String flowId, int branchVersion) throws IOException, NiFiRegistryException {
        this(rebaseCalculationContext, bucketId, flowId, Optional.of(branchVersion));
    }

    private VersionedFlowSnapshotReconciliation(RebaseCalculationContext rebaseCalculationContext, String bucketId, String flowId,
                                                Optional<Integer> branchVersion) throws IOException, NiFiRegistryException {
        this.bucketId = bucketId;
        this.flowId = flowId;

        RebaseRegistryFacade branch = rebaseCalculationContext.getBranch();
        RebaseRegistryFacade upstream = rebaseCalculationContext.getUpstream();

        VersionedFlowSnapshot branchSnapshot;
        if (branchVersion.isPresent()) {
            branchSnapshot = branch.getVersionSnapshot(bucketId, flowId, branchVersion.get());
        } else {
            branchSnapshot = branch.getLatestSnapshot(bucketId, flowId);
        }
        normalizeRootPgId(branchSnapshot);

        VersionedFlowSnapshot masterSnapshot = upstream.getLatestSnapshot(bucketId, flowId);
        normalizeRootPgId(masterSnapshot);

        this.branchVersion = branchSnapshot.getSnapshotMetadata().getVersion();
        this.upstreamVersion = masterSnapshot.getSnapshotMetadata().getVersion();
        this.branchComments = new ArrayList<>();

        int maxPossibleCommonVersion = Math.min(this.branchVersion, upstreamVersion);
        for(int i = this.branchVersion; i > maxPossibleCommonVersion; i--) {
            VersionedFlowSnapshotMetadata snapshotMetadata = branch.getVersionSnapshot(bucketId, flowId, i).getSnapshotMetadata();
            this.branchComments.add(snapshotMetadata.getAuthor() + " - " + snapshotMetadata.getComments());
        }
        int commonBase = -1;

        for (int i = maxPossibleCommonVersion; i > 0; i--) {
            if (equals(branch.getVersionSnapshot(bucketId, flowId, i), upstream.getVersionSnapshot(bucketId, flowId, i))) {
                commonBase = i;
                break;
            } else {
                VersionedFlowSnapshotMetadata snapshotMetadata = branch.getVersionSnapshot(bucketId, flowId, i).getSnapshotMetadata();
                this.branchComments.add(snapshotMetadata.getAuthor() + " - " + snapshotMetadata.getComments());
            }
        }

        this.branchComments.sort(Comparator.reverseOrder());

        if (commonBase == -1) {
            throw new IllegalStateException("Same bucket, flow id but no commonalities likely means misconfigured aliases.");
        } else if (commonBase == this.branchVersion) {
            this.differences = Collections.emptyMap();
        } else {
            VersionedFlowSnapshot baseSnapshot = upstream.getVersionSnapshot(bucketId, flowId, commonBase);
            normalizeRootPgId(baseSnapshot);

            FlowComparison branchCompare = new StandardFlowComparator(new StandardComparableDataFlow("base", baseSnapshot.getFlowContents()),
                    new StandardComparableDataFlow("branch", branchSnapshot.getFlowContents()), null, new ConciseEvolvingDifferenceDescriptor()).compare();
            FlowComparison masterCompare = new StandardFlowComparator(new StandardComparableDataFlow("base", baseSnapshot.getFlowContents()),
                    new StandardComparableDataFlow("master", masterSnapshot.getFlowContents()), null, new ConciseEvolvingDifferenceDescriptor()).compare();

            this.differences = new TreeMap<>();

            Map<String, List<FlowDifference>> upstreamDifferences = groupFlowDifferencesByComponentId(masterCompare.getDifferences());
            for (Map.Entry<String, List<FlowDifference>> branchComponentDifferences : groupFlowDifferencesByComponentId(branchCompare.getDifferences()).entrySet()) {
                VersionedComponent component = branchComponentDifferences.getValue().get(0).getComponentA();
                if (component == null) {
                    component = branchComponentDifferences.getValue().get(0).getComponentB();
                }
                ComponentType componentType = component.getComponentType();
                differences.computeIfAbsent(componentType, t -> new ArrayList<>()).add(new VersionedComponentReconciliation(rebaseCalculationContext, branchComponentDifferences.getValue(),
                        upstreamDifferences.getOrDefault(branchComponentDifferences.getKey(), Collections.emptyList())));
            }

            for (List<VersionedComponentReconciliation> reconciliations : this.differences.values()) {
                reconciliations.sort(Comparator.comparing(VersionedComponentReconciliation::getName).thenComparing(VersionedComponentReconciliation::getId));
            }
        }
    }

    @JsonCreator
    public VersionedFlowSnapshotReconciliation(@JsonProperty("bucketId") String bucketId, @JsonProperty("flowId") String flowId,
                                               @JsonProperty("branchVersion") int branchVersion, @JsonProperty("upstreamVersion") int upstreamVersion,
                                               @JsonProperty("differences") Map<ComponentType, List<VersionedComponentReconciliation>> differences,
                                               @JsonProperty("branchComments") List<String> branchComments) {
        this.bucketId = bucketId;
        this.flowId = flowId;
        this.branchVersion = branchVersion;
        this.upstreamVersion = upstreamVersion;
        this.differences = differences;
        this.branchComments = branchComments;
    }

    private Map<String, List<FlowDifference>> groupFlowDifferencesByComponentId(Set<FlowDifference> differences) {
        return differences.stream().collect(Collectors.groupingBy(f -> Optional.ofNullable(f.getComponentA()).orElse(f.getComponentB()).getIdentifier()));
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFlowId() {
        return flowId;
    }

    public int getBranchVersion() {
        return branchVersion;
    }

    public int getUpstreamVersion() {
        return upstreamVersion;
    }

    public List<String> getBranchComments() {
        return branchComments;
    }

    public Map<ComponentType, Map<String, List<MergeConflict>>> apply(RebaseApplicationContext rebaseApplicationContext, VersionedFlowSnapshot versionedFlowSnapshot) {
        VersionedProcessGroup versionedProcessGroup = versionedFlowSnapshot.getFlowContents();
        Map<ComponentType, Map<String, List<MergeConflict>>> conflicts = new HashMap<>();
        for (Map.Entry<ComponentType, List<VersionedComponentReconciliation>> componentTypeEntry : differences.entrySet()) {
            ComponentType componentType = componentTypeEntry.getKey();
            for (VersionedComponentReconciliation reconciliation : componentTypeEntry.getValue()) {
                List<MergeConflict> mergeConflicts = reconciliation.getMergeConflicts();
                if (mergeConflicts.size() > 0) {
                    conflicts.computeIfAbsent(componentType, k -> new HashMap<>())
                        .computeIfAbsent(reconciliation.getId(), k -> new ArrayList<>()).addAll(mergeConflicts);
                }

                for (MergeOperation mergeOperation : reconciliation.getMergeOperations()) {
                    mergeOperation.apply(rebaseApplicationContext, versionedProcessGroup, reconciliation.getGroupId()
                            .orElseGet(versionedProcessGroup::getIdentifier), componentType, reconciliation.getId());
                }
            }
        }
        return conflicts;
    }

    public Map<ComponentType, List<VersionedComponentReconciliation>> getDifferences() {
        return differences;
    }

    private boolean equals(VersionedFlowSnapshot a, VersionedFlowSnapshot b) {
        if (!equals(a.getSnapshotMetadata(), b.getSnapshotMetadata())) {
            return false;
        }

        FlowComparison compare = new StandardFlowComparator(new StandardComparableDataFlow("a", a.getFlowContents()),
                new StandardComparableDataFlow("b", b.getFlowContents()), null, new ConciseEvolvingDifferenceDescriptor()).compare();
        return compare.getDifferences().isEmpty();
    }

    private boolean equals(VersionedFlowSnapshotMetadata a, VersionedFlowSnapshotMetadata b) {
        if (a.getTimestamp() != b.getTimestamp()) {
            return false;
        }
        if (!a.getAuthor().equals(b.getAuthor())) {
            return false;
        }
        return a.getComments().equals(b.getComments());
    }

    protected static void normalizeRootPgId(VersionedFlowSnapshot versionedFlowSnapshot) {
        normalizeRootPgId(versionedFlowSnapshot, ROOT_PG_ID);
    }

    protected static void normalizeRootPgId(VersionedFlowSnapshot versionedFlowSnapshot, String pgId) {
        VersionedProcessGroup flowContents = versionedFlowSnapshot.getFlowContents();

        flowContents.setIdentifier(pgId);
        Consumer<VersionedComponent> groupIdSetter = c -> c.setGroupIdentifier(pgId);

        flowContents.getProcessGroups().forEach(groupIdSetter);
        flowContents.getRemoteProcessGroups().forEach(groupIdSetter);
        flowContents.getRemoteProcessGroups().stream().map(VersionedRemoteProcessGroup::getInputPorts).flatMap(Collection::stream).forEach(groupIdSetter);
        flowContents.getRemoteProcessGroups().stream().map(VersionedRemoteProcessGroup::getOutputPorts).flatMap(Collection::stream).forEach(groupIdSetter);
        flowContents.getProcessors().forEach(groupIdSetter);
        flowContents.getInputPorts().forEach(groupIdSetter);
        flowContents.getOutputPorts().forEach(groupIdSetter);
        flowContents.getConnections().forEach(groupIdSetter);
        flowContents.getLabels().forEach(groupIdSetter);
        flowContents.getFunnels().forEach(groupIdSetter);
        flowContents.getControllerServices().forEach(groupIdSetter);
    }
}
