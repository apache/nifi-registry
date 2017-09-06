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
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.service.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("/buckets/{bucketId}/flows")
@Api(
        value = "bucket >> flows",
        description = "Create flows scoped to an existing bucket in the registry."
)
public class BucketFlowResource {

    private static final Logger logger = LoggerFactory.getLogger(BucketFlowResource.class);

    private final RegistryService registryService;

    public BucketFlowResource(@Autowired final RegistryService registryService) {
        this.registryService = registryService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create a named flow and store it in the specified bucket. " +
                    "The flow id is created by the server and a location URI for the created flow resource is returned.",
            response = VersionedFlow.class
    )
    public Response createFlow(@PathParam("bucketId") final String bucketId, final VersionedFlow flow) {
        final VersionedFlow createdFlow = registryService.createFlow(bucketId, flow);
        return Response.status(Response.Status.OK).entity(createdFlow).build();
    }

    /* TODO, add redirection URIs so that GET, PUT, DELETE operations for a given flow id (once created)
     * are accessible as a subresource from /buckets as well */
//    @GET
//    @PathParam("/{bucketId}/flows/{flowSubpath}")
//    @ApiOperation("Redirects to /flows/{flowSubpath}")
//    public Response getFlowAlias(
//            @PathParam("bucketId") String bucketId,
//            @PathParam("flowSubpath") String flowSubpath) {
//        logger.info("Redirecting flow operation on bucket resource handler to flow resource handler.");
//        UriBuilder addressBuilder = uriInfo.getBaseUriBuilder();
//        addressBuilder.path("flows/" + flowSubpath);
//        return Response.seeOther(addressBuilder.build()).build();
//    }
}
