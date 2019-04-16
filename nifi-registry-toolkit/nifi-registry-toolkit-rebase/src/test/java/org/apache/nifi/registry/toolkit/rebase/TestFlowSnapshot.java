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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestFlowSnapshot {
    public interface VersionedFlowSnapshotMixin {
        @JsonIgnore
        boolean isLatest();
    }

    public static ObjectMapper OBJECT_MAPPER = initObjectMapper();

    private static ObjectMapper initObjectMapper() {
        ObjectMapper mapper = RecursiveRebase.OBJECT_MAPPER.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.addMixIn(VersionedFlowSnapshot.class, VersionedFlowSnapshotMixin.class);
        return mapper;
    }

    private final VersionedFlowSnapshot base;
    private final VersionedFlowSnapshot branch;
    private final VersionedFlowSnapshot upstream;

    public TestFlowSnapshot() throws IOException {
        this(createBase());
    }

    public TestFlowSnapshot(VersionedFlowSnapshot base) throws IOException {
        this.base = base;

        this.branch = deepCopy(base);
        this.branch.getSnapshotMetadata().setVersion(this.base.getSnapshotMetadata().getVersion() + 1);
        VersionedFlowSnapshotReconciliation.normalizeRootPgId(this.branch, UUID.randomUUID().toString());

        this.upstream = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(base), VersionedFlowSnapshot.class);
        this.upstream.getSnapshotMetadata().setVersion(this.base.getSnapshotMetadata().getVersion() + 1);
    }

    public static VersionedFlowSnapshot createBase() {
        VersionedFlowSnapshot base = new VersionedFlowSnapshot();
        VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setAuthor("test-auther");
        snapshotMetadata.setComments("base-comments");
        base.setSnapshotMetadata(snapshotMetadata);
        base.setFlowContents(new VersionedProcessGroup());
        base.getFlowContents().setIdentifier(UUID.randomUUID().toString());
        return base;
    }

    public VersionedFlowSnapshot reconcile() throws IOException, NiFiRegistryException {
        Pair<RebaseRegistryFacade, RecursiveRebase.ThrowingSupplier<InputStream, IOException>> pair = diff();

        RebaseRegistryFacade upstream = pair.getLeft();

        RecursiveRebase.apply(upstream, pair.getRight(), Optional.empty(), false);

        ArgumentCaptor<VersionedFlowSnapshot> captor = ArgumentCaptor.forClass(VersionedFlowSnapshot.class);
        verify(upstream).updateSnapshot(captor.capture());

        return captor.getValue();
    }

    public List<VersionedFlowSnapshotReconciliation> performDiff() throws IOException, NiFiRegistryException {
        return RecursiveRebase.parseDiff(diff().getRight());
    }

    private Pair<RebaseRegistryFacade, RecursiveRebase.ThrowingSupplier<InputStream, IOException>> diff() throws IOException, NiFiRegistryException {
        RebaseRegistryFacade branch = mock(RebaseRegistryFacade.class);
        RebaseRegistryFacade upstream = mock(RebaseRegistryFacade.class);

        String bucketIdentifier = base.getSnapshotMetadata().getBucketIdentifier();
        String flowIdentifier = base.getSnapshotMetadata().getFlowIdentifier();

        when(branch.getLatestSnapshot(bucketIdentifier, flowIdentifier)).thenAnswer(args -> deepCopy(this.branch));
        when(upstream.getLatestSnapshot(bucketIdentifier, flowIdentifier)).thenAnswer(args -> deepCopy(this.upstream));

        when(branch.getVersionSnapshot(bucketIdentifier, flowIdentifier, base.getSnapshotMetadata().getVersion())).thenAnswer(args -> deepCopy(base));
        when(branch.getVersionSnapshot(bucketIdentifier, flowIdentifier, this.branch.getSnapshotMetadata().getVersion())).thenAnswer(args -> deepCopy(this.branch));

        when(upstream.getVersionSnapshot(bucketIdentifier, flowIdentifier, base.getSnapshotMetadata().getVersion())).thenAnswer(args -> deepCopy(base));
        when(upstream.getVersionSnapshot(bucketIdentifier, flowIdentifier, this.branch.getSnapshotMetadata().getVersion())).thenAnswer(args -> deepCopy(this.upstream));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RecursiveRebase.diff(branch, upstream, bucketIdentifier, flowIdentifier, () -> byteArrayOutputStream);

        return Pair.of(upstream, () -> new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }

    public static <T> T deepCopy(T obj) {
        String content = null;
        try {
            content = OBJECT_MAPPER.writeValueAsString(obj);
            return (T) OBJECT_MAPPER.readValue(content, obj.getClass());
        } catch (Exception e) {
            String message = "Unable to deep copy " + obj;
            if (content != null) {
                message = message + "\n\n" + content;
            }
            throw new RuntimeException(message, e);
        }
    }

    public VersionedFlowSnapshot getBranch() {
        return branch;
    }

    public VersionedFlowSnapshot getUpstream() {
        return upstream;
    }

    public static VersionedProcessor createAndAddProcessor(String id, VersionedProcessGroup processGroup) {
        VersionedProcessor processor = new VersionedProcessor();
        processor.setGroupIdentifier(processGroup.getIdentifier());
        processor.setIdentifier(id);
        processor.setProperties(new HashMap<>());
        processor.setPropertyDescriptors(new HashMap<>());
        processGroup.getProcessors().add(processor);
        return processor;
    }

    public void assertEqualsWithNormalizedRootPg(VersionedComponent branch, VersionedComponent rebased) {
        try {
            VersionedComponent normalizedBranch = branch;
            if (this.branch.getFlowContents().getIdentifier().equals(branch.getGroupIdentifier())) {
                normalizedBranch = deepCopy(branch);
                String upstreamGroupId = upstream.getFlowContents().getIdentifier();
                normalizedBranch.setGroupIdentifier(upstreamGroupId);
                if (normalizedBranch instanceof VersionedRemoteProcessGroup) {
                    VersionedRemoteProcessGroup rpg = (VersionedRemoteProcessGroup) normalizedBranch;
                    rpg.getInputPorts().forEach(p -> p.setGroupIdentifier(upstreamGroupId));
                    rpg.getOutputPorts().forEach(p -> p.setGroupIdentifier(upstreamGroupId));
                }
            }

            assertEquals(OBJECT_MAPPER.writeValueAsString(normalizedBranch), OBJECT_MAPPER.writeValueAsString(rebased));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
