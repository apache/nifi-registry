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
package org.apache.nifi.registry.serialization;

import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.serialization.jackson.JacksonVersionedProcessGroupSerializer;
import org.apache.nifi.registry.serialization.jaxb.JAXBVersionedProcessGroupSerializer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * See {@link AbstractMultiVersionSerializer} for further information.
 *
 * <p>
 * Current data model version is 2.
 * Data Model Version Histories:
 * <ul>
 *     <li>version 2: Serialized by {@link JacksonVersionedProcessGroupSerializer}</li>
 *     <li>version 1: Serialized by {@link JAXBVersionedProcessGroupSerializer}</li>
 * </ul>
 * </p>
 */
@Service
public class VersionedProcessGroupSerializer extends AbstractMultiVersionSerializer<VersionedProcessGroup> {

    static final Integer CURRENT_DATA_MODEL_VERSION = 2;

    @Override
    protected Map<Integer, VersionedSerializer<VersionedProcessGroup>> createVersionedSerializers() {
        final Map<Integer, VersionedSerializer<VersionedProcessGroup>> tempSerializers = new HashMap<>();
        tempSerializers.put(2, new JacksonVersionedProcessGroupSerializer());
        tempSerializers.put(1, new JAXBVersionedProcessGroupSerializer());
        return tempSerializers;
    }

    @Override
    protected int getCurrentDataModelVersion() {
        return CURRENT_DATA_MODEL_VERSION;
    }

}
