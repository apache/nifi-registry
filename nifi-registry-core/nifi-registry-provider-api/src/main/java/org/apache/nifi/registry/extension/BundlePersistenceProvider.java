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
package org.apache.nifi.registry.extension;

import org.apache.nifi.registry.provider.Provider;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Responsible for storing and retrieving the binary content of a version of an extension bundle.
 */
public interface BundlePersistenceProvider extends Provider {

    /**
     * Persists the binary content of a version of an extension bundle.
     *
     * @param context the context about the bundle version being persisted
     * @param contentStream the stream of binary content to persist
     * @param overwrite if true the persistence provider should overwrite any content that may already exist for the given bundle version,
     *                  if false the persistence provider should throw an BundlePersistenceException if content already exists
     * @throws BundlePersistenceException if an error occurs storing the content
     */
    void saveBundleVersion(BundleContext context, InputStream contentStream, boolean overwrite) throws BundlePersistenceException;

    /**
     * Writes the binary content of the bundle specified by the bucket-group-artifact-version to the provided OutputStream.
     *
     * @param context the context about the bundle version being retrieved
     * @param outputStream the output stream to write the contents to
     * @throws BundlePersistenceException if an error occurs retrieving the content
     */
    void getBundleVersion(BundleContext context, OutputStream outputStream) throws BundlePersistenceException;

    /**
     * Deletes the content of the bundle version specified by bucket-group-artifact-version.
     *
     * @param context the context about the bundle version being deleted
     * @throws BundlePersistenceException if an error occurs deleting the content
     */
    void deleteBundleVersion(BundleContext context) throws BundlePersistenceException;

    /**
     * Deletes the content for all versions of the bundle specified by bucket-group-artifact.
     *
     * @param bucketId the id of the bucket where the bundle is located
     * @param bucketName the bucket name where the bundle is located
     * @param groupId the group id of the bundle
     * @param artifactId the artifact id of the bundle
     * @throws BundlePersistenceException if an error occurs deleting the content
     */
    void deleteAllBundleVersions(String bucketId, String bucketName, String groupId, String artifactId)
            throws BundlePersistenceException;

}
