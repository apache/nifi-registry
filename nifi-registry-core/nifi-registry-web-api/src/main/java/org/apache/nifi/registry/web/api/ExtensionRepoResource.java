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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.extension.bundle.BundleVersion;
import org.apache.nifi.registry.extension.bundle.BundleVersionFilterParams;
import org.apache.nifi.registry.extension.bundle.BundleVersionMetadata;
import org.apache.nifi.registry.extension.component.ExtensionMetadata;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoExtensionMetadata;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersion;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.web.link.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@Component
@Path("/extension-repository")
@Api(
        value = "extension repository",
        description = "Interact with extension bundles via the hierarchy of bucket/group/artifact/version. ",
        authorizations = { @Authorization("Authorization") }
)
public class ExtensionRepoResource extends AuthorizableApplicationResource {

    public static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
    private final RegistryService registryService;
    private final LinkService linkService;

    @Autowired
    public ExtensionRepoResource(
            final RegistryService registryService,
            final LinkService linkService,
            final AuthorizationService authorizationService,
            final EventService eventService) {
        super(authorizationService, eventService);
        this.registryService = registryService;
        this.linkService = linkService;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo buckets",
            notes = "Gets the names of the buckets the current user is authorized for in order to browse the repo by bucket. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionRepoBucket.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoBuckets() {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final SortedSet<ExtensionRepoBucket> repoBuckets = registryService.getExtensionRepoBuckets(authorizedBucketIds);
        linkService.populateFullLinks(repoBuckets, getBaseUri());
        return Response.status(Response.Status.OK).entity(repoBuckets).build();
    }

    @GET
    @Path("{bucketName}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo groups",
            notes = "Gets the groups in the extension repository in the given bucket. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionRepoGroup.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoGroups(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final SortedSet<ExtensionRepoGroup> repoGroups = registryService.getExtensionRepoGroups(bucket);
        linkService.populateFullLinks(repoGroups, getBaseUri());
        return Response.status(Response.Status.OK).entity(repoGroups).build();
    }

    @GET
    @Path("{bucketName}/{groupId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo artifacts",
            notes = "Gets the artifacts in the extension repository in the given bucket and group. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionRepoArtifact.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoArtifacts(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group id")
                final String groupId
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final SortedSet<ExtensionRepoArtifact> repoArtifacts = registryService.getExtensionRepoArtifacts(bucket, groupId);
        linkService.populateFullLinks(repoArtifacts, getBaseUri());
        return Response.status(Response.Status.OK).entity(repoArtifacts).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo versions",
            notes = "Gets the versions in the extension repository for the given bucket, group, and artifact. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionRepoVersionSummary.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersions(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final SortedSet<ExtensionRepoVersionSummary> repoVersions = registryService.getExtensionRepoVersions(bucket, groupId, artifactId);
        linkService.populateFullLinks(repoVersions, getBaseUri());
        return Response.status(Response.Status.OK).entity(repoVersions).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo version",
            notes = "Gets information about the version in the given bucket, group, and artifact. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionRepoVersion.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersion(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);

        final String extensionsUri = generateResourceUri(
                "extension-repository",
                bundleVersion.getBucket().getName(),
                bundleVersion.getBundle().getGroupId(),
                bundleVersion.getBundle().getArtifactId(),
                bundleVersion.getVersionMetadata().getVersion(),
                "extensions");

        final String downloadUri = generateResourceUri(
                "extension-repository",
                bundleVersion.getBucket().getName(),
                bundleVersion.getBundle().getGroupId(),
                bundleVersion.getBundle().getArtifactId(),
                bundleVersion.getVersionMetadata().getVersion(),
                "content");

        final String sha256Uri = generateResourceUri(
                "extension-repository",
                bundleVersion.getBucket().getName(),
                bundleVersion.getBundle().getGroupId(),
                bundleVersion.getBundle().getArtifactId(),
                bundleVersion.getVersionMetadata().getVersion(),
                "sha256");

        final ExtensionRepoVersion repoVersion = new ExtensionRepoVersion();
        repoVersion.setExtensionsLink(Link.fromUri(extensionsUri).rel("extensions").build());
        repoVersion.setDownloadLink(Link.fromUri(downloadUri).rel("content").build());
        repoVersion.setSha256Link(Link.fromUri(sha256Uri).rel("sha256").build());
        repoVersion.setSha256Supplied(bundleVersion.getVersionMetadata().getSha256Supplied());

        return Response.ok(repoVersion).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/extensions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo extensions",
            notes = "Gets information about the extensions in the given bucket, group, artifact, and version. " + NON_GUARANTEED_ENDPOINT,
            response = ExtensionMetadata.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionExtensions(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final SortedSet<ExtensionMetadata> extensions = registryService.getExtensionMetadata(bundleVersion);

        final List<ExtensionRepoExtensionMetadata> extensionRepoExtensions = new ArrayList<>(extensions.size());
        extensions.forEach(e -> extensionRepoExtensions.add(new ExtensionRepoExtensionMetadata(e)));
        linkService.populateFullLinks(extensionRepoExtensions, getBaseUri());

        return Response.ok(extensionRepoExtensions).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/extensions/{name}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension repo extension",
            notes = "Gets information about the extension with the given name in " +
                    "the given bucket, group, artifact, and version. " + NON_GUARANTEED_ENDPOINT,
            response = org.apache.nifi.registry.extension.component.manifest.Extension.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionExtension(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final org.apache.nifi.registry.extension.component.manifest.Extension extension = registryService.getExtension(bundleVersion, name);
        return Response.ok(extension).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/extensions/{name}/docs")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(
            value = "Get extension repo extension docs",
            notes = "Gets the documentation for the extension with the given name in " +
                    "the given bucket, group, artifact, and version. " + NON_GUARANTEED_ENDPOINT,
            response = String.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionExtensionDocs(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final StreamingOutput streamingOutput = (output) -> registryService.writeExtensionDocs(bundleVersion, name, output);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/extensions/{name}/docs/additional-details")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(
            value = "Get extension repo extension details",
            notes = "Gets the additional details documentation for the extension with the given name in " +
                    "the given bucket, group, artifact, and version. " + NON_GUARANTEED_ENDPOINT,
            response = String.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionExtensionAdditionalDetailsDocs(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final StreamingOutput streamingOutput = (output) -> registryService.writeAdditionalDetailsDocs(bundleVersion, name, output);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/content")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(
            value = "Get extension repo version content",
            notes = "Gets the binary content of the bundle with the given bucket, group, artifact, and version. " + NON_GUARANTEED_ENDPOINT,
            response = byte[].class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionContent(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final StreamingOutput streamingOutput = (output) -> registryService.writeBundleVersionContent(bundleVersion, output);

        return Response.ok(streamingOutput)
                .header(CONTENT_DISPOSITION_HEADER,"attachment; filename = " + bundleVersion.getFilename())
                .build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/sha256")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Get extension repo version checksum",
            notes = "Gets the hex representation of the SHA-256 digest for the binary content of the bundle " +
                    "with the given bucket, group, artifact, and version." + NON_GUARANTEED_ENDPOINT,
            response = String.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionRepoVersionSha256(
            @PathParam("bucketName")
            @ApiParam("The bucket name")
                final String bucketName,
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version
    ) {
        final Bucket bucket = registryService.getBucketByName(bucketName);
        authorizeBucketAccess(RequestAction.READ, bucket.getIdentifier());

        final BundleVersion bundleVersion = registryService.getBundleVersion(bucket.getIdentifier(), groupId, artifactId, version);
        final String sha256Hex = bundleVersion.getVersionMetadata().getSha256();
        return Response.ok(sha256Hex, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("{groupId}/{artifactId}/{version}/sha256")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Get global extension repo version checksum",
            notes = "Gets the hex representation of the SHA-256 digest for the binary content with the given bucket, group, artifact, and version. " +
                    "Since the same group-artifact-version can exist in multiple buckets, this will return the checksum of the first one returned. " +
                    "This will be consistent since the checksum must be the same when existing in multiple buckets. " + NON_GUARANTEED_ENDPOINT,
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getGlobalExtensionRepoVersionSha256(
            @PathParam("groupId")
            @ApiParam("The group identifier")
                final String groupId,
            @PathParam("artifactId")
            @ApiParam("The artifact identifier")
                final String artifactId,
            @PathParam("version")
            @ApiParam("The version")
                final String version
    ) {
        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        // Since we are using the filter params which are optional in the service layer, we need to validate these path params here

        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("Group id cannot be null or blank");
        }

        if (StringUtils.isBlank(artifactId)) {
            throw new IllegalArgumentException("Artifact id cannot be null or blank");
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }

        final BundleVersionFilterParams filterParams = BundleVersionFilterParams.of(groupId, artifactId, version);

        final SortedSet<BundleVersionMetadata> bundleVersions = registryService.getBundleVersions(authorizedBucketIds, filterParams);
        if (bundleVersions.isEmpty()) {
            throw new ResourceNotFoundException("An extension bundle version does not exist with the specific group, artifact, and version");
        } else {
            BundleVersionMetadata latestVersionMetadata = null;
            for (BundleVersionMetadata versionMetadata : bundleVersions) {
                if (latestVersionMetadata == null || versionMetadata.getTimestamp() > latestVersionMetadata.getTimestamp()) {
                    latestVersionMetadata = versionMetadata;
                }
            }
            return Response.ok(latestVersionMetadata.getSha256(), MediaType.TEXT_PLAIN).build();
        }
    }

}
