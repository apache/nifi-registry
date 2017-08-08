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
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/flows")
@Api(
        value = "/flows",
        description = "Create named flows that can be versioned. Search for and retrieve existing flows."
)
public class FlowResource {

    private static final Logger logger = LoggerFactory.getLogger(FlowResource.class);

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for all flows in all buckets that the registry has stored for which the client is authorized. TODO: Will add some search parameters as well.",
            response = VersionedFlow.class,
            responseContainer = "List"
    )
    public Response getFlows() {
        // TODO implement getFlows
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/{flowId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get metadata for an existing flow the registry has stored.",
            response = VersionedFlow.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response getFlow(
            @PathParam("flowId") String flowId) {
        // TODO implement getFlow
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @PUT
    @Path("/{flowId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update an existing flow the registry has stored.",
            response = VersionedFlow.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response updateFlow(
            @PathParam("flowId") String flowId) {
        // TODO implement updateFlow
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @DELETE
    @Path("/{flowId}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Delete an existing flow the registry has stored.",
            response = VersionedFlow.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response deleteFlow(
            @PathParam("flowId") String flowId) {
        // TODO implement deleteFlow
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("/{flowId}/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create the next version of a given flow ID. " +
            "The version number is created by the server and a location URI for the created version resource is returned.",
            response = VersionedFlowSnapshot.class
    )
    public Response createFlowVersion(
            @PathParam("flowId") String flowId) {
        // TODO implement createFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/{flowId}/versions")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get summary of all versions of a flow for a given flow ID.",
            response = VersionedFlowSnapshot.class, /* TODO, add a JSON serialization view for VersionedFlowSnapshot
                                                       for this endpoint that  hides the flowContents property when
                                                       this object is returned as part of a collection. */
            responseContainer = "List"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response getFlowVersions(
            @PathParam("flowId") String flowId) {
        // TODO implement getFlowVersions
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/{flowId}/versions/latest")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get the latest version of a flow for a given flow ID",
            response = VersionedFlowSnapshot.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response getLatestFlowVersion(
            @PathParam("flowId") String flowId) {
        // TODO implement getLatestFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/{flowId}/versions/{versionNumber: \\d+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get a given version of a flow for a given flow ID",
            response = VersionedFlowSnapshot.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response getFlowVersion(
            @PathParam("flowId") String flowId,
            @PathParam("versionNumber") Integer versionNumber) {
        // TODO implement getFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

}
