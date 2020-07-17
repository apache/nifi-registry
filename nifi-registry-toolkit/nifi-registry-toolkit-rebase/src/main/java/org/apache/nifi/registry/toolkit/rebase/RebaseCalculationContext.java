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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowCoordinates;
import org.apache.nifi.registry.toolkit.rebase.merge.BranchCoordinatesOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeConflict;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeOperation;
import org.apache.nifi.registry.toolkit.rebase.merge.VersionFlowCoordinatesOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RebaseCalculationContext {
    private final RebaseRegistryFacade branch;
    private final RebaseRegistryFacade upstream;
    private final Map<Pair<String, String>, VersionedFlowSnapshotReconciliation> reconciledSnapshots;

    public RebaseCalculationContext(RebaseRegistryFacade branch, RebaseRegistryFacade upstream) {
        this.branch = branch;
        this.upstream = upstream;
        this.reconciledSnapshots = new LinkedHashMap<>();
    }

    public List<VersionedFlowSnapshotReconciliation> getReconciledSnapshots() {
        return new ArrayList<>(reconciledSnapshots.values());
    }

    public RebaseRegistryFacade getBranch() {
        return branch;
    }

    public RebaseRegistryFacade getUpstream() {
        return upstream;
    }

    /**
     * We need to push all internal referenced flows that have changed upstream as well.
     */
    public Pair<Optional<MergeOperation>, Optional<MergeConflict>> resolve(VersionedFlowCoordinates branchCoordinates, Optional<VersionedFlowCoordinates> maybeUpstreamCoordinates)
            throws IOException, NiFiRegistryException {
        if (branch.isInternal(branchCoordinates.getRegistryUrl())) {
            if (maybeUpstreamCoordinates.isPresent()) {
                VersionedFlowCoordinates upstreamCoordinates = maybeUpstreamCoordinates.get();
                if (!upstream.isInternal(upstreamCoordinates.getRegistryUrl())) {
                    throw new IllegalStateException("Upstream is now pointed at external registry " + upstreamCoordinates);
                }
                int upstreamVersion = upstream.getLatestSnapshot(upstreamCoordinates.getBucketId(), upstreamCoordinates.getFlowId()).getSnapshotMetadata().getVersion();
                if (upstreamCoordinates.getVersion() != upstreamVersion) {
                    throw new IllegalStateException("Cannot rebase when master is pointing at old coordinates " + upstreamCoordinates);
                }
                if (!upstreamCoordinates.getBucketId().equals(branchCoordinates.getBucketId())) {
                    throw new IllegalStateException("Branch " + branchCoordinates + " coordinates target different bucket from " + upstreamCoordinates);
                }
                if (!upstreamCoordinates.getFlowId().equals(branchCoordinates.getFlowId())) {
                    throw new IllegalStateException("Branch " + branchCoordinates + " coordinates target different flow from " + upstreamCoordinates);
                }
            }
            Pair<String, String> flowKey = Pair.of(branchCoordinates.getBucketId(), branchCoordinates.getFlowId());
            VersionedFlowSnapshotReconciliation reconciliation = reconciledSnapshots.get(flowKey);
            if (reconciliation == null) {
                reconciliation = new VersionedFlowSnapshotReconciliation(this, branchCoordinates.getBucketId(), branchCoordinates.getFlowId(), branchCoordinates.getVersion());
                reconciledSnapshots.put(flowKey, reconciliation);
            } else if (reconciliation.getBranchVersion() != branchCoordinates.getVersion()) {
                throw new IllegalStateException("Cannot rebase with multiple different snapshot versions.  Already seen: " + reconciliation.getBranchVersion() + " new: "+ branchCoordinates);
            }
            if (reconciliation.getDifferences().size() == 0) {
                return Pair.of(Optional.empty(), Optional.empty());
            }
            return Pair.of(Optional.of(new BranchCoordinatesOperation(branchCoordinates.getBucketId(), branchCoordinates.getFlowId())), Optional.empty());
        } else if (maybeUpstreamCoordinates.isPresent()) {
            if (!maybeUpstreamCoordinates.get().equals(branchCoordinates)) {
                return Pair.of(Optional.empty(), Optional.of(new MergeConflict(new VersionFlowCoordinatesOperation(branchCoordinates),
                        new VersionFlowCoordinatesOperation(maybeUpstreamCoordinates.get()))));
            }
            return Pair.of(Optional.empty(), Optional.empty());
        } else {
            return Pair.of(Optional.of(new VersionFlowCoordinatesOperation(branchCoordinates)), Optional.empty());
        }
    }
}
