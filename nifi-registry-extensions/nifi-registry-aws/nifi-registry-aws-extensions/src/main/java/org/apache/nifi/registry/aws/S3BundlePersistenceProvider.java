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
package org.apache.nifi.registry.aws;

import org.apache.nifi.registry.extension.BundleContext;
import org.apache.nifi.registry.extension.BundlePersistenceException;
import org.apache.nifi.registry.extension.BundlePersistenceProvider;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An {@link BundlePersistenceProvider} that uses AWS S3 for storage.
 */
public class S3BundlePersistenceProvider implements BundlePersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BundlePersistenceProvider.class);

    public static final String REGION_PROP = "Region";
    public static final String BUCKET_NAME_PROP = "Bucket Name";
    public static final String KEY_PREFIX_PROP = "Key Prefix";
    public static final String CREDENTIALS_PROVIDER_PROP = "Credentials Provider";
    public static final String ACCESS_KEY_PROP = "Access Key";
    public static final String SECRET_ACCESS_KEY_PROP = "Secret Access Key";

    public static final String NAR_EXTENSION = ".nar";
    public static final String CPP_EXTENSION = ".cpp";

    public enum CredentialProvider {
        STATIC,
        DEFAULT_CHAIN
    }

    private volatile S3Client s3Client;
    private volatile String s3BucketName;
    private volatile String s3KeyPrefix;

    @Override
    public void onConfigured(final ProviderConfigurationContext configurationContext) throws ProviderCreationException {
        s3BucketName = configurationContext.getProperties().get(BUCKET_NAME_PROP);
        if (StringUtils.isBlank(s3BucketName)) {
            throw new ProviderCreationException("The property '" + BUCKET_NAME_PROP + "' must be provided");
        }

        final String keyPrefixValue = configurationContext.getProperties().get(KEY_PREFIX_PROP);
        s3KeyPrefix = StringUtils.isBlank(keyPrefixValue) ? null : keyPrefixValue;

        s3Client = createS3Client(configurationContext);
    }

    protected S3Client createS3Client(final ProviderConfigurationContext configurationContext) {

        return S3Client.builder()
                .region(getRegion(configurationContext))
                .credentialsProvider(getCredentialsProvider(configurationContext))
                .build();
    }

    private Region getRegion(final ProviderConfigurationContext configurationContext) {
        final String regionValue = configurationContext.getProperties().get(REGION_PROP);
        if (StringUtils.isBlank(regionValue)) {
            throw new ProviderCreationException("The property '" + REGION_PROP + "' must be provided");
        }

        Region region = null;
        for (Region r : Region.regions()) {
            if (r.id().equals(regionValue)) {
                region = r;
                break;
            }
        }

        if (region == null) {
            LOGGER.warn("The provided region was not found in the list of known regions. This may indicate an invalid region, " +
                    "or may indicate a region that is newer than the known list of regions");
            region = Region.of(regionValue);
        }

        LOGGER.debug("Using region {}", new Object[] {region.id()});
        return region;
    }

    private AwsCredentialsProvider getCredentialsProvider(final ProviderConfigurationContext configurationContext) {
        final String credentialsProviderValue = configurationContext.getProperties().get(CREDENTIALS_PROVIDER_PROP);
        if (StringUtils.isBlank(credentialsProviderValue)) {
            throw new ProviderCreationException("The property '" + CREDENTIALS_PROVIDER_PROP + "' must be provided");
        }

        CredentialProvider credentialProvider;
        try {
            credentialProvider = CredentialProvider.valueOf(credentialsProviderValue);
        } catch (Exception e) {
            throw new ProviderCreationException("The property '" + CREDENTIALS_PROVIDER_PROP + "' must be one of ["
                    + CredentialProvider.STATIC + ", " + CredentialProvider.DEFAULT_CHAIN + " ]");
        }

        if (CredentialProvider.STATIC == credentialProvider) {
            final String accesKeyValue = configurationContext.getProperties().get(ACCESS_KEY_PROP);
            final String secretAccessKey = configurationContext.getProperties().get(SECRET_ACCESS_KEY_PROP);

            if (StringUtils.isBlank(accesKeyValue) || StringUtils.isBlank(secretAccessKey)) {
                throw new ProviderCreationException("The properties '" + ACCESS_KEY_PROP + "' and '" + SECRET_ACCESS_KEY_PROP
                        + "' must be provided when using " + CredentialProvider.STATIC + " credentials provider");
            }

            LOGGER.debug("Creating StaticCredentialsProvider");
            final AwsCredentials awsCredentials = AwsBasicCredentials.create(accesKeyValue, secretAccessKey);
            return StaticCredentialsProvider.create(awsCredentials);

        } else {
            LOGGER.debug("Creating DefaultCredentialsProvider");
            return DefaultCredentialsProvider.create();
        }
    }

    @Override
    public void saveBundleVersion(final BundleContext context, final InputStream contentStream, final boolean overwrite)
            throws BundlePersistenceException {
        final String key = getKey(context);
        LOGGER.debug("Saving bundle version to S3 in bucket '{}' with key '{}'", new Object[]{s3BucketName, key});

        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build();

        final RequestBody requestBody = RequestBody.fromInputStream(contentStream, context.getBundleSize());
        try {
            s3Client.putObject(request, requestBody);
            LOGGER.debug("Successfully saved bundle version to S3 bucket '{}' with key '{}'", new Object[]{s3BucketName, key});
        } catch (Exception e) {
            throw new BundlePersistenceException("Error saving bundle version to S3 due to: " + e.getMessage(), e);
        }
    }

    @Override
    public void getBundleVersion(final BundleContext context, final OutputStream outputStream)
            throws BundlePersistenceException {
        final String key = getKey(context);
        LOGGER.debug("Retrieving bundle version from S3 bucket '{}' with key '{}'", new Object[]{s3BucketName, key});

        final GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build();

        try (final ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            IoUtils.copy(response, outputStream);
            LOGGER.debug("Successfully retrieved bundle version from S3 bucket '{}' with key '{}'", new Object[]{s3BucketName, key});
        } catch (Exception e) {
            throw new BundlePersistenceException("Error retrieving bundle version from S3 due to: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBundleVersion(final BundleContext context) throws BundlePersistenceException {
        final String key = getKey(context);
        LOGGER.debug("Deleting bundle version from S3 bucket '{}' with key '{}'", new Object[]{s3BucketName, key});

        final DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
            LOGGER.debug("Successfully deleted bundle version from S3 bucket '{}' with key '{}'", new Object[]{s3BucketName, key});
        } catch (Exception e) {
            throw new BundlePersistenceException("Error deleting bundle version from S3 due to: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAllBundleVersions(final String bucketId, final String bucketName, final String groupId, final String artifactId)
            throws BundlePersistenceException {
        final String basePrefix = s3KeyPrefix == null ? "" : s3KeyPrefix + "/";
        final String prefix = basePrefix + sanitize(groupId) + "/" + sanitize(artifactId) + "/";
        LOGGER.debug("Deleting all bundle versions from S3 bucket '{}' with prefix '{}'", new Object[]{bucketName, prefix});

        try {
            // List all the objects in the bucket with the given prefix of group/artifact...
            final ListObjectsResponse objectsResponse = s3Client.listObjects(
                    ListObjectsRequest.builder()
                            .bucket(s3BucketName)
                            .prefix(prefix)
                            .build()
            );

            // Now delete each object, might be able to do this more efficiently with bulk delete
            for (final S3Object s3Object : objectsResponse.contents()) {
                final String s3ObjectKey = s3Object.key();
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(s3BucketName)
                        .key(s3ObjectKey)
                        .build()
                );
                LOGGER.debug("Successfully object from S3 bucket '{}' with key '{}'", new Object[]{bucketName, s3ObjectKey});
            }

            LOGGER.debug("Successfully deleted all bundle versions from S3 bucket '{}' with prefix '{}'", new Object[]{bucketName, prefix});
        } catch (Exception e) {
            throw new BundlePersistenceException("Error deleting bundle versions from S3 due to: " + e.getMessage(), e);
        }
    }

    @Override
    public void preDestruction() {
        s3Client.close();
    }

    private String getKey(final BundleContext context) {
        final String sanitizedGroup = sanitize(context.getBundleGroupId());
        final String sanitizedArtifact = sanitize(context.getBundleArtifactId());
        final String sanitizedVersion = sanitize(context.getBundleVersion());

        final String bundleFileExtension = getBundleFileExtension(context.getBundleType());
        final String bundleFilename = sanitizedArtifact + "-" + sanitizedVersion + bundleFileExtension;

        final String key = sanitizedGroup + "/" + sanitizedArtifact + "/" + sanitizedVersion + "/" + bundleFilename;

        if (s3KeyPrefix == null) {
            return key;
        } else {
            return s3KeyPrefix + "/" + key;
        }
    }

    static String sanitize(final String input) {
        return FileUtils.sanitizeFilename(input).trim().toLowerCase();
    }

    static String getBundleFileExtension(final BundleContext.BundleType bundleType) {
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
