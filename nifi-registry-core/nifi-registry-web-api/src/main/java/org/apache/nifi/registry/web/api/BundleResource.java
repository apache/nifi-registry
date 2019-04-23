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
import org.apache.nifi.registry.extension.bundle.Bundle;
import org.apache.nifi.registry.extension.bundle.BundleFilterParams;
import org.apache.nifi.registry.extension.bundle.BundleVersion;
import org.apache.nifi.registry.extension.bundle.BundleVersionFilterParams;
import org.apache.nifi.registry.extension.bundle.BundleVersionMetadata;
import org.apache.nifi.registry.extension.component.ExtensionMetadata;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
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
@Path("/bundles")
@Api(
        value = "bundles",
        description = "Gets metadata about extension bundles and their versions. ",
        authorizations = { @Authorization("Authorization") }
)
public class BundleResource extends AuthorizableApplicationResource {

    public static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
    private final RegistryService registryService;
    private final LinkService linkService;
    private final PermissionsService permissionsService;

    @Autowired
    public BundleResource(final RegistryService registryService,
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
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all bundles",
            notes = "Gets the metadata for all bundles across all authorized buckets with optional filters applied. " +
                    "The returned results will include only items from buckets for which the user is authorized. " +
                    "If the user is not authorized to any buckets, an empty list will be returned. " + NON_GUARANTEED_ENDPOINT,
            response = Bundle.class,
            responseContainer = "List"
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getBundles(
            @QueryParam("bucketName")
            @ApiParam("Optional bucket name to filter results. The value may be an exact match, or a wildcard, " +
                    "such as 'My Bucket%' to select all bundles where the bucket name starts with 'My Bucket'.")
                final String bucketName,
            @QueryParam("groupId")
            @ApiParam("Optional groupId to filter results. The value may be an exact match, or a wildcard, " +
                    "such as 'com.%' to select all bundles where the groupId starts with 'com.'.")
                final String groupId,
            @QueryParam("artifactId")
            @ApiParam("Optional artifactId to filter results. The value may be an exact match, or a wildcard, " +
                    "such as 'nifi-%' to select all bundles where the artifactId starts with 'nifi-'.")
                final String artifactId) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final BundleFilterParams filterParams = BundleFilterParams.of(bucketName, groupId, artifactId);

        List<Bundle> bundles = registryService.getBundles(authorizedBucketIds, filterParams);
        if (bundles == null) {
            bundles = Collections.emptyList();
        }
        permissionsService.populateItemPermissions(bundles);
        linkService.populateLinks(bundles);

        return Response.status(Response.Status.OK).entity(bundles).build();
    }

    @GET
    @Path("{bundleId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get bundle",
            notes = "Gets the metadata about an extension bundle. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetExtensionBundle",
            response = Bundle.class,
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
    public Response getBundle(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        permissionsService.populateItemPermissions(bundle);
        linkService.populateLinks(bundle);

        return Response.status(Response.Status.OK).entity(bundle).build();
    }

    @DELETE
    @Path("{bundleId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Delete bundle",
            notes = "Deletes the given extension bundle and all of it's versions. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalDeleteExtensionBundle",
            response = Bundle.class,
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
    public Response deleteBundle(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);

        final Bundle deletedBundle = registryService.deleteBundle(bundle);
        publish(EventFactory.extensionBundleDeleted(deletedBundle));

        permissionsService.populateItemPermissions(deletedBundle);
        linkService.populateLinks(deletedBundle);

        return Response.status(Response.Status.OK).entity(deletedBundle).build();
    }

    // ---------- Extension Bundle Versions ----------

    @GET
    @Path("versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all bundle versions",
            notes = "Gets the metadata about extension bundle versions across all authorized buckets with optional filters applied. " +
                    "If the user is not authorized to any buckets, an empty list will be returned. " + NON_GUARANTEED_ENDPOINT,
            response = BundleVersionMetadata.class,
            responseContainer = "List"
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getBundleVersions(
            @QueryParam("groupId")
            @ApiParam("Optional groupId to filter results. The value may be an exact match, or a wildcard, " +
                    "such as 'com.%' to select all bundle versions where the groupId starts with 'com.'.")
                final String groupId,
            @QueryParam("artifactId")
            @ApiParam("Optional artifactId to filter results. The value may be an exact match, or a wildcard, " +
                    "such as 'nifi-%' to select all bundle versions where the artifactId starts with 'nifi-'.")
                final String artifactId,
            @QueryParam("version")
            @ApiParam("Optional version to filter results. The value maye be an exact match, or a wildcard, " +
                    "such as '1.0.%' to select all bundle versions where the version starts with '1.0.'.")
                final String version
            ) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final BundleVersionFilterParams filterParams = BundleVersionFilterParams.of(groupId, artifactId, version);
        final SortedSet<BundleVersionMetadata> bundleVersions = registryService.getBundleVersions(authorizedBucketIds, filterParams);
        linkService.populateLinks(bundleVersions);

        return Response.status(Response.Status.OK).entity(bundleVersions).build();
    }

    @GET
    @Path("{bundleId}/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get bundle versions",
            notes = "Gets the metadata for the versions of the given extension bundle. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetBundleVersions",
            response = BundleVersionMetadata.class,
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
    public Response getBundleVersions(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final SortedSet<BundleVersionMetadata> bundleVersions = registryService.getBundleVersions(bundle.getIdentifier());
        linkService.populateLinks(bundleVersions);

        return Response.status(Response.Status.OK).entity(bundleVersions).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get bundle version",
            notes = "Gets the descriptor for the given version of the given extension bundle. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetBundleVersion",
            response = BundleVersion.class,
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
    public Response getBundleVersion(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);
        linkService.populateLinks(bundleVersion);

        return Response.ok(bundleVersion).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}/content")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(
            value = "Get bundle version content",
            notes = "Gets the binary content for the given version of the given extension bundle. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetBundleVersionContent",
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
    public Response getBundleVersionContent(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final StreamingOutput streamingOutput = (output) -> registryService.writeBundleVersionContent(bundleVersion, output);

        return Response.ok(streamingOutput)
                .header(CONTENT_DISPOSITION_HEADER,"attachment; filename = " + bundleVersion.getFilename())
                .build();
    }

    @DELETE
    @Path("{bundleId}/versions/{version}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Delete bundle version",
            notes = "Deletes the given extension bundle version and it's associated binary content. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalDeleteBundleVersion",
            response = BundleVersion.class,
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
    public Response deleteBundleVersion(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final BundleVersion deletedBundleVersion = registryService.deleteBundleVersion(bundleVersion);
        publish(EventFactory.extensionBundleVersionDeleted(deletedBundleVersion));
        linkService.populateLinks(deletedBundleVersion);

        return Response.status(Response.Status.OK).entity(deletedBundleVersion).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}/extensions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get bundle version extensions",
            notes = "Gets the metadata about the extensions in the given extension bundle version. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetBundleVersionExtensions",
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
    public Response getBundleVersionExtensions(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final SortedSet<ExtensionMetadata> extensions = registryService.getExtensionMetadata(bundleVersion);
        linkService.populateLinks(extensions);
        return Response.ok(extensions).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}/extensions/{name}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get bundle version extension",
            notes = "Gets the metadata about the extension with the given name in the given extension bundle version. " + NON_GUARANTEED_ENDPOINT,
            nickname = "globalGetBundleVersionExtension",
            response = org.apache.nifi.registry.extension.component.manifest.Extension.class,
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
    public Response getBundleVersionExtension(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
            ) {

        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final org.apache.nifi.registry.extension.component.manifest.Extension extension = registryService.getExtension(bundleVersion, name);
        return Response.ok(extension).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}/extensions/{name}/docs")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(
            value = "Get bundle version extension docs",
            notes = "Gets the documentation for the given extension in the given extension bundle version. " + NON_GUARANTEED_ENDPOINT,
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
    public Response getBundleVersionExtensionDocs(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
    ) {
        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final StreamingOutput streamingOutput = (output) -> registryService.writeExtensionDocs(bundleVersion, name, output);
        return Response.ok(streamingOutput).build();
    }

    @GET
    @Path("{bundleId}/versions/{version}/extensions/{name}/docs/additional-details")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(
            value = "Get bundle version extension docs details",
            notes = "Gets the additional details documentation for the given extension in the given extension bundle version. " + NON_GUARANTEED_ENDPOINT,
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
    public Response getBundleVersionExtensionAdditionalDetailsDocs(
            @PathParam("bundleId")
            @ApiParam("The extension bundle identifier")
                final String bundleId,
            @PathParam("version")
            @ApiParam("The version of the bundle")
                final String version,
            @PathParam("name")
            @ApiParam("The fully qualified name of the extension")
                final String name
    ) {
        final Bundle bundle = getBundleWithBucketReadAuthorization(bundleId);
        final BundleVersion bundleVersion = registryService.getBundleVersion(bundle.getBucketIdentifier(), bundleId, version);

        final StreamingOutput streamingOutput = (output) -> registryService.writeAdditionalDetailsDocs(bundleVersion, name, output);
        return Response.ok(streamingOutput).build();
    }

    /**
     * Retrieves the extension bundle with the given id and ensures the current user has authorization to read the bucket it belongs to.
     *
     * @param bundleId the bundle id
     * @return the extension bundle
     */
    private Bundle getBundleWithBucketReadAuthorization(final String bundleId) {
        final Bundle bundle = registryService.getBundle(bundleId);

        // this should never happen, but if somehow the back-end didn't populate the bucket id let's make sure the flow isn't returned
        if (StringUtils.isBlank(bundle.getBucketIdentifier())) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, bundle.getBucketIdentifier());
        return bundle;
    }

}
