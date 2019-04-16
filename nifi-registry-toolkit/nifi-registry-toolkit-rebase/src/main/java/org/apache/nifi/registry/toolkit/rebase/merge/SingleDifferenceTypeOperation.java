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
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.BatchSize;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedExtensionComponent;
import org.apache.nifi.registry.flow.VersionedLabel;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.apache.nifi.registry.flow.diff.DifferenceType;
import org.apache.nifi.registry.flow.diff.FlowDifference;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.apache.nifi.registry.toolkit.rebase.merge.util.AggregatePropertySetter;
import org.apache.nifi.registry.toolkit.rebase.merge.util.PropertySetter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.nifi.registry.toolkit.rebase.VersionedFlowSnapshotReconciliation.ROOT_PG_ID;

/**
 * For difference types that are considered a single value for purposes of the rebase.
 */
public class SingleDifferenceTypeOperation implements MergeOperation {
    private static final Map<DifferenceType, BiConsumer<VersionedComponent, Object>> propertySetters = Collections.unmodifiableMap(Stream.of(
            PropertySetter.createStringSetter(DifferenceType.NAME_CHANGED, VersionedComponent.class, VersionedComponent::setName).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.BUNDLE_CHANGED, VersionedExtensionComponent.class, Bundle.class, VersionedExtensionComponent::setBundle).toPair(),
            PropertySetter.createStringSetter(DifferenceType.PENALTY_DURATION_CHANGED, VersionedProcessor.class, VersionedProcessor::setPenaltyDuration).toPair(),

            AggregatePropertySetter.Builder.createStringBuilder(DifferenceType.YIELD_DURATION_CHANGED)
                    .addDelegate(VersionedProcessor.class, VersionedProcessor::setYieldDuration)
                    .addDelegate(VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setYieldDuration)
                    .build().toPair(),

            PropertySetter.createStringSetter(DifferenceType.BULLETIN_LEVEL_CHANGED, VersionedProcessor.class, VersionedProcessor::setBulletinLevel).toPair(),
            PropertySetter.createStringCollectionSetter(DifferenceType.AUTO_TERMINATED_RELATIONSHIPS_CHANGED, VersionedProcessor.class,
                    VersionedProcessor::setAutoTerminatedRelationships, Collectors.toSet()).toPair(),
            PropertySetter.createStringSetter(DifferenceType.SCHEDULING_STRATEGY_CHANGED, VersionedProcessor.class, VersionedProcessor::setSchedulingStrategy).toPair(),

            AggregatePropertySetter.Builder.createNumberBuilder(DifferenceType.CONCURRENT_TASKS_CHANGED, Number::intValue)
                    .addDelegate(VersionedProcessor.class, VersionedProcessor::setConcurrentlySchedulableTaskCount)
                    .addDelegate(VersionedRemoteGroupPort.class, VersionedRemoteGroupPort::setConcurrentlySchedulableTaskCount)
                    .build().toPair(),

            PropertySetter.createStringSetter(DifferenceType.RUN_SCHEDULE_CHANGED, VersionedProcessor.class, VersionedProcessor::setSchedulingPeriod).toPair(),
            PropertySetter.createStringSetter(DifferenceType.EXECUTION_MODE_CHANGED, VersionedProcessor.class, VersionedProcessor::setExecutionNode).toPair(),
            PropertySetter.createNumberSetter(DifferenceType.RUN_DURATION_CHANGED, VersionedProcessor.class, VersionedProcessor::setRunDurationMillis, Number::longValue).toPair(),

            AggregatePropertySetter.Builder.createStringBuilder(DifferenceType.ANNOTATION_DATA_CHANGED)
                    .addDelegate(VersionedProcessor.class, VersionedProcessor::setAnnotationData)
                    .addDelegate(VersionedControllerService.class, VersionedControllerService::setAnnotationData)
                    .build().toPair(),

            PropertySetter.createStringSetter(DifferenceType.COMMENTS_CHANGED, VersionedComponent.class, VersionedComponent::setComments).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.POSITION_CHANGED, VersionedComponent.class, Position.class, VersionedComponent::setPosition).toPair(),
            new AggregatePropertySetter.Builder<>(DifferenceType.STYLE_CHANGED, o -> (Map<String, String>) o)
                    .addDelegate(VersionedProcessor.class, VersionedProcessor::setStyle)
                    .addDelegate(VersionedLabel.class, VersionedLabel::setStyle)
                    .build().toPair(),
            PropertySetter.createStringCollectionSetter(DifferenceType.SELECTED_RELATIONSHIPS_CHANGED, VersionedConnection.class,
                    VersionedConnection::setSelectedRelationships, Collectors.toSet()).toPair(),
            PropertySetter.createStringCollectionSetter(DifferenceType.PRIORITIZERS_CHANGED, VersionedConnection.class, VersionedConnection::setPrioritizers, Collectors.toList()).toPair(),
            PropertySetter.createStringSetter(DifferenceType.FLOWFILE_EXPIRATION_CHANGED, VersionedConnection.class, VersionedConnection::setFlowFileExpiration).toPair(),
            PropertySetter.createNumberSetter(DifferenceType.BACKPRESSURE_OBJECT_THRESHOLD_CHANGED, VersionedConnection.class,
                    VersionedConnection::setBackPressureObjectThreshold, Number::longValue).toPair(),
            PropertySetter.createStringSetter(DifferenceType.BACKPRESSURE_DATA_SIZE_THRESHOLD_CHANGED, VersionedConnection.class, VersionedConnection::setBackPressureDataSizeThreshold).toPair(),
            PropertySetter.createStringSetter(DifferenceType.LOAD_BALANCE_STRATEGY_CHANGED, VersionedConnection.class, VersionedConnection::setLoadBalanceStrategy).toPair(),
            PropertySetter.createStringSetter(DifferenceType.PARTITIONING_ATTRIBUTE_CHANGED, VersionedConnection.class, VersionedConnection::setPartitioningAttribute).toPair(),
            PropertySetter.createStringSetter(DifferenceType.LOAD_BALANCE_COMPRESSION_CHANGED, VersionedConnection.class, VersionedConnection::setLoadBalanceCompression).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.BENDPOINTS_CHANGED, VersionedConnection.class, new TypeReference<List<Position>>() {}, VersionedConnection::setBends).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.SOURCE_CHANGED, VersionedConnection.class, ConnectableComponent.class, VersionedConnection::setSource).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.DESTINATION_CHANGED, VersionedConnection.class, ConnectableComponent.class, VersionedConnection::setDestination).toPair(),
            PropertySetter.createStringSetter(DifferenceType.LABEL_VALUE_CHANGED, VersionedLabel.class, VersionedLabel::setLabel).toPair(),
            PropertySetter.createStringSetter(DifferenceType.RPG_TRANSPORT_PROTOCOL_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setTransportProtocol).toPair(),
            PropertySetter.createStringSetter(DifferenceType.RPG_PROXY_HOST_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setProxyHost).toPair(),
            PropertySetter.createNumberSetter(DifferenceType.RPG_PROXY_PORT_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setProxyPort, Number::intValue).toPair(),
            PropertySetter.createStringSetter(DifferenceType.RPG_PROXY_USER_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setProxyUser).toPair(),
            PropertySetter.createStringSetter(DifferenceType.RPG_NETWORK_INTERFACE_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setLocalNetworkInterface).toPair(),
            PropertySetter.createStringSetter(DifferenceType.RPG_COMMS_TIMEOUT_CHANGED, VersionedRemoteProcessGroup.class, VersionedRemoteProcessGroup::setCommunicationsTimeout).toPair(),
            PropertySetter.createReparseSetter(DifferenceType.REMOTE_PORT_BATCH_SIZE_CHANGED, VersionedRemoteGroupPort.class, BatchSize.class, VersionedRemoteGroupPort::setBatchSize).toPair(),
            new PropertySetter<>(DifferenceType.REMOTE_PORT_COMPRESSION_CHANGED, VersionedRemoteGroupPort.class, Boolean.class::cast, VersionedRemoteGroupPort::setUseCompression).toPair()
    ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

    private final DifferenceType differenceType;
    private final Object from;
    private final Object to;

    public SingleDifferenceTypeOperation(FlowDifference flowDifference) {
        this(flowDifference.getDifferenceType(),
                getValue(flowDifference.getDifferenceType(),
                        flowDifference.getComponentA(), flowDifference.getValueA()), getValue(flowDifference.getDifferenceType(), flowDifference.getComponentB(), flowDifference.getValueB()));
    }

    private static Object getValue(DifferenceType differenceType, VersionedComponent component, Object value) {
        switch (differenceType) {
            case SOURCE_CHANGED:
                return component;
            default:
                return value;
        }
    }

    @JsonCreator
    public SingleDifferenceTypeOperation(@JsonProperty("differenceType") DifferenceType differenceType, @JsonProperty("from") Object from, @JsonProperty("to") Object to) {
        if (!getSupportedDifferenceTypes().contains(differenceType)) {
            throw new IllegalArgumentException(SingleDifferenceTypeOperation.class.getSimpleName() + " doesn't support difference type " + differenceType);
        }

        this.differenceType = differenceType;
        this.from = from;
        this.to = to;
    }

    public static Set<DifferenceType> getSupportedDifferenceTypes() {
        return propertySetters.keySet();
    }

    @Override
    public void apply(RebaseApplicationContext rebaseApplicationContext, VersionedProcessGroup processGroup, String groupId, ComponentType componentType, String componentId) {
        VersionedComponent component;
        if (ROOT_PG_ID.equals(componentId) && processGroup.getGroupIdentifier() == null) {
            component = processGroup;
        } else {
            component = VersionedComponentCollection.get(processGroup, groupId, componentType).getById(componentId).orElseThrow(() ->
                    new IllegalStateException("Can't find component " + componentId + " with type " + componentType));
        }
        propertySetters.get(differenceType).accept(component, to);
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public Object getFrom() {
        return from;
    }

    public Object getTo() {
        return to;
    }
}
