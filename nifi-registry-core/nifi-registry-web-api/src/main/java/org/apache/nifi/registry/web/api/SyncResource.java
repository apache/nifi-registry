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

import io.swagger.annotations.*;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.event.EventFactory;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.sync.RepositorySyncStatus;
import org.apache.nifi.registry.web.link.LinkService;
import org.apache.nifi.registry.web.security.PermissionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;

@Component
@Path("/sync")
@Api(
        value = "sync",
        description = "Provides methods to sync bucket metadata with persistence providers",
        authorizations = { @Authorization("Authorization") }
)
public class SyncResource extends AuthorizableApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(SyncResource.class);

    @Context
    UriInfo uriInfo;

    private final LinkService linkService;

    private final RegistryService registryService;

    private final PermissionsService permissionsService;

    @Autowired
    public SyncResource(
            final RegistryService registryService,
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
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get current sync status",
            response = RepositorySyncStatus.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/sync")})
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403)})
    public Response getSyncStatus() throws IOException {
        authorizeAccess(RequestAction.READ);

        RepositorySyncStatus currentStatus = this.registryService.getStatus();

        return Response.status(Response.Status.OK).entity(currentStatus).build();
    }


    @PUT
    @Path("metadata")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Replaces the registry metadata (buckets, etc.) with the data of the underlying (local) persistence provider",
            response = Bucket.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/sync") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403) })
    public Response syncMetaDataWithProviderData() {
        authorizeAccess(RequestAction.WRITE);

        Collection<Bucket> buckets = syncRegistryMetadata();

        return Response.status(Response.Status.OK).entity(buckets).build();
    }

    @PUT
    @Path("remote")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Reset provider repository to an initial state and re-syncs metadata (same as /sync/metadata). " +
                    "What initial state means depends on the underlying provider, in case of a GitFlowPersistenceProvider" +
                    " an 'initial state' invokes a clone command to synchronize the remote repository with the local repository.",
            response = Bucket.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/sync") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403) })
    public Response resetProviderRepository() throws IOException {
        authorizeAccess(RequestAction.WRITE);

        registryService.resetProviderRepository();
        Collection<Bucket> buckets = syncRegistryMetadata();

        return Response.status(Response.Status.OK).entity(buckets).build();
    }

    @POST
    @Path("remote")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get latest changes of remote provider repository and rebuild nifi-registry metadata. Use this only after " +
                    "having initialized the persistence provider.",
            response = Bucket.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/sync") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403) })
    public Response getLatestChangesOfRemoteRepository() throws IOException {
        authorizeAccess(RequestAction.WRITE);

        registryService.getLatestChangesOfRemoteRepository();
        Collection<Bucket> buckets = syncRegistryMetadata();
        return Response.status(Response.Status.OK).entity(buckets).build();
    }

    private Collection<Bucket> syncRegistryMetadata() {
        Collection<Bucket> buckets = registryService.syncBuckets();
        for (Bucket bucket : buckets) {
            publish(EventFactory.bucketCreated(bucket));
            permissionsService.populateBucketPermissions(bucket);
            linkService.populateLinks(bucket);
        }
        return buckets;
    }

    private void authorizeAccess(RequestAction actionType) throws AccessDeniedException {
        final Authorizable bucketsAuthorizable = authorizableLookup.getBucketsAuthorizable();
        authorizationService.authorize(bucketsAuthorizable, actionType);
    }

}
