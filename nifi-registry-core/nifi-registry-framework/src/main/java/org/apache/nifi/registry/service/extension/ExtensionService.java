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

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.filter.ExtensionBundleFilterParams;
import org.apache.nifi.registry.extension.filter.ExtensionBundleVersionFilterParams;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public interface ExtensionService {

    // ----- Extension Bundles -----

    /**
     * Creates a version of an extension bundle.
     *
     * The InputStream is expected to contain the binary contents of a bundle in the format specified by bundleType.
     *
     * The metadata will be extracted from the bundle and used to determine if this is a new version of an existing bundle,
     * or it will create a new bundle and this as the first version if one doesn't already exist.
     *
     * @param bucketIdentifier the bucket id
     * @param bundleType the type of bundle
     * @param inputStream the binary content of the bundle
     * @param clientSha256 the SHA-256 hex supplied by the client
     * @return the ExtensionBundleVersion representing all of the information about the bundle
     * @throws IOException if an error occurs processing the InputStream
     */
    ExtensionBundleVersion createExtensionBundleVersion(String bucketIdentifier, ExtensionBundleType bundleType,
                                                        InputStream inputStream, String clientSha256) throws IOException;

    /**
     * Retrieves the extension bundles in the given buckets.
     *
     * @param bucketIdentifiers the bucket identifiers
     * @param filterParams the optional filter params
     * @return the bundles in the given buckets
     */
    List<ExtensionBundle> getExtensionBundles(Set<String> bucketIdentifiers, ExtensionBundleFilterParams filterParams);

    /**
     * Retrieves the extension bundles in the given bucket.
     *
     * @param bucketIdentifier the bucket identifier
     * @return the bundles in the given bucket
     */
    List<ExtensionBundle> getExtensionBundlesByBucket(String bucketIdentifier);

    /**
     * Retrieve the extension bundle with the given id.
     *
     * @param extensionBundleIdentifier the extension bundle id
     * @return the bundle
     */
    ExtensionBundle getExtensionBundle(String extensionBundleIdentifier);

    /**
     * Deletes the given extension bundle and all it's versions.
     *
     * @param extensionBundle the extension bundle to delete
     * @return the deleted bundle
     */
    ExtensionBundle deleteExtensionBundle(ExtensionBundle extensionBundle);

    // ----- Extension Bundle Versions -----

    /**
     * Retrieves the extension bundle versions in the given buckets.
     *
     * @param bucketIdentifiers the bucket identifiers
     * @param filterParams the optional filter params
     * @return the set of extension bundle versions
     */
    SortedSet<ExtensionBundleVersionMetadata> getExtensionBundleVersions(
            Set<String> bucketIdentifiers, ExtensionBundleVersionFilterParams filterParams);

    /**
     * Retrieves the versions of the given extension bundle.
     *
     * @param extensionBundleIdentifier the extension bundle id
     * @return the sorted set of versions for the given bundle
     */
    SortedSet<ExtensionBundleVersionMetadata> getExtensionBundleVersions(String extensionBundleIdentifier);

    /**
     * Retrieves the full ExtensionBundleVersion object, including version metadata, bundle metadata, and bucket metadata.
     *
     * @param versionCoordinate the coordinate of the version
     * @return the extension bundle version
     */
    ExtensionBundleVersion getExtensionBundleVersion(ExtensionBundleVersionCoordinate versionCoordinate);

    /**
     * Writes the binary content of the extension bundle version to the given OutputStream.
     *
     * @param extensionBundleVersion the version to write the content for
     * @param out the output stream to write to
     */
    void writeExtensionBundleVersionContent(ExtensionBundleVersion extensionBundleVersion, OutputStream out);

    /**
     * Deletes the given version of the extension bundle.
     *
     * @param bundleVersion the version to delete
     * @return the deleted extension bundle version
     */
    ExtensionBundleVersion deleteExtensionBundleVersion(ExtensionBundleVersion bundleVersion);

    // ----- Extension Repo Methods -----

    /**
     * Retrieves the extension repo buckets for the given bucket ids.
     *
     * @param bucketIds the bucket ids
     * @return the set of buckets
     */
    SortedSet<ExtensionRepoBucket> getExtensionRepoBuckets(Set<String> bucketIds);

    /**
     * Retrieves the extension repo groups for the given bucket.
     *
     * @param bucket the bucket
     * @return the groups for the bucket
     */
    SortedSet<ExtensionRepoGroup> getExtensionRepoGroups(Bucket bucket);

    /**
     * Retrieves the extension repo artifacts for the given bucket and group.
     *
     * @param bucket the bucket
     * @param groupId the group id
     * @return the artifacts for the bucket and group
     */
    SortedSet<ExtensionRepoArtifact> getExtensionRepoArtifacts(Bucket bucket, String groupId);

    /**
     * Retrieves the extension repo version summaries for the given bucket, group, and artifact.
     *
     * @param bucket the bucket
     * @param groupId the group id
     * @param artifactId the artifact id
     * @return the version summaries for the bucket, group, and artifact
     */
    SortedSet<ExtensionRepoVersionSummary> getExtensionRepoVersions(Bucket bucket, String groupId, String artifactId);

}
