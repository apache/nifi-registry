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
package org.apache.nifi.registry.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.client.ExtensionBundleVersionClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jersey implementation of ExtensionBundleVersionClient.
 */
public class JerseyExtensionBundleVersionClient extends AbstractJerseyClient implements ExtensionBundleVersionClient {

    private final WebTarget bucketExtensionBundlesTarget;
    private final WebTarget extensionBundlesTarget;

    public JerseyExtensionBundleVersionClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyExtensionBundleVersionClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.bucketExtensionBundlesTarget = baseTarget.path("buckets/{bucketId}/extensions/bundles");
        this.extensionBundlesTarget = baseTarget.path("extensions/bundles");
    }

    @Override
    public ExtensionBundleVersion create(final String bucketId, final ExtensionBundleType bundleType, final InputStream bundleContentStream)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket id cannot be null or blank");
        }

        if (bundleType == null) {
            throw new IllegalArgumentException("Bundle type cannot be null");
        }

        if (bundleContentStream == null) {
            throw new IllegalArgumentException("Bundle content cannot be null");
        }

        return executeAction("Error creating extension bundle version", () -> {
            final WebTarget target = bucketExtensionBundlesTarget
                    .path("{bundleType}")
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("bundleType", bundleType.toString());

            final StreamDataBodyPart streamBodyPart = new StreamDataBodyPart("file", bundleContentStream);

            final FormDataMultiPart multipart = new FormDataMultiPart();
            multipart.bodyPart(streamBodyPart);

            return getRequestBuilder(target)
                    .post(
                            Entity.entity(multipart, multipart.getMediaType()),
                            ExtensionBundleVersion.class
                    );
        });
    }

    @Override
    public ExtensionBundleVersion create(String bucketId, ExtensionBundleType bundleType, File bundleFile)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket id cannot be null or blank");
        }

        if (bundleType == null) {
            throw new IllegalArgumentException("Bundle type cannot be null");
        }

        if (bundleFile == null) {
            throw new IllegalArgumentException("Bundle file cannot be null");
        }

        return executeAction("Error creating extension bundle version", () -> {
            final WebTarget target = bucketExtensionBundlesTarget
                    .path("{bundleType}")
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("bundleType", bundleType.toString());

            final FileDataBodyPart fileBodyPart = new FileDataBodyPart("file", bundleFile, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final FormDataMultiPart multipart = new FormDataMultiPart();
            multipart.bodyPart(fileBodyPart);

            return getRequestBuilder(target)
                    .post(
                            Entity.entity(multipart, multipart.getMediaType()),
                            ExtensionBundleVersion.class
                    );
        });
    }

    @Override
    public List<ExtensionBundleVersionMetadata> getBundleVersions(final String bundleId)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        return executeAction("Error getting extension bundle versions", () -> {
            final WebTarget target = extensionBundlesTarget
                    .path("{bundleId}/versions")
                    .resolveTemplate("bundleId", bundleId);

            final ExtensionBundleVersionMetadata[] bundleVersions = getRequestBuilder(target).get(ExtensionBundleVersionMetadata[].class);
            return  bundleVersions == null ? Collections.emptyList() : Arrays.asList(bundleVersions);
        });
    }

    @Override
    public ExtensionBundleVersion getBundleVersion(final String bundleId, final String version)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        return executeAction("Error getting extension bundle version", () -> {
            final WebTarget target = extensionBundlesTarget
                    .path("{bundleId}/versions/{version}")
                    .resolveTemplate("bundleId", bundleId)
                    .resolveTemplate("version", version);

            return getRequestBuilder(target).get(ExtensionBundleVersion.class);
         });
    }

    @Override
    public InputStream getBundleVersionContent(final String bundleId, final String version)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        return executeAction("Error getting extension bundle version", () -> {
            final WebTarget target = extensionBundlesTarget
                    .path("{bundleId}/versions/{version}/content")
                    .resolveTemplate("bundleId", bundleId)
                    .resolveTemplate("version", version);

            return getRequestBuilder(target)
                    .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .get()
                    .readEntity(InputStream.class);
        });
    }

    @Override
    public File writeBundleVersionContent(final String bundleId, final String version, final File directory)
            throws IOException, NiFiRegistryException {

        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory must exist and be a valid directory");
        }

        return executeAction("Error getting extension bundle version", () -> {
            final WebTarget target = extensionBundlesTarget
                    .path("{bundleId}/versions/{version}/content")
                    .resolveTemplate("bundleId", bundleId)
                    .resolveTemplate("version", version);

            final Response response = getRequestBuilder(target)
                    .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .get();

            final String contentDispositionHeader = response.getHeaderString("Content-Disposition");
            if (StringUtils.isBlank(contentDispositionHeader)) {
                throw new IllegalStateException("Content-Disposition header was blank or missing");
            }

            final int equalsIndex = contentDispositionHeader.lastIndexOf("=");
            final String filename = contentDispositionHeader.substring(equalsIndex + 1).trim();
            final File bundleFile = new File(directory, filename);

            try (final InputStream responseInputStream = response.readEntity(InputStream.class)) {
                Files.copy(responseInputStream, bundleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return bundleFile;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to write bundle content due to: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public ExtensionBundleVersion delete(final String bundleId, final String version) throws IOException, NiFiRegistryException {
        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        return executeAction("Error deleting extension bundle version", () -> {
            final WebTarget target = extensionBundlesTarget
                    .path("{bundleId}/versions/{version}")
                    .resolveTemplate("bundleId", bundleId)
                    .resolveTemplate("version", version);

            return getRequestBuilder(target).delete(ExtensionBundleVersion.class);
        });
    }

}
