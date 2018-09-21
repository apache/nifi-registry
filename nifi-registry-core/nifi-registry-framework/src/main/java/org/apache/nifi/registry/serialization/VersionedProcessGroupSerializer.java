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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A serializer for VersionedProcessGroup that maps a "version" of the data model to a serializer.
 * </p>
 *
 * <p>
 * When serializing, the serializer associated with the {@link #CURRENT_DATA_MODEL_VERSION} is used.
 * The version will be written as a header at the beginning of the OutputStream then followed by the content.
 * </p>
 *
 * <p>
 * When deserializing, each registered serializer will be asked to read a data model version number from the input stream
 * in descending version order until a version number is read successfully.
 * Then the associated serializer to the read data model version is used to deserialize content back to the target object.
 * If no serializer can read the version, or no serializer is registered for the read version, then SerializationException is thrown.
 * </p>
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
public class VersionedProcessGroupSerializer implements Serializer<VersionedProcessGroup> {

    private static final Logger logger = LoggerFactory.getLogger(VersionedProcessGroupSerializer.class);

    static final Integer CURRENT_DATA_MODEL_VERSION = 2;

    private final Map<Integer, VersionedSerializer<VersionedProcessGroup>> serializersByVersion;
    private final VersionedSerializer<VersionedProcessGroup> defaultSerializer;
    private final List<Integer> descendingVersions;
    public static final int MAX_HEADER_BYTES = 1024;

    public VersionedProcessGroupSerializer() {

        final Map<Integer, VersionedSerializer<VersionedProcessGroup>> tempSerializers = new HashMap<>();
        tempSerializers.put(2, new JacksonVersionedProcessGroupSerializer());
        tempSerializers.put(1, new JAXBVersionedProcessGroupSerializer());

        this.serializersByVersion = Collections.unmodifiableMap(tempSerializers);
        this.defaultSerializer = tempSerializers.get(CURRENT_DATA_MODEL_VERSION);

        final List<Integer> sortedVersions = new ArrayList<>(serializersByVersion.keySet());
        sortedVersions.sort(Collections.reverseOrder(Integer::compareTo));
        this.descendingVersions = sortedVersions;
    }

    @Override
    public void serialize(final VersionedProcessGroup versionedProcessGroup, final OutputStream out) throws SerializationException {

        defaultSerializer.serialize(CURRENT_DATA_MODEL_VERSION, versionedProcessGroup, out);
    }

    @Override
    public VersionedProcessGroup deserialize(final InputStream input) throws SerializationException {

        final InputStream markSupportedInput = input.markSupported() ? input : new BufferedInputStream(input);

        // Mark the beginning of the stream.
        markSupportedInput.mark(MAX_HEADER_BYTES);

        // Applying each serializer
        for (int serializerVersion : descendingVersions) {
            final VersionedSerializer<VersionedProcessGroup> serializer = serializersByVersion.get(serializerVersion);

            // Serializer version will not be the data model version always.
            // E.g. higher version of serializer can read the old data model version number if it has the same header structure,
            // but it does not mean the serializer is compatible with the old format.
            final int version;
            try {
                version = serializer.readDataModelVersion(markSupportedInput);
                if (!serializersByVersion.containsKey(version)) {
                    throw new SerializationException(String.format(
                            "Version %d was returned by %s, but no serializer is registered for that version.", version, serializer));
                }
            } catch (SerializationException e) {
                logger.debug("Deserialization failed with {}", serializer, e);
                continue;
            } finally {
                // Either when continue with the next serializer, or proceed deserialization with the corresponding serializer,
                // reset the stream position.
                try {
                    markSupportedInput.reset();
                } catch (IOException resetException) {
                    // Should not happen.
                    logger.error("Unable to reset the input stream.", resetException);
                }
            }

            return serializersByVersion.get(version).deserialize(markSupportedInput);
        }

        throw new SerializationException("Unable to find a process group serializer compatible with the input.");

    }

}
