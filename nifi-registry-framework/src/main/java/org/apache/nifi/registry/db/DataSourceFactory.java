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
package org.apache.nifi.registry.db;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.h2.jdbcx.JdbcConnectionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;

/**
 * Overriding Spring Boot's normal automatic creation of a DataSource in order to use the properties
 * from NiFiRegistryProperties rather than the standard application.properties/yaml.
 */
@Configuration
public class DataSourceFactory {

    private static final String DB_USERNAME_PASSWORD = "nifireg";
    private static final int MAX_CONNECTIONS = 5;

    // database file name
    private static final String DATABASE_FILE_NAME = "nifi-registry";

    private final NiFiRegistryProperties properties;

    private JdbcConnectionPool connectionPool;

    @Autowired
    public DataSourceFactory(final NiFiRegistryProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public DataSource getDataSource() {
        if (connectionPool == null) {
            // locate the repository directory
            final String repositoryDirectoryPath = properties.getDatabaseDirectory();

            // ensure the repository directory is specified
            if (repositoryDirectoryPath == null) {
                throw new NullPointerException("Database directory must be specified.");
            }

            // create a handle to the repository directory
            final File repositoryDirectory = new File(repositoryDirectoryPath);

            // get a handle to the database file
            final File databaseFile = new File(repositoryDirectory, DATABASE_FILE_NAME);

            // format the database url
            String databaseUrl = "jdbc:h2:" + databaseFile + ";AUTOCOMMIT=OFF;DB_CLOSE_ON_EXIT=FALSE;LOCK_MODE=3";
            String databaseUrlAppend = properties.getDatabaseUrlAppend();
            if (StringUtils.isNotBlank(databaseUrlAppend)) {
                databaseUrl += databaseUrlAppend;
            }

            // create the pool
            connectionPool = JdbcConnectionPool.create(databaseUrl, DB_USERNAME_PASSWORD, DB_USERNAME_PASSWORD);
            connectionPool.setMaxConnections(MAX_CONNECTIONS);
        }

        return connectionPool;
    }

}
