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
package org.apache.nifi.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.Properties;

/**
 * Main class for starting the NiFi Registry Web API as a Spring Boot application.
 *
 * This class is purposely in the org.apache.nifi.registry package since that is the common base
 * package across other modules. This is done because spring-boot will use the package of this
 * class to automatically scan for beans/config/entities/etc. and would otherwise require
 * configuring custom packages to scan in several different places.
 */
@SpringBootApplication
public class NiFiRegistryApiApplication extends SpringBootServletInitializer {

    public static final String NIFI_REGISTRY_PROPERTIES_ATTRIBUTE = "nifi-registry.properties";

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        final Properties defaultProperties = new Properties();
        defaultProperties.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        defaultProperties.setProperty("spring.jpa.hibernate.naming.physical-strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        return application
                .sources(NiFiRegistryApiApplication.class)
                .properties(defaultProperties);
    }

    public static void main(String[] args) {
        SpringApplication.run(NiFiRegistryApiApplication.class, args);
    }

}
