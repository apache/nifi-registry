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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.service.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

@Path("/buckets")
@Api(
        value = "/buckets",
        description = "Create named buckets in the registry to store NiFI objects such flows and extensions. " +
                "Search for and retrieve existing buckets."
)
public class BucketResource {

    private static final Logger logger = LoggerFactory.getLogger(BucketResource.class);

    @Context
    UriInfo uriInfo;

    private final RegistryService registryService;

    public BucketResource(final RegistryService registryService) {
        this.registryService = registryService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create a named bucket capable of storing NiFi bucket objects such as flows and extension bundles.",
            response = Bucket.class
    )
    public Response createBucket(final Bucket bucket) {
        final Bucket createdBucket = registryService.createBucket(bucket);
        return Response.status(Response.Status.OK).entity(createdBucket).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for all buckets in the registry for which the client is authorized. TODO: Will add some search parameters as well.",
            response = Bucket.class,
            responseContainer = "List"
    )
    public Response getBuckets() {
        final Set<Bucket> buckets = registryService.getBuckets();
        return Response.status(Response.Status.OK).entity(buckets).build();
    }

    @GET
    @Path("{bucketId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for an existing bucket in the registry.",
            response = Bucket.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response getBucket(@PathParam("bucketId") final String bucketId) {
        final Bucket bucket = registryService.getBucket(bucketId);
        return Response.status(Response.Status.OK).entity(bucket).build();
    }

    @PUT
    @Path("{bucketId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update the metadata for an existing bucket in the registry. Objects stored in the bucket will not be modified.",
            response = Bucket.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response updateBucket(@PathParam("bucketId") final String bucketId, final Bucket bucket) {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Id cannot be blank");
        }

        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        if (bucket.getIdentifier() != null && !bucketId.equals(bucket.getIdentifier())) {
            throw new IllegalArgumentException("Bucket id in path param must match bucket id in body");
        }

        if (bucket.getIdentifier() == null) {
            bucket.setIdentifier(bucketId);
        }

        final Bucket updatedBucket = registryService.updateBucket(bucket);
        return Response.status(Response.Status.OK).entity(updatedBucket).build();
    }

    @DELETE
    @Path("{bucketId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Delete an existing bucket in the registry, along with all the objects it is storing.",
            response = Bucket.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response deleteBucket(@PathParam("bucketId") final String bucketId) {
        final Bucket deletedBucket = registryService.deleteBucket(bucketId);
        return Response.status(Response.Status.OK).entity(deletedBucket).build();
    }

}
