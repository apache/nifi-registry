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
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.authorization.User;
import org.apache.nifi.registry.authorization.UserGroup;
import org.apache.nifi.registry.event.EventFactory;
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
import java.util.List;

/**
 * RESTful endpoints for managing tenants, ie, users and user groups.
 */
@Component
@Path("tenants")
@Api(
        value = "tenants",
        description = "Endpoint for managing users and user groups.",
        authorizations = { @Authorization("Authorization") }
)
public class TenantResource extends AuthorizableApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(TenantResource.class);

    private Authorizer authorizer;

    @Autowired
    public TenantResource(AuthorizationService authorizationService, EventService eventService) {
        super(authorizationService, eventService);
        authorizer = authorizationService.getAuthorizer();
    }


    // ---------- User endpoints --------------------------------------------------------------------------------------

    /**
     * Creates a new user.
     *
     * @param httpServletRequest request
     * @param requestUser the user to create
     * @return the user that was created
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users")
    @ApiOperation(
            value = "Create user",
            notes = NON_GUARANTEED_ENDPOINT,
            response = User.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response createUser(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user configuration details.", required = true)
            final User requestUser) {

        verifyAuthorizerSupportsConfigurableUserGroups();

        if (requestUser == null) {
            throw new IllegalArgumentException("User details must be specified when creating a new user.");
        }
        if (requestUser.getIdentifier() != null) {
            throw new IllegalArgumentException("User identifier cannot be specified when creating a new user.");
        }
        if (StringUtils.isBlank(requestUser.getIdentity())) {
            throw new IllegalArgumentException("User identity must be specified when creating a new user.");
        }

        authorizeAccess(RequestAction.WRITE);

        User createdUser = authorizationService.createUser(requestUser);
        publish(EventFactory.userCreated(createdUser));

        String locationUri = generateUserUri(createdUser);
        return generateCreatedResponse(URI.create(locationUri), createdUser).build();
    }

    /**
     * Retrieves all the of users in this NiFi.
     *
     * @return a list of users
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users")
    @ApiOperation(
            value = "Get all users",
            notes = NON_GUARANTEED_ENDPOINT,
            response = User.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getUsers() {
        verifyAuthorizerIsManaged();

        authorizeAccess(RequestAction.READ);

        // get all the users
        final List<User> users = authorizationService.getUsers();

        // generate the response
        return generateOkResponse(users).build();
    }

    /**
     * Retrieves the specified user.
     *
     * @param identifier The id of the user to retrieve
     * @return An userEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{id}")
    @ApiOperation(
            value = "Get user",
            notes = NON_GUARANTEED_ENDPOINT,
            response = User.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getUser(
            @ApiParam(value = "The user id.", required = true)
            @PathParam("id") final String identifier) {
        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        final User user = authorizationService.getUser(identifier);
        if (user == null) {
            logger.warn("The specified user id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user ID does not exist in this registry.");
        }
        return generateOkResponse(user).build();
    }

    /**
     * Updates a user.
     *
     * @param httpServletRequest request
     * @param identifier The id of the user to update
     * @param requestUser The user with updated fields.
     * @return The updated user
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{id}")
    @ApiOperation(
            value = "Update user",
            notes = NON_GUARANTEED_ENDPOINT,
            response = User.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response updateUser(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user id.", required = true)
            @PathParam("id")
            final String identifier,
            @ApiParam(value = "The user configuration details.", required = true)
            final User requestUser) {

        verifyAuthorizerSupportsConfigurableUserGroups();
        authorizeAccess(RequestAction.WRITE);

        if (requestUser == null) {
            throw new IllegalArgumentException("User details must be specified when updating a user.");
        }
        if (!identifier.equals(requestUser.getIdentifier())) {
            throw new IllegalArgumentException(String.format("The user id in the request body (%s) does not equal the "
                    + "user id of the requested resource (%s).", requestUser.getIdentifier(), identifier));
        }

        final User updatedUser = authorizationService.updateUser(requestUser);
        if (updatedUser == null) {
            logger.warn("The specified user id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user ID does not exist in this registry.");
        }
        publish(EventFactory.userUpdated(updatedUser));

        return generateOkResponse(updatedUser).build();
    }

    /**
     * Removes the specified user.
     *
     * @param httpServletRequest request
     * @param identifier         The id of the user to remove.
     * @return A entity containing the client id and an updated revision.
     */
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{id}")
    @ApiOperation(
            value = "Delete user",
            notes = NON_GUARANTEED_ENDPOINT,
            response = User.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "delete"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response removeUser(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user id.", required = true)
            @PathParam("id")
            final String identifier) {

        verifyAuthorizerSupportsConfigurableUserGroups();
        authorizeAccess(RequestAction.DELETE);

        final User user = authorizationService.deleteUser(identifier);
        if (user == null) {
            logger.warn("The specified user id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user ID does not exist in this registry.");
        }
        publish(EventFactory.userDeleted(user));

        return generateOkResponse(user).build();
    }


    // ---------- User Group endpoints --------------------------------------------------------------------------------

    /**
     * Creates a new user group.
     *
     * @param httpServletRequest request
     * @param requestUserGroup the user group to create
     * @return the created user group
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-groups")
    @ApiOperation(
            value = "Create user group",
            notes = NON_GUARANTEED_ENDPOINT,
            response = UserGroup.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response createUserGroup(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user group configuration details.", required = true)
            final UserGroup requestUserGroup) {

        verifyAuthorizerSupportsConfigurableUserGroups();
        authorizeAccess(RequestAction.WRITE);

        if (requestUserGroup == null) {
            throw new IllegalArgumentException("User group details must be specified when creating a new group.");
        }
        if (requestUserGroup.getIdentifier() != null) {
            throw new IllegalArgumentException("User group ID cannot be specified when creating a new group.");
        }
        if (StringUtils.isBlank(requestUserGroup.getIdentity())) {
            throw new IllegalArgumentException("User group identity must be specified when creating a new group.");
        }

        UserGroup createdGroup = authorizationService.createUserGroup(requestUserGroup);
        publish(EventFactory.userGroupCreated(createdGroup));

        String locationUri = generateUserGroupUri(createdGroup);
        return generateCreatedResponse(URI.create(locationUri), createdGroup).build();
    }

    /**
     * Retrieves all the of user groups in this NiFi.
     *
     * @return a list of all user groups in this NiFi.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-groups")
    @ApiOperation(
            value = "Get user groups",
            notes = NON_GUARANTEED_ENDPOINT,
            response = UserGroup.class,
            responseContainer = "List",
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getUserGroups() {
        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        final List<UserGroup> userGroups = authorizationService.getUserGroups();
        return generateOkResponse(userGroups).build();
    }

    /**
     * Retrieves the specified user group.
     *
     * @param identifier The id of the user group to retrieve
     * @return An userGroupEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-groups/{id}")
    @ApiOperation(
            value = "Get user group",
            notes = NON_GUARANTEED_ENDPOINT,
            response = UserGroup.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "read"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getUserGroup(
            @ApiParam(value = "The user group id.", required = true)
            @PathParam("id") final String identifier) {
        verifyAuthorizerIsManaged();
        authorizeAccess(RequestAction.READ);

        final UserGroup userGroup = authorizationService.getUserGroup(identifier);
        if (userGroup == null) {
            logger.warn("The specified user group id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user group ID does not exist in this registry.");
        }

        return generateOkResponse(userGroup).build();
    }

    /**
     * Updates a user group.
     *
     * @param httpServletRequest request
     * @param identifier The id of the user group to update.
     * @param requestUserGroup The user group with updated fields.
     * @return The resulting, updated user group.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-groups/{id}")
    @ApiOperation(
            value = "Update user group",
            notes = NON_GUARANTEED_ENDPOINT,
            response = UserGroup.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "write"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response updateUserGroup(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user group id.", required = true)
            @PathParam("id")
            final String identifier,
            @ApiParam(value = "The user group configuration details.", required = true)
            final UserGroup requestUserGroup) {

        verifyAuthorizerSupportsConfigurableUserGroups();

        if (requestUserGroup == null) {
            throw new IllegalArgumentException("User group details must be specified to update a user group.");
        }
        if (!identifier.equals(requestUserGroup.getIdentifier())) {
            throw new IllegalArgumentException(String.format("The user group id in the request body (%s) does not equal the "
                    + "user group id of the requested resource (%s).", requestUserGroup.getIdentifier(), identifier));
        }

        authorizeAccess(RequestAction.WRITE);

        UserGroup updatedUserGroup = authorizationService.updateUserGroup(requestUserGroup);
        if (updatedUserGroup == null) {
            logger.warn("The specified user group id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user group ID does not exist in this registry.");
        }
        publish(EventFactory.userGroupUpdated(updatedUserGroup));

        return generateOkResponse(updatedUserGroup).build();
    }

    /**
     * Removes the specified user group.
     *
     * @param httpServletRequest request
     * @param identifier                 The id of the user group to remove.
     * @return The deleted user group.
     */
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-groups/{id}")
    @ApiOperation(
            value = "Delete user group",
            notes = NON_GUARANTEED_ENDPOINT,
            response = UserGroup.class,
            extensions = {
                    @Extension(name = "access-policy", properties = {
                            @ExtensionProperty(name = "action", value = "delete"),
                            @ExtensionProperty(name = "resource", value = "/tenants") })
            }
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response removeUserGroup(
            @Context
            final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The user group id.", required = true)
            @PathParam("id")
            final String identifier) {
        verifyAuthorizerSupportsConfigurableUserGroups();
        authorizeAccess(RequestAction.DELETE);

        final UserGroup userGroup = authorizationService.deleteUserGroup(identifier);
        if (userGroup == null) {
            logger.warn("The specified user group id [{}] does not exist.", identifier);

            throw new ResourceNotFoundException("The specified user group ID does not exist in this registry.");
        }
        publish(EventFactory.userGroupDeleted(userGroup));

        return generateOkResponse(userGroup).build();
    }


    private void verifyAuthorizerIsManaged() {
        if (!AuthorizerCapabilityDetection.isManagedAuthorizer(authorizer)) {
            throw new IllegalStateException(AuthorizationService.MSG_NON_MANAGED_AUTHORIZER);
        }
    }

    private void verifyAuthorizerSupportsConfigurableUserGroups() {
        if (!AuthorizerCapabilityDetection.isConfigurableUserGroupProvider(authorizer)) {
            throw new IllegalStateException(AuthorizationService.MSG_NON_CONFIGURABLE_USERS);
        }
    }

    private void authorizeAccess(RequestAction actionType) {
        final Authorizable tenantsAuthorizable = authorizableLookup.getTenantsAuthorizable();
        authorizationService.authorize(tenantsAuthorizable, actionType);
    }

    private String generateUserUri(final User user) {
        return generateResourceUri("tenants", "users", user.getIdentifier());
    }

    private String generateUserGroupUri(final UserGroup userGroup) {
        return generateResourceUri("tenants", "user-groups", userGroup.getIdentifier());
    }

}
