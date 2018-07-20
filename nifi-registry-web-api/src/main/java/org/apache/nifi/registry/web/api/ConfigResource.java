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
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import org.apache.nifi.registry.RegistryConfiguration;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.AuthorizerCapabilityDetection;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("/config")
@Api(
        value = "config",
        description = "Retrieves the configuration for this NiFi Registry.",
        authorizations = { @Authorization("Authorization") }
)
public class ConfigResource extends AuthorizableApplicationResource {

    @Autowired
    public ConfigResource(
            final AuthorizationService authorizationService,
            final EventService eventService) {
        super(authorizationService, eventService);
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets NiFi Registry configurations",
            response = RegistryConfiguration.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/config") })
            }
    )
    @ApiResponses({ @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401) })
    public Response getConfiguration() {

        final RegistryConfiguration config = new RegistryConfiguration();

        boolean hasAnyConfigurationAccess = false;
        AccessDeniedException lastAccessDeniedException = null;
        final Authorizer authorizer = authorizationService.getAuthorizer();
        try {
            final Authorizable policyAuthorizer = authorizableLookup.getPoliciesAuthorizable();
            authorizationService.authorize(policyAuthorizer, RequestAction.READ);
            config.setSupportsManagedAuthorizer(AuthorizerCapabilityDetection.isManagedAuthorizer(authorizer));
            config.setSupportsConfigurableAuthorizer(AuthorizerCapabilityDetection.isConfigurableAccessPolicyProvider(authorizer));
            hasAnyConfigurationAccess = true;
        } catch (AccessDeniedException e) {
            lastAccessDeniedException = e;
        }

        try {
            authorizationService.authorize(authorizableLookup.getTenantsAuthorizable(), RequestAction.READ);
            config.setSupportsConfigurableUsersAndGroups(AuthorizerCapabilityDetection.isConfigurableUserGroupProvider(authorizer));
            hasAnyConfigurationAccess = true;
        } catch (AccessDeniedException e) {
            lastAccessDeniedException = e;
        }

        if (!hasAnyConfigurationAccess) {
            // If the user doesn't have access to any configuration, then throw the exception.
            // Otherwise, return what they can access.
            throw lastAccessDeniedException;
        }

        return Response.status(Response.Status.OK).entity(config).build();
    }
}
