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
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.web.link.LinkService;
import org.apache.nifi.registry.web.security.PermissionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.SortedSet;

@Component
@Path("/flows")
@Api(
        value = "flows",
        description = "Gets metadata about flows.",
        authorizations = { @Authorization("Authorization") }
)
public class FlowResource extends AuthorizableApplicationResource {

    private final RegistryService registryService;
    private final LinkService linkService;
    private final PermissionsService permissionsService;

    @Autowired
    public FlowResource(final RegistryService registryService,
                        final LinkService linkService,
                        final PermissionsService permissionsService,
                        final AuthorizationService authorizationService,
                        final EventService eventService) {
        super(authorizationService, eventService);
        this.registryService = registryService;
        this.linkService = linkService;
        this.permissionsService = permissionsService;
    }

    @GET
    @Path("fields")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get flow fields",
            notes = "Retrieves the flow field names that can be used for searching or sorting on flows.",
            response = Fields.class
    )
    public Response getAvailableFlowFields() {
        final Set<String> flowFields = registryService.getFlowFields();
        final Fields fields = new Fields(flowFields);
        return Response.status(Response.Status.OK).entity(fields).build();
    }

    @GET
    @Path("{flowId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get flow",
            notes = "Gets a flow by id.",
            nickname = "globalGetFlow",
            response = VersionedFlow.class,
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
    public Response getFlow(
            @PathParam("flowId")
            @ApiParam("The flow identifier")
            final String flowId) {

        final VersionedFlow flow = registryService.getFlow(flowId);

        // this should never happen, but if somehow the back-end didn't populate the bucket id let's make sure the flow isn't returned
        if (StringUtils.isBlank(flow.getBucketIdentifier())) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, flow.getBucketIdentifier());

        permissionsService.populateItemPermissions(flow);
        linkService.populateLinks(flow);

        return Response.status(Response.Status.OK).entity(flow).build();
    }

    @GET
    @Path("{flowId}/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get flow versions",
            notes = "Gets summary information for all versions of a given flow. Versions are ordered newest->oldest.",
            nickname = "globalGetFlowVersions",
            response = VersionedFlowSnapshotMetadata.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getFlowVersions(
            @PathParam("flowId")
            @ApiParam("The flow identifier")
            final String flowId) {

        final VersionedFlow flow = registryService.getFlow(flowId);

        final String bucketId = flow.getBucketIdentifier();
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, bucketId);

        final SortedSet<VersionedFlowSnapshotMetadata> snapshots = registryService.getFlowSnapshots(bucketId, flowId);
        if (snapshots != null ) {
            linkService.populateLinks(snapshots);
        }

        return Response.status(Response.Status.OK).entity(snapshots).build();
    }

    @GET
    @Path("{flowId}/versions/{versionNumber: \\d+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get flow version",
            notes = "Gets the given version of a flow, including metadata and flow content.",
            nickname = "globalGetFlowVersion",
            response = VersionedFlowSnapshot.class,
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
    public Response getFlowVersion(
            @PathParam("flowId")
            @ApiParam("The flow identifier")
            final String flowId,
            @PathParam("versionNumber")
            @ApiParam("The version number")
            final Integer versionNumber) {

        final VersionedFlowSnapshotMetadata latestMetadata = registryService.getLatestFlowSnapshotMetadata(flowId);

        final String bucketId = latestMetadata.getBucketIdentifier();
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, bucketId);

        final VersionedFlowSnapshot snapshot = registryService.getFlowSnapshot(bucketId, flowId, versionNumber);
        populateLinksAndPermissions(snapshot);
        return Response.status(Response.Status.OK).entity(snapshot).build();
    }

    @GET
    @Path("{flowId}/versions/latest")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get latest flow version",
            notes = "Gets the latest version of a flow, including metadata and flow content.",
            nickname = "globalGetLatestFlowVersion",
            response = VersionedFlowSnapshot.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getLatestFlowVersion(
            @PathParam("flowId")
            @ApiParam("The flow identifier")
            final String flowId) {

        final VersionedFlowSnapshotMetadata latestMetadata = registryService.getLatestFlowSnapshotMetadata(flowId);

        final String bucketId = latestMetadata.getBucketIdentifier();
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, bucketId);

        final VersionedFlowSnapshot lastSnapshot = registryService.getFlowSnapshot(bucketId, flowId, latestMetadata.getVersion());
        populateLinksAndPermissions(lastSnapshot);

        return Response.status(Response.Status.OK).entity(lastSnapshot).build();
    }

    @GET
    @Path("{flowId}/versions/latest/metadata")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get latest flow version metadata",
            notes = "Gets the metadata for the latest version of a flow.",
            nickname = "globalGetLatestFlowVersionMetadata",
            response = VersionedFlowSnapshotMetadata.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/buckets/{bucketId}") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getLatestFlowVersionMetadata(
            @PathParam("flowId")
            @ApiParam("The flow identifier")
            final String flowId) {

        final VersionedFlowSnapshotMetadata latestMetadata = registryService.getLatestFlowSnapshotMetadata(flowId);

        final String bucketId = latestMetadata.getBucketIdentifier();
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalStateException("Unable to authorize access because bucket identifier is null or blank");
        }

        authorizeBucketAccess(RequestAction.READ, bucketId);

        linkService.populateLinks(latestMetadata);
        return Response.status(Response.Status.OK).entity(latestMetadata).build();
    }

    // override the base implementation so we can provide a different error message that doesn't include the bucket id
    protected void authorizeBucketAccess(RequestAction action, String bucketId) {
        try {
            super.authorizeBucketAccess(RequestAction.READ, bucketId);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException("User not authorized to view the specified flow.", e);
        }
    }

    private void populateLinksAndPermissions(VersionedFlowSnapshot snapshot) {
        if (snapshot.getSnapshotMetadata() != null) {
            linkService.populateLinks(snapshot.getSnapshotMetadata());
        }

        if (snapshot.getFlow() != null) {
            linkService.populateLinks(snapshot.getFlow());
        }

        if (snapshot.getBucket() != null) {
            permissionsService.populateBucketPermissions(snapshot.getBucket());
            linkService.populateLinks(snapshot.getBucket());
        }

    }
}
