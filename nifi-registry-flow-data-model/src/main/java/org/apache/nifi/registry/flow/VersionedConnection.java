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

import java.util.List;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModelProperty;

public class VersionedConnection extends VersionedComponent {
    private ConnectableComponent source;
    private ConnectableComponent destination;
    private Integer labelIndex;
    private Long zIndex;
    private Set<String> selectedRelationships;

    private Long backPressureObjectThreshold;
    private String backPressureDataSizeThreshold;
    private String flowFileExpiration;
    private List<String> prioritizers;
    private List<Position> bends;

    @ApiModelProperty("The source of the connection.")
    public ConnectableComponent getSource() {
        return source;
    }

    public void setSource(ConnectableComponent source) {
        this.source = source;
    }

    @ApiModelProperty("The destination of the connection.")
    public ConnectableComponent getDestination() {
        return destination;
    }

    public void setDestination(ConnectableComponent destination) {
        this.destination = destination;
    }

    @ApiModelProperty("The bend points on the connection.")
    public List<Position> getBends() {
        return bends;
    }

    public void setBends(List<Position> bends) {
        this.bends = bends;
    }

    @ApiModelProperty("The index of the bend point where to place the connection label.")
    public Integer getLabelIndex() {
        return labelIndex;
    }

    public void setLabelIndex(Integer labelIndex) {
        this.labelIndex = labelIndex;
    }

    @ApiModelProperty("The z index of the connection.")
    public Long getzIndex() {
        return zIndex;
    }

    public void setzIndex(Long zIndex) {
        this.zIndex = zIndex;
    }

    @ApiModelProperty("The selected relationship that comprise the connection.")
    public Set<String> getSelectedRelationships() {
        return selectedRelationships;
    }

    public void setSelectedRelationships(Set<String> relationships) {
        this.selectedRelationships = relationships;
    }


    @ApiModelProperty("The object count threshold for determining when back pressure is applied. Updating this value is a passive change in the sense that it won't impact whether existing files "
        + "over the limit are affected but it does help feeder processors to stop pushing too much into this work queue.")
    public Long getBackPressureObjectThreshold() {
        return backPressureObjectThreshold;
    }

    public void setBackPressureObjectThreshold(Long backPressureObjectThreshold) {
        this.backPressureObjectThreshold = backPressureObjectThreshold;
    }


    @ApiModelProperty("The object data size threshold for determining when back pressure is applied. Updating this value is a passive change in the sense that it won't impact whether existing "
        + "files over the limit are affected but it does help feeder processors to stop pushing too much into this work queue.")
    public String getBackPressureDataSizeThreshold() {
        return backPressureDataSizeThreshold;
    }

    public void setBackPressureDataSizeThreshold(String backPressureDataSizeThreshold) {
        this.backPressureDataSizeThreshold = backPressureDataSizeThreshold;
    }


    @ApiModelProperty("The amount of time a flow file may be in the flow before it will be automatically aged out of the flow. Once a flow file reaches this age it will be terminated from "
        + "the flow the next time a processor attempts to start work on it.")
    public String getFlowFileExpiration() {
        return flowFileExpiration;
    }

    public void setFlowFileExpiration(String flowFileExpiration) {
        this.flowFileExpiration = flowFileExpiration;
    }


    @ApiModelProperty("The comparators used to prioritize the queue.")
    public List<String> getPrioritizers() {
        return prioritizers;
    }

    public void setPrioritizers(List<String> prioritizers) {
        this.prioritizers = prioritizers;
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONNECTION;
    }
}
