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
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.service.params.QueryParameters;
import org.apache.nifi.registry.service.params.SortParameter;
import org.apache.nifi.registry.web.link.LinkService;
import org.apache.nifi.registry.web.response.FieldsEntity;
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
import java.util.List;
import java.util.Set;

@Component
@Path("/items")
@Api(
        value = "/items",
        description = "Retrieve items across all buckets for which the user is authorized."
)
public class ItemResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemResource.class);

    @Context
    UriInfo uriInfo;

    private final LinkService linkService;

    private final RegistryService registryService;

    @Autowired
    public ItemResource(final RegistryService registryService, final LinkService linkService) {
        this.registryService = registryService;
        this.linkService = linkService;
    }


    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for items across all buckets for which the user is authorized.",
            response = BucketItem.class,
            responseContainer = "List"
    )
    public Response getItems(@QueryParam("sort") final List<SortParameter> sortParameters) {

        final QueryParameters params = new QueryParameters.Builder()
                .addSorts(sortParameters)
                .build();

        final List<BucketItem> items = registryService.getBucketItems(params);
        linkService.populateItemLinks(items);

        return Response.status(Response.Status.OK).entity(items).build();
    }

    @GET
    @Path("{bucketId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for items of the given bucket.",
            response = BucketItem.class,
            responseContainer = "List"
    )
    public Response getItems(@PathParam("bucketId") final String bucketId,
                             @QueryParam("sort") final List<SortParameter> sortParameters) {

        final QueryParameters params = new QueryParameters.Builder()
                .addSorts(sortParameters)
                .build();

        final List<BucketItem> items = registryService.getBucketItems(params, bucketId);
        linkService.populateItemLinks(items);

        return Response.status(Response.Status.OK).entity(items).build();
    }

    @GET
    @Path("fields")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the available field names that can be used for searching or sorting on bucket items.",
            response = FieldsEntity.class
    )
    public Response getAvailableBucketFields() {
        final Set<String> bucketFields = registryService.getBucketItemFields();
        final FieldsEntity fieldsEntity = new FieldsEntity(bucketFields);
        return Response.status(Response.Status.OK).entity(fieldsEntity).build();
    }

}
