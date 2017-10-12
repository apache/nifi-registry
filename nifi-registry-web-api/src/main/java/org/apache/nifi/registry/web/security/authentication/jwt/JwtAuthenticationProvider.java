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
package org.apache.nifi.registry.web.security.authentication.jwt;

import io.jsonwebtoken.JwtException;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.NiFiUserDetails;
import org.apache.nifi.registry.security.authorization.user.StandardNiFiUser;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.web.security.authentication.exception.InvalidAuthenticationException;
import org.apache.nifi.registry.web.security.authentication.NiFiAuthenticationProvider;
import org.apache.nifi.registry.web.security.authentication.token.NiFiAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class JwtAuthenticationProvider extends NiFiAuthenticationProvider {

    private final JwtService jwtService;

    @Autowired
    public JwtAuthenticationProvider(JwtService jwtService, NiFiRegistryProperties nifiProperties, Authorizer authorizer) {
        super(nifiProperties, authorizer);
        this.jwtService = jwtService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final JwtAuthenticationRequestToken request = (JwtAuthenticationRequestToken) authentication;

        try {
            final String jwtPrincipal = jwtService.getAuthenticationFromToken(request.getToken());
            final String mappedIdentity = mapIdentity(jwtPrincipal);
            final NiFiUser user = new StandardNiFiUser.Builder()
                    .identity(mappedIdentity)
                    .groups(getUserGroups(mappedIdentity))
                    .clientAddress(request.getClientAddress())
                    .build();
            return new NiFiAuthenticationToken(new NiFiUserDetails(user));
        } catch (JwtException e) {
            throw new InvalidAuthenticationException(e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationRequestToken.class.isAssignableFrom(authentication);
    }
}
