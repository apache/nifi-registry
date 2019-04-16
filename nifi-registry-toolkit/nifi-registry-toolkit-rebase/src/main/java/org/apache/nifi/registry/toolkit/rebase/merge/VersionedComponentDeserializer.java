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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.ComponentType;
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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionedComponentDeserializer extends StdDeserializer<VersionedComponent> {
    private static final Map<ComponentType, Class<? extends VersionedComponent>> componentTypeMap = Collections.unmodifiableMap(Stream.of(
            Pair.of(ComponentType.CONNECTION, VersionedConnection.class),
            Pair.of(ComponentType.PROCESSOR, VersionedProcessor.class),
            Pair.of(ComponentType.PROCESS_GROUP, VersionedProcessGroup.class),
            Pair.of(ComponentType.REMOTE_PROCESS_GROUP, VersionedRemoteProcessGroup.class),
            Pair.of(ComponentType.INPUT_PORT, VersionedPort.class),
            Pair.of(ComponentType.OUTPUT_PORT, VersionedPort.class),
            Pair.of(ComponentType.REMOTE_INPUT_PORT, VersionedRemoteGroupPort.class),
            Pair.of(ComponentType.REMOTE_OUTPUT_PORT, VersionedRemoteGroupPort.class),
            Pair.of(ComponentType.FUNNEL, VersionedFunnel.class),
            Pair.of(ComponentType.LABEL, VersionedLabel.class),
            Pair.of(ComponentType.CONTROLLER_SERVICE, VersionedControllerService.class)
    ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

    public VersionedComponentDeserializer() {
        super(VersionedComponent.class);
    }

    @Override
    public VersionedComponent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        TreeNode treeNode = p.readValueAsTree();
        return mapper.readValue(mapper.treeAsTokens(treeNode), componentTypeMap.get(ComponentType.valueOf(((TextNode) treeNode.get("componentType")).textValue())));
    }
}
