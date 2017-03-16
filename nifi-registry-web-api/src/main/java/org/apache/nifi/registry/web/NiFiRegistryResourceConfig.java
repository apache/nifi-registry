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

import org.apache.nifi.registry.web.api.TestResource;
import org.apache.nifi.registry.web.mapper.IllegalArgumentExceptionMapper;
import org.apache.nifi.registry.web.mapper.ThrowableMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

public class NiFiRegistryResourceConfig extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistryResourceConfig.class);

    public NiFiRegistryResourceConfig(@Context ServletContext servletContext) {
        register(HttpMethodOverrideFilter.class);

        // register the exception mappers
        register(new IllegalArgumentExceptionMapper());
        register(new ThrowableMapper());

        // register endpoints
        register(new TestResource());
    }
}
