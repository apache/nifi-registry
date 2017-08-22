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
package org.apache.nifi.registry.service;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.metadata.BucketMetadata;
import org.apache.nifi.registry.metadata.FlowMetadata;
import org.apache.nifi.registry.metadata.FlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.MetadataProvider;
import org.apache.nifi.registry.metadata.StandardBucketMetadata;
import org.apache.nifi.registry.metadata.StandardFlowMetadata;
import org.apache.nifi.registry.metadata.StandardFlowSnapshotMetadata;
import org.apache.nifi.registry.serialization.FlowSnapshotSerializer;
import org.apache.nifi.registry.serialization.Serializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestRegistryService {

    private MetadataProvider metadataProvider;
    private FlowPersistenceProvider flowPersistenceProvider;
    private Serializer<VersionedFlowSnapshot> snapshotSerializer;
    private Validator validator;

    private RegistryService registryService;

    @Before
    public void setup() {
        metadataProvider = mock(MetadataProvider.class);
        flowPersistenceProvider = mock(FlowPersistenceProvider.class);
        snapshotSerializer = mock(FlowSnapshotSerializer.class);

        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        registryService = new RegistryService(metadataProvider, flowPersistenceProvider, snapshotSerializer, validator);
    }

    // ---------------------- Test Bucket methods ---------------------------------------------

    @Test
    public void testCreateBucketValid() {
        final Bucket bucket = new Bucket();
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket.");

        when(metadataProvider.getBucketByName(bucket.getName())).thenReturn(null);

        doAnswer(createBucketAnswer()).when(metadataProvider).createBucket(any(BucketMetadata.class));

        final Bucket createdBucket = registryService.createBucket(bucket);
        assertNotNull(createdBucket);
        assertNotNull(createdBucket.getIdentifier());
        assertNotNull(createdBucket.getCreatedTimestamp());

        assertEquals(bucket.getName(), createdBucket.getName());
        assertEquals(bucket.getDescription(), createdBucket.getDescription());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateBucketWithSameName() {
        final Bucket bucket = new Bucket();
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket.");

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketByName(bucket.getName())).thenReturn(existingBucket);

        // should throw exception since a bucket with the same name exists
        registryService.createBucket(bucket);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateBucketWithMissingName() {
        final Bucket bucket = new Bucket();
        when(metadataProvider.getBucketByName(bucket.getName())).thenReturn(null);
        registryService.createBucket(bucket);
    }

    @Test
    public void testGetExistingBucket() {
        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        final Bucket bucket = registryService.getBucket(existingBucket.getIdentifier());
        assertNotNull(bucket);
        assertEquals(existingBucket.getIdentifier(), bucket.getIdentifier());
        assertEquals(existingBucket.getName(), bucket.getName());
        assertEquals(existingBucket.getDescription(), bucket.getDescription());
        assertEquals(existingBucket.getCreatedTimestamp(), bucket.getCreatedTimestamp());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetBucketDoesNotExist() {
        when(metadataProvider.getBucketById(any(String.class))).thenReturn(null);
        registryService.getBucket("does-not-exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateBucketWithoutId() {
        final Bucket bucket = new Bucket();
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket.");
        registryService.updateBucket(bucket);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testUpdateBucketDoesNotExist() {
        final Bucket bucket = new Bucket();
        bucket.setIdentifier("b1");
        bucket.setName("My Bucket");
        bucket.setDescription("This is my bucket.");
        registryService.updateBucket(bucket);

        when(metadataProvider.getBucketById(any(String.class))).thenReturn(null);
        registryService.updateBucket(bucket);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateBucketWithSameNameAsExistingBucket() {
        final BucketMetadata bucketToUpdate = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(bucketToUpdate.getIdentifier())).thenReturn(bucketToUpdate);

        final BucketMetadata otherBucket = new StandardBucketMetadata.Builder()
                .identifier("b2")
                .name("My Bucket #2")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketByName(otherBucket.getName())).thenReturn(otherBucket);

        // should fail because other bucket has the same name
        final Bucket updatedBucket = new Bucket();
        updatedBucket.setIdentifier(bucketToUpdate.getIdentifier());
        updatedBucket.setName("My Bucket #2");
        updatedBucket.setDescription(bucketToUpdate.getDescription());

        registryService.updateBucket(updatedBucket);
    }

    @Test
    public void testUpdateBucket() {
        final BucketMetadata bucketToUpdate = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(bucketToUpdate.getIdentifier())).thenReturn(bucketToUpdate);

        doAnswer(updateBucketAnswer()).when(metadataProvider).updateBucket(any(BucketMetadata.class));

        final Bucket updatedBucket = new Bucket();
        updatedBucket.setIdentifier(bucketToUpdate.getIdentifier());
        updatedBucket.setName("Updated Name");
        updatedBucket.setDescription("Updated Description");

        final Bucket result = registryService.updateBucket(updatedBucket);
        assertNotNull(result);
        assertEquals(updatedBucket.getName(), result.getName());
        assertEquals(updatedBucket.getDescription(), result.getDescription());
    }

    @Test
    public void testUpdateBucketPartial() {
        final BucketMetadata bucketToUpdate = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(bucketToUpdate.getIdentifier())).thenReturn(bucketToUpdate);

        doAnswer(updateBucketAnswer()).when(metadataProvider).updateBucket(any(BucketMetadata.class));

        final Bucket updatedBucket = new Bucket();
        updatedBucket.setIdentifier(bucketToUpdate.getIdentifier());
        updatedBucket.setName("Updated Name");
        updatedBucket.setDescription(null);

        // name should be updated but description should not be changed
        final Bucket result = registryService.updateBucket(updatedBucket);
        assertNotNull(result);
        assertEquals(updatedBucket.getName(), result.getName());
        assertEquals(bucketToUpdate.getDescription(), result.getDescription());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteBucketDoesNotExist() {
        final String bucketId = "b1";
        when(metadataProvider.getBucketById(bucketId)).thenReturn(null);
        registryService.deleteBucket(bucketId);
    }

    @Test
    public void testDeleteBucketWithFlows() {
        final BucketMetadata bucketToDelete = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(bucketToDelete.getIdentifier())).thenReturn(bucketToDelete);

        final FlowMetadata flowToDelete = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("Flow 1")
                .description("This is flow 1")
                .created(System.currentTimeMillis())
                .build();

        final Set<FlowMetadata> flowsToDelete = new HashSet<>();
        flowsToDelete.add(flowToDelete);

        when(metadataProvider.getFlows(bucketToDelete.getIdentifier())).thenReturn(flowsToDelete);

        final Bucket deletedBucket = registryService.deleteBucket(bucketToDelete.getIdentifier());
        assertNotNull(deletedBucket);
        assertEquals(bucketToDelete.getIdentifier(), deletedBucket.getIdentifier());

        verify(flowPersistenceProvider, times(1))
                .deleteSnapshots(eq(bucketToDelete.getIdentifier()), eq(flowToDelete.getIdentifier()));
    }

    // ---------------------- Test VersionedFlow methods ---------------------------------------------

    @Test(expected = ConstraintViolationException.class)
    public void testCreateFlowInvalid() {
        final VersionedFlow versionedFlow = new VersionedFlow();
        registryService.createFlow("b1", versionedFlow);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateFlowBucketDoesNotExist() {

        when(metadataProvider.getBucketById(any(String.class))).thenReturn(null);

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName("My Flow");
        versionedFlow.setBucketIdentifier("b1");

        registryService.createFlow(versionedFlow.getBucketIdentifier(), versionedFlow);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFlowWithSameName() {
        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        // setup a flow with the same name that already exists

        final FlowMetadata flowMetadata = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowByName(flowMetadata.getName())).thenReturn(flowMetadata);

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName(flowMetadata.getName());
        versionedFlow.setBucketIdentifier("b1");

        registryService.createFlow(versionedFlow.getBucketIdentifier(), versionedFlow);
    }

    @Test
    public void testCreateFlowValid() {
        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName("My Flow");
        versionedFlow.setBucketIdentifier("b1");

        doAnswer(createFlowAnswer()).when(metadataProvider).createFlow(any(String.class), any(FlowMetadata.class));

        final VersionedFlow createdFlow = registryService.createFlow(versionedFlow.getBucketIdentifier(), versionedFlow);
        assertNotNull(createdFlow);
        assertNotNull(createdFlow.getIdentifier());
        assertTrue(createdFlow.getCreatedTimestamp() > 0);
        assertTrue(createdFlow.getModifiedTimestamp() > 0);
        assertEquals(versionedFlow.getName(), createdFlow.getName());
        assertEquals(versionedFlow.getBucketIdentifier(), createdFlow.getBucketIdentifier());
        assertEquals(versionedFlow.getDescription(), createdFlow.getDescription());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetFlowDoesNotExist() {
        when(metadataProvider.getFlowById(any(String.class))).thenReturn(null);
        registryService.getFlow("flow1");
    }

    @Test
    public void testGetFlowExists() {
        final FlowMetadata flowMetadata = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(flowMetadata.getIdentifier())).thenReturn(flowMetadata);

        final VersionedFlow versionedFlow = registryService.getFlow(flowMetadata.getIdentifier());
        assertNotNull(versionedFlow);
        assertEquals(flowMetadata.getIdentifier(), versionedFlow.getIdentifier());
        assertEquals(flowMetadata.getName(), versionedFlow.getName());
        assertEquals(flowMetadata.getDescription(), versionedFlow.getDescription());
        assertEquals(flowMetadata.getBucketIdentifier(), versionedFlow.getBucketIdentifier());
        assertEquals(flowMetadata.getCreatedTimestamp(), versionedFlow.getCreatedTimestamp());
        assertEquals(flowMetadata.getModifiedTimestamp(), versionedFlow.getModifiedTimestamp());
    }

    @Test
    public void testGetFlows() {
        final FlowMetadata flowMetadata1 = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final FlowMetadata flowMetadata2 = new StandardFlowMetadata.Builder()
                .identifier("flow2")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final Set<FlowMetadata> flows = new LinkedHashSet<>();
        flows.add(flowMetadata1);
        flows.add(flowMetadata2);

        when(metadataProvider.getFlows()).thenReturn(flows);

        final Set<VersionedFlow> allFlows = registryService.getFlows();
        assertNotNull(allFlows);
        assertEquals(2, allFlows.size());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetFlowsByBucketDoesNotExist() {
        when(metadataProvider.getBucketById(any(String.class))).thenReturn(null);
        registryService.getFlows("b1");
    }

    @Test
    public void testGetFlowsByBucketExists() {
        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier("b1")
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        final FlowMetadata flowMetadata1 = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final FlowMetadata flowMetadata2 = new StandardFlowMetadata.Builder()
                .identifier("flow2")
                .name("My Flow")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        final Set<FlowMetadata> flows = new LinkedHashSet<>();
        flows.add(flowMetadata1);
        flows.add(flowMetadata2);

        when(metadataProvider.getFlows(existingBucket.getIdentifier())).thenReturn(flows);

        final Set<VersionedFlow> allFlows = registryService.getFlows(existingBucket.getIdentifier());
        assertNotNull(allFlows);
        assertEquals(2, allFlows.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateFlowWithoutId() {
        final VersionedFlow versionedFlow = new VersionedFlow();
        registryService.updateFlow(versionedFlow);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testUpdateFlowDoesNotExist() {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier("flow1");

        when(metadataProvider.getFlowById(versionedFlow.getIdentifier())).thenReturn(null);

        registryService.updateFlow(versionedFlow);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFlowWithSameNameAsExistingFlow() {
        final FlowMetadata flowToUpdate = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(flowToUpdate.getIdentifier())).thenReturn(flowToUpdate);

        final FlowMetadata otherFlow = new StandardFlowMetadata.Builder()
                .identifier("flow2")
                .name("My Flow 2")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowByName(otherFlow.getName())).thenReturn(otherFlow);

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flowToUpdate.getIdentifier());
        versionedFlow.setName(otherFlow.getName());

        registryService.updateFlow(versionedFlow);
    }

    @Test
    public void testUpdateFlow() throws InterruptedException {
        final FlowMetadata flowToUpdate = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(flowToUpdate.getIdentifier())).thenReturn(flowToUpdate);
        when(metadataProvider.getFlowByName(flowToUpdate.getName())).thenReturn(flowToUpdate);

        doAnswer(updateFlowAnswer()).when(metadataProvider).updateFlow(any(FlowMetadata.class));

        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flowToUpdate.getIdentifier());
        versionedFlow.setName("New Flow Name");
        versionedFlow.setDescription("This is a new description");

        Thread.sleep(10);

        final VersionedFlow updatedFlow = registryService.updateFlow(versionedFlow);
        assertNotNull(updatedFlow);
        assertEquals(versionedFlow.getIdentifier(), updatedFlow.getIdentifier());

        // name and description should be updated
        assertEquals(versionedFlow.getName(), updatedFlow.getName());
        assertEquals(versionedFlow.getDescription(), updatedFlow.getDescription());

        // other fields should not be updated
        assertEquals(flowToUpdate.getBucketIdentifier(), updatedFlow.getBucketIdentifier());
        assertEquals(flowToUpdate.getCreatedTimestamp(), updatedFlow.getCreatedTimestamp());

        // modified timestamp should be auto updated
        assertTrue(updatedFlow.getModifiedTimestamp() > flowToUpdate.getModifiedTimestamp());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteFlowDoesNotExist() {
        when(metadataProvider.getFlowById(any(String.class))).thenReturn(null);
        registryService.deleteFlow("flow1");
    }

    @Test
    public void testDeleteFlowWithSnapshots() {
        final FlowMetadata flowToDelete = new StandardFlowMetadata.Builder()
                .identifier("flow1")
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier("b1")
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(flowToDelete.getIdentifier())).thenReturn(flowToDelete);
        when(metadataProvider.getFlowByName(flowToDelete.getName())).thenReturn(flowToDelete);

        final VersionedFlow deletedFlow = registryService.deleteFlow(flowToDelete.getIdentifier());
        assertNotNull(deletedFlow);
        assertEquals(flowToDelete.getIdentifier(), deletedFlow.getIdentifier());

        verify(flowPersistenceProvider, times(1))
                .deleteSnapshots(flowToDelete.getBucketIdentifier(), flowToDelete.getIdentifier());

        verify(metadataProvider, times(1))
                .deleteFlow(flowToDelete.getIdentifier());
    }

    // ---------------------- Test VersionedFlowSnapshot methods ---------------------------------------------

    private VersionedFlowSnapshot createSnapshot() {
        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setFlowIdentifier("flow1");
        snapshotMetadata.setFlowName("First Flow");
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setComments("This is the first snapshot");
        snapshotMetadata.setBucketIdentifier("b1");

        final VersionedProcessGroup processGroup = new VersionedProcessGroup();
        processGroup.setIdentifier("pg1");
        processGroup.setName("My Process Group");

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(processGroup);

        return snapshot;
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateSnapshotInvalidMetadata() {
        final VersionedFlowSnapshot snapshot = createSnapshot();
        snapshot.getSnapshotMetadata().setFlowName(null);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateSnapshotInvalidFlowContents() {
        final VersionedFlowSnapshot snapshot = createSnapshot();
        snapshot.setFlowContents(null);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateSnapshotNullMetadata() {
        final VersionedFlowSnapshot snapshot = createSnapshot();
        snapshot.setSnapshotMetadata(null);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCreateSnapshotNullFlowContents() {
        final VersionedFlowSnapshot snapshot = createSnapshot();
        snapshot.setFlowContents(null);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateSnapshotBucketDoesNotExist() {
        when(metadataProvider.getBucketById(any(String.class))).thenReturn(null);

        final VersionedFlowSnapshot snapshot = createSnapshot();
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateSnapshotFlowDoesNotExist() {
        final VersionedFlowSnapshot snapshot = createSnapshot();

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        when(metadataProvider.getFlowById(snapshot.getSnapshotMetadata().getFlowIdentifier())).thenReturn(null);

        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSnapshotVersionAlreadyExists() {
        final VersionedFlowSnapshot snapshot = createSnapshot();

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        // make a snapshot that has the same version as the one being created
        final FlowSnapshotMetadata existingSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .flowIdentifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .flowName(snapshot.getSnapshotMetadata().getFlowName())
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .version(snapshot.getSnapshotMetadata().getVersion())
                .comments("This is an existing snapshot")
                .created(System.currentTimeMillis())
                .build();

        // return a flow with the existing snapshot when getFlowById is called
        final FlowMetadata existingFlow = new StandardFlowMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .addSnapshot(existingSnapshot)
                .build();

        when(metadataProvider.getFlowById(existingFlow.getIdentifier())).thenReturn(existingFlow);

        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSnapshotVersionNotNextVersion() {
        final VersionedFlowSnapshot snapshot = createSnapshot();

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        // make a snapshot for version 1
        final FlowSnapshotMetadata existingSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .flowIdentifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .flowName(snapshot.getSnapshotMetadata().getFlowName())
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .version(1)
                .comments("This is an existing snapshot")
                .created(System.currentTimeMillis())
                .build();

        // return a flow with the existing snapshot when getFlowById is called
        final FlowMetadata existingFlow = new StandardFlowMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .addSnapshot(existingSnapshot)
                .build();

        when(metadataProvider.getFlowById(existingFlow.getIdentifier())).thenReturn(existingFlow);

        // set the version to something that is not the next one-up version
        snapshot.getSnapshotMetadata().setVersion(100);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test
    public void testCreateFirstSnapshot() {
        final VersionedFlowSnapshot snapshot = createSnapshot();

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        // return a flow with the existing snapshot when getFlowById is called
        final FlowMetadata existingFlow = new StandardFlowMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(existingFlow.getIdentifier())).thenReturn(existingFlow);

        final VersionedFlowSnapshot createdSnapshot = registryService.createFlowSnapshot(snapshot);
        assertNotNull(createdSnapshot);

        verify(snapshotSerializer, times(1)).serialize(eq(snapshot), any(OutputStream.class));
        verify(flowPersistenceProvider, times(1)).saveSnapshot(any(), any());
        verify(metadataProvider, times(1)).createFlowSnapshot(any(FlowSnapshotMetadata.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFirstSnapshotWithBadVersion() {
        final VersionedFlowSnapshot snapshot = createSnapshot();

        final BucketMetadata existingBucket = new StandardBucketMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .name("My Bucket #1")
                .description("This is my bucket.")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getBucketById(existingBucket.getIdentifier())).thenReturn(existingBucket);

        // return a flow with the existing snapshot when getFlowById is called
        final FlowMetadata existingFlow = new StandardFlowMetadata.Builder()
                .identifier(snapshot.getSnapshotMetadata().getFlowIdentifier())
                .name("My Flow 1")
                .description("This is my flow.")
                .bucketIdentifier(snapshot.getSnapshotMetadata().getBucketIdentifier())
                .created(System.currentTimeMillis())
                .modified(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowById(existingFlow.getIdentifier())).thenReturn(existingFlow);

        // set the first version to something other than 1
        snapshot.getSnapshotMetadata().setVersion(100);
        registryService.createFlowSnapshot(snapshot);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetSnapshotDoesNotExistInMetadataProvider() {
        final String flowId = "flow1";
        final Integer version = 1;
        when(metadataProvider.getFlowSnapshot(flowId, version)).thenReturn(null);
        registryService.getFlowSnapshot(flowId, version);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSnapshotDoesNotExistInPersistenceProvider() {
        final FlowSnapshotMetadata existingSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .bucketIdentifier("b1")
                .flowIdentifier("flow1")
                .flowName("Flow 1")
                .version(1)
                .comments("This is snapshot 1")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowSnapshot(existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion()))
                .thenReturn(existingSnapshot);

        when(flowPersistenceProvider.getSnapshot(
                existingSnapshot.getBucketIdentifier(),
                existingSnapshot.getFlowIdentifier(),
                existingSnapshot.getVersion()
        )).thenReturn(null);

        registryService.getFlowSnapshot(existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion());
    }

    @Test
    public void testGetSnapshotExists() {
        final FlowSnapshotMetadata existingSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .bucketIdentifier("b1")
                .flowIdentifier("flow1")
                .flowName("Flow 1")
                .version(1)
                .comments("This is snapshot 1")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowSnapshot(existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion()))
                .thenReturn(existingSnapshot);

        // return a non-null, non-zero-length array so something gets passed to the serializer
        when(flowPersistenceProvider.getSnapshot(
                existingSnapshot.getBucketIdentifier(),
                existingSnapshot.getFlowIdentifier(),
                existingSnapshot.getVersion()
        )).thenReturn(new byte[10]);

        final VersionedFlowSnapshot snapshotToDeserialize = createSnapshot();
        when(snapshotSerializer.deserialize(any(InputStream.class))).thenReturn(snapshotToDeserialize);

        final VersionedFlowSnapshot returnedSnapshot = registryService.getFlowSnapshot(
                existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion());
        assertNotNull(returnedSnapshot);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteSnapshotDoesNotExist() {
        final String flowId = "flow1";
        final Integer version = 1;
        when(metadataProvider.getFlowSnapshot(flowId, version)).thenReturn(null);
        registryService.deleteFlowSnapshot(flowId, version);
    }

    @Test
    public void testDeleteSnapshotExists() {
        final FlowSnapshotMetadata existingSnapshot = new StandardFlowSnapshotMetadata.Builder()
                .bucketIdentifier("b1")
                .flowIdentifier("flow1")
                .flowName("Flow 1")
                .version(1)
                .comments("This is snapshot 1")
                .created(System.currentTimeMillis())
                .build();

        when(metadataProvider.getFlowSnapshot(existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion()))
                .thenReturn(existingSnapshot);

        final VersionedFlowSnapshotMetadata deletedSnapshot = registryService.deleteFlowSnapshot(
                existingSnapshot.getFlowIdentifier(), existingSnapshot.getVersion());
        assertNotNull(deletedSnapshot);
        assertEquals(existingSnapshot.getFlowIdentifier(), deletedSnapshot.getFlowIdentifier());

        verify(flowPersistenceProvider, times(1)).deleteSnapshot(
                existingSnapshot.getBucketIdentifier(),
                existingSnapshot.getFlowIdentifier(),
                existingSnapshot.getVersion()
        );

        verify(metadataProvider, times(1)).deleteFlowSnapshot(
                existingSnapshot.getFlowIdentifier(),
                existingSnapshot.getVersion()
        );
    }

    // -------------------------------------------------------------------

    private Answer<BucketMetadata> createBucketAnswer() {
        return (InvocationOnMock invocation) -> {
            BucketMetadata bucketMetadata = (BucketMetadata) invocation.getArguments()[0];
            return bucketMetadata;
        };
    }

    private Answer<BucketMetadata> updateBucketAnswer() {
        return (InvocationOnMock invocation) -> {
            BucketMetadata bucketMetadata = (BucketMetadata) invocation.getArguments()[0];
            return bucketMetadata;
        };
    }

    private Answer<FlowMetadata> createFlowAnswer() {
        return (InvocationOnMock invocation) -> {
            final FlowMetadata flowMetadata = (FlowMetadata) invocation.getArguments()[1];
            return flowMetadata;
        };
    }

    private Answer<FlowMetadata> updateFlowAnswer() {
        return (InvocationOnMock invocation) -> {
            final FlowMetadata flowMetadata = (FlowMetadata) invocation.getArguments()[0];
            return flowMetadata;
        };
    }
}
