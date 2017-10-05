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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.nifi.registry.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.authorization.user.NiFiUser;
import org.apache.nifi.registry.authorization.user.NiFiUserDetails;
import org.apache.nifi.registry.exception.AdministrationException;
import org.apache.nifi.registry.model.authorization.AccessStatus;
import org.apache.nifi.registry.web.security.InvalidAuthenticationException;
import org.apache.nifi.registry.web.security.ProxiedEntitiesUtils;
import org.apache.nifi.registry.web.security.UntrustedProxyException;
import org.apache.nifi.registry.web.security.token.NiFiAuthenticationToken;
import org.apache.nifi.registry.web.security.x509.X509AuthenticationProvider;
import org.apache.nifi.registry.web.security.x509.X509AuthenticationRequestToken;
import org.apache.nifi.registry.web.security.x509.X509CertificateExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;

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

    public AccessResource(X509CertificateExtractor certificateExtractor, X509AuthenticationProvider x509AuthenticationProvider, X509PrincipalExtractor x509principalExtractor) {
        this.certificateExtractor = certificateExtractor;
        this.x509AuthenticationProvider = x509AuthenticationProvider;
        this.x509principalExtractor = x509principalExtractor;
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
    @Path("")
    @ApiOperation(
            value = "Gets the status the client's access",
            response = AccessStatus.class
    )
    @ApiResponses({
                    @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
                    @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
                    @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
                    @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
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

                // TODO - add JWT Authentication support
                throw new NotImplementedException("NiFi Registry client is trying to authentication with something other than a client cert. " +
                        "At this time, only client certificate authentication is supported.");

//                // look for an authorization token
//                final String authorization = httpServletRequest.getHeader(JwtAuthenticationFilter.AUTHORIZATION);
//
//                // if there is no authorization header, we don't know the user
//                if (authorization == null) {
//                    accessStatus.setStatus(AccessStatus.Status.UNKNOWN.name());
//                    accessStatus.setMessage("No credentials supplied, unknown user.");
//                } else {
//                    try {
//                        // Extract the Base64 encoded token from the Authorization header
//                        final String token = StringUtils.substringAfterLast(authorization, " ");
//
//                        final JwtAuthenticationRequestToken jwtRequest = new JwtAuthenticationRequestToken(token, httpServletRequest.getRemoteAddr());
//                        final NiFiAuthenticationToken authenticationResponse = (NiFiAuthenticationToken) jwtAuthenticationProvider.authenticate(jwtRequest);
//                        final NiFiUser nifiUser = ((NiFiUserDetails) authenticationResponse.getDetails()).getNiFiUser();
//
//                        // set the user identity
//                        accessStatus.setIdentity(nifiUser.getIdentity());
//
//                        // attempt authorize to /flow
//                        accessStatus.setStatus(AccessStatus.Status.ACTIVE.name());
//                        accessStatus.setMessage("You are already logged in.");
//                    } catch (JwtException e) {
//                        throw new InvalidAuthenticationException(e.getMessage(), e);
//                    }
//                }
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



}
