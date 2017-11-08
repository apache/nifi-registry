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
import org.apache.nifi.registry.serialization.jaxb.JAXBVersionedProcessGroupSerializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A serializer for VersionedFlowSnapshots that maps a "version" of the data model to a serializer. The version
 * will be written to a header at the beginning of the OutputStream, and then the object and the OutputStream will
 * be passed on to the real serializer for the given version. Similarly, when deserializing, the header will first be
 * read from the InputStream to determine the version, and then the InputStream will be passed to the deserializer
 * for the given version.
 */
@Service
public class VersionedProcessGroupSerializer implements Serializer<VersionedProcessGroup> {

    static final String MAGIC_HEADER = "Flows";
    static final byte[] MAGIC_HEADER_BYTES = MAGIC_HEADER.getBytes(StandardCharsets.UTF_8);

    static final Integer CURRENT_VERSION = 1;

    private final Map<Integer, Serializer<VersionedProcessGroup>> serializersByVersion;

    public VersionedProcessGroupSerializer() {
        final Map<Integer, Serializer<VersionedProcessGroup>> tempSerializers = new HashMap<>();
        tempSerializers.put(CURRENT_VERSION, new JAXBVersionedProcessGroupSerializer());
        this.serializersByVersion = Collections.unmodifiableMap(tempSerializers);
    }

    @Override
    public void serialize(final VersionedProcessGroup versionedProcessGroup, final OutputStream out) throws SerializationException {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put(MAGIC_HEADER_BYTES);
        byteBuffer.putInt(CURRENT_VERSION);

        try {
            out.write(byteBuffer.array());
        } catch (final IOException e) {
            throw new SerializationException("Unable to write header while serializing process group", e);
        }

        final Serializer<VersionedProcessGroup> serializer = serializersByVersion.get(CURRENT_VERSION);
        if (serializer == null) {
            throw new SerializationException("No process group serializer for version " + CURRENT_VERSION);
        }

        serializer.serialize(versionedProcessGroup, out);
    }

    @Override
    public VersionedProcessGroup deserialize(final InputStream input) throws SerializationException {
        final int headerLength = 9;
        final byte[] buffer = new byte[headerLength];

        int bytesRead = -1;
        try {
            bytesRead = input.read(buffer, 0, headerLength);
        } catch (final IOException e) {
            throw new SerializationException("Unable to read header while deserializing process group", e);
        }

        if (bytesRead < headerLength) {
            throw new SerializationException("Unable to read header while deserializing process group, expected"
                    + headerLength + " bytes, but found " + bytesRead);
        }

        final ByteBuffer bb = ByteBuffer.wrap(buffer);
        final int version = bb.getInt(MAGIC_HEADER_BYTES.length);

        final Serializer<VersionedProcessGroup> serializer = serializersByVersion.get(Integer.valueOf(version));
        if (serializer == null) {
            throw new SerializationException("No process group serializer for version " + version);
        }

        return serializer.deserialize(input);
    }

}
