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
package org.apache.nifi.registry.service.extension;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionDependencyEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionEntity;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.extension.BundleCoordinate;
import org.apache.nifi.registry.extension.BundleDetails;
import org.apache.nifi.registry.extension.BundleExtractor;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleContext;
import org.apache.nifi.registry.extension.ExtensionBundlePersistenceProvider;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionDependency;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.filter.ExtensionBundleFilterParams;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.provider.extension.StandardExtensionBundleContext;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.service.DataModelMapper;
import org.apache.nifi.registry.service.MetadataService;
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StandardExtensionService implements ExtensionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardExtensionService.class);

    private final MetadataService metadataService;
    private final Map<ExtensionBundleType, BundleExtractor> extractors;
    private final ExtensionBundlePersistenceProvider bundlePersistenceProvider;
    private final Validator validator;
    private final File extensionsWorkingDir;

    @Autowired
    public StandardExtensionService(final MetadataService metadataService,
                                    final Map<ExtensionBundleType, BundleExtractor> extractors,
                                    final ExtensionBundlePersistenceProvider bundlePersistenceProvider,
                                    final Validator validator,
                                    final NiFiRegistryProperties properties) {
        this.metadataService = metadataService;
        this.extractors = extractors;
        this.bundlePersistenceProvider = bundlePersistenceProvider;
        this.validator = validator;
        this.extensionsWorkingDir = properties.getExtensionsWorkingDirectory();
        Validate.notNull(this.metadataService);
        Validate.notNull(this.extractors);
        Validate.notNull(this.bundlePersistenceProvider);
        Validate.notNull(this.validator);
        Validate.notNull(this.extensionsWorkingDir);
    }

    private <T>  void validate(T t, String invalidMessage) {
        final Set<ConstraintViolation<T>> violations = validator.validate(t);
        if (violations.size() > 0) {
            throw new ConstraintViolationException(invalidMessage, violations);
        }
    }

    @Override
    public ExtensionBundleVersion createExtensionBundleVersion(final String bucketIdentifier, final ExtensionBundleType bundleType,
                                                               final InputStream inputStream, final String clientSha256) throws IOException {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        if (bundleType == null) {
            throw new IllegalArgumentException("Bundle type cannot be null");
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("Extension bundle input stream cannot be null");
        }

        if (!extractors.containsKey(bundleType)) {
            throw new IllegalArgumentException("No metadata extractor is registered for bundle-type: " + bundleType);
        }

        // ensure the bucket exists
        final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
        if (existingBucket == null) {
            LOGGER.warn("The specified bucket id [{}] does not exist.", bucketIdentifier);
            throw new ResourceNotFoundException("The specified bucket ID does not exist in this registry.");
        }

        // ensure the extensions directory exists and we can read and write to it
        FileUtils.ensureDirectoryExistAndCanReadAndWrite(extensionsWorkingDir);

        final String extensionWorkingFilename = UUID.randomUUID().toString();
        final File extensionWorkingFile = new File(extensionsWorkingDir, extensionWorkingFilename);
        LOGGER.debug("Writing bundle contents to working directory at {}", new Object[]{extensionWorkingFile.getAbsolutePath()});

        try {
            // write the contents of the input stream to a temporary file in the extensions working directory
            final MessageDigest sha256Digest = DigestUtils.getSha256Digest();
            try (final DigestInputStream digestInputStream = new DigestInputStream(inputStream, sha256Digest);
                 final OutputStream out = new FileOutputStream(extensionWorkingFile)) {
                IOUtils.copy(digestInputStream, out);
            }

            // get the hex of the SHA-256 computed by the server and compare to the client provided SHA-256, if one was provided
            final String sha256Hex = Hex.encodeHexString(sha256Digest.digest());
            final boolean sha256Supplied = !StringUtils.isBlank(clientSha256);
            if (sha256Supplied && !sha256Hex.equalsIgnoreCase(clientSha256)) {
                LOGGER.error("Client provided SHA-256 of '{}', but server calculated '{}'", new Object[]{clientSha256, sha256Hex});
                throw new IllegalStateException("The SHA-256 of the received extension bundle does not match the SHA-256 provided by the client");
            }

            // extract the details of the bundle from the temp file in the working directory
            final BundleDetails bundleDetails;
            try (final InputStream in = new FileInputStream(extensionWorkingFile)) {
                final BundleExtractor extractor = extractors.get(bundleType);
                bundleDetails = extractor.extract(in);
            }

            final BundleCoordinate bundleCoordinate = bundleDetails.getBundleCoordinate();
            final Set<BundleCoordinate> dependencyCoordinates = bundleDetails.getDependencyBundleCoordinates();

            final String groupId = bundleCoordinate.getGroupId();
            final String artifactId = bundleCoordinate.getArtifactId();
            final String version = bundleCoordinate.getVersion();
            LOGGER.debug("Extracted bundle details - '{}' - '{}' - '{}'", new Object[]{groupId, artifactId, version});

            // a bundle with the same group, artifact, and version can exist in multiple buckets, but only if it contains the same binary content,
            // we can determine that by comparing the SHA-256 digest of the incoming bundle against existing bundles with the same group, artifact, version
            final List<ExtensionBundleVersionEntity> allExistingVersions = metadataService.getExtensionBundleVersionsGlobal(groupId, artifactId, version);
            for (final ExtensionBundleVersionEntity existingVersionEntity : allExistingVersions) {
                if (!existingVersionEntity.getSha256Hex().equals(sha256Hex)) {
                    throw new IllegalStateException("Found existing extension bundle with same group, artifact, and version, but different SHA-256 check-sum");
                }
            }

            // get the existing extension bundle entity, or create a new one if one does not exist in the bucket with the group + artifact
            final long currentTime = System.currentTimeMillis();
            final ExtensionBundleEntity extensionBundle = getOrCreateExtensionBundle(bucketIdentifier, groupId, artifactId, bundleType, currentTime);

            // ensure there isn't already a version of the bundle with the same version
            final ExtensionBundleVersionEntity existingVersion = metadataService.getExtensionBundleVersion(bucketIdentifier, groupId, artifactId, version);
            if (existingVersion != null) {
                LOGGER.warn("The specified version [{}] already exists for extension bundle [{}].", new Object[]{version, extensionBundle.getId()});
                throw new IllegalStateException("The specified version already exists for the given extension bundle");
            }

            // create the version metadata instance and validate it has all the required fields
            final String userIdentity = NiFiUserUtils.getNiFiUserIdentity();
            final ExtensionBundleVersionMetadata versionMetadata = new ExtensionBundleVersionMetadata();
            versionMetadata.setId(UUID.randomUUID().toString());
            versionMetadata.setExtensionBundleId(extensionBundle.getId());
            versionMetadata.setBucketId(bucketIdentifier);
            versionMetadata.setVersion(version);
            versionMetadata.setTimestamp(currentTime);
            versionMetadata.setAuthor(userIdentity);
            versionMetadata.setSha256(sha256Hex);
            versionMetadata.setSha256Supplied(sha256Supplied);
            versionMetadata.setContentSize(extensionWorkingFile.length());

            validate(versionMetadata, "Cannot create extension bundle version");

            // create the version dependency instances and validate they have the required fields
            final Set<ExtensionBundleVersionDependency> versionDependencies = new HashSet<>();
            for (final BundleCoordinate dependencyCoordinate : dependencyCoordinates) {
                final ExtensionBundleVersionDependency versionDependency = new ExtensionBundleVersionDependency();
                versionDependency.setGroupId(dependencyCoordinate.getGroupId());
                versionDependency.setArtifactId(dependencyCoordinate.getArtifactId());
                versionDependency.setVersion(dependencyCoordinate.getVersion());

                validate(versionDependency, "Cannot create extension bundle version dependency");
                versionDependencies.add(versionDependency);
            }

            // create the bundle version in the metadata db
            final ExtensionBundleVersionEntity versionEntity = DataModelMapper.map(versionMetadata);
            metadataService.createExtensionBundleVersion(versionEntity);

            // create the bundle version dependencies in the metadata db
            for (final ExtensionBundleVersionDependency versionDependency : versionDependencies) {
                final ExtensionBundleVersionDependencyEntity versionDependencyEntity = DataModelMapper.map(versionDependency);
                versionDependencyEntity.setId(UUID.randomUUID().toString());
                versionDependencyEntity.setExtensionBundleVersionId(versionEntity.getId());
                metadataService.createDependency(versionDependencyEntity);
            }

            // persist the content of the bundle to the persistence provider
            final ExtensionBundleContext context = new StandardExtensionBundleContext.Builder()
                    .bundleType(getProviderBundleType(bundleType))
                    .bucketId(existingBucket.getId())
                    .bucketName(existingBucket.getName())
                    .bundleId(extensionBundle.getId())
                    .bundleGroupId(extensionBundle.getGroupId())
                    .bundleArtifactId(extensionBundle.getArtifactId())
                    .bundleVersion(versionMetadata.getVersion())
                    .author(versionMetadata.getAuthor())
                    .timestamp(versionMetadata.getTimestamp())
                    .build();

            try (final InputStream in = new FileInputStream(extensionWorkingFile);
                 final InputStream bufIn = new BufferedInputStream(in)) {
                bundlePersistenceProvider.saveBundleVersion(context, bufIn);
                LOGGER.debug("Bundle saved to persistence provider - '{}' - '{}' - '{}'",
                        new Object[]{groupId, artifactId, version});
            }

            // get the updated extension bundle so it contains the correct version count
            final ExtensionBundleEntity updatedBundle = metadataService.getExtensionBundle(bucketIdentifier, groupId, artifactId);

            // create the full ExtensionBundleVersion instance to return
            final ExtensionBundleVersion extensionBundleVersion = new ExtensionBundleVersion();
            extensionBundleVersion.setVersionMetadata(versionMetadata);
            extensionBundleVersion.setExtensionBundle(DataModelMapper.map(existingBucket, updatedBundle));
            extensionBundleVersion.setBucket(DataModelMapper.map(existingBucket));
            extensionBundleVersion.setDependencies(versionDependencies);
            return extensionBundleVersion;

        } finally {
            if (extensionWorkingFile.exists()) {
                try {
                    extensionWorkingFile.delete();
                } catch (Exception e) {
                    LOGGER.warn("Error removing temporary extension bundle file at {}",
                            new Object[]{extensionWorkingFile.getAbsolutePath()});
                }
            }
        }
    }

    private ExtensionBundleEntity getOrCreateExtensionBundle(final String bucketId, final String groupId,
                                                             final String artifactId, final ExtensionBundleType bundleType,
                                                             final long currentTime) {
        ExtensionBundleEntity existingBundleEntity = metadataService.getExtensionBundle(bucketId, groupId, artifactId);
        if (existingBundleEntity == null) {
            final ExtensionBundle bundle = new ExtensionBundle();
            bundle.setIdentifier(UUID.randomUUID().toString());
            bundle.setBucketIdentifier(bucketId);
            bundle.setName(groupId + ":" + artifactId);
            bundle.setGroupId(groupId);
            bundle.setArtifactId(artifactId);
            bundle.setBundleType(bundleType);
            bundle.setCreatedTimestamp(currentTime);
            bundle.setModifiedTimestamp(currentTime);

            validate(bundle, "Cannot create extension bundle");
            existingBundleEntity = metadataService.createExtensionBundle(DataModelMapper.map(bundle));
        } else {
            final ExtensionBundleEntityType bundleEntityType = DataModelMapper.map(bundleType);
            if (bundleEntityType != existingBundleEntity.getBundleType()) {
                throw new IllegalStateException("A bundle already exists with the same group id and artifact id, but a different bundle type");
            }
        }

        return existingBundleEntity;
    }

    private ExtensionBundleContext.BundleType getProviderBundleType(final ExtensionBundleType bundleType) {
        switch (bundleType) {
            case NIFI_NAR:
                return ExtensionBundleContext.BundleType.NIFI_NAR;
            case MINIFI_CPP:
                return ExtensionBundleContext.BundleType.MINIFI_CPP;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleType.toString());
        }
    }

    @Override
    public List<ExtensionBundle> getExtensionBundles(final Set<String> bucketIdentifiers, final ExtensionBundleFilterParams filterParams) {
        if (bucketIdentifiers == null) {
            throw new IllegalArgumentException("Bucket identifiers cannot be null");
        }

        final List<ExtensionBundleEntity> bundleEntities = metadataService.getExtensionBundles(bucketIdentifiers,
                filterParams == null ? ExtensionBundleFilterParams.empty() : filterParams);
        return bundleEntities.stream().map(b -> DataModelMapper.map(null, b)).collect(Collectors.toList());
    }

    @Override
    public List<ExtensionBundle> getExtensionBundlesByBucket(final String bucketIdentifier) {
        if (StringUtils.isBlank(bucketIdentifier)) {
            throw new IllegalArgumentException("Bucket identifier cannot be null or blank");
        }

        // ensure the bucket exists
        final BucketEntity existingBucket = metadataService.getBucketById(bucketIdentifier);
        if (existingBucket == null) {
            LOGGER.warn("The specified bucket id [{}] does not exist.", bucketIdentifier);
            throw new ResourceNotFoundException("The specified bucket ID does not exist in this registry.");
        }

        final List<ExtensionBundleEntity> bundleEntities = metadataService.getExtensionBundlesByBucket(bucketIdentifier);
        return bundleEntities.stream().map(b -> DataModelMapper.map(existingBucket, b)).collect(Collectors.toList());
    }

    @Override
    public ExtensionBundle getExtensionBundle(final String extensionBundleIdentifier) {
        if (StringUtils.isBlank(extensionBundleIdentifier)) {
            throw new IllegalArgumentException("Extension bundle identifier cannot be null or blank");
        }

        final ExtensionBundleEntity existingBundle = metadataService.getExtensionBundle(extensionBundleIdentifier);
        if (existingBundle == null) {
            LOGGER.warn("The specified extension bundle id [{}] does not exist.", extensionBundleIdentifier);
            throw new ResourceNotFoundException("The specified extension bundle ID does not exist.");
        }

        final BucketEntity existingBucket = metadataService.getBucketById(existingBundle.getBucketId());
        return DataModelMapper.map(existingBucket, existingBundle);
    }

    @Override
    public ExtensionBundle deleteExtensionBundle(final ExtensionBundle extensionBundle) {
        if (extensionBundle == null) {
            throw new IllegalArgumentException("Extension bundle cannot be null");
        }

        // delete the bundle from the database
        metadataService.deleteExtensionBundle(extensionBundle.getIdentifier());

        // delete all content associated with the bundle in the persistence provider
        bundlePersistenceProvider.deleteAllBundleVersions(
                extensionBundle.getBucketIdentifier(),
                extensionBundle.getBucketName(),
                extensionBundle.getGroupId(),
                extensionBundle.getArtifactId());

        return extensionBundle;
    }

    @Override
    public SortedSet<ExtensionBundleVersionMetadata> getExtensionBundleVersions(final String extensionBundleIdentifier) {
        if (StringUtils.isBlank(extensionBundleIdentifier)) {
            throw new IllegalArgumentException("Extension bundle identifier cannot be null or blank");
        }

        // ensure the bundle exists
        final ExtensionBundleEntity existingBundle = metadataService.getExtensionBundle(extensionBundleIdentifier);
        if (existingBundle == null) {
            LOGGER.warn("The specified extension bundle id [{}] does not exist.", extensionBundleIdentifier);
            throw new ResourceNotFoundException("The specified extension bundle ID does not exist in this bucket.");
        }

        return getExtensionBundleVersionsSet(existingBundle);
    }

    private SortedSet<ExtensionBundleVersionMetadata> getExtensionBundleVersionsSet(ExtensionBundleEntity existingBundle) {
        final SortedSet<ExtensionBundleVersionMetadata> sortedVersions = new TreeSet<>(Collections.reverseOrder());

        final List<ExtensionBundleVersionEntity> existingVersions = metadataService.getExtensionBundleVersions(existingBundle.getId());
        if (existingVersions != null) {
            final BucketEntity existingBucket = metadataService.getBucketById(existingBundle.getBucketId());
            existingVersions.stream().forEach(s -> sortedVersions.add(DataModelMapper.map(existingBucket, s)));
        }

        return sortedVersions;
    }

    @Override
    public ExtensionBundleVersion getExtensionBundleVersion(ExtensionBundleVersionCoordinate versionCoordinate) {
        if (versionCoordinate == null) {
            throw new IllegalArgumentException("Extension bundle version coordinate cannot be null");
        }

        // ensure the bucket exists
        final BucketEntity existingBucket = metadataService.getBucketById(versionCoordinate.getBucketId());
        if (existingBucket == null) {
            LOGGER.warn("The specified bucket id [{}] does not exist.", versionCoordinate.getBucketId());
            throw new ResourceNotFoundException("The specified bucket ID does not exist in this registry.");
        }

        // ensure the bundle exists
        final ExtensionBundleEntity existingBundle = metadataService.getExtensionBundle(
                versionCoordinate.getBucketId(),
                versionCoordinate.getGroupId(),
                versionCoordinate.getArtifactId());

        if (existingBundle == null) {
            LOGGER.warn("The specified extension bundle [{}] does not exist.", versionCoordinate.toString());
            throw new ResourceNotFoundException("The specified extension bundle does not exist in this bucket.");
        }

        //ensure the version of the bundle exists
        final ExtensionBundleVersionEntity existingVersion = metadataService.getExtensionBundleVersion(
                versionCoordinate.getBucketId(),
                versionCoordinate.getGroupId(),
                versionCoordinate.getArtifactId(),
                versionCoordinate.getVersion());

        if (existingVersion == null) {
            LOGGER.warn("The specified extension bundle version [{}] does not exist.", versionCoordinate.toString());
            throw new ResourceNotFoundException("The specified extension bundle version does not exist in this bucket.");
        }

        // get the dependencies for the bundle version
        final List<ExtensionBundleVersionDependencyEntity> existingVersionDependencies = metadataService
                .getDependenciesForBundleVersion(existingVersion.getId());

        // convert the dependency db entities
        final Set<ExtensionBundleVersionDependency> dependencies = existingVersionDependencies.stream()
                .map(d -> DataModelMapper.map(d))
                .collect(Collectors.toSet());

        // create the full ExtensionBundleVersion instance to return
        final ExtensionBundleVersion extensionBundleVersion = new ExtensionBundleVersion();
        extensionBundleVersion.setVersionMetadata(DataModelMapper.map(existingBucket, existingVersion));
        extensionBundleVersion.setExtensionBundle(DataModelMapper.map(existingBucket, existingBundle));
        extensionBundleVersion.setBucket(DataModelMapper.map(existingBucket));
        extensionBundleVersion.setDependencies(dependencies);
        return extensionBundleVersion;
    }

    @Override
    public void writeExtensionBundleVersionContent(final ExtensionBundleVersion bundleVersion, final OutputStream out) {
        // get the content from the persistence provider and write it to the output stream
        final ExtensionBundleContext context = getExtensionBundleContext(bundleVersion);
        bundlePersistenceProvider.getBundleVersion(context, out);
    }

    @Override
    public ExtensionBundleVersion deleteExtensionBundleVersion(final ExtensionBundleVersion bundleVersion) {
        if (bundleVersion == null) {
            throw new IllegalArgumentException("Extension bundle version cannot be null");
        }

        // delete from the metadata db
        final String extensionBundleVersionId = bundleVersion.getVersionMetadata().getId();
        metadataService.deleteExtensionBundleVersion(extensionBundleVersionId);

        // delete content associated with the bundle version in the persistence provider
        final ExtensionBundleContext context = new StandardExtensionBundleContext.Builder()
                .bundleType(getProviderBundleType(bundleVersion.getExtensionBundle().getBundleType()))
                .bucketId(bundleVersion.getBucket().getIdentifier())
                .bucketName(bundleVersion.getBucket().getName())
                .bundleId(bundleVersion.getExtensionBundle().getIdentifier())
                .bundleGroupId(bundleVersion.getExtensionBundle().getGroupId())
                .bundleArtifactId(bundleVersion.getExtensionBundle().getArtifactId())
                .bundleVersion(bundleVersion.getVersionMetadata().getVersion())
                .author(bundleVersion.getVersionMetadata().getAuthor())
                .timestamp(bundleVersion.getVersionMetadata().getTimestamp())
                .build();

        bundlePersistenceProvider.deleteBundleVersion(context);

        return bundleVersion;
    }

    // ------ Extension Repository Methods -------

    @Override
    public SortedSet<ExtensionRepoBucket> getExtensionRepoBuckets(final Set<String> bucketIds) {
        if (bucketIds == null) {
            throw new IllegalArgumentException("Bucket ids cannot be null");
        }

        if (bucketIds.isEmpty()) {
            return new TreeSet<>();
        }

        final SortedSet<ExtensionRepoBucket> repoBuckets = new TreeSet<>();

        final List<BucketEntity> buckets = metadataService.getBuckets(bucketIds);
        buckets.forEach(b -> {
            final ExtensionRepoBucket repoBucket = new ExtensionRepoBucket();
            repoBucket.setBucketName(b.getName());
            repoBuckets.add(repoBucket);
        });

        return repoBuckets;
    }

    @Override
    public SortedSet<ExtensionRepoGroup> getExtensionRepoGroups(final Bucket bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        final SortedSet<ExtensionRepoGroup> repoGroups = new TreeSet<>();

        final List<ExtensionBundleEntity> bundleEntities = metadataService.getExtensionBundlesByBucket(bucket.getIdentifier());
        bundleEntities.forEach(b -> {
            final ExtensionRepoGroup repoGroup = new ExtensionRepoGroup();
            repoGroup.setBucketName(bucket.getName());
            repoGroup.setGroupId(b.getGroupId());
            repoGroups.add(repoGroup);
        });

        return repoGroups;
    }

    @Override
    public SortedSet<ExtensionRepoArtifact> getExtensionRepoArtifacts(final Bucket bucket, final String groupId) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("Group id cannot be null or blank");
        }

        final SortedSet<ExtensionRepoArtifact> repoArtifacts = new TreeSet<>();

        final List<ExtensionBundleEntity> bundleEntities = metadataService.getExtensionBundlesByBucketAndGroup(bucket.getIdentifier(), groupId);
        bundleEntities.forEach(b -> {
            final ExtensionRepoArtifact repoArtifact = new ExtensionRepoArtifact();
            repoArtifact.setBucketName(bucket.getName());
            repoArtifact.setGroupId(b.getGroupId());
            repoArtifact.setArtifactId(b.getArtifactId());
            repoArtifacts.add(repoArtifact);
        });

        return repoArtifacts;
    }

    @Override
    public SortedSet<ExtensionRepoVersionSummary> getExtensionRepoVersions(final Bucket bucket, final String groupId, final String artifactId) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("Group id cannot be null or blank");
        }

        if (StringUtils.isBlank(artifactId)) {
            throw new IllegalArgumentException("Artifact id cannot be null or blank");
        }

        final SortedSet<ExtensionRepoVersionSummary> repoVersions = new TreeSet<>();

        final List<ExtensionBundleVersionEntity> versionEntities = metadataService.getExtensionBundleVersions(bucket.getIdentifier(), groupId, artifactId);
        if (!versionEntities.isEmpty()) {
            final ExtensionBundleEntity bundleEntity = metadataService.getExtensionBundle(bucket.getIdentifier(), groupId, artifactId);
            if (bundleEntity == null) {
                // should never happen if the list of versions is not empty, but just in case
                throw new ResourceNotFoundException("The specified extension bundle does not exist in this bucket");
            }

            versionEntities.forEach(v -> {
                final ExtensionRepoVersionSummary repoVersion = new ExtensionRepoVersionSummary();
                repoVersion.setBucketName(bucket.getName());
                repoVersion.setGroupId(bundleEntity.getGroupId());
                repoVersion.setArtifactId(bundleEntity.getArtifactId());
                repoVersion.setVersion(v.getVersion());
                repoVersions.add(repoVersion);
            });
        }

        return repoVersions;
    }

    // ------ Helper Methods -------

    private ExtensionBundleContext getExtensionBundleContext(final ExtensionBundleVersion bundleVersion) {
        return getExtensionBundleContext(bundleVersion.getBucket(), bundleVersion.getExtensionBundle(), bundleVersion.getVersionMetadata());
    }

    private ExtensionBundleContext getExtensionBundleContext(final Bucket bucket, final ExtensionBundle bundle,
                                                             final ExtensionBundleVersionMetadata bundleVersionMetadata) {
        return new StandardExtensionBundleContext.Builder()
                .bundleType(getProviderBundleType(bundle.getBundleType()))
                .bucketId(bucket.getIdentifier())
                .bucketName(bucket.getName())
                .bundleId(bundle.getIdentifier())
                .bundleGroupId(bundle.getGroupId())
                .bundleArtifactId(bundle.getArtifactId())
                .bundleVersion(bundleVersionMetadata.getVersion())
                .author(bundleVersionMetadata.getAuthor())
                .timestamp(bundleVersionMetadata.getTimestamp())
                .build();
    }
}
