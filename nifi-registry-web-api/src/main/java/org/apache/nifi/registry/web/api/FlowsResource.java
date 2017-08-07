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
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.web.response.FlowEntity;
import org.apache.nifi.registry.web.response.FlowVersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/flows")
@Api(
        value = "/flows",
        description = "Create named flows that can be versioned. Search for existing flows."
)
public class FlowsResource {

    private static final Logger logger = LoggerFactory.getLogger(FlowsResource.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create a named flow at its initial version",
            response = VersionedFlowSnapshot.class
    )
    public Response createFlow() {
        // TODO implement createFlow
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all flows (name and id) the registry has stored for which the client is authorized. Probably will add some search url parameters as well.",
            response = FlowEntity.class,
            responseContainer = "List"
    )
    public Response getFlows() {
        // TODO implement getFlows
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("/{flowId}/versions/")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create the next version of a given flow ID",
            response = VersionedFlowSnapshot.class
    )
    public Response createFlowVersion(
            @PathParam("flowId") String flowId) {
        // TODO implement createFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/{flowId}/versions/")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get summary of all versions of a flow for a given flow ID.",
            response = FlowVersionEntity.class,
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

    @PUT
    @Path("/{flowId}/versions/{versionNumber: \\d+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update a given version of a flow for a given flow ID",
            response = VersionedFlowSnapshot.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response updateFlowVersion(
            @PathParam("flowId") String flowId,
            @PathParam("versionNumber") Integer versionNumber) {
        // TODO implement updateFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @DELETE
    @Path("/{flowId}/versions/{versionNumber: \\d+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @ApiOperation(
            value = "Delete a given version of a flow for a given flow ID",
            response = VersionedFlowSnapshot.class
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 404, message = "The specified resource could not be found."),
            }
    )
    public Response deleteFlowVersion(
            @PathParam("flowId") String flowId,
            @PathParam("versionNumber") Integer versionNumber) {
        // TODO implement deleteFlowVersion
        logger.error("This API functionality has not yet been implemented.");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

}
