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

        final VersionedProcessGroup processGroup = new VersionedProcessGroup();
        processGroup.setIdentifier("pg1");
        processGroup.setName("My Process Group");

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(processGroup);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(snapshot, out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final VersionedFlowSnapshot deserializedSnapshot = serializer.deserialize(in);
        final VersionedFlowSnapshotMetadata deserializedMetadata = deserializedSnapshot.getSnapshotMetadata();
        final VersionedProcessGroup deserializedProcessGroup = deserializedSnapshot.getFlowContents();

        Assert.assertEquals(snapshotMetadata.getFlowIdentifier(), deserializedMetadata.getFlowIdentifier());
        Assert.assertEquals(snapshotMetadata.getFlowName(), deserializedMetadata.getFlowName());
        Assert.assertEquals(snapshotMetadata.getVersion(), deserializedMetadata.getVersion());
        Assert.assertEquals(snapshotMetadata.getComments(), deserializedMetadata.getComments());
        Assert.assertEquals(snapshotMetadata.getTimestamp(), deserializedMetadata.getTimestamp());

        Assert.assertEquals(processGroup.getIdentifier(), deserializedProcessGroup.getIdentifier());
        Assert.assertEquals(processGroup.getName(), deserializedProcessGroup.getName());
    }
}
