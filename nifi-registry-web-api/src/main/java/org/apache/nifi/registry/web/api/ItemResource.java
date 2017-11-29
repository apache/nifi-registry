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
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.params.SortParameter;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.QueryParameters;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
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
            notes = "The returned items will include only items from buckets for which the is authorized.",
            response = BucketItem.class,
            responseContainer = "List"
    )
    public Response getItems(
            @ApiParam(value = SortParameter.API_PARAM_DESCRIPTION, format = "field:order", allowMultiple = true, example = "name:ASC")
            @QueryParam("sort")
            final List<String> sortParameters) {

        Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);

        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<BucketItem>()).build();
        }

        final QueryParameters.Builder paramsBuilder = new QueryParameters.Builder();
        for (String sortParam : sortParameters) {
            paramsBuilder.addSort(SortParameter.fromString(sortParam));
        }

        final List<BucketItem> items = registryService.getBucketItems(paramsBuilder.build(), authorizedBucketIds);
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
    public Response getItems(
            @PathParam("bucketId")
            @ApiParam("The bucket identifier")
                final String bucketId,
            @QueryParam("sort")
            @ApiParam(value = SortParameter.API_PARAM_DESCRIPTION, format = "field:order", allowMultiple = true, example = "name:ASC")
                final List<String> sortParameters) {

        authorizeBucketAccess(RequestAction.READ, bucketId);
        final QueryParameters.Builder paramsBuilder = new QueryParameters.Builder();
        for (String sortParam : sortParameters) {
            paramsBuilder.addSort(SortParameter.fromString(sortParam));
        }

        final List<BucketItem> items = registryService.getBucketItems(paramsBuilder.build(), bucketId);
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
