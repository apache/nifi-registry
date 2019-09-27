/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.gcp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.extension.BundleCoordinate;
import org.apache.nifi.registry.extension.BundlePersistenceContext;
import org.apache.nifi.registry.extension.BundlePersistenceException;
import org.apache.nifi.registry.extension.BundlePersistenceProvider;
import org.apache.nifi.registry.extension.BundleVersionCoordinate;
import org.apache.nifi.registry.extension.BundleVersionType;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.IOUtils;
import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

/**
 * An {@link BundlePersistenceProvider} that uses GCP storage.
 */
public class GCPBundlePersistenceProvider implements BundlePersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCPBundlePersistenceProvider.class);

    public static final String BUCKET_NAME_PROP = "Bucket Name";
    public static final String KEY_PREFIX_PROP = "Key Prefix";
    public static final String SERVICE_ACCOUNT_PROP = "Service Account Credentials";

    public static final String NAR_EXTENSION = ".nar";
    public static final String CPP_EXTENSION = ".cpp";

    private volatile Storage storageClient;
    private volatile String bucketName;
    private volatile String keyPrefix;

    @Override
    public void onConfigured(final ProviderConfigurationContext configurationContext) throws ProviderCreationException {
        try {
            storageClient = getClient(configurationContext);
        } catch (IOException e) {
            throw new ProviderCreationException("Cannot create the Google Cloud Storage client", e);
        }

        bucketName = configurationContext.getProperties().get(BUCKET_NAME_PROP);
        if (StringUtils.isBlank(bucketName)) {
            throw new ProviderCreationException("The property '" + BUCKET_NAME_PROP + "' must be provided");
        }

        final String keyPrefixValue = configurationContext.getProperties().get(KEY_PREFIX_PROP);
        keyPrefix = StringUtils.isBlank(keyPrefixValue) ? null : keyPrefixValue;
    }

    protected Storage getClient(final ProviderConfigurationContext configurationContext) throws IOException {
        final String serviceAccountFile = configurationContext.getProperties().get(SERVICE_ACCOUNT_PROP);
        final Credentials credentials = GoogleCredentials.fromStream(new BufferedInputStream(Files.newInputStream(Paths.get(serviceAccountFile))));
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }

    @Override
    public synchronized void createBundleVersion(final BundlePersistenceContext context, final InputStream contentStream)
            throws BundlePersistenceException {
        createOrUpdateBundleVersion(context, contentStream);
    }

    @Override
    public synchronized void updateBundleVersion(final BundlePersistenceContext context, final InputStream contentStream) throws BundlePersistenceException {
        createOrUpdateBundleVersion(context, contentStream);
    }

    private synchronized void createOrUpdateBundleVersion(final BundlePersistenceContext context, final InputStream contentStream) throws BundlePersistenceException {
        final String key = getKey(context.getCoordinate());
        LOGGER.debug("Saving bundle version to GCS in bucket '{}' with key '{}'", new Object[] { bucketName, key });

        final BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, key)).build();

        try {
            storageClient.create(blobInfo, contentStream);
            LOGGER.debug("Successfully saved bundle version to GCS bucket '{}' with key '{}'", new Object[] { bucketName, key });
        } catch (StorageException e) {
            throw new BundlePersistenceException("Error saving bundle version to GCS due to: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void getBundleVersionContent(final BundleVersionCoordinate versionCoordinate, final OutputStream outputStream) throws BundlePersistenceException {
        final String key = getKey(versionCoordinate);
        LOGGER.debug("Retrieving bundle version from GCS bucket '{}' with key '{}'", new Object[] { bucketName, key });

        try (final ReadChannel reader = storageClient.get(BlobId.of(bucketName, key)).reader()) {
            IOUtils.copy(Channels.newInputStream(reader), outputStream);
            LOGGER.debug("Successfully retrieved bundle version from GCS bucket '{}' with key '{}'", new Object[] { bucketName, key });
        } catch (StorageException | IOException e) {
            throw new BundlePersistenceException("Error retrieving bundle version from GCS due to: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void deleteBundleVersion(final BundleVersionCoordinate versionCoordinate) throws BundlePersistenceException {
        final String key = getKey(versionCoordinate);
        LOGGER.debug("Deleting bundle version from GCS bucket '{}' with key '{}'", new Object[] { bucketName, key });

        try {
            storageClient.delete(BlobId.of(bucketName, key));
            LOGGER.debug("Successfully deleted bundle version from GCS bucket '{}' with key '{}'", new Object[] { bucketName, key });
        } catch (Exception e) {
            throw new BundlePersistenceException("Error deleting bundle version from GCS due to: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void deleteAllBundleVersions(final BundleCoordinate bundleCoordinate) throws BundlePersistenceException {
        final String basePrefix = keyPrefix == null ? "" : keyPrefix + "/";
        final String bundlePrefix = getBundlePrefix(bundleCoordinate.getBucketId(), bundleCoordinate.getGroupId(), bundleCoordinate.getArtifactId());

        final String prefix = basePrefix + bundlePrefix;
        LOGGER.debug("Deleting all bundle versions from GCS bucket '{}' with prefix '{}'", new Object[] { bucketName, prefix });

        try {
            // List all the objects in the bucket with the given prefix of group/artifact...
            Page<Blob> list = storageClient.list(bucketName, BlobListOption.prefix(prefix));

            // Now delete each object, might be able to do this more efficiently with bulk delete
            for (final Blob blob : list.getValues()) {
                storageClient.delete(blob.getBlobId());
                LOGGER.debug("Successfully object from GCS bucket '{}' with key '{}'", new Object[] { bucketName, blob.getName() });
            }

            LOGGER.debug("Successfully deleted all bundle versions from GCS bucket '{}' with prefix '{}'", new Object[] { bucketName, prefix });
        } catch (Exception e) {
            throw new BundlePersistenceException("Error deleting bundle versions from GCS due to: " + e.getMessage(), e);
        }
    }

    private String getKey(final BundleVersionCoordinate coordinate) {
        final String bundlePrefix = getBundlePrefix(coordinate.getBucketId(), coordinate.getGroupId(), coordinate.getArtifactId());

        final String sanitizedArtifact = sanitize(coordinate.getArtifactId());
        final String sanitizedVersion = sanitize(coordinate.getVersion());

        final String bundleFileExtension = getBundleFileExtension(coordinate.getType());
        final String bundleFilename = sanitizedArtifact + "-" + sanitizedVersion + bundleFileExtension;

        final String key = bundlePrefix + "/" + sanitizedVersion + "/" + bundleFilename;
        if (keyPrefix == null) {
            return key;
        } else {
            return keyPrefix + "/" + key;
        }
    }

    private String getBundlePrefix(final String bucketId, final String groupId, final String artifactId) {
        final String sanitizedBucketId = sanitize(bucketId);
        final String sanitizedGroup = sanitize(groupId);
        final String sanitizedArtifact = sanitize(artifactId);
        return sanitizedBucketId + "/" + sanitizedGroup + "/" + sanitizedArtifact;
    }

    private static String sanitize(final String input) {
        return FileUtils.sanitizeFilename(input).trim().toLowerCase();
    }

    static String getBundleFileExtension(final BundleVersionType bundleType) {
        switch (bundleType) {
        case NIFI_NAR:
            return NAR_EXTENSION;
        case MINIFI_CPP:
            return CPP_EXTENSION;
        default:
            LOGGER.warn("Unknown bundle type: " + bundleType);
            return "";
        }
    }
}
