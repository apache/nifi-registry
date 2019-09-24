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
package org.apache.nifi.registry.security.authorization.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.properties.util.IdentityMapping;
import org.apache.nifi.registry.properties.util.IdentityMappingUtil;
import org.apache.nifi.registry.security.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.security.authorization.Group;
import org.apache.nifi.registry.security.authorization.UserGroupProvider;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.util.PropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods related to access policies for use by various {@link org.apache.nifi.registry.security.authorization.AccessPolicyProvider} implementations.
 */
public final class AccessPolicyProviderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessPolicyProviderUtils.class);

    /**
     * The prefix of a property from an AuthorizerConfigurationContext that specifies a NiFi Identity.
     */
    public static final String PROP_NIFI_IDENTITY_PREFIX = "NiFi Identity ";

    /**
     * The name of the property from an AuthorizerConfigurationContext that specifies the initial admin identity.
     */
    public static final String PROP_INITIAL_ADMIN_IDENTITY = "Initial Admin Identity";

    /**
     * A Pattern for identifying properties that represent NiFi Identities.
     */
    public static final Pattern NIFI_IDENTITY_PATTERN = Pattern.compile(PROP_NIFI_IDENTITY_PREFIX + "\\S+");

    /**
     * The name of the property from AuthorizerConfigurationContext that specifies a name of a group for NiFi Identities.
     */
    public static final String PROP_NIFI_GROUP_NAME = "NiFi Group Name";

    /**
     * Returns the value of the 'Initial Admin Identity' property with any identity mappings applied.
     *
     * @param configurationContext the configuration context
     * @param identityMappings the identity mappings
     * @return the value for the initial admin identity
     */
    public static String getInitialAdminIdentity(final AuthorizerConfigurationContext configurationContext, final List<IdentityMapping> identityMappings) {
        final PropertyValue initialAdminIdentityProp = configurationContext.getProperty(PROP_INITIAL_ADMIN_IDENTITY);
        return initialAdminIdentityProp.isSet() ? IdentityMappingUtil.mapIdentity(initialAdminIdentityProp.getValue(), identityMappings) : null;
    }

    /**
     * Returns the values for the 'NiFi Identity' properties with any identity mappings applied.
     *
     * @param configurationContext the configuration context
     * @param identityMappings the identity mappings
     * @return the values for the NiFi identities
     */
    public static Set<String> getNiFiIdentities(final AuthorizerConfigurationContext configurationContext, final List<IdentityMapping> identityMappings) {
        final Set<String> nifiIdentities = new HashSet<>();

        for (final Map.Entry<String,String> entry : configurationContext.getProperties().entrySet()) {
            final Matcher matcher = NIFI_IDENTITY_PATTERN.matcher(entry.getKey());
            if (matcher.matches() && !StringUtils.isBlank(entry.getValue())) {
                nifiIdentities.add(IdentityMappingUtil.mapIdentity(entry.getValue(), identityMappings));
            }
        }

        return nifiIdentities;
    }

    /**
     * Returns the value for the property 'NiFi Group Name' from the given configuration context.
     *
     * @param configurationContext the configuration context
     * @return the group name, or null if not sepcified
     */
    public static String getNiFiGroupName(final AuthorizerConfigurationContext configurationContext) {
        final PropertyValue identityGroupNameProp = configurationContext.getProperty(PROP_NIFI_GROUP_NAME);

        final String identityGroupName = (identityGroupNameProp != null && identityGroupNameProp.isSet()) ? identityGroupNameProp.getValue() : null;
        if (StringUtils.isBlank(identityGroupName)) {
            LOGGER.debug("NiFi Group Name was not specified");
            return null;
        }

        return identityGroupName;
    }

    /**
     * Returns the identifier of the group with the given group name.
     *
     * If no group exists with the given name then SecurityProviderCreationException is thrown.
     *
     * @param groupName the group name
     * @param userGroupProvider the UserGroupProvider
     * @return the identifier of the group, or null
     */
    public static Group getGroup(final String groupName, final UserGroupProvider userGroupProvider) {
        final Set<Group> groups = userGroupProvider.getGroups();
        LOGGER.trace("All groups: {}", groups);

        final Optional<Group> identityGroupsOptional = groups.stream()
                .filter(group -> group.getName().equals(groupName))
                .findFirst();

        final Group identityGroup = identityGroupsOptional.orElseThrow(() ->
                        new SecurityProviderCreationException(
                                String.format("Authorizations node group '%s' could not be found", groupName))
        );

        LOGGER.debug("Group identifier is: {}", identityGroup);
        return identityGroup;
    }

    private AccessPolicyProviderUtils() {

    }

}
