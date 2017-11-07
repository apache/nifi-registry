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
import org.apache.nifi.registry.security.authentication.AuthenticationResponse;
import org.apache.nifi.registry.security.authentication.LoginCredentials;
import org.apache.nifi.registry.security.authentication.LoginIdentityProvider;
import org.apache.nifi.registry.security.authentication.exception.IdentityAccessException;
import org.apache.nifi.registry.security.authentication.exception.InvalidLoginCredentialsException;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.NiFiUserDetails;
import org.apache.nifi.registry.exception.AdministrationException;
import org.apache.nifi.registry.model.authorization.AccessStatus;
import org.apache.nifi.registry.web.security.authentication.exception.InvalidAuthenticationException;
import org.apache.nifi.registry.web.security.authentication.ProxiedEntitiesUtils;
import org.apache.nifi.registry.web.security.authentication.exception.UntrustedProxyException;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtAuthenticationFilter;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtAuthenticationProvider;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtAuthenticationRequestToken;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtService;
import org.apache.nifi.registry.web.security.authentication.token.LoginAuthenticationToken;
import org.apache.nifi.registry.web.security.authentication.token.NiFiAuthenticationToken;
import org.apache.nifi.registry.web.security.authentication.x509.X509AuthenticationProvider;
import org.apache.nifi.registry.web.security.authentication.x509.X509AuthenticationRequestToken;
import org.apache.nifi.registry.web.security.authentication.x509.X509CertificateExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
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
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Component
@Path("/access")
@Api(
        value = "/access",
        description = "Endpoints for obtaining an access token or checking access status."
)
public class AccessResource extends ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AccessResource.class);

    private X509CertificateExtractor certificateExtractor;
    private X509AuthenticationProvider x509AuthenticationProvider;
    private X509PrincipalExtractor x509principalExtractor;
    private LoginIdentityProvider loginIdentityProvider;
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    private JwtService jwtService;

    @Autowired
    public AccessResource(X509CertificateExtractor certificateExtractor,
                          X509AuthenticationProvider x509AuthenticationProvider,
                          X509PrincipalExtractor x509principalExtractor,
                          LoginIdentityProvider loginIdentityProvider,
                          JwtAuthenticationProvider jwtAuthenticationProvider,
                          JwtService jwtService
    ) {
        this.certificateExtractor = certificateExtractor;
        this.x509AuthenticationProvider = x509AuthenticationProvider;
        this.x509principalExtractor = x509principalExtractor;
        this.loginIdentityProvider = loginIdentityProvider;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.jwtService = jwtService;
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

        try {
            final X509Certificate[] certificates = certificateExtractor.extractClientCertificate(httpServletRequest);

            // if there is not certificate, consider a token
            if (certificates == null) {

                // look for an authorization token
                final String authorization = httpServletRequest.getHeader(JwtAuthenticationFilter.AUTHORIZATION);

                // if there is no authorization header, we don't know the user
                if (authorization == null) {
                    accessStatus.setStatus(AccessStatus.Status.UNKNOWN.name());
                    accessStatus.setMessage("No credentials supplied, unknown user.");
                } else {
                    try {
                        // Extract the Base64 encoded token from the Authorization header
                        final String token = StringUtils.substringAfterLast(authorization, " ");

                        final JwtAuthenticationRequestToken jwtRequest = new JwtAuthenticationRequestToken(token, httpServletRequest.getRemoteAddr());
                        final NiFiAuthenticationToken authenticationResponse = (NiFiAuthenticationToken) jwtAuthenticationProvider.authenticate(jwtRequest);
                        final NiFiUser nifiUser = ((NiFiUserDetails) authenticationResponse.getDetails()).getNiFiUser();

                        // set the user identity
                        accessStatus.setIdentity(nifiUser.getIdentity());

                        // attempt authorize to /flow
                        accessStatus.setStatus(AccessStatus.Status.ACTIVE.name());
                        accessStatus.setMessage("You are already logged in.");
                    } catch (JwtException e) {
                        throw new InvalidAuthenticationException(e.getMessage(), e);
                    }
                }
            } else {
                try {
                    final X509AuthenticationRequestToken x509Request = new X509AuthenticationRequestToken(
                            httpServletRequest.getHeader(ProxiedEntitiesUtils.PROXY_ENTITIES_CHAIN), x509principalExtractor, certificates, httpServletRequest.getRemoteAddr());

                    final NiFiAuthenticationToken authenticationResponse = (NiFiAuthenticationToken) x509AuthenticationProvider.authenticate(x509Request);
                    final NiFiUser nifiUser = ((NiFiUserDetails) authenticationResponse.getDetails()).getNiFiUser();

                    // set the user identity
                    accessStatus.setIdentity(nifiUser.getIdentity());

                    // attempt authorize to /flow
                    accessStatus.setStatus(AccessStatus.Status.ACTIVE.name());
                    accessStatus.setMessage("You are already logged in.");
                } catch (final IllegalArgumentException iae) {
                    throw new InvalidAuthenticationException(iae.getMessage(), iae);
                }
            }
        } catch (final UntrustedProxyException upe) {
            throw new AccessDeniedException(upe.getMessage(), upe);
        } catch (final AuthenticationServiceException ase) {
            throw new AdministrationException(ase.getMessage(), ase);
        }

        return generateOkResponse(accessStatus).build();
    }

    /**
     * Creates a token for accessing the REST API via username/password.
     *
     * @param httpServletRequest the servlet request
     * @param username           the username
     * @param password           the password
     * @return A JWT (string)
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/token")
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
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409 + " The NiFi Registry may not be configured to support username/password login."),
            @ApiResponse(code = 500, message = HttpStatusMessages.MESSAGE_500) })
    public Response createAccessToken(
            @Context HttpServletRequest httpServletRequest,
            @FormParam("username") String username,
            @FormParam("password") String password) {

        // only support access tokens when communicating over HTTPS
        if (!httpServletRequest.isSecure()) {
            throw new IllegalStateException("Access tokens are only issued over HTTPS");
        }

        // if not configuration for login, don't consider credentials
        if (loginIdentityProvider == null) {
            throw new IllegalStateException("Username/Password login not supported by this NiFi");
        }

        final LoginAuthenticationToken loginAuthenticationToken;

        // ensure we have login credentials
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("The username and password must be specified");
        }

        try {
            // attempt to authenticate
            final AuthenticationResponse authenticationResponse = loginIdentityProvider.authenticate(new LoginCredentials(username, password));
            long expiration = validateTokenExpiration(authenticationResponse.getExpiration(), authenticationResponse.getIdentity());

            // create the authentication token
            loginAuthenticationToken = new LoginAuthenticationToken(authenticationResponse.getIdentity(), expiration, authenticationResponse.getIssuer());
        } catch (final InvalidLoginCredentialsException ilce) {
            throw new IllegalArgumentException("The supplied username and password are not valid.", ilce);
        } catch (final IdentityAccessException iae) {
            throw new AdministrationException(iae.getMessage(), iae);
        }

        // generate JWT for response
        final String token = jwtService.generateSignedToken(loginAuthenticationToken);

        // build the response
        final URI uri = URI.create(generateResourceUri("access", "token"));
        return generateCreatedResponse(uri, token).build();
    }

    private long validateTokenExpiration(long proposedTokenExpiration, String identity) {
        final long maxExpiration = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
        final long minExpiration = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

        if (proposedTokenExpiration > maxExpiration) {
            logger.warn(String.format("Max token expiration exceeded. Setting expiration to %s from %s for %s", maxExpiration,
                    proposedTokenExpiration, identity));
            proposedTokenExpiration = maxExpiration;
        } else if (proposedTokenExpiration < minExpiration) {
            logger.warn(String.format("Min token expiration not met. Setting expiration to %s from %s for %s", minExpiration,
                    proposedTokenExpiration, identity));
            proposedTokenExpiration = minExpiration;
        }

        return proposedTokenExpiration;
    }



}
