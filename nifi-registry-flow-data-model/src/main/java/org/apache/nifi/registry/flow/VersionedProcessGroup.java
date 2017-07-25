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

import java.util.LinkedHashSet;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModelProperty;


public class VersionedProcessGroup extends VersionedComponent {

    private Set<VersionedProcessGroup> processGroups = new LinkedHashSet<>();
    private Set<VersionedRemoteProcessGroup> remoteProcessGroups = new LinkedHashSet<>();
    private Set<VersionedProcessor> processors = new LinkedHashSet<>();
    private Set<VersionedPort> inputPorts = new LinkedHashSet<>();
    private Set<VersionedPort> outputPorts = new LinkedHashSet<>();
    private Set<VersionedConnection> connections = new LinkedHashSet<>();
    private Set<VersionedLabel> labels = new LinkedHashSet<>();
    private Set<VersionedFunnel> funnels = new LinkedHashSet<>();
    private Set<VersionedControllerService> controllerServices = new LinkedHashSet<>();

    @ApiModelProperty("The child Process Groups")
    public Set<VersionedProcessGroup> getProcessGroups() {
        return processGroups;
    }

    public void setProcessGroups(Set<VersionedProcessGroup> processGroups) {
        this.processGroups = processGroups;
    }

    @ApiModelProperty("The Remote Process Groups")
    public Set<VersionedRemoteProcessGroup> getRemoteProcessGroups() {
        return remoteProcessGroups;
    }

    public void setRemoteProcessGroups(Set<VersionedRemoteProcessGroup> remoteProcessGroups) {
        this.remoteProcessGroups = remoteProcessGroups;
    }

    @ApiModelProperty("The Processors")
    public Set<VersionedProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Set<VersionedProcessor> processors) {
        this.processors = processors;
    }

    @ApiModelProperty("The Input Ports")
    public Set<VersionedPort> getInputPorts() {
        return inputPorts;
    }

    public void setInputPorts(Set<VersionedPort> inputPorts) {
        this.inputPorts = inputPorts;
    }

    @ApiModelProperty("The Output Ports")
    public Set<VersionedPort> getOutputPorts() {
        return outputPorts;
    }

    public void setOutputPorts(Set<VersionedPort> outputPorts) {
        this.outputPorts = outputPorts;
    }

    @ApiModelProperty("The Connections")
    public Set<VersionedConnection> getConnections() {
        return connections;
    }

    public void setConnections(Set<VersionedConnection> connections) {
        this.connections = connections;
    }

    @ApiModelProperty("The Labels")
    public Set<VersionedLabel> getLabels() {
        return labels;
    }

    public void setLabels(Set<VersionedLabel> labels) {
        this.labels = labels;
    }

    @ApiModelProperty("The Funnels")
    public Set<VersionedFunnel> getFunnels() {
        return funnels;
    }

    public void setFunnels(Set<VersionedFunnel> funnels) {
        this.funnels = funnels;
    }

    @ApiModelProperty("The Controller Services")
    public Set<VersionedControllerService> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(Set<VersionedControllerService> controllerServices) {
        this.controllerServices = controllerServices;
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PROCESS_GROUP;
    }
}
