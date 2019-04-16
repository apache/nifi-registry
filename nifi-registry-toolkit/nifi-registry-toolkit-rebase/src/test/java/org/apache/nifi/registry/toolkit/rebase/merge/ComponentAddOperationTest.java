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

import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot.createAndAddProcessor;
import static org.apache.nifi.registry.toolkit.rebase.VersionedFlowSnapshotReconciliation.ROOT_PG_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComponentAddOperationTest {
    private VersionedComponentCollectionTest.PgMocker pgMocker;

    @Before
    public void setup() {
        pgMocker = new VersionedComponentCollectionTest.PgMocker();
    }

    @Test
    public void testApply() {
        String testId = "testId";
        VersionedProcessor processor = mock(VersionedProcessor.class);

        String grandChildIdentifier = pgMocker.getGrandChildProcessGroup().getIdentifier();
        Set<VersionedProcessor> processors = new HashSet<>();
        when(pgMocker.getGrandChildProcessGroup().getProcessors()).thenReturn(processors);

        when(processor.getGroupIdentifier()).thenReturn(grandChildIdentifier);
        when(processor.getIdentifier()).thenReturn(testId);

        new ComponentAddOperation(processor).apply(mock(RebaseApplicationContext.class), pgMocker.getRootProcessGroup(), grandChildIdentifier, ComponentType.PROCESSOR, testId);
        assertEquals(1, processors.size());
        assertEquals(processor, processors.iterator().next());

        verify(processor, never()).setGroupIdentifier(anyString());
    }

    @Test
    public void testApplyRootGroup() {
        String testId = "testId";
        VersionedProcessor processor = mock(VersionedProcessor.class);

        when(processor.getGroupIdentifier()).thenReturn(ROOT_PG_ID);

        Set<VersionedProcessor> processors = new HashSet<>();
        when(pgMocker.getRootProcessGroup().getProcessors()).thenReturn(processors);

        when(processor.getIdentifier()).thenReturn(testId);

        String rootPgId = pgMocker.getRootProcessGroup().getIdentifier();
        new ComponentAddOperation(processor).apply(mock(RebaseApplicationContext.class), pgMocker.getRootProcessGroup(), rootPgId, ComponentType.PROCESSOR, testId);
        assertEquals(1, processors.size());
        assertEquals(processor, processors.iterator().next());

        verify(processor).setGroupIdentifier(rootPgId);
    }

    @Test
    public void testRoundTrip() {
        RoundTrip.testRoundTrip(MergeOperation.class, new ComponentAddOperation(new VersionedProcessor()));
    }

    @Test
    public void flowTest() throws IOException, NiFiRegistryException {
        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot();

        String id = "test-id";
        VersionedProcessor processor = createAndAddProcessor(id, testFlowSnapshot.getBranch().getFlowContents());

        VersionedFlowSnapshot reconciled = testFlowSnapshot.reconcile();
        Set<VersionedProcessor> processors = reconciled.getFlowContents().getProcessors();
        assertEquals(1, processors.size());
        testFlowSnapshot.assertEqualsWithNormalizedRootPg(processor, processors.iterator().next());
    }

    @Test
    public void flowTestRemoteInputPort() throws IOException, NiFiRegistryException {
        VersionedFlowSnapshot base = TestFlowSnapshot.createBase();

        String rpgId = "rpgId";
        VersionedRemoteProcessGroup remoteProcessGroup = new VersionedRemoteProcessGroup();
        remoteProcessGroup.setInputPorts(new HashSet<>());
        remoteProcessGroup.setOutputPorts(new HashSet<>());
        remoteProcessGroup.setGroupIdentifier(base.getFlowContents().getIdentifier());
        remoteProcessGroup.setIdentifier(rpgId);

        base.getFlowContents().getRemoteProcessGroups().add(remoteProcessGroup);

        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot(base);

        VersionedRemoteGroupPort groupPort = new VersionedRemoteGroupPort();
        groupPort.setComponentType(ComponentType.REMOTE_INPUT_PORT);
        groupPort.setGroupIdentifier(testFlowSnapshot.getBranch().getFlowContents().getIdentifier());
        groupPort.setRemoteGroupId(rpgId);
        groupPort.setIdentifier("rpgInputId");
        VersionedRemoteProcessGroup branchRpg = testFlowSnapshot.getBranch().getFlowContents().getRemoteProcessGroups().iterator().next();
        branchRpg.getInputPorts().add(groupPort);

        VersionedFlowSnapshot reconcile = testFlowSnapshot.reconcile();
        Set<VersionedRemoteProcessGroup> reconciledRpgs = reconcile.getFlowContents().getRemoteProcessGroups();
        assertEquals(1, reconciledRpgs.size());
        testFlowSnapshot.assertEqualsWithNormalizedRootPg(branchRpg, reconciledRpgs.iterator().next());
    }

    @Test(expected = RuntimeException.class)
    public void flowTestDoubleAdd() throws IOException, NiFiRegistryException {
        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot();

        String id = "test-id";
        createAndAddProcessor(id, testFlowSnapshot.getBranch().getFlowContents());
        createAndAddProcessor(id, testFlowSnapshot.getUpstream().getFlowContents());

        // need another difference or merge will be a no-op
        createAndAddProcessor("test-id-2", testFlowSnapshot.getBranch().getFlowContents());

        testFlowSnapshot.reconcile();
    }
}
