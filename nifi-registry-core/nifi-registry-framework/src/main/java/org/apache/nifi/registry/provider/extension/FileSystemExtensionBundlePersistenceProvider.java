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
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.extension.ExtensionBundleContext;
import org.apache.nifi.registry.extension.ExtensionBundlePersistenceException;
import org.apache.nifi.registry.extension.ExtensionBundlePersistenceProvider;
import org.apache.nifi.registry.flow.FlowPersistenceException;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * An {@link ExtensionBundlePersistenceProvider} that uses local file-system for storage.
 */
public class FileSystemExtensionBundlePersistenceProvider implements ExtensionBundlePersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemExtensionBundlePersistenceProvider.class);

    static final String BUNDLE_STORAGE_DIR_PROP = "Extension Bundle Storage Directory";

    static final String NAR_EXTENSION = ".nar";
    static final String CPP_EXTENSION = ".cpp";

    private File bundleStorageDir;

    @Override
    public void onConfigured(final ProviderConfigurationContext configurationContext)
            throws ProviderCreationException {
        final Map<String,String> props = configurationContext.getProperties();
        if (!props.containsKey(BUNDLE_STORAGE_DIR_PROP)) {
            throw new ProviderCreationException("The property " + BUNDLE_STORAGE_DIR_PROP + " must be provided");
        }

        final String bundleStorageDirValue = props.get(BUNDLE_STORAGE_DIR_PROP);
        if (StringUtils.isBlank(bundleStorageDirValue)) {
            throw new ProviderCreationException("The property " + BUNDLE_STORAGE_DIR_PROP + " cannot be null or blank");
        }

        try {
            bundleStorageDir = new File(bundleStorageDirValue);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(bundleStorageDir);
            LOGGER.info("Configured ExtensionBundlePersistenceProvider with Extension Bundle Storage Directory {}",
                    new Object[] {bundleStorageDir.getAbsolutePath()});
        } catch (IOException e) {
            throw new ProviderCreationException(e);
        }
    }

    @Override
    public synchronized void saveBundleVersion(final ExtensionBundleContext context, final InputStream contentStream, boolean overwrite)
            throws ExtensionBundlePersistenceException {

        final File bundleVersionDir = getBundleVersionDirectory(bundleStorageDir, context.getBucketName(),
                context.getBundleGroupId(), context.getBundleArtifactId(), context.getBundleVersion());
        try {
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(bundleVersionDir);
        } catch (IOException e) {
            throw new FlowPersistenceException("Error accessing directory for extension bundle version at "
                    + bundleVersionDir.getAbsolutePath(), e);
        }

        final File bundleFile = getBundleFile(bundleVersionDir, context.getBundleArtifactId(),
                context.getBundleVersion(), context.getBundleType());

        if (bundleFile.exists() && !overwrite) {
            throw new ExtensionBundlePersistenceException("Unable to save because an extension bundle already exists at "
                    + bundleFile.getAbsolutePath());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Writing extension bundle to {}", new Object[]{bundleFile.getAbsolutePath()});
        }

        try (final OutputStream out = new FileOutputStream(bundleFile)) {
            IOUtils.copy(contentStream, out);
            out.flush();
        } catch (Exception e) {
            throw new FlowPersistenceException("Unable to write bundle file to disk due to " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void getBundleVersion(final ExtensionBundleContext context, final OutputStream outputStream)
            throws ExtensionBundlePersistenceException {

        final File bundleVersionDir = getBundleVersionDirectory(bundleStorageDir, context.getBucketName(),
                context.getBundleGroupId(), context.getBundleArtifactId(), context.getBundleVersion());

        final File bundleFile = getBundleFile(bundleVersionDir, context.getBundleArtifactId(),
                context.getBundleVersion(), context.getBundleType());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reading extension bundle from {}", new Object[]{bundleFile.getAbsolutePath()});
        }

        try (final InputStream in = new FileInputStream(bundleFile);
             final BufferedInputStream bufIn = new BufferedInputStream(in)) {
            IOUtils.copy(bufIn, outputStream);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            throw new ExtensionBundlePersistenceException("Extension bundle content was not found for: " + bundleFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new ExtensionBundlePersistenceException("Error reading extension bundle content", e);
        }
    }

    @Override
    public synchronized void deleteBundleVersion(final ExtensionBundleContext context) throws ExtensionBundlePersistenceException {
        final File bundleVersionDir = getBundleVersionDirectory(bundleStorageDir, context.getBucketName(),
                context.getBundleGroupId(), context.getBundleArtifactId(), context.getBundleVersion());

        final File bundleFile = getBundleFile(bundleVersionDir, context.getBundleArtifactId(),
                context.getBundleVersion(), context.getBundleType());

        if (!bundleFile.exists()) {
            LOGGER.warn("Extension bundle content does not exist at {}", new Object[] {bundleFile.getAbsolutePath()});
            return;
        }

        final boolean deleted = bundleFile.delete();
        if (!deleted) {
            throw new ExtensionBundlePersistenceException("Unable to delete extension bundle content at " + bundleFile.getAbsolutePath());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleted extension bundle content at {}", new Object[] {bundleFile.getAbsolutePath()});
        }
    }

    @Override
    public synchronized void deleteAllBundleVersions(final String bucketId, final String bucketName, final String groupId, final String artifactId)
            throws ExtensionBundlePersistenceException {

        final File bundleDir = getBundleDirectory(bundleStorageDir, bucketName, groupId, artifactId);
        if (!bundleDir.exists()) {
            LOGGER.warn("Extension bundle directory does not exist at {}", new Object[] {bundleDir.getAbsolutePath()});
            return;
        }

        // delete everything under the bundle directory
        try {
            org.apache.commons.io.FileUtils.cleanDirectory(bundleDir);
        } catch (IOException e) {
            throw new FlowPersistenceException("Error deleting extension bundles at " + bundleDir.getAbsolutePath(), e);
        }

        // delete the directory for the bundle
        final boolean bundleDirDeleted = bundleDir.delete();
        if (!bundleDirDeleted) {
            LOGGER.error("Unable to delete extension bundle directory: " + bundleDir.getAbsolutePath());
        }

        // delete the directory for the group and bucket if there is nothing left
        final File groupDir = bundleDir.getParentFile();
        final File[] groupFiles = groupDir.listFiles();
        if (groupFiles.length == 0) {
            final boolean deletedGroup = groupDir.delete();
            if (!deletedGroup) {
                LOGGER.error("Unable to delete group directory: " + groupDir.getAbsolutePath());
            } else {
                final File bucketDir = groupDir.getParentFile();
                final File[] bucketFiles = bucketDir.listFiles();
                if (bucketFiles.length == 0){
                    final boolean deletedBucket = bucketDir.delete();
                    if (!deletedBucket) {
                        LOGGER.error("Unable to delete bucket directory: " + bucketDir.getAbsolutePath());
                    }
                }
            }
        }
    }

    static File getBundleDirectory(final File bundleStorageDir, final String bucketName, final String groupId, final String artifactId) {
        return new File(bundleStorageDir, sanitize(bucketName) + "/" + sanitize(groupId) + "/" + sanitize(artifactId));
    }

    static File getBundleVersionDirectory(final File bundleStorageDir, final String bucketName, final String groupId, final String artifactId, final String version) {
        return new File(bundleStorageDir, sanitize(bucketName) + "/" + sanitize(groupId) + "/" + sanitize(artifactId) + "/" + sanitize(version));
    }

    static File getBundleFile(final File parentDir, final String artifactId, final String version, final ExtensionBundleContext.BundleType bundleType) {
        final String bundleFileExtension = getBundleFileExtension(bundleType);
        final String bundleFilename = sanitize(artifactId) + "-" + sanitize(version) + bundleFileExtension;
        return new File(parentDir, bundleFilename);
    }

    static String sanitize(final String input) {
        return FileUtils.sanitizeFilename(input).trim().toLowerCase();
    }

    static String getBundleFileExtension(final ExtensionBundleContext.BundleType bundleType) {
        switch (bundleType) {
            case NIFI_NAR:
                return NAR_EXTENSION;
            case MINIFI_CPP:
                return CPP_EXTENSION;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
        }
    }

}
