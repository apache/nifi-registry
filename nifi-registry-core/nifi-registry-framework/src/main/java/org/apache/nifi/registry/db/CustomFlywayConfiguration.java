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

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.jdbc.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class CustomFlywayConfiguration implements FlywayConfigurationCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFlywayConfiguration.class);

    private static final String LOCATION_COMMON = "classpath:db/migration/common";

    private static final String LOCATION_DEFAULT = "classpath:db/migration/default";
    private static final String[] LOCATIONS_DEFAULT = {LOCATION_COMMON, LOCATION_DEFAULT};

    private static final String LOCATION_MYSQL = "classpath:db/migration/mysql";
    private static final String[] LOCATIONS_MYSQL = {LOCATION_COMMON, LOCATION_MYSQL};

    @Override
    public void customize(final FluentConfiguration configuration) {
        final DatabaseType databaseType = getDatabaseType(configuration.getDataSource());
        LOGGER.info("Determined database type is {}", new Object[] {databaseType.name()});

        switch (databaseType) {
            case MYSQL:
                LOGGER.info("Setting migration locations to {}", new Object[] {LOCATIONS_MYSQL});
                configuration.locations(LOCATIONS_MYSQL);
                break;
            default:
                LOGGER.info("Setting migration locations to {}", new Object[] {LOCATIONS_DEFAULT});
                configuration.locations(LOCATIONS_DEFAULT);
                break;
        }
    }

    /**
     * Determines the database type from the given data source.
     *
     * @param dataSource the data source
     * @return the database type
     */
    private DatabaseType getDatabaseType(final DataSource dataSource) {
        try (final Connection connection = dataSource.getConnection()) {
            return DatabaseType.fromJdbcConnection(connection);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new FlywayException("Unable to obtain connection from Flyway DataSource", e);
        }
    }
}
