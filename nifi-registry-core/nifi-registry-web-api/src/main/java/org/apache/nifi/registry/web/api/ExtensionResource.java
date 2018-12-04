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
import org.apache.nifi.registry.event.EventFactory;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.filter.ExtensionBundleFilterParams;
import org.apache.nifi.registry.extension.filter.ExtensionBundleVersionFilterParams;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.service.extension.ExtensionBundleVersionCoordinate;
import org.apache.nifi.registry.web.link.LinkService;
import org.apache.nifi.registry.web.security.PermissionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@Component
@Path("/extensions")
@Api(
        value = "extensions",
        description = "Gets metadata about extension bundles and extensions.",
        authorizations = { @Authorization("Authorization") }
)
public class ExtensionResource extends AuthorizableApplicationResource {

    public static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
    private final RegistryService registryService;
    private final LinkService linkService;
    private final PermissionsService permissionsService;

    @Autowired
    public ExtensionResource(final RegistryService registryService,
                             final LinkService linkService,
                             final PermissionsService permissionsService,
                             final AuthorizationService authorizationService,
                             final EventService eventService) {
        super(authorizationService, eventService);
        this.registryService = registryService;
        this.linkService = linkService;
        this.permissionsService = permissionsService;
    }

    // ---------- Extension Bundles ----------

    @GET
    @Path("bundles")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension bundles across all authorized buckets",
            notes = "The returned items will include only items from buckets for which the user is authorized. " +
                    "If the user is not authorized to any buckets, an empty list will be returned.",
            response = ExtensionBundle.class,
            responseContainer = "List"
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getExtensionBundles(
            @QueryParam("groupId")
            @ApiParam("Optional groupId to filter results. The value may be an exact match, or a trailing wildcard, " +
                    "such as 'com.%' to select all bundles where the groupId starts with 'com.'.")
                final String groupId,
            @QueryParam("artifactId")
            @ApiParam("Optional artifactId to filter results. The value may be an exact match, or a trailing wildcard, " +
                    "such as 'nifi-%' to select all bundles where the artifactId starts with 'nifi-'.")
                final String artifactId) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final ExtensionBundleFilterParams filterParams = ExtensionBundleFilterParams.of(groupId, artifactId);

        List<ExtensionBundle> bundles = registryService.getExtensionBundles(authorizedBucketIds, filterParams);
        if (bundles == null) {
            bundles = Collections.emptyList();
        }
        permissionsService.populateItemPermissions(bundles);
        linkService.populateLinks(bundles);

        return Response.status(Response.Status.OK).entity(bundles).build();
    }

    @GET
    @Path("bundles/{bundleId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the metadata about an extension bundle",
            nickname = "globalGetExtensionBundle",
            response = ExtensionBundle.class,
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
    public Response getExtensionBundle(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        permissionsService.populateItemPermissions(extensionBundle);
        linkService.populateLinks(extensionBundle);

        return Response.status(Response.Status.OK).entity(extensionBundle).build();
    }

    @DELETE
    @Path("bundles/{bundleId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the given extension bundle and all of it's versions",
            nickname = "globalDeleteExtensionBundle",
            response = ExtensionBundle.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response deleteExtensionBundle(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        final ExtensionBundle deletedExtensionBundle = registryService.deleteExtensionBundle(extensionBundle);
        publish(EventFactory.extensionBundleDeleted(deletedExtensionBundle));

        permissionsService.populateItemPermissions(deletedExtensionBundle);
        linkService.populateLinks(deletedExtensionBundle);

        return Response.status(Response.Status.OK).entity(deletedExtensionBundle).build();
    }

    // ---------- Extension Bundle Versions ----------

    @GET
    @Path("bundles/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get extension bundles versions across all authorized buckets",
            notes = "The returned items will include only items from buckets for which the user is authorized. " +
                    "If the user is not authorized to any buckets, an empty list will be returned.",
            response = ExtensionBundleVersionMetadata.class,
            responseContainer = "List"
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getExtensionBundleVersions(
            @QueryParam("groupId")
            @ApiParam("Optional groupId to filter results. The value may be an exact match, or a trailing wildcard, " +
                    "such as 'com.%' to select all bundle versions where the groupId starts with 'com.'.")
                final String groupId,
            @QueryParam("artifactId")
            @ApiParam("Optional artifactId to filter results. The value may be an exact match, or a trailing wildcard, " +
                    "such as 'nifi-%' to select all bundle versions where the artifactId starts with 'nifi-'.")
                final String artifactId,
            @QueryParam("version")
            @ApiParam("Optional version to filter results. The value maye be an exact match, or a trailing wildcard, " +
                    "such as '1.0.%' to select all bundle versions where the version starts with '1.0.'.")
                final String version
            ) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final ExtensionBundleVersionFilterParams filterParams = ExtensionBundleVersionFilterParams.of(groupId, artifactId, version);
        final SortedSet<ExtensionBundleVersionMetadata> bundleVersions = registryService.getExtensionBundleVersions(authorizedBucketIds, filterParams);
        linkService.populateLinks(bundleVersions);

        return Response.status(Response.Status.OK).entity(bundleVersions).build();
    }

    @GET
    @Path("bundles/{bundleId}/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the metadata about the versions of an extension bundle",
            nickname = "globalGetExtensionBundleVersions",
            response = ExtensionBundleVersionMetadata.class,
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
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        final SortedSet<ExtensionBundleVersionMetadata> bundleVersions = registryService.getExtensionBundleVersions(extensionBundle.getIdentifier());
        linkService.populateLinks(bundleVersions);

        return Response.status(Response.Status.OK).entity(bundleVersions).build();
    }

    @GET
    @Path("bundles/{bundleId}/versions/{version}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the descriptor for the specified version of the extension bundle",
            nickname = "globalGetExtensionBundleVersionDescriptor",
            response = ExtensionBundleVersion.class,
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
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                extensionBundle.getBucketIdentifier(),
                extensionBundle.getGroupId(),
                extensionBundle.getArtifactId(),
                version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);
        linkService.populateLinks(bundleVersion);

        return Response.ok(bundleVersion).build();
    }

    @GET
    @Path("bundles/{bundleId}/versions/{version}/content")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(
            value = "Gets the binary content for the specified version of the extension bundle",
            nickname = "globalGetExtensionBundleVersion",
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
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                extensionBundle.getBucketIdentifier(),
                extensionBundle.getGroupId(),
                extensionBundle.getArtifactId(),
                version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);
        final StreamingOutput streamingOutput = (output) -> registryService.writeExtensionBundleVersionContent(bundleVersion, output);

        return Response.ok(streamingOutput)
                .header(CONTENT_DISPOSITION_HEADER,"attachment; filename = " + bundleVersion.getFilename())
                .build();
    }

    @DELETE
    @Path("bundles/{bundleId}/versions/{version}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the given extension bundle version",
            nickname = "globalDeleteExtensionBundleVersion",
            response = ExtensionBundleVersion.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response deleteExtensionBundleVersion(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final ExtensionBundle extensionBundle = getExtensionBundleWithBucketReadAuthorization(bundleId);

        final ExtensionBundleVersionCoordinate versionCoordinate = new ExtensionBundleVersionCoordinate(
                extensionBundle.getBucketIdentifier(),
                extensionBundle.getGroupId(),
                extensionBundle.getArtifactId(),
                version);

        final ExtensionBundleVersion bundleVersion = registryService.getExtensionBundleVersion(versionCoordinate);

        final ExtensionBundleVersion deletedBundleVersion = registryService.deleteExtensionBundleVersion(bundleVersion);
        publish(EventFactory.extensionBundleVersionDeleted(deletedBundleVersion));
        linkService.populateLinks(deletedBundleVersion);

        return Response.status(Response.Status.OK).entity(deletedBundleVersion).build();
    }

    /**
     * Retrieves the extension bundle with the given id and ensures the current user has authorization to read the bucket it belongs to.
     *
     * @param bundleId the bundle id
     * @return the extension bundle
     */
    private ExtensionBundle getExtensionBundleWithBucketReadAuthorization(final String bundleId) {
        final ExtensionBundle extensionBundle = registryService.getExtensionBundle(bundleId);

        // this should never happen, but if somehow the back-end didn't populate the bucket id let's make sure the flow isn't returned
        if (StringUtils.isBlank(extensionBundle.getBucketIdentifier())) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, extensionBundle.getBucketIdentifier());
        return extensionBundle;
    }

}
