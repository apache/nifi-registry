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
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot.createAndAddProcessor;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentRemoveOperationTest {
    private VersionedComponentCollectionTest.PgMocker pgMocker;

    @Before
    public void setup() {
        pgMocker = new VersionedComponentCollectionTest.PgMocker();
    }

    @Test
    public void testApply() {
        String testId = "testId";
        VersionedProcessor processor = mock(VersionedProcessor.class);

        when(processor.getIdentifier()).thenReturn(testId);

        String grandChildIdentifier = pgMocker.getGrandChildProcessGroup().getIdentifier();
        Set<VersionedProcessor> processors = new HashSet<>(Collections.singleton(processor));
        when(pgMocker.getGrandChildProcessGroup().getProcessors()).thenReturn(processors);

        RoundTrip.testRoundTrip(MergeOperation.class, new ComponentRemoveOperation()).apply(mock(RebaseApplicationContext.class), pgMocker.getRootProcessGroup(),
                grandChildIdentifier, ComponentType.PROCESSOR, testId);
        assertEquals(0, processors.size());
    }

    @Test
    public void flowTest() throws IOException, NiFiRegistryException {
        VersionedFlowSnapshot base = TestFlowSnapshot.createBase();

        String id = "test-id";
        VersionedProcessGroup flowContents = base.getFlowContents();

        createAndAddProcessor(id, flowContents);

        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot(base);

        testFlowSnapshot.getBranch().getFlowContents().getProcessors().clear();

        VersionedFlowSnapshot reconciled = testFlowSnapshot.reconcile();
        Set<VersionedProcessor> processors = reconciled.getFlowContents().getProcessors();
        assertEquals(0, processors.size());
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

        VersionedRemoteGroupPort groupPort = new VersionedRemoteGroupPort();
        groupPort.setComponentType(ComponentType.REMOTE_INPUT_PORT);
        groupPort.setGroupIdentifier(base.getFlowContents().getIdentifier());
        groupPort.setRemoteGroupId(rpgId);
        groupPort.setIdentifier("rpgInputId");
        remoteProcessGroup.getInputPorts().add(groupPort);

        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot(base);

        testFlowSnapshot.getBranch().getFlowContents().getRemoteProcessGroups().iterator().next().getInputPorts().clear();

        VersionedFlowSnapshot reconcile = testFlowSnapshot.reconcile();
        Set<VersionedRemoteProcessGroup> reconciledRpgs = reconcile.getFlowContents().getRemoteProcessGroups();
        assertEquals(1, reconciledRpgs.size());
        assertEquals(0, reconciledRpgs.iterator().next().getInputPorts().size());
    }
}
