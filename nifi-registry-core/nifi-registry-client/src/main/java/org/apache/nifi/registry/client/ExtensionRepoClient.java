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
package org.apache.nifi.registry.client;

import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersion;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Client for interacting with the extension repository.
 */
public interface ExtensionRepoClient {

    /**
     * Gets the buckets in the extension repo.
     *
     * @return the list of extension repo buckets.
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    List<ExtensionRepoBucket> getBuckets() throws IOException, NiFiRegistryException;

    /**
     * Gets the extension repo groups in the specified bucket.
     *
     * @param bucketName the bucket name
     * @return the list of groups
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    List<ExtensionRepoGroup> getGroups(String bucketName) throws IOException, NiFiRegistryException;

    /**
     * Gets the extension repo artifacts in the given bucket and group.
     *
     * @param bucketName the bucket name
     * @param groupId the group id
     * @return the list of artifacts
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    List<ExtensionRepoArtifact> getArtifacts(String bucketName, String groupId) throws IOException, NiFiRegistryException;

    /**
     * Gets the extension repo versions for the given bucket, group, artifact.
     *
     * @param bucketName the bucket name
     * @param groupId the group id
     * @param artifactId the artifact id
     * @return the list of version summaries
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    List<ExtensionRepoVersionSummary> getVersions(String bucketName, String groupId, String artifactId)
            throws IOException, NiFiRegistryException;

    /**
     * Gets the extension repo version for the given bucket, group, artifact, and version.
     *
     * @param bucketName the bucket name
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the extension repo version
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    ExtensionRepoVersion getVersion(String bucketName, String groupId, String artifactId, String version)
            throws IOException, NiFiRegistryException;

    /**
     * Gets an InputStream for the binary content of the specified version.
     *
     * @param bucketName the bucket name
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the input stream
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    InputStream getVersionContent(String bucketName, String groupId, String artifactId, String version)
            throws IOException, NiFiRegistryException;

    /**
     * Gets the hex representation of the SHA-256 hash of the binary content for the given version.
     *
     * @param bucketName the bucket name
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the SHA-256 hex string
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    String getVersionSha256(String bucketName, String groupId, String artifactId, String version)
            throws IOException, NiFiRegistryException;

    /**
     * Gets the hex representation of the SHA-256 hash of the binary content for the given version.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the SHA-256 hex string
     *
     * @throws IOException if an I/O error occurs
     * @throws NiFiRegistryException if an non I/O error occurs
     */
    Optional<String> getVersionSha256(String groupId, String artifactId, String version)
            throws IOException, NiFiRegistryException;

}
