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

import org.apache.nifi.registry.flow.BatchSize;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.ConnectableComponentType;
import org.apache.nifi.registry.flow.Position;
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
import org.apache.nifi.registry.flow.diff.DifferenceType;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.apache.nifi.registry.toolkit.rebase.merge.VersionedComponentCollectionTest.PgMocker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleDifferenceTypeOperationTest {

    @Test
    public void testName() {
        testStringOperations(DifferenceType.NAME_CHANGED, Arrays.asList(ComponentType.CONNECTION, ComponentType.PROCESSOR, ComponentType.PROCESS_GROUP, ComponentType.REMOTE_PROCESS_GROUP,
                ComponentType.INPUT_PORT, ComponentType.OUTPUT_PORT, ComponentType.FUNNEL, ComponentType.LABEL, ComponentType.CONTROLLER_SERVICE), VersionedComponent::setName);
    }

    @Test
    public void testBundle() {
        Bundle from = new Bundle("beforeGroup", "beforeArtifact", "beforeVersion");
        Bundle to = new Bundle("afterGroup", "afterArtifact", "afterVersion");

        testOperation(DifferenceType.BUNDLE_CHANGED, from, to, ComponentType.PROCESSOR, VersionedProcessor::setBundle);
        testOperation(DifferenceType.BUNDLE_CHANGED, from, to, ComponentType.CONTROLLER_SERVICE, VersionedControllerService::setBundle);
    }

    @Test
    public void testPenaltyDuration() {
        testStringOperation(DifferenceType.PENALTY_DURATION_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setPenaltyDuration);
    }

    @Test
    public void testYieldDuration() {
        testStringOperation(DifferenceType.YIELD_DURATION_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setYieldDuration);
        testStringOperation(DifferenceType.YIELD_DURATION_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setYieldDuration);
    }

    @Test
    public void testBulletinLevel() {
        testStringOperation(DifferenceType.BULLETIN_LEVEL_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setBulletinLevel);
    }

    @Test
    public void testAutoTerminatedRelationships() {
        testOperation(DifferenceType.AUTO_TERMINATED_RELATIONSHIPS_CHANGED, Collections.emptySet(), new HashSet<>(Arrays.asList("a", "b", "c")), ComponentType.PROCESSOR,
                VersionedProcessor::setAutoTerminatedRelationships);
    }

    @Test
    public void testSchedulingStrategy() {
        testStringOperation(DifferenceType.SCHEDULING_STRATEGY_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setSchedulingStrategy);
    }

    @Test
    public void testConcurrentTasksChanged() {
        testOperation(DifferenceType.CONCURRENT_TASKS_CHANGED, 1, 2, ComponentType.PROCESSOR, VersionedProcessor::setConcurrentlySchedulableTaskCount);
    }

    @Test
    public void testRunSchedule() {
        testStringOperation(DifferenceType.RUN_SCHEDULE_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setSchedulingPeriod);
    }

    @Test
    public void testExecutionMode() {
        testStringOperation(DifferenceType.EXECUTION_MODE_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setExecutionNode);
    }

    @Test
    public void testRunDuration() {
        testOperation(DifferenceType.RUN_DURATION_CHANGED, 0L, 50L, ComponentType.PROCESSOR, VersionedProcessor::setRunDurationMillis);
    }

    @Test
    public void testAnnotationData() {
        testStringOperation(DifferenceType.ANNOTATION_DATA_CHANGED, ComponentType.PROCESSOR, VersionedProcessor::setAnnotationData);
        testStringOperation(DifferenceType.ANNOTATION_DATA_CHANGED, ComponentType.CONTROLLER_SERVICE, VersionedControllerService::setAnnotationData);
    }

    @Test
    public void testComments() {
        testStringOperations(DifferenceType.COMMENTS_CHANGED, Arrays.asList(ComponentType.CONNECTION, ComponentType.PROCESSOR, ComponentType.PROCESS_GROUP, ComponentType.REMOTE_PROCESS_GROUP,
                ComponentType.INPUT_PORT, ComponentType.OUTPUT_PORT, ComponentType.FUNNEL, ComponentType.LABEL, ComponentType.CONTROLLER_SERVICE), VersionedComponent::setComments);
    }

    @Test
    public void testPosition() {
        testOperations(DifferenceType.POSITION_CHANGED, new Position(0, 0), new Position(1, 1), Arrays.asList(ComponentType.CONNECTION, ComponentType.PROCESSOR,
                ComponentType.PROCESS_GROUP, ComponentType.REMOTE_PROCESS_GROUP, ComponentType.INPUT_PORT, ComponentType.OUTPUT_PORT, ComponentType.FUNNEL, ComponentType.LABEL,
                ComponentType.CONTROLLER_SERVICE), VersionedComponent::setPosition);
    }

    @Test
    public void testStyle() {
        Map<String, String> styles = new HashMap<>();
        styles.put("a", "b");
        testOperation(DifferenceType.STYLE_CHANGED, Collections.emptyMap(), styles, ComponentType.PROCESSOR, VersionedProcessor::setStyle);
        testOperation(DifferenceType.STYLE_CHANGED, Collections.emptyMap(), styles, ComponentType.LABEL, VersionedLabel::setStyle);
    }

    @Test
    public void testSelectedRelationships() {
        testOperation(DifferenceType.SELECTED_RELATIONSHIPS_CHANGED, Collections.emptySet(), new HashSet<>(Arrays.asList("a", "b", "c")), ComponentType.CONNECTION,
                VersionedConnection::setSelectedRelationships);
    }

    @Test
    public void testPrioritizers() {
        testOperation(DifferenceType.PRIORITIZERS_CHANGED, Collections.emptyList(), new ArrayList<>(Arrays.asList("a", "b", "c")), ComponentType.CONNECTION, VersionedConnection::setPrioritizers);
    }

    @Test
    public void testFlowfileExpiration() {
        testStringOperation(DifferenceType.FLOWFILE_EXPIRATION_CHANGED, ComponentType.CONNECTION, VersionedConnection::setFlowFileExpiration);
    }

    @Test
    public void testBackpressureObjectThreshold() {
        testOperation(DifferenceType.BACKPRESSURE_OBJECT_THRESHOLD_CHANGED, 0L, 50L, ComponentType.CONNECTION, VersionedConnection::setBackPressureObjectThreshold);
    }

    @Test
    public void testBackpressureDataSizeThreshold() {
        testStringOperation(DifferenceType.BACKPRESSURE_DATA_SIZE_THRESHOLD_CHANGED, ComponentType.CONNECTION, VersionedConnection::setBackPressureDataSizeThreshold);
    }

    @Test
    public void testLoadBalanceStrategy() {
        testStringOperation(DifferenceType.LOAD_BALANCE_STRATEGY_CHANGED, ComponentType.CONNECTION, VersionedConnection::setLoadBalanceStrategy);
    }

    @Test
    public void testPartitioningAttribute() {
        testStringOperation(DifferenceType.PARTITIONING_ATTRIBUTE_CHANGED, ComponentType.CONNECTION, VersionedConnection::setPartitioningAttribute);
    }

    @Test
    public void testLoadBalanceCompression() {
        testStringOperation(DifferenceType.LOAD_BALANCE_COMPRESSION_CHANGED, ComponentType.CONNECTION, VersionedConnection::setLoadBalanceCompression);
    }

    @Test
    public void testBendpoints() {
        List<Position> from = new ArrayList<>(Arrays.asList(new Position(1, 2), new Position(3, 4)));
        List<Position> to = new ArrayList<>(Arrays.asList(new Position(5, 6), new Position(7, 8)));
        testOperation(DifferenceType.BENDPOINTS_CHANGED, from, to, ComponentType.CONNECTION, VersionedConnection::setBends);
    }

    @Test
    public void testDestination() {
        ConnectableComponent from = new ConnectableComponent();
        from.setId("from-id");
        from.setName("from-name");
        from.setGroupId("from-group-id");
        from.setType(ConnectableComponentType.PROCESSOR);
        from.setComments("abc");

        ConnectableComponent to = new ConnectableComponent();
        to.setId("to-id");
        to.setName("to-name");
        to.setGroupId("to-group-id");
        to.setType(ConnectableComponentType.FUNNEL);
        to.setComments("def");
        testOperation(DifferenceType.DESTINATION_CHANGED, from, to, ComponentType.CONNECTION, VersionedConnection::setDestination);
    }

    @Test
    public void testSource() {
        ConnectableComponent from = new ConnectableComponent();
        from.setId("from-id");
        from.setName("from-name");
        from.setGroupId("from-group-id");
        from.setType(ConnectableComponentType.PROCESSOR);
        from.setComments("abc");

        ConnectableComponent to = new ConnectableComponent();
        to.setId("to-id");
        to.setName("to-name");
        to.setGroupId("to-group-id");
        to.setType(ConnectableComponentType.FUNNEL);
        to.setComments("def");
        testOperation(DifferenceType.SOURCE_CHANGED, from, to, ComponentType.CONNECTION, VersionedConnection::setSource);
    }

    @Test
    public void testLabelValue() {
        testStringOperation(DifferenceType.LABEL_VALUE_CHANGED, ComponentType.LABEL, VersionedLabel::setLabel);
    }

    @Test
    public void testRPGTransportProtocol() {
        testStringOperation(DifferenceType.RPG_TRANSPORT_PROTOCOL_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setTransportProtocol);
    }

    @Test
    public void testRPGProxyHost() {
        testStringOperation(DifferenceType.RPG_PROXY_HOST_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setProxyHost);
    }

    @Test
    public void setRPGProxyPort() {
        testOperation(DifferenceType.RPG_PROXY_PORT_CHANGED, 1, 2, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setProxyPort);
    }

    @Test
    public void testRPGProxyUser() {
        testStringOperation(DifferenceType.RPG_PROXY_USER_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setProxyUser);
    }

    @Test
    public void testRPGNetworkInterface() {
        testStringOperation(DifferenceType.RPG_NETWORK_INTERFACE_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setLocalNetworkInterface);
    }

    @Test
    public void testRPGCommsTimeout() {
        testStringOperation(DifferenceType.RPG_COMMS_TIMEOUT_CHANGED, ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup::setCommunicationsTimeout);
    }

    @Test
    public void testRemotePortBatchSize() {
        BatchSize from = new BatchSize();
        from.setCount(1);
        from.setDuration("from-duration");
        from.setSize("from-size");

        BatchSize to = new BatchSize();
        to.setCount(2);
        to.setDuration("to-duration");
        to.setSize("to-size");

        PgMocker pgMocker = new PgMocker();
        VersionedRemoteGroupPort port = mockPortOnGrandchild(pgMocker, ComponentType.REMOTE_INPUT_PORT);
        testOperation(pgMocker, DifferenceType.REMOTE_PORT_BATCH_SIZE_CHANGED, from, to, VersionedRemoteGroupPort::setBatchSize, port);

        pgMocker = new PgMocker();
        port = mockPortOnGrandchild(pgMocker, ComponentType.REMOTE_OUTPUT_PORT);
        testOperation(pgMocker, DifferenceType.REMOTE_PORT_BATCH_SIZE_CHANGED, from, to, VersionedRemoteGroupPort::setBatchSize, port);
    }

    @Test
    public void setRemotePortCompression() {
        PgMocker pgMocker = new PgMocker();
        VersionedRemoteGroupPort port = mockPortOnGrandchild(pgMocker, ComponentType.REMOTE_INPUT_PORT);
        testOperation(pgMocker, DifferenceType.REMOTE_PORT_COMPRESSION_CHANGED, false, true, VersionedRemoteGroupPort::setUseCompression, port);


        pgMocker = new PgMocker();
        port = mockPortOnGrandchild(pgMocker, ComponentType.REMOTE_OUTPUT_PORT);
        testOperation(pgMocker, DifferenceType.REMOTE_PORT_COMPRESSION_CHANGED, false, true, VersionedRemoteGroupPort::setUseCompression, port);
    }

    @Test
    public void testOnlyExpectedDifferenceTypesUncovered() {
        List<DifferenceType> coveredElsewhere = Arrays.asList(DifferenceType.COMPONENT_ADDED, DifferenceType.COMPONENT_REMOVED, DifferenceType.PROPERTY_ADDED, DifferenceType.PROPERTY_REMOVED,
                DifferenceType.PROPERTY_CHANGED, DifferenceType.VERSIONED_FLOW_COORDINATES_CHANGED, DifferenceType.VARIABLE_ADDED, DifferenceType.VARIABLE_REMOVED);
        List<DifferenceType> unsupported = Arrays.asList(DifferenceType.TYPE_CHANGED, DifferenceType.SERVICE_API_CHANGED);

        Set<DifferenceType> unexpectedMissingTypes = Arrays.stream(DifferenceType.values()).collect(Collectors.toSet());
        unexpectedMissingTypes.removeAll(coveredElsewhere);
        unexpectedMissingTypes.removeAll(unsupported);
        unexpectedMissingTypes.removeAll(SingleDifferenceTypeOperation.getSupportedDifferenceTypes());
        assertEquals(new ArrayList<>(), unexpectedMissingTypes.stream().sorted(Comparator.comparing(DifferenceType::ordinal)).collect(Collectors.toList()));
    }

    private static <T extends VersionedComponent> void testStringOperation(DifferenceType type, ComponentType componentType, BiConsumer<T, String> setter) {
        testStringOperations(type, Collections.singletonList(componentType), setter);
    }

    private static <T extends VersionedComponent> void testStringOperations(DifferenceType type, List<ComponentType> componentTypes, BiConsumer<T, String> setter) {
        testOperations(type, "old" + type, "new" + type, componentTypes, setter);
    }

    private static <T extends VersionedComponent, U> void testOperations(DifferenceType type, U from, U to, List<ComponentType> componentTypes, BiConsumer<T, U> setter) {
        for (ComponentType componentType : componentTypes) {
            testOperation(type, from, to, componentType, setter);
        }
    }

    private static <T extends VersionedComponent, U> void testOperation(DifferenceType type, U from, U to, ComponentType componentType, BiConsumer<T, U> setter) {
        Class clazz;
        Function<VersionedProcessGroup, Set> pgGetter;

        switch (componentType) {
            case CONNECTION:
                clazz = VersionedConnection.class;
                pgGetter = VersionedProcessGroup::getConnections;
                break;
            case PROCESSOR:
                clazz = VersionedProcessor.class;
                pgGetter = VersionedProcessGroup::getProcessors;
                break;
            case PROCESS_GROUP:
                clazz = VersionedProcessGroup.class;
                pgGetter = VersionedProcessGroup::getProcessGroups;
                break;
            case REMOTE_PROCESS_GROUP:
                clazz = VersionedRemoteProcessGroup.class;
                pgGetter = VersionedProcessGroup::getRemoteProcessGroups;
                break;
            case INPUT_PORT:
                clazz = VersionedPort.class;
                pgGetter = VersionedProcessGroup::getInputPorts;
                break;
            case OUTPUT_PORT:
                clazz = VersionedPort.class;
                pgGetter = VersionedProcessGroup::getOutputPorts;
                break;
            case FUNNEL:
                clazz = VersionedFunnel.class;
                pgGetter = VersionedProcessGroup::getFunnels;
                break;
            case LABEL:
                clazz = VersionedLabel.class;
                pgGetter = VersionedProcessGroup::getLabels;
                break;
            case CONTROLLER_SERVICE:
                clazz = VersionedControllerService.class;
                pgGetter = VersionedProcessGroup::getControllerServices;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + componentType);
        }
        T component = (T) mock(clazz);
        when(component.getComponentType()).thenReturn(componentType);
        PgMocker pgMocker = new PgMocker();
        when(pgGetter.andThen(s -> (Set<T>) s).apply(pgMocker.getGrandChildProcessGroup())).thenReturn(Collections.singleton(component));

        testOperation(pgMocker, type, from, to, setter, component);
    }

    private static <T extends VersionedComponent, U> void testOperation(PgMocker pgMocker, DifferenceType type, U from, U to, BiConsumer<T, U> setter, T component) {
        String testId = "testId";

        when(component.getIdentifier()).thenReturn(testId);

        SingleDifferenceTypeOperation operation = new SingleDifferenceTypeOperation(type, from, to);

        RoundTrip.testRoundTrip(MergeOperation.class, operation).apply(mock(RebaseApplicationContext.class), pgMocker.getRootProcessGroup(), pgMocker.getGrandChildProcessGroup().getIdentifier(),
                component.getComponentType(), testId);

        setter.accept(verify(component), eq(to));
    }

    private static VersionedRemoteGroupPort mockPortOnGrandchild(PgMocker pgMocker, ComponentType componentType) {
        VersionedRemoteGroupPort port = mock(VersionedRemoteGroupPort.class);
        when(port.getComponentType()).thenReturn(componentType);
        VersionedRemoteProcessGroup group = mock(VersionedRemoteProcessGroup.class);
        if (componentType == ComponentType.REMOTE_INPUT_PORT) {
            when(group.getInputPorts()).thenReturn(Collections.singleton(port));
        } else {
            when(group.getOutputPorts()).thenReturn(Collections.singleton(port));
        }
        when(pgMocker.getGrandChildProcessGroup().getRemoteProcessGroups()).thenReturn(Collections.singleton(group));
        return port;
    }
}
