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
package org.apache.nifi.registry.web.security;

import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.web.security.authentication.AnonymousIdentityFilter;
import org.apache.nifi.registry.web.security.authentication.IdentityAuthenticationProvider;
import org.apache.nifi.registry.web.security.authentication.IdentityFilter;
import org.apache.nifi.registry.web.security.authentication.jwt.JwtIdentityProvider;
import org.apache.nifi.registry.web.security.authentication.x509.X509IdentityAuthenticationProvider;
import org.apache.nifi.registry.web.security.authentication.x509.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * NiFi Registry Web Api Spring security
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class NiFiRegistrySecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistrySecurityConfig.class);

    @Autowired private NiFiRegistryProperties properties;

    @Autowired private Authorizer authorizer;

    private AnonymousIdentityFilter anonymousAuthenticationFilter = new AnonymousIdentityFilter();

    @Autowired private X509IdentityProvider x509IdentityProvider;
    private IdentityFilter x509AuthenticationFilter;
    private IdentityAuthenticationProvider x509AuthenticationProvider;

    @Autowired private JwtIdentityProvider jwtIdentityProvider;
    private IdentityFilter jwtAuthenticationFilter;
    private IdentityAuthenticationProvider jwtAuthenticationProvider;

    public NiFiRegistrySecurityConfig() {
        super(true); // disable defaults
    }

    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        // allow any client to access the endpoint for logging in to generate an access token
        webSecurity.ignoring().antMatchers( "/access/token/*");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .rememberMe().disable()
                .authorizeRequests()
                    .anyRequest().fullyAuthenticated()
                    .and()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // x509
        http.addFilterBefore(x509AuthenticationFilter(), AnonymousAuthenticationFilter.class);

        // jwt
        http.addFilterBefore(jwtAuthenticationFilter(), AnonymousAuthenticationFilter.class);

        // otp
        // todo, if needed one-time password auth filter goes here

        // anonymous
        http.anonymous().authenticationFilter(anonymousAuthenticationFilter);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(x509AuthenticationProvider())
                .authenticationProvider(jwtAuthenticationProvider());
    }

    private IdentityFilter x509AuthenticationFilter() throws Exception {
        if (x509AuthenticationFilter == null) {
            x509AuthenticationFilter = new IdentityFilter(x509IdentityProvider);
        }
        return x509AuthenticationFilter;
    }

    private IdentityAuthenticationProvider x509AuthenticationProvider() {
        if (x509AuthenticationProvider == null) {
            x509AuthenticationProvider = new X509IdentityAuthenticationProvider(properties, authorizer, x509IdentityProvider);
        }
        return x509AuthenticationProvider;
    }

    private IdentityFilter jwtAuthenticationFilter() throws Exception {
        if (jwtAuthenticationFilter == null) {
            jwtAuthenticationFilter = new IdentityFilter(jwtIdentityProvider);
        }
        return jwtAuthenticationFilter;
    }

    private IdentityAuthenticationProvider jwtAuthenticationProvider() {
        if (jwtAuthenticationProvider == null) {
            jwtAuthenticationProvider = new X509IdentityAuthenticationProvider(properties, authorizer, jwtIdentityProvider);
        }
        return jwtAuthenticationProvider;
    }

}
