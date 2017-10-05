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

import org.apache.nifi.registry.authorization.Authorizer;
import org.apache.nifi.registry.authorization.Group;
import org.apache.nifi.registry.authorization.ManagedAuthorizer;
import org.apache.nifi.registry.authorization.UserAndGroups;
import org.apache.nifi.registry.authorization.UserGroupProvider;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.properties.util.IdentityMapping;
import org.apache.nifi.registry.properties.util.IdentityMappingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base AuthenticationProvider that provides common functionality to mapping identities.
 */
public abstract class NiFiAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NiFiAuthenticationProvider.class);

    private NiFiRegistryProperties properties;
    private Authorizer authorizer;
    private List<IdentityMapping> mappings;

    /**
     * @param properties the NiFiProperties instance
     */
    public NiFiAuthenticationProvider(final NiFiRegistryProperties properties, final Authorizer authorizer) {
        this.properties = properties;
        this.mappings = Collections.unmodifiableList(IdentityMappingUtil.getIdentityMappings(properties));
        this.authorizer = authorizer;
    }

    public List<IdentityMapping> getMappings() {
        return mappings;
    }

    protected String mapIdentity(final String identity) {
        return IdentityMappingUtil.mapIdentity(identity, mappings);
    }

    protected Set<String> getUserGroups(final String identity) {
        return getUserGroups(authorizer, identity);
    }

    protected static Set<String> getUserGroups(final Authorizer authorizer, final String userIdentity) {
        if (authorizer instanceof ManagedAuthorizer) {
            final ManagedAuthorizer managedAuthorizer = (ManagedAuthorizer) authorizer;
            final UserGroupProvider userGroupProvider = managedAuthorizer.getAccessPolicyProvider().getUserGroupProvider();
            final UserAndGroups userAndGroups = userGroupProvider.getUserAndGroups(userIdentity);
            final Set<Group> userGroups = userAndGroups.getGroups();

            if (userGroups == null || userGroups.isEmpty()) {
                return Collections.EMPTY_SET;
            } else {
                return userAndGroups.getGroups().stream().map(group -> group.getName()).collect(Collectors.toSet());
            }
        } else {
            return null;
        }
    }
}
