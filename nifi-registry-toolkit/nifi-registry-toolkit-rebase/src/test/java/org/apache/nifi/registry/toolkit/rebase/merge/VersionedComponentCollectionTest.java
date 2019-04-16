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

import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedLabel;
import org.apache.nifi.registry.flow.VersionedPort;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.VersionedFlowSnapshotReconciliation;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionedComponentCollectionTest {
    public static class PgMocker {
        private final VersionedProcessGroup rootProcessGroup;
        private final VersionedProcessGroup childProcessGroup;
        private final VersionedProcessGroup grandChildProcessGroup;

        public PgMocker() {
            String rootId = VersionedFlowSnapshotReconciliation.ROOT_PG_ID;
            String childId = "test-child-id";
            String grandChildId = "test-grandchild-id";

            rootProcessGroup = mock(VersionedProcessGroup.class);
            childProcessGroup = mock(VersionedProcessGroup.class);
            grandChildProcessGroup = mock(VersionedProcessGroup.class);

            when(rootProcessGroup.getIdentifier()).thenReturn(rootId);

            when(childProcessGroup.getGroupIdentifier()).thenReturn(rootId);
            when(childProcessGroup.getIdentifier()).thenReturn(childId);
            when(rootProcessGroup.getProcessGroups()).thenReturn(Collections.singleton(childProcessGroup));

            when(grandChildProcessGroup.getGroupIdentifier()).thenReturn(childId);
            when(grandChildProcessGroup.getIdentifier()).thenReturn(grandChildId);
            when(childProcessGroup.getProcessGroups()).thenReturn(Collections.singleton(grandChildProcessGroup));
        }

        public VersionedProcessGroup getRootProcessGroup() {
            return rootProcessGroup;
        }

        public VersionedProcessGroup getChildProcessGroup() {
            return childProcessGroup;
        }

        public VersionedProcessGroup getGrandChildProcessGroup() {
            return grandChildProcessGroup;
        }
    }

    private <T extends VersionedComponent> void testComponentType(Class<T> clazz, ComponentType componentType, Function<VersionedProcessGroup, Set<T>> getter) {
        PgMocker pgMocker = new PgMocker();
        testComponentType(pgMocker.getRootProcessGroup(), pgMocker.getRootProcessGroup(), VersionedFlowSnapshotReconciliation.ROOT_PG_ID, clazz, componentType, getter);
        pgMocker = new PgMocker();
        testComponentType(pgMocker.getRootProcessGroup(), pgMocker.getChildProcessGroup(), pgMocker.getChildProcessGroup().getIdentifier(), clazz, componentType, getter);
        pgMocker = new PgMocker();
        testComponentType(pgMocker.getRootProcessGroup(), pgMocker.getGrandChildProcessGroup(), pgMocker.getGrandChildProcessGroup().getIdentifier(), clazz, componentType, getter);
    }

    private <T extends VersionedComponent> void testComponentType(VersionedProcessGroup rootProcessGroup, VersionedProcessGroup versionedProcessGroup, String groupId,
                                                                  Class<T> clazz, ComponentType componentType, Function<VersionedProcessGroup, Set<T>> getter) {
        T component = mock(clazz);
        String testId = "testId";

        when(component.getIdentifier()).thenReturn(testId);

        Set<T> collection = new HashSet<>();

        when(getter.apply(versionedProcessGroup)).thenReturn(collection);

        VersionedComponentCollection<? extends VersionedComponent> componentCollection = VersionedComponentCollection.get(rootProcessGroup, groupId, componentType);

        componentCollection.add(component);
        assertEquals(1, collection.size());
        assertEquals(component, collection.iterator().next());

        Optional<? extends VersionedComponent> optional = componentCollection.getById(testId);
        assertTrue(optional.isPresent());
        assertEquals(component, optional.get());

        assertFalse(componentCollection.getById("badId").isPresent());
        assertFalse(componentCollection.removeById("badId"));
        assertEquals(1, collection.size());

        assertTrue(componentCollection.removeById(testId));
        assertEquals(0, collection.size());
    }

    @Test
    public void testConnection() {
        testComponentType(VersionedConnection.class, ComponentType.CONNECTION, VersionedProcessGroup::getConnections);
    }

    @Test
    public void testProcessor() {
        testComponentType(VersionedProcessor.class, ComponentType.PROCESSOR, VersionedProcessGroup::getProcessors);
    }

    @Test
    public void testProcessGroup() {
        testComponentType(VersionedProcessGroup.class, ComponentType.PROCESS_GROUP, VersionedProcessGroup::getProcessGroups);
    }

    @Test
    public void testRemoteProcessGroup() {
        testComponentType(VersionedRemoteProcessGroup.class, ComponentType.REMOTE_PROCESS_GROUP, VersionedProcessGroup::getRemoteProcessGroups);
    }

    @Test
    public void testInputPort() {
        testComponentType(VersionedPort.class, ComponentType.INPUT_PORT, VersionedProcessGroup::getInputPorts);
    }

    @Test
    public void testOutputPort() {
        testComponentType(VersionedPort.class, ComponentType.OUTPUT_PORT, VersionedProcessGroup::getOutputPorts);
    }

    @Test
    public void testFunnel() {
        testComponentType(VersionedFunnel.class, ComponentType.FUNNEL, VersionedProcessGroup::getFunnels);
    }

    @Test
    public void testLabel() {
        testComponentType(VersionedLabel.class, ComponentType.LABEL, VersionedProcessGroup::getLabels);
    }

    @Test
    public void testControllerService() {
        testComponentType(VersionedControllerService.class, ComponentType.CONTROLLER_SERVICE, VersionedProcessGroup::getControllerServices);
    }

    @Test
    public void testCantFindById() {
        assertFalse(VersionedComponentCollection.findById(new PgMocker().getRootProcessGroup(), "badId").isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testCantGet() {
        VersionedComponentCollection.get(new PgMocker().getRootProcessGroup(), "badId", ComponentType.LABEL);
    }
}
