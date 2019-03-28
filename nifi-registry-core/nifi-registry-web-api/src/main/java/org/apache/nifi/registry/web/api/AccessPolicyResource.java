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
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import org.apache.nifi.registry.authorization.AccessPolicy;
import org.apache.nifi.registry.authorization.AccessPolicySummary;
import org.apache.nifi.registry.authorization.Resource;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.AuthorizerCapabilityDetection;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
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
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * RESTful endpoint for managing access policies.
 */
@Component
@Path("/policies")
@Api(
        value = "policies",
        description = "Endpoint for managing access policies.",
        authorizations = { @Authorization("Authorization") }
)
public class AccessPolicyResource extends AuthorizableApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AccessPolicyResource.class);

    private Authorizer authorizer;

    @Autowired
    public AccessPolicyResource(
            Authorizer authorizer,
            AuthorizationService authorizationService,
            EventService eventService) {
        super(authorizationService, eventService);
        this.authorizer = authorizer;
    }

    /**
     * Create a new access policy.
     *
     * @param httpServletRequest request
     * @param requestAccessPolicy the access policy to create.
     * @return The created access policy.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create access policy",
            response = AccessPolicy.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry might not be configured to use a ConfigurableAccessPolicyProvider.") })
    public Response createAccessPolicy(
            @Context final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The access policy configuration details.", required = true)
            final AccessPolicy requestAccessPolicy) {

        verifyAuthorizerSupportsConfigurablePolicies();
        authorizeAccess(RequestAction.WRITE);

        if (requestAccessPolicy == null) {
            throw new IllegalArgumentException("Access policy details must be specified when creating a new policy.");
        }
        if (requestAccessPolicy.getIdentifier() != null) {
            throw new IllegalArgumentException("Access policy ID cannot be specified when creating a new policy.");
        }
        if (requestAccessPolicy.getResource() == null) {
            throw new IllegalArgumentException("Resource must be specified when creating a new access policy.");
        }
        RequestAction.valueOfValue(requestAccessPolicy.getAction());

        AccessPolicy createdPolicy = authorizationService.createAccessPolicy(requestAccessPolicy);

        String locationUri = generateAccessPolicyUri(createdPolicy);
        return generateCreatedResponse(URI.create(locationUri), createdPolicy).build();
    }

    /**
     * Retrieves all access policies
     *
     * @return A list of access policies
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all access policies",
            response = AccessPolicy.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getAccessPolicies() {

        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        List<AccessPolicy> accessPolicies = authorizationService.getAccessPolicies();
        if (accessPolicies == null) {
            accessPolicies = Collections.emptyList();
        }

        return generateOkResponse(accessPolicies).build();
    }

    /**
     * Retrieves the specified access policy.
     *
     * @param identifier The id of the access policy to retrieve
     * @return An accessPolicyEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @ApiOperation(
            value = "Get access policy",
            response = AccessPolicy.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getAccessPolicy(
            @ApiParam(value = "The access policy id.", required = true)
            @PathParam("id") final String identifier) {

        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        final AccessPolicy accessPolicy = authorizationService.getAccessPolicy(identifier);
        if (accessPolicy == null) {
            logger.warn("The specified access policy id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified policy does not exist in this registry.");
        }

        return generateOkResponse(accessPolicy).build();
    }


    /**
     * Retrieve a specified access policy for a given (action, resource) pair.
     *
     * @param action the action, i.e. "read", "write"
     * @param rawResource the name of the resource as a raw string
     * @return An access policy.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{action}/{resource: .+}")
    @ApiOperation(
            value = "Get access policy for resource",
            notes = "Gets an access policy for the specified action and resource",
            response = AccessPolicy.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getAccessPolicyForResource(
            @ApiParam(value = "The request action.", allowableValues = "read, write, delete", required = true)
            @PathParam("action")
            final String action,
            @ApiParam(value = "The resource of the policy.", required = true)
            @PathParam("resource")
            final String rawResource) {

        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        // parse the action and resource type
        final RequestAction requestAction = RequestAction.valueOfValue(action);
        final String resource = "/" + rawResource;

        AccessPolicy accessPolicy = authorizationService.getAccessPolicy(resource, requestAction);
        if (accessPolicy == null) {
            throw new ResourceNotFoundException("No policy found for action='" + action + "', resource='" + resource + "'");
        }
        return generateOkResponse(accessPolicy).build();
    }


    /**
     * Update an access policy.
     *
     * @param httpServletRequest request
     * @param identifier         The id of the access policy to update.
     * @param requestAccessPolicy An access policy.
     * @return the updated access policy.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @ApiOperation(
            value = "Update access policy",
            response = AccessPolicy.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry might not be configured to use a ConfigurableAccessPolicyProvider.") })
    public Response updateAccessPolicy(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The access policy id.", required = true)
            @PathParam("id")
            final String identifier,
            @ApiParam(value = "The access policy configuration details.", required = true)
            final AccessPolicy requestAccessPolicy) {

        verifyAuthorizerSupportsConfigurablePolicies();
        authorizeAccess(RequestAction.WRITE);

        if (requestAccessPolicy == null) {
            throw new IllegalArgumentException("Access policy details must be specified when updating a policy.");
        }
        if (!identifier.equals(requestAccessPolicy.getIdentifier())) {
            throw new IllegalArgumentException(String.format("The policy id in the request body (%s) does not equal the "
                    + "policy id of the requested resource (%s).", requestAccessPolicy.getIdentifier(), identifier));
        }

        AccessPolicy createdPolicy = authorizationService.updateAccessPolicy(requestAccessPolicy);

        String locationUri = generateAccessPolicyUri(createdPolicy);
        return generateOkResponse(createdPolicy).build();
    }


    /**
     * Remove a specified access policy.
     *
     * @param httpServletRequest request
     * @param identifier         The id of the access policy to remove.
     * @return The deleted access policy
     */
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @ApiOperation(
            value = "Delete access policy",
            response = AccessPolicy.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "delete"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry might not be configured to use a ConfigurableAccessPolicyProvider.") })
    public Response removeAccessPolicy(
            @Context final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The access policy id.", required = true)
            @PathParam("id")
            final String identifier) {

        verifyAuthorizerSupportsConfigurablePolicies();
        authorizeAccess(RequestAction.DELETE);
        AccessPolicy deletedPolicy = authorizationService.deleteAccessPolicy(identifier);
        if (deletedPolicy == null) {
            logger.warn("The specified access policy id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified policy does not exist in this registry.");
        }
        return generateOkResponse(deletedPolicy).build();
    }

    /**
     * Gets the available resources that support access/authorization policies.
     *
     * @return A resourcesEntity.
     */
    @GET
    @Path("/resources")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get available resources",
            notes = "Gets the available resources that support access/authorization policies",
            response = Resource.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/policies") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403) })
    public Response getResources() {
        authorizeAccess(RequestAction.READ);

        final List<Resource> resources = authorizationService.getResources();

        return generateOkResponse(resources).build();
    }


    private void verifyAuthorizerIsManaged() {
        if (!AuthorizerCapabilityDetection.isManagedAuthorizer(authorizer)) {
            throw new IllegalStateException(AuthorizationService.MSG_NON_MANAGED_AUTHORIZER);
        }
    }

    private void verifyAuthorizerSupportsConfigurablePolicies() {
        if (!AuthorizerCapabilityDetection.isConfigurableAccessPolicyProvider(authorizer)) {
            verifyAuthorizerIsManaged();
            throw new IllegalStateException(AuthorizationService.MSG_NON_CONFIGURABLE_POLICIES);
        }
    }

    private void authorizeAccess(RequestAction actionType) {
        final Authorizable policiesAuthorizable = authorizableLookup.getPoliciesAuthorizable();
        authorizationService.authorize(policiesAuthorizable, actionType);
    }

    private String generateAccessPolicyUri(final AccessPolicySummary accessPolicy) {
        return generateResourceUri("policies", accessPolicy.getIdentifier());
    }

}
