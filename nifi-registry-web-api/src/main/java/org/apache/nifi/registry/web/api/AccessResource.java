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
import org.apache.nifi.registry.exception.AdministrationException;
import org.apache.nifi.registry.model.authorization.AccessStatus;
import org.apache.nifi.registry.security.authentication.AuthenticationRequest;
import org.apache.nifi.registry.security.authentication.AuthenticationResponse;
import org.apache.nifi.registry.security.authentication.IdentityProvider;
import org.apache.nifi.registry.security.authentication.UsernamePasswordAuthenticationRequest;
import org.apache.nifi.registry.security.authentication.exception.IdentityAccessException;
import org.apache.nifi.registry.security.authentication.exception.InvalidCredentialsException;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Component
@Path("/access")
@Api(
        value = "/access",
        description = "Endpoints for obtaining an access token or checking access status."
)
public class AccessResource extends ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AccessResource.class);

    private IdentityProvider identityProvider;
    private JwtService jwtService;

    @Autowired
    public AccessResource(
            AuthorizationService authorizationService,
            JwtService jwtService,
            IdentityProvider identityProvider) {
        this.jwtService = jwtService;
        this.identityProvider = identityProvider;
    }

    /**
     * Gets the status the client's access.
     *
     * @param httpServletRequest the servlet request
     * @return A accessStatusEntity
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the status the client's access",
            response = AccessStatus.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry might be running unsecured.") })
    public Response getAccessStatus(@Context HttpServletRequest httpServletRequest) {
        // only consider user specific access over https
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("User authentication/authorization is only supported when running over HTTPS.");
        }

        final AccessStatus accessStatus = new AccessStatus();

        NiFiUser currentUser = NiFiUserUtils.getNiFiUser();
        if (currentUser == null || currentUser.getIdentity() == null || currentUser.isAnonymous()) {
            accessStatus.setStatus(AccessStatus.Status.UNKNOWN.name());
            accessStatus.setMessage("No credentials supplied, unknown user.");
        } else {
            final String identity = currentUser.getIdentity();
            accessStatus.setIdentity(identity);
            accessStatus.setStatus(AccessStatus.Status.ACTIVE.name());
            accessStatus.setMessage("You are logged in.");
        }

        return generateOkResponse(accessStatus).build();
    }

    /**
     * Creates a token for accessing the REST API.
     *
     * @param httpServletRequest the servlet request
     * @return A JWT (string)
     */
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token/login")
    @ApiOperation(
            value = "Creates a token for accessing the REST API via username/password",
            notes = "The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, " +
                    "the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header " +
                    "in the format 'Authorization: Bearer <token>'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login with username/password."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response createAccessTokenUsingFormLogin(
            @Context HttpServletRequest httpServletRequest,
            @FormParam("username") String username,
            @FormParam("password") String password) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        // if not configured with custom identity provider, or if provider doesn't support username/password authentication, don't consider credentials
        if (identityProvider == null || !identityProvider.supports(UsernamePasswordAuthenticationRequest.class)) {
            throw new IllegalStateException("Username/Password login not supported by this NiFi");
        }

        // ensure we have login credentials
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("The username and password must be specified");
        }

        final AuthenticationResponse authenticationResponse;

        try {
            // attempt to authenticate
            AuthenticationRequest authenticationRequest = new UsernamePasswordAuthenticationRequest(username, password);
            authenticationResponse = identityProvider.authenticate(authenticationRequest);
        } catch (final InvalidCredentialsException ice) {
            throw new IllegalArgumentException("The supplied client credentials are not valid.", ice);
        } catch (final IdentityAccessException iae) {
            throw new AdministrationException(iae.getMessage(), iae);
        }

        // generate JWT for response
        final String token = jwtService.generateSignedToken(authenticationResponse);

        // build the response
        final URI uri = URI.create(generateResourceUri("access", "token"));
        return generateCreatedResponse(uri, token).build();
    }

    /**
     * Creates a token for accessing the REST API using a custom identity provider configured using NiFi Registry extensions.
     *
     * @param httpServletRequest the servlet request
     * @return A JWT (string)
     */
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token/identity-provider")
    @ApiOperation(
            value = "Creates a token for accessing the REST API via a custom identity provider.",
            notes = "The user credentials must be passed in a format understood by the custom identity provider, e.g., a third-party auth token in an HTTP header. " +
                    "The exact format of the user credentials expected by the custom identity provider can be discovered by 'GET /token/identity-provider/usage'. " +
                    "The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, " +
                    "the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header " +
                    "in the format 'Authorization: Bearer <token>'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login with customized credentials."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response createAccessTokenUsingIdentityProviderCredentials(@Context HttpServletRequest httpServletRequest) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        // if not configured with custom identity provider, don't consider credentials
        if (identityProvider == null) {
            throw new IllegalStateException("Custom login not supported by this NiFi Registry");
        }

        final AuthenticationResponse authenticationResponse;

        try {
            // attempt to authenticate
            AuthenticationRequest authenticationRequest = identityProvider.extractCredentials(httpServletRequest);
            authenticationResponse = identityProvider.authenticate(authenticationRequest);
        } catch (final InvalidCredentialsException ice) {
            throw new IllegalArgumentException("The supplied client credentials are not valid.", ice);
        } catch (final IdentityAccessException iae) {
            throw new AdministrationException(iae.getMessage(), iae);
        }

        // generate JWT for response
        final String token = jwtService.generateSignedToken(authenticationResponse);

        // build the response
        final URI uri = URI.create(generateResourceUri("access", "token"));
        return generateCreatedResponse(uri, token).build();
    }

    /**
     * Creates a token for accessing the REST API using a custom identity provider configured using NiFi Registry extensions.
     *
     * @param httpServletRequest the servlet request
     * @return A JWT (string)
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token/identity-provider/usage")
    @ApiOperation(
            value = "Provides a description of how the currently configured identity provider expects credentials to be passed to POST /token/identity-provider",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login with customized credentials."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response getIdentityProviderUsageInstructions(@Context HttpServletRequest httpServletRequest) {

        // if not configuration for login, don't consider credentials
        if (identityProvider == null) {
            throw new IllegalStateException("Custom login not supported by this NiFi Registry");
        }

        Class ipClazz = identityProvider.getClass();
        String identityProviderName = StringUtils.isNotEmpty(ipClazz.getSimpleName()) ? ipClazz.getSimpleName() : ipClazz.getName();

        try {
            String usageInstructions = "Usage Instructions for '" + identityProviderName + "': ";
            usageInstructions += identityProvider.getUsageInstructions().getText();
            return generateOkResponse(usageInstructions).build();

        } catch (Exception e) {
            // If, for any reason, this identity provider does not support getUsageInstructions(), e.g., throws NotImplemented Exception.
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                    .entity("The currently configured identity provider, '" + identityProvider.getClass().getName() + "' does not provide usage instructions.")
                    .build();
        }

    }

    /**
     * Creates a token for accessing the REST API using a custom identity provider configured using NiFi Registry extensions.
     *
     * @param httpServletRequest the servlet request
     * @return A JWT (string)
     */
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token/identity-provider/test")
    @ApiOperation(
            value = "Tests the format of the credentials against this identity provider without preforming authentication on the credentials to validate them.",
            notes = "The user credentials should be passed in a format understood by the custom identity provider as defined by 'GET /token/identity-provider/usage'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = "The format of the credentials were not recognized by the currently configured identity provider."),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login with customized credentials."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response testIdentityProviderRecognizesCredentialsFormat(@Context HttpServletRequest httpServletRequest) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        // if not configured with custom identity provider, don't consider credentials
        if (identityProvider == null) {
            throw new IllegalStateException("Custom login not supported by this NiFi Registry");
        }

        final Class ipClazz = identityProvider.getClass();
        final String identityProviderName = StringUtils.isNotEmpty(ipClazz.getSimpleName()) ? ipClazz.getSimpleName() : ipClazz.getName();

        // attempt to extract client credentials without authenticating them
        AuthenticationRequest authenticationRequest = identityProvider.extractCredentials(httpServletRequest);
        if (authenticationRequest != null) {
            final String successMessage = identityProviderName + " recognized the format of the credentials in the HTTP request.";
            return generateOkResponse(successMessage).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity("The format of the credentials were not recognized by the currently configured identity provider " +
                        "'" + identityProviderName + "'. See GET /token/identity-provider/usage for more information.")
                .build();

    }

}
