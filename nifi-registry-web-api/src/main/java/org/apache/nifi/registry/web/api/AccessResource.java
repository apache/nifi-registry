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

import io.jsonwebtoken.JwtException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.exception.AdministrationException;
import org.apache.nifi.registry.model.authorization.AccessStatus;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.security.authentication.AuthenticationRequest;
import org.apache.nifi.registry.security.authentication.AuthenticationResponse;
import org.apache.nifi.registry.security.authentication.IdentityProvider;
import org.apache.nifi.registry.security.authentication.IdentityProviderUsage;
import org.apache.nifi.registry.security.authentication.UsernamePasswordAuthenticationRequest;
import org.apache.nifi.registry.security.authentication.exception.IdentityAccessException;
import org.apache.nifi.registry.security.authentication.exception.InvalidCredentialsException;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.web.exception.UnauthorizedException;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtService;
import org.apache.nifi.registry.web.security.authentication.kerberos.KerberosSpnegoIdentityProvider;
import org.apache.nifi.registry.web.security.authentication.x509.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Path("/access")
@Api(
        value = "/access",
        description = "Endpoints for obtaining an access token or checking access status."
)
public class AccessResource extends ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AccessResource.class);

    private NiFiRegistryProperties properties;
    private JwtService jwtService;
    private X509IdentityProvider x509IdentityProvider;
    private KerberosSpnegoIdentityProvider kerberosSpnegoIdentityProvider;
    private IdentityProvider identityProvider;

    @Autowired
    public AccessResource(
            NiFiRegistryProperties properties,
            JwtService jwtService,
            X509IdentityProvider x509IdentityProvider,
            @Nullable KerberosSpnegoIdentityProvider kerberosSpnegoIdentityProvider,
            @Nullable IdentityProvider identityProvider) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.x509IdentityProvider = x509IdentityProvider;
        this.kerberosSpnegoIdentityProvider = kerberosSpnegoIdentityProvider;
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
    @Path("/token")
    @ApiOperation(
            value = "Creates a token for accessing the REST API via auto-detected method of verifying client identity claim credentials",
            notes = "The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, " +
                    "the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header " +
                    "in the format 'Authorization: Bearer <token>'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login with username/password."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response createAccessTokenByTryingAllProviders(@Context HttpServletRequest httpServletRequest) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        List<IdentityProvider> identityProviderWaterfall = generateIdentityProviderWaterfall();

        String token = null;
        for (IdentityProvider provider : identityProviderWaterfall) {

            AuthenticationRequest authenticationRequest = identityProvider.extractCredentials(httpServletRequest);
            if (authenticationRequest == null) {
                continue;
            }
            try {
                token = createAccessToken(identityProvider, authenticationRequest);
                break;
            } catch (final InvalidCredentialsException ice){
                logger.debug("{}: the supplied client credentials are invalid.", identityProvider.getClass().getSimpleName());
                logger.debug("", ice);
            }

        }

        if (StringUtils.isEmpty(token)) {
            List<IdentityProviderUsage.AuthType> acceptableAuthTypes = identityProviderWaterfall.stream()
                    .map(IdentityProvider::getUsageInstructions)
                    .map(IdentityProviderUsage::getAuthType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            throw new UnauthorizedException("Client credentials are missing or invalid according to all configured identity providers.")
                    .withAuthenticateChallenge(acceptableAuthTypes);
        }

        // build the response
        final URI uri = URI.create(generateResourceUri("access", "token"));
        return generateCreatedResponse(uri, token).build();
    }

    /**
     * Creates a token for accessing the REST API.
     *
     * @param httpServletRequest the servlet request
     * @return A JWT (string)
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
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

        // generate JWT for response
        AuthenticationRequest authenticationRequest = new UsernamePasswordAuthenticationRequest(username, password);
        final String token;
        try {
             token = createAccessToken(identityProvider, authenticationRequest);
        } catch (final InvalidCredentialsException ice){
            throw new UnauthorizedException("The supplied client credentials are not valid.", ice)
                    .withAuthenticateChallenge("form-login");
        }

        // form the response
        final URI uri = URI.create(generateResourceUri("access", "token"));
        return generateCreatedResponse(uri, token).build();
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token/kerberos")
    @ApiOperation(
            value = "Creates a token for accessing the REST API via Kerberos Service Tickets or SPNEGO Tokens (which includes Kerberos Service Tickets)",
            notes = "The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, " +
                    "the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header " +
                    "in the format 'Authorization: Bearer <token>'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support login Kerberos credentials."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response createAccessTokenUsingKerberosTicket(@Context HttpServletRequest httpServletRequest) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        // if not configured with custom identity provider, don't consider credentials
        if (!properties.isKerberosSpnegoSupportEnabled() || kerberosSpnegoIdentityProvider == null) {
            throw new IllegalStateException("Kerberos service ticket login not supported by this NiFi Registry");
        }

        AuthenticationRequest authenticationRequest = kerberosSpnegoIdentityProvider.extractCredentials(httpServletRequest);

        if (authenticationRequest == null) {
            throw new UnauthorizedException("The client credentials are missing from the request.")
                    .withAuthenticateChallenge(kerberosSpnegoIdentityProvider.getUsageInstructions().getAuthType());
        }

        final String token;
        try {
            token = createAccessToken(kerberosSpnegoIdentityProvider, authenticationRequest);
        } catch (final InvalidCredentialsException ice){
            throw new UnauthorizedException("The supplied client credentials are not valid.", ice)
                    .withAuthenticateChallenge(kerberosSpnegoIdentityProvider.getUsageInstructions().getAuthType());
        }

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
                    "The exact format of the user credentials expected by the custom identity provider can be discovered by 'GET /access/token/identity-provider/usage'. " +
                    "The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, " +
                    "the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header " +
                    "in the format 'Authorization: Bearer <token>'.",
            response = String.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
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

        AuthenticationRequest authenticationRequest = identityProvider.extractCredentials(httpServletRequest);

        if (authenticationRequest == null) {
            throw new UnauthorizedException("The client credentials are missing from the request.")
                    .withAuthenticateChallenge(identityProvider.getUsageInstructions().getAuthType());
        }

        final String token;
        try {
            token = createAccessToken(identityProvider, authenticationRequest);
        } catch (InvalidCredentialsException ice) {
            throw new UnauthorizedException("The supplied client credentials are not valid.", ice)
                    .withAuthenticateChallenge(identityProvider.getUsageInstructions().getAuthType());
        }

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
            value = "Provides a description of how the currently configured identity provider expects credentials to be passed to POST /access/token/identity-provider",
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
            // If, for any reason, this identity provider does not support getUsageInstructions(), e.g., returns null or throws NotImplementedException.
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
            notes = "The user credentials should be passed in a format understood by the custom identity provider as defined by 'GET /access/token/identity-provider/usage'.",
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

        if (authenticationRequest == null) {
            throw new UnauthorizedException("The format of the credentials were not recognized by the currently configured identity provider " +
                    "'" + identityProviderName + "'. " + identityProvider.getUsageInstructions().getText())
                    .withAuthenticateChallenge(identityProvider.getUsageInstructions().getAuthType());
        }


        final String successMessage = identityProviderName + " recognized the format of the credentials in the HTTP request.";
        return generateOkResponse(successMessage).build();

    }

    private String createAccessToken(IdentityProvider identityProvider, AuthenticationRequest authenticationRequest)
            throws InvalidCredentialsException, AdministrationException {

        final AuthenticationResponse authenticationResponse;

        try {
            authenticationResponse = identityProvider.authenticate(authenticationRequest);
            final String token = jwtService.generateSignedToken(authenticationResponse);
            return token;
        } catch (final IdentityAccessException | JwtException e) {
            throw new AdministrationException(e.getMessage());
        }

    }

    /**
     * A helper function that generates a prioritized list of IdentityProviders to use to
     * attempt client authentication.
     *
     * Note: This is currently a hard-coded list order consisting of:
     *
     * - X509IdentityProvider (if available)
     * - KerberosProvider (if available)
     * - User-defined IdentityProvider (if available)
     *
     * However, in the future it could be entirely user-configurable
     *
     * @return a list of providers to use in order to authenticate the client.
     */
    private List<IdentityProvider> generateIdentityProviderWaterfall() {
        List<IdentityProvider> identityProviderWaterfall = new ArrayList<>();

        // if configured with an X509IdentityProvider, add it to the list of providers to try
        if (x509IdentityProvider != null) {
            identityProviderWaterfall.add(x509IdentityProvider);
        }

        // if configured with an KerberosSpnegoIdentityProvider, add it to the end of the list of providers to try
        if (kerberosSpnegoIdentityProvider != null) {
            identityProviderWaterfall.add(kerberosSpnegoIdentityProvider);
        }

        // if configured with custom identity provider, add it to the end of the list of providers to try
        if (identityProvider != null) {
            identityProviderWaterfall.add(identityProvider);
        }

        return identityProviderWaterfall;
    }

}
