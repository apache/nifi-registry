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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.properties.util.IdentityMapping;
import org.apache.nifi.registry.properties.util.IdentityMappingUtil;
import org.apache.nifi.registry.security.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.security.authorization.Group;
import org.apache.nifi.registry.security.authorization.User;
import org.apache.nifi.registry.security.authorization.annotation.AuthorizerContext;
import org.apache.nifi.registry.security.authorization.util.AccessPolicyProviderUtils;
import org.apache.nifi.registry.security.authorization.util.InitialPolicies;
import org.apache.nifi.registry.security.authorization.util.ResourceAndAction;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The NiFi Registry specific implementation of an ConfigurableAccessPolicyProvider backed by database.
 */
public class DatabaseAccessPolicyProvider extends AbstractDatabaseAccessPolicyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccessPolicyProvider.class);

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
    protected void populateInitialPolicies(final AuthorizerConfigurationContext configurationContext) {
        final List<IdentityMapping> identityMappings = Collections.unmodifiableList(IdentityMappingUtil.getIdentityMappings(properties));
        final String initialAdminIdentity = AccessPolicyProviderUtils.getInitialAdminIdentity(configurationContext, identityMappings);
        final Set<String> nifiIdentities = AccessPolicyProviderUtils.getNiFiIdentities(configurationContext, identityMappings);
        final String nifiGroupName = AccessPolicyProviderUtils.getNiFiGroupName(configurationContext);

        if (!StringUtils.isBlank(initialAdminIdentity)) {
            LOGGER.info("Populating authorizations for Initial Admin: '" + initialAdminIdentity + "'");
            populateInitialAdmin(initialAdminIdentity);
        }

        if (!CollectionUtils.isEmpty(nifiIdentities)) {
            LOGGER.info("Populating authorizations for NiFi identities: [{}]", StringUtils.join(nifiIdentities, ";"));
            populateNiFiIdentities(nifiIdentities);
        }

        if (!StringUtils.isBlank(nifiGroupName)) {
            LOGGER.info("Populating authorizations for NiFi Group: '" + nifiGroupName + "'");
            populateNiFiGroup(nifiGroupName);
        }
    }

    private void populateInitialAdmin(final String initialAdminIdentity) {
        final User initialAdmin = getUserGroupProvider().getUserByIdentity(initialAdminIdentity);
        if (initialAdmin == null) {
            throw new SecurityProviderCreationException("Unable to locate initial admin '" + initialAdminIdentity + "' to seed policies");
        }

        for (final ResourceAndAction resourceAction : InitialPolicies.ADMIN_POLICIES) {
            populateInitialPolicy(initialAdmin, resourceAction);
        }
    }

    private void populateNiFiIdentities(final Set<String> nifiIdentities) {
        for (final String nifiIdentity : nifiIdentities) {
            final User nifiUser = getUserGroupProvider().getUserByIdentity(nifiIdentity);
            if (nifiUser == null) {
                throw new SecurityProviderCreationException("Unable to locate NiFi identity '" + nifiIdentity + "' to seed policies.");
            }

            for (final ResourceAndAction resourceAction : InitialPolicies.NIFI_POLICIES) {
                populateInitialPolicy(nifiUser, resourceAction);
            }
        }
    }

    private void populateNiFiGroup(final String nifiGroupName) {
        final Group nifiGroup = AccessPolicyProviderUtils.getGroup(nifiGroupName, getUserGroupProvider());

        for (final ResourceAndAction resourceAction : InitialPolicies.NIFI_POLICIES) {
            populateInitialPolicy(nifiGroup, resourceAction);
        }
    }

}
