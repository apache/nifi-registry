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
package org.apache.nifi.registry.web.security.authentication;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.security.authentication.AuthenticationRequest;
import org.apache.nifi.registry.security.authentication.IdentityProvider;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.security.util.ProxiedEntitiesUtils;
import org.apache.nifi.registry.web.security.authentication.exception.InvalidAuthenticationException;
import org.apache.nifi.registry.web.security.authentication.exception.UntrustedProxyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Note: This class is deprecated and is being considered for complete removal in favor of using {@link IdentityFilter}.
 *       It is remaining in place for the time being until the pattern of authentication implemented by {@link IdentityFilter}
 *       has been more thoroughly vetted in real use.
 */
@Deprecated
public class IdentityAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final RequestMatcher requiresAuthenticationRequestMatcher = new RequestMatcher() {
        @Override
        public boolean matches(HttpServletRequest httpServletRequest) {
            return NiFiUserUtils.getNiFiUser() == null;
        }
    };

    private final IdentityProvider identityProvider;

    public IdentityAuthenticationFilter(IdentityProvider identityProvider, AuthenticationManager authenticationManager, String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
        super.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher(defaultFilterProcessesUrl)); // Authentication will only be initiated for the request url matching this pattern
        setAuthenticationManager(authenticationManager);
        this.identityProvider = identityProvider;
    }

    public IdentityAuthenticationFilter(IdentityProvider identityProvider, AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher);
        setAuthenticationManager(authenticationManager);
        this.identityProvider = identityProvider;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException, IOException, ServletException {

        // Only require authentication from an identity provider if the NiFi registry is running securely.
        if (!httpServletRequest.isSecure()) {
            // Otherwise, requests will be "authenticated" by the AnonymousIdentityFilter
            //return null;
            return new ContinueFilterChainAuthentication(); // see successfulAuthentication for why we do this
        }

        AuthenticationRequest authenticationRequest = identityProvider.extractCredentials(httpServletRequest);
        if (authenticationRequest == null) {
            //return null;
            return new ContinueFilterChainAuthentication(); // see successfulAuthentication for why we do this
        }
        Authentication authentication = new AuthenticationRequestToken(authenticationRequest, identityProvider.getClass(), httpServletRequest.getRemoteAddr());
        Authentication authenticationResult = getAuthenticationManager().authenticate(authentication); // See IdentityProviderAuthenticationProvider for authentication impl.
        if (authenticationResult == null) {
            return new ContinueFilterChainAuthentication(); // see successfulAuthentication for why we do this
        } else {
            return authenticationResult;
        }
        // Super class will invoke successfulAuthentication() or unsuccessfulAuthentication() depending on the outcome of the authentication attempt
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        if (authResult.getClass().equals(ContinueFilterChainAuthentication.class)) {
            logger.info("Authentication unknown, continue chain");
            // Because this NiFi Registry might be configured with multiple AbstractAuthenticationProcessingFilter's,
            // the request should continue through the filter chain. If none of the IdentityProviderAuthenticationFilters
            // can authenticate the request and register a user identity, then the AnonymousIdentityFilter will assign the
            // Anonymous identity which will not be authorized for access.
            // A refinement of this would be to extend something other than AbstractAuthenticationProcessingFilter, such as
            // GenericFilterBean, or to register different filter chains based on context, such as only include
            // AbstractAuthenticationProcessingFilter(s) when running securely, otherwise don't register any and only register
            // the AnonymousIdentityFilter.
            chain.doFilter(request, response);
        }

        logger.info("Authentication success for " + authResult);

        SecurityContextHolder.getContext().setAuthentication(authResult);
        if (StringUtils.isNotBlank(request.getHeader(ProxiedEntitiesUtils.PROXY_ENTITIES_CHAIN))) {
            response.setHeader(ProxiedEntitiesUtils.PROXY_ENTITIES_ACCEPTED, Boolean.TRUE.toString());
        }

        // continue the filter chain, which now holds a NiFiUser in the SecurityContext's authentication
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        this.logger.debug("Authentication request failed: " + failed.toString(), failed);

        SecurityContextHolder.clearContext();
        this.logger.debug("Updated SecurityContextHolder to contain null Authentication");

        // populate the response
        if (StringUtils.isNotBlank(request.getHeader(ProxiedEntitiesUtils.PROXY_ENTITIES_CHAIN))) {
            response.setHeader(ProxiedEntitiesUtils.PROXY_ENTITIES_DETAILS, failed.getMessage());
        }

        // set the response status
        response.setContentType("text/plain");

        // write the response message
        PrintWriter out = response.getWriter();

        // use the type of authentication exception to determine the response code
        if (failed instanceof InvalidAuthenticationException) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println(failed.getMessage());
        } else if (failed instanceof UntrustedProxyException) { // thrown in X509IdentityProviderAuthenticationProvider
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.println(failed.getMessage());
        } else if (failed instanceof AuthenticationServiceException) {
            logger.error(String.format("Unable to authorize: %s", failed.getMessage()), failed);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(String.format("Unable to authorize: %s", failed.getMessage()));
        } else {
            logger.error(String.format("Unable to authorize: %s", failed.getMessage()), failed);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.println("Access is denied.");
        }

        // log the failure
        logger.warn(String.format("Rejecting access to web api: %s", failed.getMessage()));
        logger.debug(StringUtils.EMPTY, failed);
    }

    protected class ContinueFilterChainAuthentication implements Authentication {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {
            throw new IllegalArgumentException("Cannot set authenticated on ContinueFilterChainAuthentication");
        }

        @Override
        public String getName() {
            return null;
        }
    }

}
