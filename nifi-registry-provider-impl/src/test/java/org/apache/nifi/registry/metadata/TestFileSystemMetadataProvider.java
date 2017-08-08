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

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
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

        final Bucket bucket1 = metadataProvider.getBucket("bucket1");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());

        final Bucket bucket2 = metadataProvider.getBucket("bucket2");
        assertNotNull(bucket2);
        assertEquals("bucket2", bucket2.getIdentifier());

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertEquals("flow1", versionedFlow.getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullBucket() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_NEW_FILE));
        metadataProvider.createBucket(null);
    }

    @Test
    public void testCreateBucket() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_NEW_FILE));

        final Bucket bucket = new Bucket();
        bucket.setIdentifier("bucket1");
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket");
        bucket.setCreatedTimestamp(System.currentTimeMillis());

        final Bucket returnedBucket = metadataProvider.createBucket(bucket);
        assertNotNull(returnedBucket);
        assertEquals(bucket.getIdentifier(), returnedBucket.getIdentifier());
        assertEquals(bucket.getName(), returnedBucket.getName());
        assertEquals(bucket.getDescription(), returnedBucket.getDescription());
        assertEquals(bucket.getCreatedTimestamp(), returnedBucket.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final Bucket bucket1 = metadataProvider.getBucket("bucket1");
        assertNotNull(bucket1);
        assertEquals("bucket1", bucket1.getIdentifier());
        assertEquals("Bryan's Bucket", bucket1.getName());
        assertEquals("The description for Bryan's Bucket.", bucket1.getDescription());
        assertEquals(111111111, bucket1.getCreatedTimestamp());
    }

    @Test
    public void testGetBucketDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final Bucket bucket1 = metadataProvider.getBucket("bucket-does-not-exist");
        assertNull(bucket1);
    }

    @Test
    public void testUpdateBucketExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final Bucket bucket = metadataProvider.getBucket("bucket1");
        assertNotNull(bucket);

        bucket.setName("New Name");
        bucket.setDescription("New Description");

        final Bucket updatedBucket = metadataProvider.updateBucket(bucket);
        assertNotNull(updatedBucket);
        assertEquals("New Name", updatedBucket.getName());
        assertEquals("New Description", updatedBucket.getDescription());
    }

    @Test
    public void testUpdateBucketDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final Bucket bucket = new Bucket();
        bucket.setIdentifier("does-not-exist");
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket");
        bucket.setCreatedTimestamp(System.currentTimeMillis());

        final Bucket updatedBucket = metadataProvider.updateBucket(bucket);
        assertNull(updatedBucket);
    }

    @Test
    public void testDeleteBucketWithFlows() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));
        assertEquals(2, metadataProvider.getBuckets().size());

        final String bucketId = "bucket1";
        assertNotNull(metadataProvider.getBucket(bucketId));

        final Set<VersionedFlow> bucketFlows = metadataProvider.getFlows(bucketId);
        assertNotNull(bucketFlows);
        assertEquals(1, bucketFlows.size());

        metadataProvider.deleteBucket(bucketId);
        assertNull(metadataProvider.getBucket(bucketId));

        final Set<VersionedFlow> bucketFlows2 = metadataProvider.getFlows(bucketId);
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
        final Bucket bucket2 = metadataProvider.getBucket("bucket2");
        assertNotNull(bucket2);
        assertEquals(0, metadataProvider.getFlows(bucket2.getIdentifier()).size());

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier("flow2");
        versionedFlow.setName("New Flow");
        versionedFlow.setDescription("This is a new flow");
        versionedFlow.setCreatedTimestamp(System.currentTimeMillis());
        versionedFlow.setModifiedTimestamp(System.currentTimeMillis());

        final VersionedFlow createdFlow = metadataProvider.createFlow(bucket2.getIdentifier(), versionedFlow);
        assertNotNull(createdFlow);
        assertEquals(versionedFlow.getIdentifier(), createdFlow.getIdentifier());
        assertEquals(versionedFlow.getName(), createdFlow.getName());
        assertEquals(versionedFlow.getDescription(), createdFlow.getDescription());
        assertEquals(versionedFlow.getCreatedTimestamp(), createdFlow.getCreatedTimestamp());
        assertEquals(versionedFlow.getModifiedTimestamp(), createdFlow.getModifiedTimestamp());

        assertEquals(1, metadataProvider.getFlows(bucket2.getIdentifier()).size());
    }

    @Test
    public void testGetFlowExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertEquals("flow1", versionedFlow.getIdentifier());
        assertEquals("Bryan's Flow", versionedFlow.getName());
        assertEquals("The description for Bryan's Flow.", versionedFlow.getDescription());
        assertEquals(333333333, versionedFlow.getCreatedTimestamp());
        assertEquals(444444444, versionedFlow.getModifiedTimestamp());
        assertEquals(3, versionedFlow.getSnapshots().size());
    }

    @Test
    public void testGetFlowDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow-does-not-exist");
        assertNull(versionedFlow);
    }

    @Test
    public void testUpdateFlowExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);

        final String newFlowName = "New Flow Name";
        final String newFlowDescription = "New Flow Description";
        assertNotEquals(versionedFlow.getName(), newFlowName);
        assertNotEquals(versionedFlow.getDescription(), newFlowDescription);

        versionedFlow.setName("New Flow Name");
        versionedFlow.setDescription("New Flow Description");

        final VersionedFlow updatedFlow = metadataProvider.updateFlow(versionedFlow);
        assertEquals(newFlowName, updatedFlow.getName());
        assertEquals(newFlowDescription, updatedFlow.getDescription());
        assertEquals(versionedFlow.getCreatedTimestamp(), updatedFlow.getCreatedTimestamp());
        assertTrue(updatedFlow.getModifiedTimestamp() > versionedFlow.getModifiedTimestamp());
    }

    @Test
    public void testUpdateFlowDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier("does-not-exist");
        versionedFlow.setName("Does Not Exist");
        versionedFlow.setDescription("Does Not Exist");
        versionedFlow.setCreatedTimestamp(System.currentTimeMillis());
        versionedFlow.setModifiedTimestamp(System.currentTimeMillis());

        final VersionedFlow updatedFlow = metadataProvider.updateFlow(versionedFlow);
        assertNull(updatedFlow);
    }

    @Test
    public void testDeleteFlowWithSnapshots() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertNotNull(versionedFlow.getSnapshots());
        assertTrue(versionedFlow.getSnapshots().size() > 0);

        metadataProvider.deleteFlow(versionedFlow.getIdentifier());

        final VersionedFlow deletedFlow = metadataProvider.getFlow("flow1");
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

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertNotNull(versionedFlow.getSnapshots());

        int lastVersion = 1;
        for (final VersionedFlowSnapshot snapshot : versionedFlow.getSnapshots()) {
            if (snapshot.getVersion() > lastVersion) {
                lastVersion = snapshot.getVersion();
            }
        }

        final VersionedFlowSnapshot nextSnapshot = new VersionedFlowSnapshot();
        nextSnapshot.setFlowIdentifier(versionedFlow.getIdentifier());
        nextSnapshot.setFlowName(versionedFlow.getName());
        nextSnapshot.setVersion(lastVersion + 1);
        nextSnapshot.setComments("This is the next snapshot");
        nextSnapshot.setTimestamp(System.currentTimeMillis());

        final VersionedFlowSnapshot createdSnapshot = metadataProvider.createFlowSnapshot(nextSnapshot);
        assertEquals(nextSnapshot.getFlowIdentifier(), createdSnapshot.getFlowIdentifier());
        assertEquals(nextSnapshot.getFlowName(), createdSnapshot.getFlowName());
        assertEquals(nextSnapshot.getVersion(), createdSnapshot.getVersion());
        assertEquals(nextSnapshot.getComments(), createdSnapshot.getComments());
        assertEquals(nextSnapshot.getTimestamp(), createdSnapshot.getTimestamp());

        final VersionedFlow updatedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshots());
        assertEquals(updatedFlow.getSnapshots().size(), versionedFlow.getSnapshots().size() + 1);
    }

    @Test
    public void testGetFlowSnapshotExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlowSnapshot snapshot = metadataProvider.getFlowSnapshot("flow1", 1);
        assertNotNull(snapshot);
        assertEquals("flow1", snapshot.getFlowIdentifier());
        assertEquals("Bryan's Flow", snapshot.getFlowName());
        assertEquals(1, snapshot.getVersion());
        assertEquals(555555555, snapshot.getTimestamp());
        assertEquals("These are the comments for snapshot #1.", snapshot.getComments());
    }

    @Test
    public void testGetFlowSnapshotNameDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlowSnapshot snapshot = metadataProvider.getFlowSnapshot("does-not-exist", 1);
        assertNull(snapshot);
    }

    @Test
    public void testGetFlowSnapshotVersionDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlowSnapshot snapshot = metadataProvider.getFlowSnapshot("flow1", Integer.MAX_VALUE);
        assertNull(snapshot);
    }

    @Test
    public void testDeleteSnapshotExists() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertNotNull(versionedFlow.getSnapshots());
        assertEquals(3, versionedFlow.getSnapshots().size());

        final VersionedFlowSnapshot firstSnapshot = versionedFlow.getSnapshots().stream().findFirst().orElse(null);
        assertNotNull(firstSnapshot);

        metadataProvider.deleteFlowSnapshot(versionedFlow.getIdentifier(), firstSnapshot.getVersion());

        final VersionedFlow updatedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshots());
        assertEquals(2, updatedFlow.getSnapshots().size());

        final VersionedFlowSnapshot deletedSnapshot = updatedFlow.getSnapshots().stream()
                .filter(s -> s.getVersion() == firstSnapshot.getVersion()).findFirst().orElse(null);
        assertNull(deletedSnapshot);
    }

    @Test
    public void testDeleteSnapshotDoesNotExist() {
        metadataProvider.onConfigured(createConfigContext(METADATA_DEST_EXISTING));

        final VersionedFlow versionedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(versionedFlow);
        assertNotNull(versionedFlow.getSnapshots());
        assertEquals(3, versionedFlow.getSnapshots().size());

        metadataProvider.deleteFlowSnapshot(versionedFlow.getIdentifier(), Integer.MAX_VALUE);

        final VersionedFlow updatedFlow = metadataProvider.getFlow("flow1");
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getSnapshots());
        assertEquals(3, updatedFlow.getSnapshots().size());
    }
}
