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

package org.apache.nifi.registry.flow.diff;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

public class StandardFlowComparator implements FlowComparator {

    private final ComparableDataFlow flowA;
    private final ComparableDataFlow flowB;

    public StandardFlowComparator(final ComparableDataFlow flowA, final ComparableDataFlow flowB) {
        this.flowA = flowA;
        this.flowB = flowB;
    }

    @Override
    public FlowComparison compare() {
        final VersionedProcessGroup groupA = flowA.getContents();
        final VersionedProcessGroup groupB = flowB.getContents();
        final Set<FlowDifference> differences = compare(groupA, groupB);

        return new StandardFlowComparison(flowA, flowB, differences);
    }

    private Set<FlowDifference> compare(final VersionedProcessGroup groupA, final VersionedProcessGroup groupB) {
        final Set<FlowDifference> differences = new HashSet<>();
        // Note that we do not compare the names, because when we import a Flow into NiFi, we may well give it a new name.
        // Child Process Groups' names will still compare but the main group that is under Version Control will not
        compare(groupA, groupB, differences, false);
        return differences;
    }


    private <T extends VersionedComponent> Set<FlowDifference> compareComponents(final Set<T> componentsA, final Set<T> componentsB, final ComponentComparator<T> comparator) {
        final Map<String, T> componentMapA = byId(componentsA == null ? Collections.emptySet() : componentsA);
        final Map<String, T> componentMapB = byId(componentsB == null ? Collections.emptySet() : componentsB);

        final Set<FlowDifference> differences = new HashSet<>();

        componentMapA.entrySet().stream()
            .forEach(entry -> {
                final T componentA = entry.getValue();
                final T componentB = componentMapB.get(entry.getKey());

                comparator.compare(componentA, componentB, differences);
            });

        componentMapB.entrySet().stream()
            .forEach(entry -> {
                final T componentB = entry.getValue();
                final T componentA = componentMapA.get(entry.getKey());

                // if component A is not null, it has already been compared above. If component A
                // is null, then it is missing from Flow A but present in Flow B, so we will just call
                // compare(), which will handle this for us.
                if (componentA == null) {
                    comparator.compare(componentA, componentB, differences);
                }
            });

        return differences;
    }


    private boolean compareComponents(final VersionedComponent componentA, final VersionedComponent componentB, final Set<FlowDifference> differences) {
        return compareComponents(componentA, componentB, differences, true);
    }

    private boolean compareComponents(final VersionedComponent componentA, final VersionedComponent componentB, final Set<FlowDifference> differences, final boolean compareNamePos) {
        if (componentA == null) {
            differences.add(difference(DifferenceType.COMPONENT_ADDED, componentA, componentB, componentA, componentB));
            return true;
        }

        if (componentB == null) {
            differences.add(difference(DifferenceType.COMPONENT_REMOVED, componentA, componentB, componentA, componentB));
            return true;
        }

        addIfDifferent(differences, DifferenceType.COMMENTS_CHANGED, componentA, componentB, c -> c.getComments());

        if (compareNamePos) {
            addIfDifferent(differences, DifferenceType.NAME_CHANGED, componentA, componentB, c -> c.getName());
            addIfDifferent(differences, DifferenceType.POSITION_CHANGED, componentA, componentB, c -> c.getPosition());
        }

        return false;
    }

    private void compare(final VersionedProcessor processorA, final VersionedProcessor processorB, final Set<FlowDifference> differences) {
        if (compareComponents(processorA, processorB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.ANNOTATION_DATA_CHANGED, processorA, processorB, p -> p.getAnnotationData());
        addIfDifferent(differences, DifferenceType.AUTO_TERMINATED_RELATIONSHIPS_CHANGED, processorA, processorB, p -> p.getAutoTerminatedRelationships());
        addIfDifferent(differences, DifferenceType.BULLETIN_LEVEL_CHANGED, processorA, processorB, p -> p.getBulletinLevel());
        addIfDifferent(differences, DifferenceType.BUNDLE_CHANGED, processorA, processorB, p -> p.getBundle());
        addIfDifferent(differences, DifferenceType.CONCURRENT_TASKS_CHANGED, processorA, processorB, p -> p.getConcurrentlySchedulableTaskCount());
        addIfDifferent(differences, DifferenceType.EXECUTION_MODE_CHANGED, processorA, processorB, p -> p.getExecutionNode());
        addIfDifferent(differences, DifferenceType.PENALTY_DURATION_CHANGED, processorA, processorB, p -> p.getPenaltyDuration());
        addIfDifferent(differences, DifferenceType.RUN_DURATION_CHANGED, processorA, processorB, p -> p.getRunDurationMillis());
        addIfDifferent(differences, DifferenceType.SCHEDULING_STRATEGY_CHANGED, processorA, processorB, p -> p.getSchedulingPeriod());
        addIfDifferent(differences, DifferenceType.SCHEDULING_STRATEGY_CHANGED, processorA, processorB, p -> p.getSchedulingStrategy());
        addIfDifferent(differences, DifferenceType.STYLE_CHANGED, processorA, processorB, p -> p.getStyle());
        addIfDifferent(differences, DifferenceType.YIELD_DURATION_CHANGED, processorA, processorB, p -> p.getYieldDuration());
        compareProperties(processorA, processorB, processorA.getProperties(), processorB.getProperties(), differences);
    }

    private void compare(final VersionedControllerService serviceA, final VersionedControllerService serviceB, final Set<FlowDifference> differences) {
        if (compareComponents(serviceA, serviceB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.ANNOTATION_DATA_CHANGED, serviceA, serviceB, s -> s.getAnnotationData());
        addIfDifferent(differences, DifferenceType.BUNDLE_CHANGED, serviceA, serviceB, s -> s.getBundle());
        compareProperties(serviceA, serviceB, serviceA.getProperties(), serviceB.getProperties(), differences);
    }


    private void compareProperties(final VersionedComponent componentA, final VersionedComponent componentB,
        final Map<String, String> propertiesA, final Map<String, String> propertiesB, final Set<FlowDifference> differences) {

        propertiesA.entrySet().stream()
            .forEach(entry -> {
                final String valueA = entry.getValue();
                final String valueB = propertiesB.get(entry.getKey());

                if (valueA == null && valueB != null) {
                    differences.add(difference(DifferenceType.PROPERTY_ADDED, componentA, componentB, entry.getKey(), entry.getKey()));
                } else if (valueA != null && valueB == null) {
                    differences.add(difference(DifferenceType.PROPERTY_REMOVED, componentA, componentB, entry.getKey(), entry.getKey()));
                } else if (valueA != null && valueB != null && !valueA.equals(valueB)) {
                    differences.add(difference(DifferenceType.PROPERTY_CHANGED, componentA, componentB, valueA, valueB));
                }
            });

        propertiesB.entrySet().stream()
            .forEach(entry -> {
                final String valueA = propertiesA.get(entry.getKey());
                final String valueB = entry.getValue();

                // If there are any properties for component B that do not exist for Component A, add those as differences as well.
                if (valueA == null && valueB != null) {
                    differences.add(difference(DifferenceType.PROPERTY_ADDED, componentA, componentB, entry.getKey(), entry.getKey()));
                }
            });
    }

    private void compareVariables(final VersionedProcessGroup groupA, final VersionedProcessGroup groupB, final Set<FlowDifference> differences) {

        final Map<String, String> variablesA = groupA.getVariables();
        final Map<String, String> variablesB = groupB.getVariables();

        if (variablesA != null) {
            variablesA.entrySet().stream()
                .forEach(entry -> {
                    final String valueA = entry.getValue();
                    final String valueB = variablesB.get(entry.getKey());

                    if (valueA == null && valueB != null) {
                        differences.add(difference(DifferenceType.VARIABLE_ADDED, groupA, groupB, entry.getKey(), entry.getKey()));
                    } else if (valueA != null && valueB == null) {
                        differences.add(difference(DifferenceType.VARIABLE_REMOVED, groupA, groupB, entry.getKey(), entry.getKey()));
                    }
                });
        }

        if (variablesB != null) {
            variablesB.entrySet().stream()
                .forEach(entry -> {
                    final String valueA = variablesA.get(entry.getKey());
                    final String valueB = entry.getValue();

                    // If there are any properties for component B that do not exist for Component A, add those as differences as well.
                    if (valueA == null && valueB != null) {
                        differences.add(difference(DifferenceType.VARIABLE_ADDED, groupA, groupB, entry.getKey(), entry.getKey()));
                    }
                });
        }
    }


    private void compare(final VersionedFunnel funnelA, final VersionedFunnel funnelB, final Set<FlowDifference> differences) {
        if (compareComponents(funnelA, funnelB, differences)) {
            return;
        }
    }

    private void compare(final VersionedLabel labelA, final VersionedLabel labelB, final Set<FlowDifference> differences) {
        if (compareComponents(labelA, labelB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.LABEL_VALUE_CHANGED, labelA, labelB, l -> l.getLabel());
        addIfDifferent(differences, DifferenceType.POSITION_CHANGED, labelA, labelB, l -> l.getHeight());
        addIfDifferent(differences, DifferenceType.POSITION_CHANGED, labelA, labelB, l -> l.getWidth());
        addIfDifferent(differences, DifferenceType.STYLE_CHANGED, labelA, labelB, l -> l.getStyle());
    }

    private void compare(final VersionedPort portA, final VersionedPort portB, final Set<FlowDifference> differences) {
        if (compareComponents(portA, portB, differences)) {
            return;
        }
    }

    private void compare(final VersionedRemoteProcessGroup rpgA, final VersionedRemoteProcessGroup rpgB, final Set<FlowDifference> differences) {
        if (compareComponents(rpgA, rpgB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.RPG_COMMS_TIMEOUT_CHANGED, rpgA, rpgB, r -> r.getCommunicationsTimeout());
        addIfDifferent(differences, DifferenceType.RPG_NETWORK_INTERFACE_CHANGED, rpgA, rpgB, r -> rpgA.getLocalNetworkInterface());
        addIfDifferent(differences, DifferenceType.RPG_PROXY_HOST_CHANGED, rpgA, rpgB, r -> rpgA.getProxyHost());
        addIfDifferent(differences, DifferenceType.RPG_PROXY_PORT_CHANGED, rpgA, rpgB, r -> rpgA.getProxyPort());
        addIfDifferent(differences, DifferenceType.RPG_PROXY_USER_CHANGED, rpgA, rpgB, r -> rpgA.getProxyUser());
        addIfDifferent(differences, DifferenceType.RPG_TRANSPORT_PROTOCOL_CHANGED, rpgA, rpgB, r -> rpgA.getTransportProtocol());
        addIfDifferent(differences, DifferenceType.YIELD_DURATION_CHANGED, rpgA, rpgB, r -> rpgA.getYieldDuration());

        differences.addAll(compareComponents(rpgA.getInputPorts(), rpgB.getInputPorts(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(rpgA.getOutputPorts(), rpgB.getOutputPorts(), (a, b, diffs) -> compare(a, b, diffs)));
    }

    private void compare(final VersionedRemoteGroupPort portA, final VersionedRemoteGroupPort portB, final Set<FlowDifference> differences) {
        if (compareComponents(portA, portB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.REMOTE_PORT_BATCH_SIZE_CHANGED, portA, portB, p -> p.getBatchSize());
        addIfDifferent(differences, DifferenceType.REMOTE_PORT_COMPRESSION_CHANGED, portA, portB, p -> p.isUseCompression());
        addIfDifferent(differences, DifferenceType.CONCURRENT_TASKS_CHANGED, portA, portB, p -> p.getConcurrentlySchedulableTaskCount());
    }


    private void compare(final VersionedProcessGroup groupA, final VersionedProcessGroup groupB, final Set<FlowDifference> differences, final boolean compareNamePos) {
        if (compareComponents(groupA, groupB, differences, compareNamePos)) {
            return;
        }

        if (groupA == null) {
            differences.add(difference(DifferenceType.COMPONENT_ADDED, groupA, groupB, groupA, groupB));
            return;
        }

        if (groupB == null) {
            differences.add(difference(DifferenceType.COMPONENT_REMOVED, groupA, groupB, groupA, groupB));
            return;
        }

        addIfDifferent(differences, DifferenceType.VERSIONED_FLOW_COORDINATES_CHANGED, groupA, groupB, g -> g.getVersionedFlowCoordinates());

        differences.addAll(compareComponents(groupA.getConnections(), groupB.getConnections(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getProcessors(), groupB.getProcessors(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getControllerServices(), groupB.getControllerServices(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getFunnels(), groupB.getFunnels(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getInputPorts(), groupB.getInputPorts(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getLabels(), groupB.getLabels(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getOutputPorts(), groupB.getOutputPorts(), (a, b, diffs) -> compare(a, b, diffs)));
        differences.addAll(compareComponents(groupA.getProcessGroups(), groupB.getProcessGroups(), (a, b, diffs) -> compare(a, b, diffs, true)));
        differences.addAll(compareComponents(groupA.getRemoteProcessGroups(), groupB.getRemoteProcessGroups(), (a, b, diffs) -> compare(a, b, diffs)));

        compareVariables(groupA, groupB, differences);
    }


    private void compare(final VersionedConnection connectionA, final VersionedConnection connectionB, final Set<FlowDifference> differences) {
        if (compareComponents(connectionA, connectionB, differences)) {
            return;
        }

        addIfDifferent(differences, DifferenceType.BACKPRESSURE_DATA_SIZE_THRESHOLD_CHANGED, connectionA, connectionB, c -> c.getBackPressureDataSizeThreshold());
        addIfDifferent(differences, DifferenceType.BACKPRESSURE_OBJECT_THRESHOLD_CHANGED, connectionA, connectionB, c -> c.getBackPressureObjectThreshold());
        addIfDifferent(differences, DifferenceType.BENDPOINTS_CHANGED, connectionA, connectionB, c -> c.getBends());
        addIfDifferent(differences, DifferenceType.DESTINATION_CHANGED, connectionA, connectionB, c -> c.getDestination());
        addIfDifferent(differences, DifferenceType.FLOWFILE_EXPIRATION_CHANGED, connectionA, connectionB, c -> c.getFlowFileExpiration());
        addIfDifferent(differences, DifferenceType.PRIORITIZERS_CHANGED, connectionA, connectionB, c -> c.getPrioritizers());
        addIfDifferent(differences, DifferenceType.SELECTED_RELATIONSHIPS_CHANGED, connectionA, connectionB, c -> c.getSelectedRelationships());
        addIfDifferent(differences, DifferenceType.SOURCE_CHANGED, connectionA, connectionB, c -> c.getSource().getId());
    }


    private <T extends VersionedComponent> Map<String, T> byId(final Set<T> components) {
        return components.stream().collect(Collectors.toMap(VersionedComponent::getIdentifier, Function.identity()));
    }


    private <T extends VersionedComponent> void addIfDifferent(final Set<FlowDifference> differences, final DifferenceType type, final T componentA, final T componentB,
        final Function<T, Object> transform) {

        final Object valueA = transform.apply(componentA);
        final Object valueB = transform.apply(componentB);

        if (Objects.equals(valueA, valueB)) {
            return;
        }

        differences.add(difference(type, componentA, componentB, valueA, valueB));
    }

    private FlowDifference difference(final DifferenceType type, final VersionedComponent componentA, final VersionedComponent componentB, final Object valueA, final Object valueB) {
        final String description;

        switch (type) {
            case COMPONENT_ADDED:
                description = String.format("%s with ID %s exists in %s but not in %s",
                    componentB.getComponentType().getTypeName(), componentB.getIdentifier(), flowB.getName(), flowA.getName());
                break;
            case COMPONENT_REMOVED:
                description = String.format("%s with ID %s exists in %s but not in %s",
                    componentA.getComponentType().getTypeName(), componentA.getIdentifier(), flowA.getName(), flowB.getName());
                break;
            case PROPERTY_ADDED:
                description = String.format("Property '%s' exists for %s with ID %s in %s but not in %s",
                    valueB, componentB.getComponentType().getTypeName(), componentB.getIdentifier(), flowB.getName(), flowA.getName());
                break;
            case PROPERTY_REMOVED:
                description = String.format("Property '%s' exists for %s with ID %s in %s but not in %s",
                    valueA, componentA.getComponentType().getTypeName(), componentA.getIdentifier(), flowA.getName(), flowB.getName());
                break;
            default:
                description = String.format("%s for %s with ID %s; flow '%s' has value %s; flow '%s' has value %s",
                    type.getDescription(), componentA.getComponentType().getTypeName(), componentA.getIdentifier(),
                    flowA.getName(), valueA, flowB.getName(), valueB);
                break;
        }

        return new StandardFlowDifference(type, componentA, componentB, valueA, valueB, description);
    }


    private static interface ComponentComparator<T extends VersionedComponent> {
        void compare(T componentA, T componentB, Set<FlowDifference> differences);
    }
}
