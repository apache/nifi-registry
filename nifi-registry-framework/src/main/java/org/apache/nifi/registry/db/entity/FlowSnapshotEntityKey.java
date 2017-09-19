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
package org.apache.nifi.registry.db.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Key for FlowSnapshotEntity made up of the flow id and the snapshot version.
 */
@Embeddable
public class FlowSnapshotEntityKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "FLOW_ID")
    private String flowId;

    @Column(name = "VERSION")
    private Integer version;

    public FlowSnapshotEntityKey() {

    }

    public FlowSnapshotEntityKey(String flowId, Integer version) {
        this.flowId = flowId;
        this.version = version;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowId, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof FlowSnapshotEntityKey)) {
            return false;
        }

        final FlowSnapshotEntityKey other = (FlowSnapshotEntityKey) obj;
        return Objects.equals(this.flowId, other.flowId) && Objects.equals(this.version, other.version);
    }

}
