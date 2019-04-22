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

import org.apache.nifi.registry.flow.ExternalControllerServiceReference;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestVersionedProcessGroupSerializer {

    @Test
    public void testSerializeDeserializeFlowSnapshot() throws SerializationException {
        final Serializer<VersionedProcessGroup> serializer = new VersionedProcessGroupSerializer();

        final VersionedProcessGroup processGroup1 = new VersionedProcessGroup();
        processGroup1.setIdentifier("pg1");
        processGroup1.setName("My Process Group");

        final VersionedProcessor processor1 = new VersionedProcessor();
        processor1.setIdentifier("processor1");
        processor1.setName("My Processor 1");

        // make sure nested objects are serialized/deserialized
        processGroup1.getProcessors().add(processor1);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(processGroup1, out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final VersionedProcessGroup deserializedProcessGroup1 = serializer.deserialize(in);

        Assert.assertEquals(processGroup1.getIdentifier(), deserializedProcessGroup1.getIdentifier());
        Assert.assertEquals(processGroup1.getName(), deserializedProcessGroup1.getName());

        Assert.assertEquals(1, deserializedProcessGroup1.getProcessors().size());

        final VersionedProcessor deserializedProcessor1 = deserializedProcessGroup1.getProcessors().iterator().next();
        Assert.assertEquals(processor1.getIdentifier(), deserializedProcessor1.getIdentifier());
        Assert.assertEquals(processor1.getName(), deserializedProcessor1.getName());

    }

    @Test
    public void testSerializeDeserializeWithExternalServices() throws SerializationException {
        final Serializer<VersionedProcessGroup> serializer = new VersionedProcessGroupSerializer();

        final VersionedProcessGroup processGroup1 = new VersionedProcessGroup();
        processGroup1.setIdentifier("pg1");
        processGroup1.setName("My Process Group");

        final ExternalControllerServiceReference serviceReference1 = new ExternalControllerServiceReference();
        serviceReference1.setIdentifier("1");
        serviceReference1.setName("Service 1");

        final ExternalControllerServiceReference serviceReference2 = new ExternalControllerServiceReference();
        serviceReference2.setIdentifier("2");
        serviceReference2.setName("Service 2");

        final Map<String,ExternalControllerServiceReference> serviceReferences = new HashMap<>();
        serviceReferences.put(serviceReference1.getIdentifier(), serviceReference1);
        serviceReferences.put(serviceReference2.getIdentifier(), serviceReference2);

        processGroup1.setExternalControllerServices(serviceReferences);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(processGroup1, out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final VersionedProcessGroup deserializedProcessGroup = serializer.deserialize(in);

        Assert.assertEquals(processGroup1.getIdentifier(), deserializedProcessGroup.getIdentifier());
        Assert.assertEquals(processGroup1.getName(), deserializedProcessGroup.getName());

        final Map<String,ExternalControllerServiceReference> deserializedServiceReferences = deserializedProcessGroup.getExternalControllerServices();
        Assert.assertNotNull(deserializedServiceReferences);
        Assert.assertEquals(2, deserializedServiceReferences.size());

        final ExternalControllerServiceReference deserializedServiceReference1 = deserializedServiceReferences.get(serviceReference1.getIdentifier());
        Assert.assertNotNull(deserializedServiceReference1);
        Assert.assertEquals(serviceReference1.getIdentifier(), deserializedServiceReference1.getIdentifier());
        Assert.assertEquals(serviceReference1.getName(), deserializedServiceReference1.getName());
    }

    @Test
    public void testDeserializeJsonNonIntegerVersion() throws IOException {
        final String file = "/serialization/json/non-integer-version.snapshot";
        final VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();
        try (final InputStream is = this.getClass().getResourceAsStream(file)) {
            try {
                serializer.deserialize(is);
                fail("Should fail");
            } catch (SerializationException e) {
                assertEquals("Unable to find a serializer compatible with the input.", e.getMessage());
            }
        }
    }

    @Test
    public void testDeserializeJsonNoVersion() throws IOException {
        final String file = "/serialization/json/no-version.snapshot";
        final VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();
        try (final InputStream is = this.getClass().getResourceAsStream(file)) {
            try {
                serializer.deserialize(is);
                fail("Should fail");
            } catch (SerializationException e) {
                assertEquals("Unable to find a serializer compatible with the input.", e.getMessage());
            }
        }
    }

    @Test
    public void testDeserializeVer1() throws IOException {
        final String file = "/serialization/ver1.snapshot";
        final VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();
        final VersionedProcessGroup processGroup;
        try (final InputStream is = this.getClass().getResourceAsStream(file)) {
            processGroup = serializer.deserialize(is);
        }
        System.out.printf("processGroup=" + processGroup);
    }

    @Test
    public void testDeserializeVer2() throws IOException {
        final String file = "/serialization/ver2.snapshot";
        final VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();
        final VersionedProcessGroup processGroup;
        try (final InputStream is = this.getClass().getResourceAsStream(file)) {
            processGroup = serializer.deserialize(is);
        }
        System.out.printf("processGroup=" + processGroup);
    }

    @Test
    public void testDeserializeVer3() throws IOException {
        final String file = "/serialization/ver3.snapshot";
        final VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();
        try (final InputStream is = this.getClass().getResourceAsStream(file)) {
            try {
                serializer.deserialize(is);
                fail("Should fail");
            } catch (SerializationException e) {
                assertEquals("Unable to find a serializer compatible with the input.", e.getMessage());
            }
        }
    }

}
