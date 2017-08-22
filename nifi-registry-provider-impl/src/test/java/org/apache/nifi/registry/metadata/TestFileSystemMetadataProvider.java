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
package org.apache.nifi.registry.metadata;

import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestFileSystemMetadataProvider {

    static final String METADATA_EMPTY = "metadata-empty.xml";
    static final String METADATA_EMPTY_CONCISE = "metadata-empty-concise.xml";
    static final String METADATA_EXISTING = "metadata-existing.xml";
    static final String METADATA_NEW_FILE = "metadata-test.xml";

    static final File METADATA_SRC_DIR = new File("src/test/resources/metadata");
    static final File METADATA_DEST_DIR = new File("target/metadata");

    static final File METADATA_SRC_EMPTY = new File(METADATA_SRC_DIR, METADATA_EMPTY);
    static final File METADATA_SRC_EMPTY_CONCISE = new File(METADATA_SRC_DIR, METADATA_EMPTY_CONCISE);
    static final File METADATA_SRC_EXISTING = new File(METADATA_SRC_DIR, METADATA_EXISTING);

    static final File METADATA_DEST_EMPTY = new File(METADATA_DEST_DIR, METADATA_EMPTY);
    static final File METADATA_DEST_EMPTY_CONCISE = new File(METADATA_DEST_DIR, METADATA_EMPTY_CONCISE);
    static final File METADATA_DEST_EXISTING = new File(METADATA_DEST_DIR, METADATA_EXISTING);
    static final File METADATA_DEST_NEW_FILE = new File(METADATA_DEST_DIR, METADATA_NEW_FILE);

    private MetadataProvider metadataProvider;

    @Before
    public void setup() throws IOException {
        FileUtils.ensureDirectoryExistAndCanReadAndWrite(METADATA_DEST_DIR);
        org.apache.commons.io.FileUtils.cleanDirectory(METADATA_DEST_DIR);

        Files.copy(METADATA_SRC_EMPTY.toPath(), METADATA_DEST_EMPTY.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(METADATA_SRC_EMPTY_CONCISE.toPath(), METADATA_DEST_EMPTY_CONCISE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(METADATA_SRC_EXISTING.toPath(), METADATA_DEST_EXISTING.toPath(), StandardCopyOption.REPLACE_EXISTING);

        metadataProvider = new FileSystemMetadataProvider();
    }

    private ProviderConfigurationContext createConfigContext(final File metadataFile) {
        return () -> {
            final Map<String,String> props = new HashMap<>();
            props.put(FileSystemMetadataProvider.METADATA_FILE_PROP, metadataFile.getAbsolutePath());
            return props;
        };
    }

    @Test(expected = ProviderCreationException.class)
    public void testOnConfiguredMissingMetadataFileProperty() {
        metadataProvider.onConfigured(() -> new HashMap<>());
    }

    @Test(expected = ProviderCreationException.class)
    public void testOnConfiguredBlankMetadataFileProperty() {
        metadataProvider.onConfigured(() -> {
            final Map<String,String> props = new HashMap<>();
            props.put(FileSystemMetadataProvider.METADATA_FILE_PROP, "    ");
            return props;
        });
    }

    @Test
    public void testOnConfiguredWithEmptyMetadata() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EMPTY));
        assertEquals(0, metadataProvider.getBuckets().size());
        assertEquals(0, metadataProvider.getFlows().size());
    }

    @Test
    public void testOnConfiguredWithEmptyConciseMetadata() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EMPTY_CONCISE));
        assertEquals(0, metadataProvider.getBuckets().size());
        assertEquals(0, metadataProvider.getFlows().size());
    }

    @Test
    public void testOnConfiguredWithExistingMetadata() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());
        assertEquals(1, metadataProvider.getFlows().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketById("bucket1");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());

        final BucketMetadata bucket2 = metadataProvider.getBucketById("bucket2");
        assertNotNull(bucket2);
        assertEquals("bucket2", bucket2.getIdentifier());

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);
        assertEquals("flow1", flowMetadata.getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullBucket() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_NEW_FILE));
        metadataProvider.createBucket(null);
    }

    @Test
    public void testCreateBucket() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_NEW_FILE));

        final BucketMetadata bucket = new StandardBucketMetadata.Builder()
                .identifier("bucket1")
                .name("My Bucket")
                .description("This is my bucket")
                .created(System.currentTimeMillis())
                .build();

        final BucketMetadata returnedBucket = metadataProvider.createBucket(bucket);
        assertNotNull(returnedBucket);
        assertEquals(bucket.getIdentifier(), returnedBucket.getIdentifier());
        assertEquals(bucket.getName(), returnedBucket.getName());
        assertEquals(bucket.getDescription(), returnedBucket.getDescription());
        assertEquals(bucket.getCreatedTimestamp(), returnedBucket.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketByIdExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketById("bucket1");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());
        assertEquals("Bryan's Bucket", bucket1.getName());
        assertEquals("The description for Bryan's Bucket.", bucket1.getDescription());
        assertEquals(111111111, bucket1.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketByIdDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketById("bucket-does-not-exist");
        assertNull(bucket1);
    }

    @Test
    public void testGetBucketByNameExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketByName("Bryan's Bucket");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());
        assertEquals("Bryan's Bucket", bucket1.getName());
        assertEquals("The description for Bryan's Bucket.", bucket1.getDescription());
        assertEquals(111111111, bucket1.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketByNameCaseInsensitive() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketByName("bryan's bucket");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());
        assertEquals("Bryan's Bucket", bucket1.getName());
        assertEquals("The description for Bryan's Bucket.", bucket1.getDescription());
        assertEquals(111111111, bucket1.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketByNameDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket1 = metadataProvider.getBucketByName("bucket-does-not-exist");
        assertNull(bucket1);
    }

    @Test
    public void testUpdateBucketExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket = metadataProvider.getBucketById("bucket1");
        assertNotNull(bucket);

        final BucketMetadata updatedBucket = new StandardBucketMetadata.Builder(bucket)
                .name("New Name")
                .description("New Description")
                .build();

        final BucketMetadata returnedBucket = metadataProvider.updateBucket(updatedBucket);
        assertNotNull(returnedBucket);
        assertEquals("New Name", returnedBucket.getName());
        assertEquals("New Description", returnedBucket.getDescription());
    }

    @Test
    public void testUpdateBucketDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final BucketMetadata bucket = new StandardBucketMetadata.Builder()
                .identifier("does-not-exist")
                .name("New Name")
                .description("New Description")
                .created(System.currentTimeMillis())
                .build();

        final BucketMetadata updatedBucket = metadataProvider.updateBucket(bucket);
        assertNull(updatedBucket);
    }

    @Test
    public void testDeleteBucketWithFlows() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final String bucketId = "bucket1";
        assertNotNull(metadataProvider.getBucketById(bucketId));

        final Set<FlowMetadata> bucketFlows = metadataProvider.getFlows(bucketId);
        assertNotNull(bucketFlows);
        assertEquals(1, bucketFlows.size());

        metadataProvider.deleteBucket(bucketId);
        assertNull(metadataProvider.getBucketById(bucketId));

        final Set<FlowMetadata> bucketFlows2 = metadataProvider.getFlows(bucketId);
        assertNotNull(bucketFlows2);
        assertEquals(0, bucketFlows2.size());
    }

    @Test
    public void testDeleteBucketDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final String bucketId = "bucket-does-not-exist";
        metadataProvider.deleteBucket(bucketId);

        assertEquals(2, metadataProvider.getBuckets().size());
    }

    @Test
    public void testCreateFlow() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        // verify bucket2 exists and has no flows
        final BucketMetadata bucket2 = metadataProvider.getBucketById("bucket2");
        assertNotNull(bucket2);
        assertEquals(0, metadataProvider.getFlows(bucket2.getIdentifier()).size());

        final FlowMetadata flowMetadata = new StandardFlowMetadata.Builder()
                .identifier("flow2")
                .name("New Flow")
                .description("This is a new flow")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final FlowMetadata createdFlow = metadataProvider.createFlow(bucket2.getIdentifier(), flowMetadata);
        assertNotNull(createdFlow);
        assertEquals(flowMetadata.getIdentifier(), createdFlow.getIdentifier());
        assertEquals(flowMetadata.getName(), createdFlow.getName());
        assertEquals(flowMetadata.getDescription(), createdFlow.getDescription());
        assertEquals(flowMetadata.getCreatedTimestamp(), createdFlow.getCreatedTimestamp());
        assertEquals(flowMetadata.getModifiedTimestamp(), createdFlow.getModifiedTimestamp());
        assertEquals(bucket2.getIdentifier(), createdFlow.getBucketIdentifier());

        assertEquals(1, metadataProvider.getFlows(bucket2.getIdentifier()).size());
    }

    @Test
    public void testGetFlowByIdExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);
        assertEquals("flow1", flowMetadata.getIdentifier());
        assertEquals("Bryan's Flow", flowMetadata.getName());
        assertEquals("The description for Bryan's Flow.", flowMetadata.getDescription());
        assertEquals(333333333, flowMetadata.getCreatedTimestamp());
        assertEquals(444444444, flowMetadata.getModifiedTimestamp());
        assertEquals(3, flowMetadata.getSnapshotMetadata().size());
        assertEquals("bucket1", flowMetadata.getBucketIdentifier());
    }

    @Test
    public void testGetFlowByIdDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow-does-not-exist");
        assertNull(flowMetadata);
    }

    @Test
    public void testGetFlowByNameExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowByName("Bryan's Flow");
        assertNotNull(flowMetadata);
        assertEquals("flow1", flowMetadata.getIdentifier());
        assertEquals("Bryan's Flow", flowMetadata.getName());
        assertEquals("The description for Bryan's Flow.", flowMetadata.getDescription());
        assertEquals(333333333, flowMetadata.getCreatedTimestamp());
        assertEquals(444444444, flowMetadata.getModifiedTimestamp());
        assertEquals(3, flowMetadata.getSnapshotMetadata().size());
    }

    @Test
    public void testGetFlowByNameCaseInsensitive() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowByName("bryan's flow");
        assertNotNull(flowMetadata);
        assertEquals("flow1", flowMetadata.getIdentifier());
        assertEquals("Bryan's Flow", flowMetadata.getName());
        assertEquals("The description for Bryan's Flow.", flowMetadata.getDescription());
        assertEquals(333333333, flowMetadata.getCreatedTimestamp());
        assertEquals(444444444, flowMetadata.getModifiedTimestamp());
        assertEquals(3, flowMetadata.getSnapshotMetadata().size());
    }

    @Test
    public void testGetFlowByNameDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowByName("flow-does-not-exist");
        assertNull(flowMetadata);
    }

    @Test
    public void testUpdateFlowExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);

        final String newFlowName = "New Flow Name";
        final String newFlowDescription = "New Flow Description";
        assertNotEquals(flowMetadata.getName(), newFlowName);
        assertNotEquals(flowMetadata.getDescription(), newFlowDescription);

        final FlowMetadata updatedFlowMetadata = new StandardFlowMetadata.Builder(flowMetadata)
                .name("New Flow Name")
                .description("New Flow Description")
                .build();

        final FlowMetadata returnedFlow = metadataProvider.updateFlow(updatedFlowMetadata);
        assertEquals(newFlowName, returnedFlow.getName());
        assertEquals(newFlowDescription, returnedFlow.getDescription());
        assertEquals(flowMetadata.getCreatedTimestamp(), returnedFlow.getCreatedTimestamp());
        assertTrue(returnedFlow.getModifiedTimestamp() > flowMetadata.getModifiedTimestamp());
    }

    @Test
    public void testUpdateFlowDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = new StandardFlowMetadata.Builder()
                .identifier("does-not-exist")
                .name("Does Not Exist")
                .description("Does Not Exist")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final FlowMetadata updatedFlow = metadataProvider.updateFlow(flowMetadata);
        assertNull(updatedFlow);
    }

    @Test
    public void testDeleteFlowWithSnapshots() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);
        assertNotNull(flowMetadata.getSnapshotMetadata());
        assertTrue(flowMetadata.getSnapshotMetadata().size() > 0);

        metadataProvider.deleteFlow(flowMetadata.getIdentifier());

        final FlowMetadata deletedFlow = metadataProvider.getFlowById("flow1");
        assertNull(deletedFlow);
    }

    @Test
    public void testDeleteFlowDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        assertEquals(1, metadataProvider.getFlows().size());
        metadataProvider.deleteFlow("does-not-exist");
        assertEquals(1, metadataProvider.getFlows().size());
    }

    @Test
    public void testCreateFlowSnapshot() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata versionedFlow = metadataProvider.getFlowById("flow1");
        assertNotNull(versionedFlow);
        assertNotNull(versionedFlow.getSnapshotMetadata());

        int lastVersion = 1;
        for (final FlowSnapshotMetadata snapshot : versionedFlow.getSnapshotMetadata()) {
            if (snapshot.getVersion() > lastVersion) {
                lastVersion = snapshot.getVersion();
            }
        }

        final FlowSnapshotMetadata nextSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .flowIdentifier(versionedFlow.getIdentifier())
                .flowName(versionedFlow.getName())
                .version(lastVersion + 1)
                .comments("This is the next snapshot")
                .created(System.currentTimeMillis())
                .build();

        final FlowSnapshotMetadata createdSnapshot = metadataProvider.createFlowSnapshot(nextSnapshot);
        assertEquals(nextSnapshot.getFlowIdentifier(), createdSnapshot.getFlowIdentifier());
        assertEquals(nextSnapshot.getFlowName(), createdSnapshot.getFlowName());
        assertEquals(nextSnapshot.getVersion(), createdSnapshot.getVersion());
        assertEquals(nextSnapshot.getComments(), createdSnapshot.getComments());
        assertEquals(nextSnapshot.getCreatedTimestamp(), createdSnapshot.getCreatedTimestamp());

        final FlowMetadata updatedFlow = metadataProvider.getFlowById("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshotMetadata());
        assertEquals(updatedFlow.getSnapshotMetadata().size(), versionedFlow.getSnapshotMetadata().size() + 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFlowSnapshotWhenFlowDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowSnapshotMetadata nextSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .flowIdentifier("does-not-exist")
                .flowName("New Snapshot")
                .version(1)
                .comments("This is the next snapshot")
                .created(System.currentTimeMillis())
                .build();

        metadataProvider.createFlowSnapshot(nextSnapshot);
    }

    @Test
    public void testGetFlowSnapshotExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowSnapshotMetadata snapshot = metadataProvider.getFlowSnapshot("flow1", 1);
        assertNotNull(snapshot);
        assertEquals("flow1", snapshot.getFlowIdentifier());
        assertEquals("Bryan's Flow", snapshot.getFlowName());
        assertEquals(1, snapshot.getVersion());
        assertEquals(555555555, snapshot.getCreatedTimestamp());
        assertEquals("These are the comments for snapshot #1.", snapshot.getComments());
        assertEquals("bucket1", snapshot.getBucketIdentifier());
    }

    @Test
    public void testGetFlowSnapshotNameDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowSnapshotMetadata snapshot = metadataProvider.getFlowSnapshot("does-not-exist", 1);
        assertNull(snapshot);
    }

    @Test
    public void testGetFlowSnapshotVersionDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowSnapshotMetadata snapshot = metadataProvider.getFlowSnapshot("flow1", Integer.MAX_VALUE);
        assertNull(snapshot);
    }

    @Test
    public void testDeleteSnapshotExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);
        assertNotNull(flowMetadata.getSnapshotMetadata());
        assertEquals(3, flowMetadata.getSnapshotMetadata().size());

        final FlowSnapshotMetadata firstSnapshot = flowMetadata.getSnapshotMetadata().stream().findFirst().orElse(null);
        assertNotNull(firstSnapshot);

        metadataProvider.deleteFlowSnapshot(flowMetadata.getIdentifier(), firstSnapshot.getVersion());

        final FlowMetadata updatedFlow = metadataProvider.getFlowById("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshotMetadata());
        assertEquals(2, updatedFlow.getSnapshotMetadata().size());

        final FlowSnapshotMetadata deletedSnapshot = updatedFlow.getSnapshotMetadata().stream()
                .filter(s -> s.getVersion() == firstSnapshot.getVersion()).findFirst().orElse(null);
        assertNull(deletedSnapshot);
    }

    @Test
    public void testDeleteSnapshotDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final FlowMetadata flowMetadata = metadataProvider.getFlowById("flow1");
        assertNotNull(flowMetadata);
        assertNotNull(flowMetadata.getSnapshotMetadata());
        assertEquals(3, flowMetadata.getSnapshotMetadata().size());

        metadataProvider.deleteFlowSnapshot(flowMetadata.getIdentifier(), Integer.MAX_VALUE);

        final FlowMetadata updatedFlow = metadataProvider.getFlowById("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshotMetadata());
        assertEquals(3, updatedFlow.getSnapshotMetadata().size());
    }
}
