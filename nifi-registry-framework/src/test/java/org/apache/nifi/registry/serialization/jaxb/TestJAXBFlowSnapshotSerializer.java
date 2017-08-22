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
package org.apache.nifi.registry.serialization.jaxb;

import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.serialization.SerializationException;
import org.apache.nifi.registry.serialization.Serializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class TestJAXBFlowSnapshotSerializer {

    @Test
    public void testSerializeDeserializeFlowSnapshot() throws SerializationException {
        final Serializer<VersionedFlowSnapshot> serializer = new JAXBFlowSnapshotSerializer();

        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setFlowIdentifier("flow1");
        snapshotMetadata.setFlowName("First Flow");
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setComments("This is the first flow");
        snapshotMetadata.setTimestamp(System.currentTimeMillis());

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(snapshot, out);

        final String snapshotStr = new String(out.toByteArray(), StandardCharsets.UTF_8);
        //System.out.println(snapshotStr);

        final ByteArrayInputStream in = new ByteArrayInputStream(snapshotStr.getBytes(StandardCharsets.UTF_8));
        final VersionedFlowSnapshot deserializedSnapshot = serializer.deserialize(in);
        final VersionedFlowSnapshotMetadata deserializedMetadata = deserializedSnapshot.getSnapshotMetadata();

        Assert.assertEquals(snapshotMetadata.getFlowIdentifier(), deserializedMetadata.getFlowIdentifier());
        Assert.assertEquals(snapshotMetadata.getFlowName(), deserializedMetadata.getFlowName());
        Assert.assertEquals(snapshotMetadata.getVersion(), deserializedMetadata.getVersion());
        Assert.assertEquals(snapshotMetadata.getComments(), deserializedMetadata.getComments());
        Assert.assertEquals(snapshotMetadata.getTimestamp(), deserializedMetadata.getTimestamp());
    }

}
