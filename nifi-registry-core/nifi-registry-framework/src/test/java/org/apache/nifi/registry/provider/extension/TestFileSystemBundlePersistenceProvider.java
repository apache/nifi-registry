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
package org.apache.nifi.registry.provider.extension;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.registry.extension.BundlePersistenceException;
import org.apache.nifi.registry.extension.BundlePersistenceProvider;
import org.apache.nifi.registry.extension.BundleContext;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class TestFileSystemBundlePersistenceProvider {

    static final String EXTENSION_STORAGE_DIR = "target/extension_storage";

    static final ProviderConfigurationContext CONFIGURATION_CONTEXT = new ProviderConfigurationContext() {
        @Override
        public Map<String, String> getProperties() {
            final Map<String,String> props = new HashMap<>();
            props.put(FileSystemBundlePersistenceProvider.BUNDLE_STORAGE_DIR_PROP, EXTENSION_STORAGE_DIR);
            return props;
        }
    };

    private File bundleStorageDir;
    private BundlePersistenceProvider fileSystemBundleProvider;

    @Before
    public void setup() throws IOException {
        bundleStorageDir = new File(EXTENSION_STORAGE_DIR);
        if (bundleStorageDir.exists()) {
            org.apache.commons.io.FileUtils.cleanDirectory(bundleStorageDir);
            bundleStorageDir.delete();
        }

        Assert.assertFalse(bundleStorageDir.exists());

        fileSystemBundleProvider = new FileSystemBundlePersistenceProvider();
        fileSystemBundleProvider.onConfigured(CONFIGURATION_CONTEXT);
        Assert.assertTrue(bundleStorageDir.exists());
    }

    @Test
    public void testSaveSuccessfully() throws IOException {
        // first version in b1
        final String content1 = "g1-a1-1.0.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, "b1", "g1", "a1", "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);
        verifyBundleVersion(bundleStorageDir, "b1", "g1", "a1", "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);

        // second version in b1
        final String content2 = "g1-a1-1.1.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, "b1", "g1", "a1", "1.1.0",
                BundleContext.BundleType.NIFI_NAR, content2);
        verifyBundleVersion(bundleStorageDir, "b1", "g1", "a1", "1.1.0",
                BundleContext.BundleType.NIFI_NAR, content2);

        // same bundle but in b2
        final String content3 = "g1-a1-1.1.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, "b2", "g1", "a1", "1.1.0",
                BundleContext.BundleType.NIFI_NAR, content3);
        verifyBundleVersion(bundleStorageDir, "b2", "g1", "a1", "1.1.0",
                BundleContext.BundleType.NIFI_NAR, content2);
    }

    @Test
    public void testSaveWhenBundleVersionAlreadyExists() throws IOException {
        final String content1 = "g1-a1-1.0.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, "b1", "g1", "a1", "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);
        verifyBundleVersion(bundleStorageDir, "b1", "g1", "a1", "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);

        // try to save same bundle version that already exists
        try {
            final String newContent = "new content";
            createAndSaveBundleVersion(fileSystemBundleProvider, "b1", "g1", "a1", "1.0.0",
                    BundleContext.BundleType.NIFI_NAR, newContent);
            Assert.fail("Should have thrown exception");
        } catch (BundlePersistenceException e) {

        }

        // verify existing content wasn't modified
        verifyBundleVersion(bundleStorageDir, "b1", "g1", "a1", "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);
    }

    @Test
    public void testSaveAndGet() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";

        final String content1 = groupId + "-" + artifactId + "-" + "1.0.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, bucketName, groupId, artifactId, "1.0.0",
                BundleContext.BundleType.NIFI_NAR, content1);

        final String content2 = groupId + "-" + artifactId + "-" + "1.1.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, bucketName, groupId, artifactId, "1.1.0",
                BundleContext.BundleType.NIFI_NAR, content2);

        try (final OutputStream out = new ByteArrayOutputStream()) {
            final BundleContext context = getExtensionBundleContext(
                    bucketName, groupId, artifactId, "1.0.0", BundleContext.BundleType.NIFI_NAR);
            fileSystemBundleProvider.getBundleVersion(context, out);

            final String retrievedContent1 = new String(((ByteArrayOutputStream) out).toByteArray(), StandardCharsets.UTF_8);
            Assert.assertEquals(content1, retrievedContent1);
        }

        try (final OutputStream out = new ByteArrayOutputStream()) {
            final BundleContext context = getExtensionBundleContext(
                    bucketName, groupId, artifactId, "1.1.0", BundleContext.BundleType.NIFI_NAR);
            fileSystemBundleProvider.getBundleVersion(context, out);

            final String retrievedContent2 = new String(((ByteArrayOutputStream) out).toByteArray(), StandardCharsets.UTF_8);
            Assert.assertEquals(content2, retrievedContent2);
        }
    }

    @Test(expected = BundlePersistenceException.class)
    public void testGetWhenDoesNotExist() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";

        try (final OutputStream out = new ByteArrayOutputStream()) {
            final BundleContext context = getExtensionBundleContext(
                    bucketName, groupId, artifactId, "1.0.0", BundleContext.BundleType.NIFI_NAR);
            fileSystemBundleProvider.getBundleVersion(context, out);
            Assert.fail("Should have thrown exception");
        }
    }

    @Test
    public void testDeleteExtensionBundleVersion() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";
        final String version = "1.0.0";
        final BundleContext.BundleType bundleType = BundleContext.BundleType.NIFI_NAR;

        // create and verify the bundle version
        final String content1 = groupId + "-" + artifactId + "-" + "1.0.0";
        createAndSaveBundleVersion(fileSystemBundleProvider, bucketName, groupId, artifactId, version, bundleType, content1);
        verifyBundleVersion(bundleStorageDir, bucketName, groupId, artifactId, version, bundleType, content1);

        // delete the bundle version
        fileSystemBundleProvider.deleteBundleVersion(getExtensionBundleContext(bucketName, groupId, artifactId, version, bundleType));

        // verify it was deleted
        final File bundleVersionDir = FileSystemBundlePersistenceProvider.getBundleVersionDirectory(
                bundleStorageDir, bucketName, groupId, artifactId, version);

        final File bundleFile = FileSystemBundlePersistenceProvider.getBundleFile(
                bundleVersionDir, artifactId, version, bundleType);
        Assert.assertFalse(bundleFile.exists());
    }

    @Test
    public void testDeleteExtensionBundleVersionWhenDoesNotExist() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";
        final String version = "1.0.0";
        final BundleContext.BundleType bundleType = BundleContext.BundleType.NIFI_NAR;

        // verify the bundle version does not already exist
        final File bundleVersionDir = FileSystemBundlePersistenceProvider.getBundleVersionDirectory(
                bundleStorageDir, bucketName, groupId, artifactId, version);

        final File bundleFile = FileSystemBundlePersistenceProvider.getBundleFile(
                bundleVersionDir, artifactId, version, bundleType);
        Assert.assertFalse(bundleFile.exists());

        // delete the bundle version
        fileSystemBundleProvider.deleteBundleVersion(getExtensionBundleContext(bucketName, groupId, artifactId, version, bundleType));
    }

    @Test
    public void testDeleteAllBundleVersions() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";
        final String version1 = "1.0.0";
        final String version2 = "2.0.0";
        final BundleContext.BundleType bundleType = BundleContext.BundleType.NIFI_NAR;

        // create and verify the bundle version 1
        final String content1 = groupId + "-" + artifactId + "-" + version1;
        createAndSaveBundleVersion(fileSystemBundleProvider, bucketName, groupId, artifactId, version1, bundleType, content1);
        verifyBundleVersion(bundleStorageDir, bucketName, groupId, artifactId, version1, bundleType, content1);

        // create and verify the bundle version 2
        final String content2 = groupId + "-" + artifactId + "-" + version2;
        createAndSaveBundleVersion(fileSystemBundleProvider, bucketName, groupId, artifactId, version2, bundleType, content2);
        verifyBundleVersion(bundleStorageDir, bucketName, groupId, artifactId, version2, bundleType, content2);

        fileSystemBundleProvider.deleteAllBundleVersions(bucketName, bucketName, groupId, artifactId);
        Assert.assertEquals(0, bundleStorageDir.listFiles().length);
    }

    @Test
    public void testDeleteAllBundleVersionsWhenDoesNotExist() throws IOException {
        final String bucketName = "b1";
        final String groupId = "g1";
        final String artifactId = "a1";

        Assert.assertEquals(0, bundleStorageDir.listFiles().length);
        fileSystemBundleProvider.deleteAllBundleVersions(bucketName, bucketName, groupId, artifactId);
        Assert.assertEquals(0, bundleStorageDir.listFiles().length);
    }

    private void createAndSaveBundleVersion(final BundlePersistenceProvider persistenceProvider,
                                            final String bucketName,
                                            final String groupId,
                                            final String artifactId,
                                            final String version,
                                            final BundleContext.BundleType bundleType,
                                            final String content) throws IOException {

        final BundleContext context = getExtensionBundleContext(bucketName, groupId, artifactId, version, bundleType);

        try (final InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            persistenceProvider.saveBundleVersion(context, in, false);
        }
    }

    private static BundleContext getExtensionBundleContext(final String bucketName,
                                                           final String groupId,
                                                           final String artifactId,
                                                           final String version,
                                                           final BundleContext.BundleType bundleType) {
        final BundleContext context = Mockito.mock(BundleContext.class);
        when(context.getBucketName()).thenReturn(bucketName);
        when(context.getBundleGroupId()).thenReturn(groupId);
        when(context.getBundleArtifactId()).thenReturn(artifactId);
        when(context.getBundleVersion()).thenReturn(version);
        when(context.getBundleType()).thenReturn(bundleType);
        return context;
    }

    private static void verifyBundleVersion(final File storageDir,
                                     final String bucketName,
                                     final String groupId,
                                     final String artifactId,
                                     final String version,
                                     final BundleContext.BundleType bundleType,
                                     final String contentString) throws IOException {

        final File bundleVersionDir = FileSystemBundlePersistenceProvider.getBundleVersionDirectory(
                storageDir, bucketName, groupId, artifactId, version);

        final File bundleFile = FileSystemBundlePersistenceProvider.getBundleFile(
                bundleVersionDir, artifactId, version, bundleType);
        Assert.assertTrue(bundleFile.exists());

        try (InputStream in = new FileInputStream(bundleFile)) {
            Assert.assertEquals(contentString, IOUtils.toString(in, StandardCharsets.UTF_8));
        }
    }

}
