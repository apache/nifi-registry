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
package org.apache.nifi.registry.security.authorization.database;

import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.properties.util.IdentityMapping;
import org.apache.nifi.registry.properties.util.IdentityMappingUtil;
import org.apache.nifi.registry.security.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.security.authorization.User;
import org.apache.nifi.registry.security.authorization.annotation.AuthorizerContext;
import org.apache.nifi.registry.security.authorization.util.UserGroupProviderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The NiFi Registry specific implementation of an ConfigurableUserGroupProvider backed by database.
 */
public class DatabaseUserGroupProvider extends AbstractDatabaseUserGroupProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUserGroupProvider.class);

    private DataSource dataSource;
    private NiFiRegistryProperties properties;

    @AuthorizerContext
    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @AuthorizerContext
    public void setProperties(final NiFiRegistryProperties properties) {
        this.properties = properties;
    }

    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

    @Override
    protected void populateInitialUsers(final AuthorizerConfigurationContext configurationContext) {
        final List<IdentityMapping> identityMappings = Collections.unmodifiableList(IdentityMappingUtil.getIdentityMappings(properties));
        final Set<String> initialUserIdentities = UserGroupProviderUtils.getInitialUserIdentities(configurationContext, identityMappings);

        for (final String initialUserIdentity : initialUserIdentities) {
            final User existingUser = getUserByIdentity(initialUserIdentity);
            if (existingUser == null) {
                final User initialUser = new User.Builder()
                        .identifierGenerateFromSeed(initialUserIdentity)
                        .identity(initialUserIdentity)
                        .build();
                addUser(initialUser);
                LOGGER.info("Created initial user with identity {}", new Object[]{initialUserIdentity});
            } else {
                LOGGER.debug("User already exists with identity {}", new Object[]{initialUserIdentity});
            }
        }
    }

}
