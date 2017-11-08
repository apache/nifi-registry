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

package org.apache.nifi.registry.flow;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VersionedProcessGroup extends VersionedComponent {

    // It is important that all of these components be held in LinkedHashSets because if a VersionedProcessGroup
    // is sent to a cluster and all nodes must add more than 1 component, we have to ensure that we add them in the
    // same order so that their generated ID's are deterministic.
    private LinkedHashSet<VersionedProcessGroup> processGroups = new LinkedHashSet<>();
    private LinkedHashSet<VersionedRemoteProcessGroup> remoteProcessGroups = new LinkedHashSet<>();
    private LinkedHashSet<VersionedProcessor> processors = new LinkedHashSet<>();
    private LinkedHashSet<VersionedPort> inputPorts = new LinkedHashSet<>();
    private LinkedHashSet<VersionedPort> outputPorts = new LinkedHashSet<>();
    private LinkedHashSet<VersionedConnection> connections = new LinkedHashSet<>();
    private LinkedHashSet<VersionedLabel> labels = new LinkedHashSet<>();
    private LinkedHashSet<VersionedFunnel> funnels = new LinkedHashSet<>();
    private LinkedHashSet<VersionedControllerService> controllerServices = new LinkedHashSet<>();
    private VersionedFlowCoordinates versionedFlowCoordinates = null;

    private Map<String, String> variables = new HashMap<>();

    @ApiModelProperty("The child Process Groups")
    public Set<VersionedProcessGroup> getProcessGroups() {
        return processGroups;
    }

    public void setProcessGroups(Set<VersionedProcessGroup> processGroups) {
        this.processGroups = new LinkedHashSet<>(processGroups);
    }

    @ApiModelProperty("The Remote Process Groups")
    public Set<VersionedRemoteProcessGroup> getRemoteProcessGroups() {
        return remoteProcessGroups;
    }

    public void setRemoteProcessGroups(Set<VersionedRemoteProcessGroup> remoteProcessGroups) {
        this.remoteProcessGroups = new LinkedHashSet<>(remoteProcessGroups);
    }

    @ApiModelProperty("The Processors")
    public Set<VersionedProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Set<VersionedProcessor> processors) {
        this.processors = new LinkedHashSet<>(processors);
    }

    @ApiModelProperty("The Input Ports")
    public Set<VersionedPort> getInputPorts() {
        return inputPorts;
    }

    public void setInputPorts(Set<VersionedPort> inputPorts) {
        this.inputPorts = new LinkedHashSet<>(inputPorts);
    }

    @ApiModelProperty("The Output Ports")
    public Set<VersionedPort> getOutputPorts() {
        return outputPorts;
    }

    public void setOutputPorts(Set<VersionedPort> outputPorts) {
        this.outputPorts = new LinkedHashSet<>(outputPorts);
    }

    @ApiModelProperty("The Connections")
    public Set<VersionedConnection> getConnections() {
        return connections;
    }

    public void setConnections(Set<VersionedConnection> connections) {
        this.connections = new LinkedHashSet<>(connections);
    }

    @ApiModelProperty("The Labels")
    public Set<VersionedLabel> getLabels() {
        return labels;
    }

    public void setLabels(Set<VersionedLabel> labels) {
        this.labels = new LinkedHashSet<>(labels);
    }

    @ApiModelProperty("The Funnels")
    public Set<VersionedFunnel> getFunnels() {
        return funnels;
    }

    public void setFunnels(Set<VersionedFunnel> funnels) {
        this.funnels = new LinkedHashSet<>(funnels);
    }

    @ApiModelProperty("The Controller Services")
    public Set<VersionedControllerService> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(Set<VersionedControllerService> controllerServices) {
        this.controllerServices = new LinkedHashSet<>(controllerServices);
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PROCESS_GROUP;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    @ApiModelProperty("The Variables in the Variable Registry for this Process Group (not including any ancestor or descendant Process Groups)")
    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVersionedFlowCoordinates(VersionedFlowCoordinates flowCoordinates) {
        this.versionedFlowCoordinates = flowCoordinates;
    }

    @ApiModelProperty("The coordinates where the remote flow is stored, or null if the Process Group is not directly under Version Control")
    public VersionedFlowCoordinates getVersionedFlowCoordinates() {
        return versionedFlowCoordinates;
    }
}
