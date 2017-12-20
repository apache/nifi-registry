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
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.web.link.LinkService;
import org.apache.nifi.registry.web.security.PermissionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@Path("/items")
@Api(
        value = "/items",
        description = "Retrieve items across all buckets for which the user is authorized."
)
public class ItemResource extends AuthorizableApplicationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemResource.class);

    @Context
    UriInfo uriInfo;

    private final LinkService linkService;
    private final PermissionsService permissionsService;
    private final RegistryService registryService;

    @Autowired
    public ItemResource(
            final RegistryService registryService,
            final LinkService linkService,
            final PermissionsService permissionsService,
            final AuthorizationService authorizationService,
            final Authorizer authorizer) {
        super(authorizer, authorizationService);
        this.registryService = registryService;
        this.linkService = linkService;
        this.permissionsService = permissionsService;
    }


    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get items across all buckets",
            notes = "The returned items will include only items from buckets for which the user is authorized. " +
                    "If the user is not authorized to any buckets, an empty list will be returned.",
            response = BucketItem.class,
            responseContainer = "List"
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getItems() {

        // Note: We don't explicitly check for access to (READ, /buckets) or
        // (READ, /items ) because a user might have access to individual buckets
        // without top-level access. For example, a user that has
        // (READ, /buckets/bucket-id-1) but not access to /buckets should not
        // get a 403 error returned from this endpoint. This has the side effect
        // that a user with no access to any buckets gets an empty array returned
        // from this endpoint instead of 403 as one might expect.

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<BucketItem>()).build();
        }

        List<BucketItem> items = registryService.getBucketItems(authorizedBucketIds);
        if (items == null) {
            items = Collections.emptyList();
        }
        permissionsService.populateItemPermissions(items);
        linkService.populateItemLinks(items);

        return Response.status(Response.Status.OK).entity(items).build();
    }

    @GET
    @Path("{bucketId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets items of the given bucket",
            response = BucketItem.class,
            responseContainer = "List",
            nickname = "getItemsInBucket"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404) })
    public Response getItems(
            @PathParam("bucketId")
            @ApiParam("The bucket identifier")
            final String bucketId) {

        authorizeBucketAccess(RequestAction.READ, bucketId);

        final List<BucketItem> items = registryService.getBucketItems(bucketId);
        permissionsService.populateItemPermissions(items);
        linkService.populateItemLinks(items);

        return Response.status(Response.Status.OK).entity(items).build();
    }

    @GET
    @Path("fields")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the available field names for searching or sorting on bucket items.",
            response = Fields.class
    )
    public Response getAvailableBucketItemFields() {
        final Set<String> bucketFields = registryService.getBucketItemFields();
        final Fields fields = new Fields(bucketFields);
        return Response.status(Response.Status.OK).entity(fields).build();
    }

}
