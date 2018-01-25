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
import io.swagger.annotations.Authorization;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Component
@Path("/flows")
@Api(
        value = "flows",
        description = "Gets metadata about flows.",
        authorizations = { @Authorization("Authorization") }
)
public class FlowResource extends ApplicationResource {

    private final RegistryService registryService;

    @Autowired
    public FlowResource(final RegistryService registryService) {
        this.registryService = registryService;
    }

    @GET
    @Path("fields")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the available field names that can be used for searching or sorting on flows.",
            response = Fields.class
    )
    public Response getAvailableFlowFields() {
        final Set<String> flowFields = registryService.getFlowFields();
        final Fields fields = new Fields(flowFields);
        return Response.status(Response.Status.OK).entity(fields).build();
    }

}
