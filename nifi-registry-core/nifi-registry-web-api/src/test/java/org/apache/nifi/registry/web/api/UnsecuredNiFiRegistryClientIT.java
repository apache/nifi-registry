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
package org.apache.nifi.registry.web.api;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.authorization.CurrentUser;
import org.apache.nifi.registry.authorization.Permissions;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.bucket.BucketItemType;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.ExtensionBundleClient;
import org.apache.nifi.registry.client.ExtensionBundleVersionClient;
import org.apache.nifi.registry.client.ExtensionRepoClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.ItemsClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.client.UserClient;
import org.apache.nifi.registry.client.impl.JerseyNiFiRegistryClient;
import org.apache.nifi.registry.diff.VersionedFlowDifference;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionDependency;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.filter.ExtensionBundleFilterParams;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersion;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedPropertyDescriptor;
import org.apache.nifi.registry.util.FileUtils;
import org.bouncycastle.util.encoders.Hex;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test all basic functionality of JerseyNiFiRegistryClient.
 */
public class UnsecuredNiFiRegistryClientIT extends UnsecuredITBase {

    static final Logger LOGGER = LoggerFactory.getLogger(UnsecuredNiFiRegistryClientIT.class);

    private NiFiRegistryClient client;

    @Before
    public void setup() throws IOException {
        final String baseUrl = createBaseURL();
        LOGGER.info("Using base url = " + baseUrl);

        final NiFiRegistryClientConfig clientConfig = new NiFiRegistryClientConfig.Builder()
                .baseUrl(baseUrl)
                .build();

        Assert.assertNotNull(clientConfig);

        final NiFiRegistryClient client = new JerseyNiFiRegistryClient.Builder()
                .config(clientConfig)
                .build();

        Assert.assertNotNull(client);
        this.client = client;

        // Clear the extension bundles storage directory in case previous tests left data
        final File extensionsStorageDir = new File("./target/test-classes/extension_bundles");
        if (extensionsStorageDir.exists()) {
            try {
                FileUtils.deleteFile(extensionsStorageDir, true);
            } catch (Exception e) {
                LOGGER.warn("Unable to delete extensions storage dir due to: " + e.getMessage(), e);
            }
        }
    }

    @After
    public void teardown() {
        try {
            client.close();
        } catch (Exception e) {

        }
    }

    @Test
    public void testGetAccessStatus() throws IOException, NiFiRegistryException {
        final UserClient userClient = client.getUserClient();
        final CurrentUser currentUser = userClient.getAccessStatus();
        Assert.assertEquals("anonymous", currentUser.getIdentity());
        Assert.assertTrue(currentUser.isAnonymous());
        Assert.assertNotNull(currentUser.getResourcePermissions());
        Permissions fullAccess = new Permissions().withCanRead(true).withCanWrite(true).withCanDelete(true);
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getAnyTopLevelResource());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getBuckets());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getTenants());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getPolicies());
        Assert.assertEquals(fullAccess, currentUser.getResourcePermissions().getProxy());
    }

    @Test
    public void testNiFiRegistryClient() throws IOException, NiFiRegistryException, NoSuchAlgorithmException {
        // ---------------------- TEST BUCKETS --------------------------//

        final BucketClient bucketClient = client.getBucketClient();

        // create buckets
        final int numBuckets = 10;
        final List<Bucket> createdBuckets = new ArrayList<>();

        for (int i=0; i < numBuckets; i++) {
            final Bucket createdBucket = createBucket(bucketClient, i);
            LOGGER.info("Created bucket # " + i + " with id " + createdBucket.getIdentifier());
            createdBuckets.add(createdBucket);
        }

        // get each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket retrievedBucket = bucketClient.get(bucket.getIdentifier());
            Assert.assertNotNull(retrievedBucket);
            LOGGER.info("Retrieved bucket " + retrievedBucket.getIdentifier());
        }

        //final Bucket nonExistentBucket = bucketClient.get("does-not-exist");
        //Assert.assertNull(nonExistentBucket);

        // get bucket fields
        final Fields bucketFields = bucketClient.getFields();
        Assert.assertNotNull(bucketFields);
        LOGGER.info("Retrieved bucket fields, size = " + bucketFields.getFields().size());
        Assert.assertTrue(bucketFields.getFields().size() > 0);

        // get all buckets
        final List<Bucket> allBuckets = bucketClient.getAll();
        LOGGER.info("Retrieved buckets, size = " + allBuckets.size());
        Assert.assertEquals(numBuckets, allBuckets.size());
        allBuckets.stream().forEach(b -> System.out.println("Retrieve bucket " + b.getIdentifier()));

        // update each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket bucketUpdate = new Bucket();
            bucketUpdate.setIdentifier(bucket.getIdentifier());
            bucketUpdate.setDescription(bucket.getDescription() + " UPDATE");

            final Bucket updatedBucket = bucketClient.update(bucketUpdate);
            Assert.assertNotNull(updatedBucket);
            LOGGER.info("Updated bucket " + updatedBucket.getIdentifier());
        }

        // ---------------------- TEST FLOWS --------------------------//

        final FlowClient flowClient = client.getFlowClient();

        // create flows
        final Bucket flowsBucket = createdBuckets.get(0);

        final VersionedFlow flow1 = createFlow(flowClient, flowsBucket, 1);
        LOGGER.info("Created flow # 1 with id " + flow1.getIdentifier());

        final VersionedFlow flow2 = createFlow(flowClient, flowsBucket, 2);
        LOGGER.info("Created flow # 2 with id " + flow2.getIdentifier());

        // get flow
        final VersionedFlow retrievedFlow1 = flowClient.get(flowsBucket.getIdentifier(), flow1.getIdentifier());
        Assert.assertNotNull(retrievedFlow1);
        LOGGER.info("Retrieved flow # 1 with id " + retrievedFlow1.getIdentifier());

        final VersionedFlow retrievedFlow2 = flowClient.get(flowsBucket.getIdentifier(), flow2.getIdentifier());
        Assert.assertNotNull(retrievedFlow2);
        LOGGER.info("Retrieved flow # 2 with id " + retrievedFlow2.getIdentifier());

        // get flow without bucket
        final VersionedFlow retrievedFlow1WithoutBucket = flowClient.get(flow1.getIdentifier());
        Assert.assertNotNull(retrievedFlow1WithoutBucket);
        Assert.assertEquals(flow1.getIdentifier(), retrievedFlow1WithoutBucket.getIdentifier());
        LOGGER.info("Retrieved flow # 1 without bucket id, with id " + retrievedFlow1WithoutBucket.getIdentifier());

        // update flows
        final VersionedFlow flow1Update = new VersionedFlow();
        flow1Update.setIdentifier(flow1.getIdentifier());
        flow1Update.setName(flow1.getName() + " UPDATED");

        final VersionedFlow updatedFlow1 = flowClient.update(flowsBucket.getIdentifier(), flow1Update);
        Assert.assertNotNull(updatedFlow1);
        LOGGER.info("Updated flow # 1 with id " + updatedFlow1.getIdentifier());

        // get flow fields
        final Fields flowFields = flowClient.getFields();
        Assert.assertNotNull(flowFields);
        LOGGER.info("Retrieved flow fields, size = " + flowFields.getFields().size());
        Assert.assertTrue(flowFields.getFields().size() > 0);

        // get flows in bucket
        final List<VersionedFlow> flowsInBucket = flowClient.getByBucket(flowsBucket.getIdentifier());
        Assert.assertNotNull(flowsInBucket);
        Assert.assertEquals(2, flowsInBucket.size());
        flowsInBucket.stream().forEach(f -> LOGGER.info("Flow in bucket, flow id " + f.getIdentifier()));

        // ---------------------- TEST SNAPSHOTS --------------------------//

        final FlowSnapshotClient snapshotClient = client.getFlowSnapshotClient();

        // create snapshots
        final VersionedFlow snapshotFlow = flow1;

        final VersionedFlowSnapshot snapshot1 = createSnapshot(snapshotClient, snapshotFlow, 1);
        LOGGER.info("Created snapshot # 1 with version " + snapshot1.getSnapshotMetadata().getVersion());

        final VersionedFlowSnapshot snapshot2 = createSnapshot(snapshotClient, snapshotFlow, 2);
        LOGGER.info("Created snapshot # 2 with version " + snapshot2.getSnapshotMetadata().getVersion());

        // get snapshot
        final VersionedFlowSnapshot retrievedSnapshot1 = snapshotClient.get(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier(), 1);
        Assert.assertNotNull(retrievedSnapshot1);
        Assert.assertFalse(retrievedSnapshot1.isLatest());
        LOGGER.info("Retrieved snapshot # 1 with version " + retrievedSnapshot1.getSnapshotMetadata().getVersion());

        final VersionedFlowSnapshot retrievedSnapshot2 = snapshotClient.get(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier(), 2);
        Assert.assertNotNull(retrievedSnapshot2);
        Assert.assertTrue(retrievedSnapshot2.isLatest());
        LOGGER.info("Retrieved snapshot # 2 with version " + retrievedSnapshot2.getSnapshotMetadata().getVersion());

        // get snapshot without bucket
        final VersionedFlowSnapshot retrievedSnapshot1WithoutBucket = snapshotClient.get(snapshotFlow.getIdentifier(), 1);
        Assert.assertNotNull(retrievedSnapshot1WithoutBucket);
        Assert.assertFalse(retrievedSnapshot1WithoutBucket.isLatest());
        Assert.assertEquals(snapshotFlow.getIdentifier(), retrievedSnapshot1WithoutBucket.getSnapshotMetadata().getFlowIdentifier());
        Assert.assertEquals(1, retrievedSnapshot1WithoutBucket.getSnapshotMetadata().getVersion());
        LOGGER.info("Retrieved snapshot # 1 without using bucket id, with version " + retrievedSnapshot1WithoutBucket.getSnapshotMetadata().getVersion());

        // get latest
        final VersionedFlowSnapshot retrievedSnapshotLatest = snapshotClient.getLatest(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedSnapshotLatest);
        Assert.assertEquals(snapshot2.getSnapshotMetadata().getVersion(), retrievedSnapshotLatest.getSnapshotMetadata().getVersion());
        Assert.assertTrue(retrievedSnapshotLatest.isLatest());
        LOGGER.info("Retrieved latest snapshot with version " + retrievedSnapshotLatest.getSnapshotMetadata().getVersion());

        // get latest without bucket
        final VersionedFlowSnapshot retrievedSnapshotLatestWithoutBucket = snapshotClient.getLatest(snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedSnapshotLatestWithoutBucket);
        Assert.assertEquals(snapshot2.getSnapshotMetadata().getVersion(), retrievedSnapshotLatestWithoutBucket.getSnapshotMetadata().getVersion());
        Assert.assertTrue(retrievedSnapshotLatestWithoutBucket.isLatest());
        LOGGER.info("Retrieved latest snapshot without bucket, with version " + retrievedSnapshotLatestWithoutBucket.getSnapshotMetadata().getVersion());

        // get metadata
        final List<VersionedFlowSnapshotMetadata> retrievedMetadata = snapshotClient.getSnapshotMetadata(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedMetadata);
        Assert.assertEquals(2, retrievedMetadata.size());
        Assert.assertEquals(2, retrievedMetadata.get(0).getVersion());
        Assert.assertEquals(1, retrievedMetadata.get(1).getVersion());
        retrievedMetadata.stream().forEach(s -> LOGGER.info("Retrieved snapshot metadata " + s.getVersion()));

        // get metadata without bucket
        final List<VersionedFlowSnapshotMetadata> retrievedMetadataWithoutBucket = snapshotClient.getSnapshotMetadata(snapshotFlow.getIdentifier());
        Assert.assertNotNull(retrievedMetadataWithoutBucket);
        Assert.assertEquals(2, retrievedMetadataWithoutBucket.size());
        Assert.assertEquals(2, retrievedMetadataWithoutBucket.get(0).getVersion());
        Assert.assertEquals(1, retrievedMetadataWithoutBucket.get(1).getVersion());
        retrievedMetadataWithoutBucket.stream().forEach(s -> LOGGER.info("Retrieved snapshot metadata " + s.getVersion()));

        // get latest metadata
        final VersionedFlowSnapshotMetadata latestMetadata = snapshotClient.getLatestMetadata(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier());
        Assert.assertNotNull(latestMetadata);
        Assert.assertEquals(2, latestMetadata.getVersion());

        // get latest metadata that doesn't exist
        try {
            snapshotClient.getLatestMetadata(snapshotFlow.getBucketIdentifier(), "DOES-NOT-EXIST");
            Assert.fail("Should have thrown exception");
        } catch (NiFiRegistryException nfe) {
            Assert.assertEquals("Error retrieving latest snapshot metadata: The specified flow ID does not exist in this bucket.", nfe.getMessage());
        }

        // get latest metadata without bucket
        final VersionedFlowSnapshotMetadata latestMetadataWithoutBucket = snapshotClient.getLatestMetadata(snapshotFlow.getIdentifier());
        Assert.assertNotNull(latestMetadataWithoutBucket);
        Assert.assertEquals(snapshotFlow.getIdentifier(), latestMetadataWithoutBucket.getFlowIdentifier());
        Assert.assertEquals(2, latestMetadataWithoutBucket.getVersion());

        // ---------------------- TEST EXTENSIONS ----------------------//

        // verify we have no bundles yet
        final ExtensionBundleClient bundleClient = client.getExtensionBundleClient();
        final List<ExtensionBundle> allBundles = bundleClient.getAll();
        Assert.assertEquals(0, allBundles.size());

        final Bucket bundlesBucket = createdBuckets.get(1);
        final ExtensionBundleVersionClient bundleVersionClient = client.getExtensionBundleVersionClient();

        // create version 1.0.0 of nifi-test-nar
        final String testNar1 = "src/test/resources/extensions/nars/nifi-test-nar-1.0.0.nar";
        final ExtensionBundleVersion createdTestNarV1 = createExtensionBundleVersionWithStream(bundlesBucket, bundleVersionClient, testNar1, null);

        final ExtensionBundle testNarV1Bundle = createdTestNarV1.getExtensionBundle();
        LOGGER.info("Created bundle with id {}", new Object[]{testNarV1Bundle.getIdentifier()});

        Assert.assertEquals("org.apache.nifi", testNarV1Bundle.getGroupId());
        Assert.assertEquals("nifi-test-nar", testNarV1Bundle.getArtifactId());
        Assert.assertEquals(ExtensionBundleType.NIFI_NAR, testNarV1Bundle.getBundleType());
        Assert.assertEquals(1, testNarV1Bundle.getVersionCount());

        Assert.assertEquals("org.apache.nifi:nifi-test-nar", testNarV1Bundle.getName());
        Assert.assertEquals(bundlesBucket.getIdentifier(), testNarV1Bundle.getBucketIdentifier());
        Assert.assertEquals(bundlesBucket.getName(), testNarV1Bundle.getBucketName());
        Assert.assertNotNull(testNarV1Bundle.getPermissions());
        Assert.assertTrue(testNarV1Bundle.getCreatedTimestamp() > 0);
        Assert.assertTrue(testNarV1Bundle.getModifiedTimestamp() > 0);

        final ExtensionBundleVersionMetadata testNarV1Metadata = createdTestNarV1.getVersionMetadata();
        Assert.assertEquals("1.0.0", testNarV1Metadata.getVersion());
        Assert.assertNotNull(testNarV1Metadata.getId());
        Assert.assertNotNull(testNarV1Metadata.getSha256());
        Assert.assertNotNull(testNarV1Metadata.getAuthor());
        Assert.assertEquals(testNarV1Bundle.getIdentifier(), testNarV1Metadata.getExtensionBundleId());
        Assert.assertEquals(bundlesBucket.getIdentifier(), testNarV1Metadata.getBucketId());
        Assert.assertTrue(testNarV1Metadata.getTimestamp() > 0);
        Assert.assertFalse(testNarV1Metadata.getSha256Supplied());
        Assert.assertTrue(testNarV1Metadata.getContentSize() > 1);

        final Set<ExtensionBundleVersionDependency> dependencies = createdTestNarV1.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());

        final ExtensionBundleVersionDependency testNarV1Dependency = dependencies.stream().findFirst().get();
        Assert.assertEquals("org.apache.nifi", testNarV1Dependency.getGroupId());
        Assert.assertEquals("nifi-test-api-nar", testNarV1Dependency.getArtifactId());
        Assert.assertEquals("1.0.0", testNarV1Dependency.getVersion());

        final String testNar2 = "src/test/resources/extensions/nars/nifi-test-nar-2.0.0.nar";

        // try to create version 2.0.0 of nifi-test-nar when the supplied SHA-256 does not match server's
        final String madeUpSha256 = "MADE-UP-SHA-256";
        try {
            createExtensionBundleVersionWithStream(bundlesBucket, bundleVersionClient, testNar2, madeUpSha256);
            Assert.fail("Should have thrown exception");
        } catch (Exception e) {
            // should have thrown exception from mismatched SHA-256
        }

        // create version 2.0.0 of nifi-test-nar using correct supplied SHA-256
        final String testNar2Sha256 = calculateSha256Hex(testNar2);
        final ExtensionBundleVersion createdTestNarV2 = createExtensionBundleVersionWithStream(bundlesBucket, bundleVersionClient, testNar2, testNar2Sha256);
        Assert.assertTrue(createdTestNarV2.getVersionMetadata().getSha256Supplied());

        final ExtensionBundle testNarV2Bundle = createdTestNarV2.getExtensionBundle();
        LOGGER.info("Created bundle with id {}", new Object[]{testNarV2Bundle.getIdentifier()});

        // create version 1.0.0 of nifi-foo-nar, use the file variant
        final String fooNar = "src/test/resources/extensions/nars/nifi-foo-nar-1.0.0.nar";
        final ExtensionBundleVersion createdFooNarV1 = createExtensionBundleVersionWithFile(bundlesBucket, bundleVersionClient, fooNar, null);
        Assert.assertFalse(createdFooNarV1.getVersionMetadata().getSha256Supplied());

        final ExtensionBundle fooNarV1Bundle = createdFooNarV1.getExtensionBundle();
        LOGGER.info("Created bundle with id {}", new Object[]{fooNarV1Bundle.getIdentifier()});

        // verify there are 2 bundles now
        final List<ExtensionBundle> allBundlesAfterCreate = bundleClient.getAll();
        Assert.assertEquals(2, allBundlesAfterCreate.size());

        // verify getting bundles by bucket
        Assert.assertEquals(2, bundleClient.getByBucket(bundlesBucket.getIdentifier()).size());
        Assert.assertEquals(0, bundleClient.getByBucket(flowsBucket.getIdentifier()).size());

        // verify getting bundles by id
        final ExtensionBundle retrievedBundle = bundleClient.get(testNarV1Bundle.getIdentifier());
        Assert.assertNotNull(retrievedBundle);
        Assert.assertEquals(testNarV1Bundle.getIdentifier(), retrievedBundle.getIdentifier());
        Assert.assertEquals(testNarV1Bundle.getGroupId(), retrievedBundle.getGroupId());
        Assert.assertEquals(testNarV1Bundle.getArtifactId(), retrievedBundle.getArtifactId());

        // verify getting list of version metadata for a bundle
        final List<ExtensionBundleVersionMetadata> bundleVersions = bundleVersionClient.getBundleVersions(testNarV1Bundle.getIdentifier());
        Assert.assertNotNull(bundleVersions);
        Assert.assertEquals(2, bundleVersions.size());

        // verify getting a bundle version by the bundle id + version string
        final ExtensionBundleVersion bundleVersion1 = bundleVersionClient.getBundleVersion(testNarV1Bundle.getIdentifier(), "1.0.0");
        Assert.assertNotNull(bundleVersion1);
        Assert.assertEquals("1.0.0", bundleVersion1.getVersionMetadata().getVersion());
        Assert.assertNotNull(bundleVersion1.getDependencies());
        Assert.assertEquals(1, bundleVersion1.getDependencies().size());

        final ExtensionBundleVersion bundleVersion2 = bundleVersionClient.getBundleVersion(testNarV1Bundle.getIdentifier(), "2.0.0");
        Assert.assertNotNull(bundleVersion2);
        Assert.assertEquals("2.0.0", bundleVersion2.getVersionMetadata().getVersion());

        // verify getting the input stream for a bundle version
        try (final InputStream bundleVersion1InputStream = bundleVersionClient.getBundleVersionContent(testNarV1Bundle.getIdentifier(), "1.0.0")) {
            final String sha256Hex = DigestUtils.sha256Hex(bundleVersion1InputStream);
            Assert.assertEquals(testNarV1Metadata.getSha256(), sha256Hex);
        }

        // verify writing a bundle version to an output stream
        final File targetDir = new File("./target");
        final File bundleFile = bundleVersionClient.writeBundleVersionContent(testNarV1Bundle.getIdentifier(), "1.0.0", targetDir);
        Assert.assertNotNull(bundleFile);

        try (final InputStream bundleInputStream = new FileInputStream(bundleFile)) {
            final String sha256Hex = DigestUtils.sha256Hex(bundleInputStream);
            Assert.assertEquals(testNarV1Metadata.getSha256(), sha256Hex);
        }

        // Verify deleting a bundle version
        final ExtensionBundleVersion deletedBundleVersion2 = bundleVersionClient.delete(testNarV1Bundle.getIdentifier(), "2.0.0");
        Assert.assertNotNull(deletedBundleVersion2);
        Assert.assertEquals(testNarV1Bundle.getIdentifier(), deletedBundleVersion2.getExtensionBundle().getIdentifier());
        Assert.assertEquals("2.0.0", deletedBundleVersion2.getVersionMetadata().getVersion());

        try {
            bundleVersionClient.getBundleVersion(testNarV1Bundle.getIdentifier(), "2.0.0");
            Assert.fail("Should have thrown exception");
        } catch (Exception e) {
            // should catch exception
        }

        // Verify getting bundles with filter params
        Assert.assertEquals(2, bundleClient.getAll(ExtensionBundleFilterParams.empty()).size());

        final List<ExtensionBundle> filteredBundles = bundleClient.getAll(ExtensionBundleFilterParams.of("org.apache.nifi", "nifi-test-nar"));
        Assert.assertEquals(1, filteredBundles.size());

        // ---------------------- TEST EXTENSION REPO ----------------------//

        final ExtensionRepoClient extensionRepoClient = client.getExtensionRepoClient();

        final List<ExtensionRepoBucket> repoBuckets = extensionRepoClient.getBuckets();
        Assert.assertEquals(createdBuckets.size(), repoBuckets.size());

        final String bundlesBucketName = bundlesBucket.getName();
        final List<ExtensionRepoGroup> repoGroups = extensionRepoClient.getGroups(bundlesBucketName);
        Assert.assertEquals(1, repoGroups.size());

        final String repoGroupId = "org.apache.nifi";
        final ExtensionRepoGroup repoGroup = repoGroups.get(0);
        Assert.assertEquals(repoGroupId, repoGroup.getGroupId());

        final List<ExtensionRepoArtifact> repoArtifacts = extensionRepoClient.getArtifacts(bundlesBucketName, repoGroupId);
        Assert.assertEquals(2, repoArtifacts.size());

        final String repoArtifactId = "nifi-test-nar";
        final List<ExtensionRepoVersionSummary> repoVersions = extensionRepoClient.getVersions(bundlesBucketName, repoGroupId, repoArtifactId);
        Assert.assertEquals(1, repoVersions.size());

        final String repoVersionString = "1.0.0";
        final ExtensionRepoVersion repoVersion = extensionRepoClient.getVersion(bundlesBucketName, repoGroupId, repoArtifactId, repoVersionString);
        Assert.assertNotNull(repoVersion);
        Assert.assertNotNull(repoVersion.getDownloadLink());
        Assert.assertNotNull(repoVersion.getSha256Link());

        // verify the version links for content and sha256
        final Client jerseyClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();

        final WebTarget downloadLinkTarget = jerseyClient.target(repoVersion.getDownloadLink().getUri());
        try (final InputStream downloadLinkInputStream = downloadLinkTarget.request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get().readEntity(InputStream.class)) {
            final String sha256DownloadResult = DigestUtils.sha256Hex(downloadLinkInputStream);

            final WebTarget sha256LinkTarget = jerseyClient.target(repoVersion.getSha256Link().getUri());
            final String sha256LinkResult = sha256LinkTarget.request().get(String.class);
            Assert.assertEquals(sha256DownloadResult, sha256LinkResult);
        }

        // verify the client methods for content input stream and content sha256
        try (final InputStream repoVersionInputStream = extensionRepoClient.getVersionContent(bundlesBucketName, repoGroupId, repoArtifactId, repoVersionString)) {
            final String sha256Hex = DigestUtils.sha256Hex(repoVersionInputStream);
            final String repoSha256Hex = extensionRepoClient.getVersionSha256(bundlesBucketName, repoGroupId, repoArtifactId, repoVersionString);
            Assert.assertEquals(sha256Hex, repoSha256Hex);
        }

        // ---------------------- TEST ITEMS -------------------------- //

        final ItemsClient itemsClient = client.getItemsClient();

        // get fields
        final Fields itemFields = itemsClient.getFields();
        Assert.assertNotNull(itemFields.getFields());
        Assert.assertTrue(itemFields.getFields().size() > 0);

        // get all items
        final List<BucketItem> allItems = itemsClient.getAll();
        Assert.assertEquals(4, allItems.size());
        allItems.stream().forEach(i -> {
            Assert.assertNotNull(i.getBucketName());
            Assert.assertNotNull(i.getLink());
        });
        allItems.stream().forEach(i -> LOGGER.info("All items, item " + i.getIdentifier()));

        // verify 2 flow items
        final List<BucketItem> flowItems = allItems.stream()
                .filter(i -> i.getType() == BucketItemType.Flow)
                .collect(Collectors.toList());
        Assert.assertEquals(2, flowItems.size());

        // verify 2 bundle items
        final List<BucketItem> extensionBundleItems = allItems.stream()
                .filter(i -> i.getType() == BucketItemType.Extension_Bundle)
                .collect(Collectors.toList());
        Assert.assertEquals(2, extensionBundleItems.size());

        // get items for bucket
        final List<BucketItem> bucketItems = itemsClient.getByBucket(flowsBucket.getIdentifier());
        Assert.assertEquals(2, bucketItems.size());
        allItems.stream().forEach(i -> Assert.assertNotNull(i.getBucketName()));
        bucketItems.stream().forEach(i -> LOGGER.info("Items in bucket, item " + i.getIdentifier()));

        // ----------------------- TEST DIFF ---------------------------//

        final VersionedFlowSnapshot snapshot3 = buildSnapshot(snapshotFlow, 3);
        final VersionedProcessGroup newlyAddedPG = new VersionedProcessGroup();
        newlyAddedPG.setIdentifier("new-pg");
        newlyAddedPG.setName("NEW Process Group");
        snapshot3.getFlowContents().getProcessGroups().add(newlyAddedPG);
        snapshotClient.create(snapshot3);

        VersionedFlowDifference diff = flowClient.diff(snapshotFlow.getBucketIdentifier(), snapshotFlow.getIdentifier(), 3, 2);
        Assert.assertNotNull(diff);
        Assert.assertEquals(1, diff.getComponentDifferenceGroups().size());

        // ---------------------- DELETE DATA --------------------------//

        final VersionedFlow deletedFlow1 = flowClient.delete(flowsBucket.getIdentifier(), flow1.getIdentifier());
        Assert.assertNotNull(deletedFlow1);
        LOGGER.info("Deleted flow " + deletedFlow1.getIdentifier());

        final VersionedFlow deletedFlow2 = flowClient.delete(flowsBucket.getIdentifier(), flow2.getIdentifier());
        Assert.assertNotNull(deletedFlow2);
        LOGGER.info("Deleted flow " + deletedFlow2.getIdentifier());

        final ExtensionBundle deletedBundle1 = bundleClient.delete(testNarV1Bundle.getIdentifier());
        Assert.assertNotNull(deletedBundle1);
        LOGGER.info("Deleted extension bundle " + deletedBundle1.getIdentifier());

        final ExtensionBundle deletedBundle2 = bundleClient.delete(fooNarV1Bundle.getIdentifier());
        Assert.assertNotNull(deletedBundle2);
        LOGGER.info("Deleted extension bundle " + deletedBundle2.getIdentifier());

        // delete each bucket
        for (final Bucket bucket : createdBuckets) {
            final Bucket deletedBucket = bucketClient.delete(bucket.getIdentifier());
            Assert.assertNotNull(deletedBucket);
            LOGGER.info("Deleted bucket " + deletedBucket.getIdentifier());
        }
        Assert.assertEquals(0, bucketClient.getAll().size());

        LOGGER.info("!!! SUCCESS !!!");

    }

    private ExtensionBundleVersion createExtensionBundleVersionWithStream(final Bucket bundlesBucket,
                                                                          final ExtensionBundleVersionClient bundleVersionClient,
                                                                          final String narFile, final String sha256)
            throws IOException, NiFiRegistryException {

        final ExtensionBundleVersion createdBundleVersion;
        try (final InputStream bundleInputStream = new FileInputStream(narFile)) {
            if (StringUtils.isBlank(sha256)) {
                createdBundleVersion = bundleVersionClient.create(
                        bundlesBucket.getIdentifier(), ExtensionBundleType.NIFI_NAR, bundleInputStream);
            } else {
                createdBundleVersion = bundleVersionClient.create(
                        bundlesBucket.getIdentifier(), ExtensionBundleType.NIFI_NAR, bundleInputStream, sha256);
            }
        }

        Assert.assertNotNull(createdBundleVersion);
        Assert.assertNotNull(createdBundleVersion.getBucket());
        Assert.assertNotNull(createdBundleVersion.getExtensionBundle());
        Assert.assertNotNull(createdBundleVersion.getVersionMetadata());

        return createdBundleVersion;
    }

    private ExtensionBundleVersion createExtensionBundleVersionWithFile(final Bucket bundlesBucket,
                                                                        final ExtensionBundleVersionClient bundleVersionClient,
                                                                        final String narFile, final String sha256)
            throws IOException, NiFiRegistryException {

        final ExtensionBundleVersion createdBundleVersion;
        if (StringUtils.isBlank(sha256)) {
            createdBundleVersion = bundleVersionClient.create(
                    bundlesBucket.getIdentifier(), ExtensionBundleType.NIFI_NAR, new File(narFile));
        } else {
            createdBundleVersion = bundleVersionClient.create(
                    bundlesBucket.getIdentifier(), ExtensionBundleType.NIFI_NAR, new File(narFile), sha256);
        }

        Assert.assertNotNull(createdBundleVersion);
        Assert.assertNotNull(createdBundleVersion.getBucket());
        Assert.assertNotNull(createdBundleVersion.getExtensionBundle());
        Assert.assertNotNull(createdBundleVersion.getVersionMetadata());

        return createdBundleVersion;
    }

    private String calculateSha256Hex(final String narFile) throws IOException {
        try (final InputStream bundleInputStream = new FileInputStream(narFile)) {
            return Hex.toHexString(DigestUtils.sha256(bundleInputStream));
        }
    }

    private static Bucket createBucket(BucketClient bucketClient, int num) throws IOException, NiFiRegistryException {
        final Bucket bucket = new Bucket();
        bucket.setName("Bucket #" + num);
        bucket.setDescription("This is bucket #" + num);
        return bucketClient.create(bucket);
    }

    private static VersionedFlow createFlow(FlowClient client, Bucket bucket, int num) throws IOException, NiFiRegistryException {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName(bucket.getName() + " Flow #" + num);
        versionedFlow.setDescription("This is " + bucket.getName() + " flow #" + num);
        versionedFlow.setBucketIdentifier(bucket.getIdentifier());
        return client.create(versionedFlow);
    }

    private static VersionedFlowSnapshot buildSnapshot(VersionedFlow flow, int num) {
        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setBucketIdentifier(flow.getBucketIdentifier());
        snapshotMetadata.setFlowIdentifier(flow.getIdentifier());
        snapshotMetadata.setVersion(num);
        snapshotMetadata.setComments("This is snapshot #" + num);

        final VersionedProcessGroup rootProcessGroup = new VersionedProcessGroup();
        rootProcessGroup.setIdentifier("root-pg");
        rootProcessGroup.setName("Root Process Group");

        final VersionedProcessGroup subProcessGroup = new VersionedProcessGroup();
        subProcessGroup.setIdentifier("sub-pg");
        subProcessGroup.setName("Sub Process Group");
        rootProcessGroup.getProcessGroups().add(subProcessGroup);

        final Map<String,String> processorProperties = new HashMap<>();
        processorProperties.put("Prop 1", "Val 1");
        processorProperties.put("Prop 2", "Val 2");

        final Map<String, VersionedPropertyDescriptor> propertyDescriptors = new HashMap<>();

        final VersionedProcessor processor1 = new VersionedProcessor();
        processor1.setIdentifier("p1");
        processor1.setName("Processor 1");
        processor1.setProperties(processorProperties);
        processor1.setPropertyDescriptors(propertyDescriptors);

        final VersionedProcessor processor2 = new VersionedProcessor();
        processor2.setIdentifier("p2");
        processor2.setName("Processor 2");
        processor2.setProperties(processorProperties);
        processor2.setPropertyDescriptors(propertyDescriptors);

        subProcessGroup.getProcessors().add(processor1);
        subProcessGroup.getProcessors().add(processor2);

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(rootProcessGroup);
        return snapshot;
    }

    private static VersionedFlowSnapshot createSnapshot(FlowSnapshotClient client, VersionedFlow flow, int num) throws IOException, NiFiRegistryException {
        final VersionedFlowSnapshot snapshot = buildSnapshot(flow, num);

        return client.create(snapshot);
    }
}
