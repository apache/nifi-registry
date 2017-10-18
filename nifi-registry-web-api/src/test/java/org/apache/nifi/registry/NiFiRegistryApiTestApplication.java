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

import org.apache.nifi.registry.db.DataSourceFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.Properties;

@SpringBootApplication
@ComponentScan(
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = SpringBootServletInitializer.class), // Avoid loading NiFiRegistryApiApplication
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = DataSourceFactory.class), // Avoid loading DataSourceFactory
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "org\\.apache\\.nifi\\.registry\\.NiFiRegistryPropertiesFactory"), // Avoid loading NiFiRegistryPropertiesFactory
        })
public class NiFiRegistryApiTestApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        final Properties fixedProps = new Properties();
        fixedProps.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        fixedProps.setProperty("spring.jpa.hibernate.naming.physical-strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        return application
                .sources(NiFiRegistryApiTestApplication.class)
                .properties(fixedProps);
    }

    public static void main(String[] args) {
        SpringApplication.run(NiFiRegistryApiTestApplication.class, args);
    }

}
