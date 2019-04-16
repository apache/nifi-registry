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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComponentAddOperation.class, name = "add"),
        @JsonSubTypes.Type(value = ComponentRemoveOperation.class, name = "remove"),
        @JsonSubTypes.Type(value = SingleDifferenceTypeOperation.class, name = "setValue"),
        @JsonSubTypes.Type(value = BranchCoordinatesOperation.class, name = "branchedChildPg"),
        @JsonSubTypes.Type(value = VersionFlowCoordinatesOperation.class, name = "externalChildPg"),
        @JsonSubTypes.Type(value = PropertyOperation.class, name = "propertySet"),
        @JsonSubTypes.Type(value = VariableAddedOperation.class, name = "variableAdded"),
        @JsonSubTypes.Type(value = VariableRemovedOperation.class, name = "variableRemoved")
})
public interface MergeOperation {
    void apply(RebaseApplicationContext rebaseApplicationContext, VersionedProcessGroup processGroup, String groupId, ComponentType componentType, String componentId);
}
