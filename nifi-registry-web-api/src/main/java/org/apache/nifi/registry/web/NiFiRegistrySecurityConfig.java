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
package org.apache.nifi.registry.web;

import org.apache.nifi.registry.authorization.AuthorizableLookup;
import org.apache.nifi.registry.authorization.Authorizer;
import org.apache.nifi.registry.authorization.AuthorizerFactory;
import org.apache.nifi.registry.authorization.StandardAuthorizableLookup;
import org.apache.nifi.registry.authorization.StandardAuthorizerFactory;
import org.apache.nifi.registry.extension.ExtensionManager;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.web.security.NiFiAnonymousUserFilter;
import org.apache.nifi.registry.web.security.x509.SubjectDnX509PrincipalExtractor;
import org.apache.nifi.registry.web.security.x509.X509AuthenticationFilter;
import org.apache.nifi.registry.web.security.x509.X509AuthenticationProvider;
import org.apache.nifi.registry.web.security.x509.X509CertificateExtractor;
import org.apache.nifi.registry.web.security.x509.X509CertificateValidator;
import org.apache.nifi.registry.web.security.x509.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

/**
 * NiFi Web Api Spring security
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class NiFiRegistrySecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistrySecurityConfig.class);

    private final NiFiRegistryProperties properties;
    private final ExtensionManager extensionManager;

    private X509AuthenticationFilter x509AuthenticationFilter;

//    private JwtAuthenticationFilter jwtAuthenticationFilter;
//    private JwtAuthenticationProvider jwtAuthenticationProvider;
//
//    private OtpAuthenticationFilter otpAuthenticationFilter;
//    private OtpAuthenticationProvider otpAuthenticationProvider;

    private NiFiAnonymousUserFilter anonymousAuthenticationFilter;

    @Autowired
    public NiFiRegistrySecurityConfig(final NiFiRegistryProperties properties, final ExtensionManager extensionManager) {
        super(true); // disable defaults
        this.properties = properties;
        this.extensionManager = extensionManager;
    }

    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        // ignore the access endpoints for obtaining the access config, access token
        // granting, and access status for a given user (note: we are not ignoring the
        // the /access/download-token endpoints)
        webSecurity
                .ignoring()
                    .antMatchers("/access", "/access/config", "/access/token");
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
        // http.addFilterBefore(jwtFilterBean(), AnonymousAuthenticationFilter.class);

        // otp
        // http.addFilterBefore(otpFilterBean(), AnonymousAuthenticationFilter.class);

        // anonymous
        http.anonymous().authenticationFilter(anonymousFilter());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        // override xxxBean method so the authentication manager is available in app context (necessary for the method level security)
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(x509AuthenticationProvider());
        /* TODO, add Jwt and Otp support */
//                .authenticationProvider(jwtAuthenticationProvider)
//                .authenticationProvider(otpAuthenticationProvider);
    }

//    @Bean // TODO JwtAuthenticationFilter
//    public JwtAuthenticationFilter jwtFilterBean() throws Exception {
//        if (jwtAuthenticationFilter == null) {
//            jwtAuthenticationFilter = new JwtAuthenticationFilter();
//            jwtAuthenticationFilter.setProperties(properties);
//            jwtAuthenticationFilter.setAuthenticationManager(authenticationManager());
//        }
//        return jwtAuthenticationFilter;
//    }
//
//    @Bean // TODO OtpAuthenticationFilter
//    public OtpAuthenticationFilter otpFilterBean() throws Exception {
//        if (otpAuthenticationFilter == null) {
//            otpAuthenticationFilter = new OtpAuthenticationFilter();
//            otpAuthenticationFilter.setProperties(properties);
//            otpAuthenticationFilter.setAuthenticationManager(authenticationManager());
//        }
//        return otpAuthenticationFilter;
//    }

    @Bean
    public X509AuthenticationFilter x509AuthenticationFilter() throws Exception {
        if (x509AuthenticationFilter == null) {
            x509AuthenticationFilter = new X509AuthenticationFilter();
            x509AuthenticationFilter.setProperties(properties);
            x509AuthenticationFilter.setCertificateExtractor(certificateExtractor());
            x509AuthenticationFilter.setPrincipalExtractor(principalExtractor());
            x509AuthenticationFilter.setAuthenticationManager(authenticationManager());
        }
        return x509AuthenticationFilter;
    }

    @Bean
    public NiFiAnonymousUserFilter anonymousFilter() throws Exception {
        if (anonymousAuthenticationFilter == null) {
            anonymousAuthenticationFilter = new NiFiAnonymousUserFilter();
        }
        return anonymousAuthenticationFilter;
    }

    @Bean
    public X509CertificateExtractor certificateExtractor() {
        return new X509CertificateExtractor();
    }

    public X509CertificateValidator certificateValidator() {
        return new X509CertificateValidator();
    }

    @Bean
    public X509PrincipalExtractor principalExtractor() {
        return new SubjectDnX509PrincipalExtractor();
    }

    @Bean
    public X509IdentityProvider x509IdentityProvider() {
        X509IdentityProvider x509IdentityProvider = new X509IdentityProvider();
        x509IdentityProvider.setCertificateValidator(certificateValidator());
        x509IdentityProvider.setPrincipalExtractor(principalExtractor());
        return x509IdentityProvider;
    }

    @Bean
    public X509AuthenticationProvider x509AuthenticationProvider() {
        return new X509AuthenticationProvider(x509IdentityProvider(), authorizer(), this.properties);
    }

    @Bean(initMethod = "initialize")
    public AuthorizerFactory authorizerFactory() {
        return new StandardAuthorizerFactory(this.properties, this.extensionManager);
    }

    @Bean
    public Authorizer authorizer() {
        return authorizerFactory().getAuthorizer();
    }

    @Bean
    public AuthorizableLookup authorizableLookup() {
        return new StandardAuthorizableLookup();
    }


    // TODO, add Jwt and Otp support
//    @Autowired
//    public void setJwtAuthenticationProvider(JwtAuthenticationProvider jwtAuthenticationProvider) {
//        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
//    }
//
//    @Autowired
//    public void setOtpAuthenticationProvider(OtpAuthenticationProvider otpAuthenticationProvider) {
//        this.otpAuthenticationProvider = otpAuthenticationProvider;
//    }

}
