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
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.filter.ExtensionBundleVersionFilterParams;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersion;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.service.extension.ExtensionBundleVersionCoordinate;
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
import java.util.Set;
import java.util.SortedSet;

@Component
@Path("/extensions/repo")
@Api(
        value = "extension_repository",
        description = "Interact with extension bundles via the hierarchy of bucket/group/artifact/version.",
        authorizations = { @Authorization("Authorization") }
)
public class ExtensionRepositoryResource extends AuthorizableApplicationResource {

    public static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
    private final RegistryService registryService;
    private final LinkService linkService;

    @Autowired
    public ExtensionRepositoryResource(
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
            value = "Gets the names of the buckets the current user is authorized for in order to browse the repo by bucket",
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
            value = "Gets the groups in the extension repository in the bucket with the given name",
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
            value = "Gets the artifacts in the extension repository with the given group in the bucket with the given name",
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
            value = "Gets the versions of the artifact in the extension repository specified by the given bucket, group, artifact, and version",
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
    public Response getExtensionBundleVersions(
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
            value = "Gets the information about the version specified by the given bucket, group, artifact, and version",
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
    public Response getExtensionBundleVersion(
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

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                bucket.getIdentifier(), groupId, artifactId, version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);

        final String downloadUri = generateResourceUri(
                "extensions", "repo",
                bundleVersion.getBucket().getName(),
                bundleVersion.getExtensionBundle().getGroupId(),
                bundleVersion.getExtensionBundle().getArtifactId(),
                bundleVersion.getVersionMetadata().getVersion(),
                "content");

        final String sha256Uri = generateResourceUri(
                "extensions", "repo",
                bundleVersion.getBucket().getName(),
                bundleVersion.getExtensionBundle().getGroupId(),
                bundleVersion.getExtensionBundle().getArtifactId(),
                bundleVersion.getVersionMetadata().getVersion(),
                "sha256");

        final ExtensionRepoVersion repoVersion = new ExtensionRepoVersion();
        repoVersion.setDownloadLink(Link.fromUri(downloadUri).rel("content").build());
        repoVersion.setSha256Link(Link.fromUri(sha256Uri).rel("sha256").build());
        repoVersion.setSha256Supplied(bundleVersion.getVersionMetadata().getSha256Supplied());

        return Response.ok(repoVersion).build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/content")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(
            value = "Gets the binary content of the extension bundle specified by the given bucket, group, artifact, and version",
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
    public Response getExtensionBundleVersionContent(
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

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                bucket.getIdentifier(), groupId, artifactId, version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);
        final StreamingOutput streamingOutput = (output) -> registryService.writeExtensionBundleVersionContent(bundleVersion, output);

        return Response.ok(streamingOutput)
                .header(CONTENT_DISPOSITION_HEADER,"attachment; filename = " + bundleVersion.getFilename())
                .build();
    }

    @GET
    @Path("{bucketName}/{groupId}/{artifactId}/{version}/sha256")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Gets the hex representation of the SHA-256 digest for the binary content of the version of the extension bundle",
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
    public Response getExtensionBundleVersionSha256(
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

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                bucket.getIdentifier(), groupId, artifactId, version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);
        final String sha256Hex = bundleVersion.getVersionMetadata().getSha256();

        return Response.ok(sha256Hex, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("{groupId}/{artifactId}/{version}/sha256")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Gets the hex representation of the SHA-256 digest for the binary content of the version of the extension bundle. Since the " +
                    "same group-artifact-version can exist in multiple buckets, this will return the checksum of the first one returned. This will be " +
                    "consistent since the checksum must be the same when existing in multiple buckets.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionBundleVersionSha256(
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

        final ExtensionBundleVersionFilterParams filterParams = ExtensionBundleVersionFilterParams.of(groupId, artifactId, version);

        final SortedSet<ExtensionBundleVersionMetadata> bundleVersions = registryService.getExtensionBundleVersions(authorizedBucketIds, filterParams);
        if (bundleVersions.isEmpty()) {
            throw new ResourceNotFoundException("An extension bundle version does not exist with the specific group, artifact, and version");
        } else {
            final ExtensionBundleVersionMetadata firstVersion = bundleVersions.first();
            return Response.ok(firstVersion.getSha256(), MediaType.TEXT_PLAIN).build();
        }
    }
}
