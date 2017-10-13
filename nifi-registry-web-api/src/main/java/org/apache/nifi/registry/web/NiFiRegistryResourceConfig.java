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

import org.apache.nifi.registry.web.api.AccessPolicyResource;
import org.apache.nifi.registry.web.api.AccessResource;
import org.apache.nifi.registry.web.api.BucketFlowResource;
import org.apache.nifi.registry.web.api.BucketResource;
import org.apache.nifi.registry.web.api.FlowResource;
import org.apache.nifi.registry.web.api.ItemResource;
import org.apache.nifi.registry.web.api.ResourceResource;
import org.apache.nifi.registry.web.api.TenantResource;
import org.apache.nifi.registry.web.mapper.AccessDeniedExceptionMapper;
import org.apache.nifi.registry.web.mapper.AdministrationExceptionMapper;
import org.apache.nifi.registry.web.mapper.AuthenticationCredentialsNotFoundExceptionMapper;
import org.apache.nifi.registry.web.mapper.AuthorizationAccessExceptionMapper;
import org.apache.nifi.registry.web.mapper.IllegalArgumentExceptionMapper;
import org.apache.nifi.registry.web.mapper.IllegalStateExceptionMapper;
import org.apache.nifi.registry.web.mapper.InvalidAuthenticationExceptionMapper;
import org.apache.nifi.registry.web.mapper.NotFoundExceptionMapper;
import org.apache.nifi.registry.web.mapper.ResourceNotFoundExceptionMapper;
import org.apache.nifi.registry.web.mapper.SerializationExceptionMapper;
import org.apache.nifi.registry.web.mapper.ThrowableMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

/**
 * This is the main Jersey configuration for the application.
 *
 *  NOTE: Don't set @ApplicationPath here because it has already been set to 'nifi-registry-api' in JettyServer
 */
@Configuration
public class NiFiRegistryResourceConfig extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistryResourceConfig.class);

    public NiFiRegistryResourceConfig(@Context ServletContext servletContext) {
        // register filters
        register(HttpMethodOverrideFilter.class);

        // register the exception mappers - TODO, see if these can be registered via scanning
        register(new AccessDeniedExceptionMapper());
        register(new AdministrationExceptionMapper());
        register(new AuthenticationCredentialsNotFoundExceptionMapper());
        register(new AuthorizationAccessExceptionMapper());
        register(new IllegalArgumentExceptionMapper());
        register(new IllegalStateExceptionMapper());
        register(new InvalidAuthenticationExceptionMapper());
        register(new NotFoundExceptionMapper());
        register(new ResourceNotFoundExceptionMapper());
        register(new SerializationExceptionMapper());
        register(new ThrowableMapper());

        // register endpoints
        register(AccessPolicyResource.class);
        register(AccessResource.class);
        register(BucketResource.class);
        register(BucketFlowResource.class);
        register(FlowResource.class);
        register(ItemResource.class);
        register(ResourceResource.class);
        register(TenantResource.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }
}
