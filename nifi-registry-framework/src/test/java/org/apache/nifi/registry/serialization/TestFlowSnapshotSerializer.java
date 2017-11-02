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

import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestFlowSnapshotSerializer {

    @Test
    public void testSerializeDeserializeFlowSnapshot() throws SerializationException {
        final Serializer<VersionedFlowSnapshot> serializer = new FlowSnapshotSerializer();

        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setFlowIdentifier("flow1");
        snapshotMetadata.setFlowName("First Flow");
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setComments("This is the first flow");
        snapshotMetadata.setTimestamp(System.currentTimeMillis());

        final VersionedProcessGroup processGroup1 = new VersionedProcessGroup();
        processGroup1.setIdentifier("pg1");
        processGroup1.setName("My Process Group");

        final VersionedProcessor processor1 = new VersionedProcessor();
        processor1.setIdentifier("processor1");
        processor1.setName("My Processor 1");

        // make sure nested objects are serialized/deserialized
        processGroup1.getProcessors().add(processor1);

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(processGroup1);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(snapshot, out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final VersionedFlowSnapshot deserializedSnapshot = serializer.deserialize(in);
        final VersionedFlowSnapshotMetadata deserializedMetadata = deserializedSnapshot.getSnapshotMetadata();
        final VersionedProcessGroup deserializedProcessGroup1 = deserializedSnapshot.getFlowContents();

        Assert.assertEquals(snapshotMetadata.getFlowIdentifier(), deserializedMetadata.getFlowIdentifier());
        Assert.assertEquals(snapshotMetadata.getFlowName(), deserializedMetadata.getFlowName());
        Assert.assertEquals(snapshotMetadata.getVersion(), deserializedMetadata.getVersion());
        Assert.assertEquals(snapshotMetadata.getComments(), deserializedMetadata.getComments());
        Assert.assertEquals(snapshotMetadata.getTimestamp(), deserializedMetadata.getTimestamp());

        Assert.assertEquals(processGroup1.getIdentifier(), deserializedProcessGroup1.getIdentifier());
        Assert.assertEquals(processGroup1.getName(), deserializedProcessGroup1.getName());

        Assert.assertEquals(1, deserializedProcessGroup1.getProcessors().size());

        final VersionedProcessor deserializedProcessor1 = deserializedProcessGroup1.getProcessors().iterator().next();
        Assert.assertEquals(processor1.getIdentifier(), deserializedProcessor1.getIdentifier());
        Assert.assertEquals(processor1.getName(), deserializedProcessor1.getName());

    }
}
