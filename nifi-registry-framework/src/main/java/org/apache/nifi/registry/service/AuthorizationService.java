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
package org.apache.nifi.registry.service;

import org.apache.nifi.registry.authorization.AccessDeniedException;
import org.apache.nifi.registry.authorization.AccessPolicyProvider;
import org.apache.nifi.registry.authorization.AccessPolicyProviderInitializationContext;
import org.apache.nifi.registry.authorization.AuthorizableLookup;
import org.apache.nifi.registry.authorization.AuthorizationAccessException;
import org.apache.nifi.registry.authorization.AuthorizeAccess;
import org.apache.nifi.registry.authorization.Authorizer;
import org.apache.nifi.registry.authorization.AuthorizerCapabilityDetection;
import org.apache.nifi.registry.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.authorization.AuthorizerCreationException;
import org.apache.nifi.registry.authorization.AuthorizerDestructionException;
import org.apache.nifi.registry.authorization.ConfigurableAccessPolicyProvider;
import org.apache.nifi.registry.authorization.ConfigurableUserGroupProvider;
import org.apache.nifi.registry.authorization.Group;
import org.apache.nifi.registry.authorization.ManagedAuthorizer;
import org.apache.nifi.registry.authorization.RequestAction;
import org.apache.nifi.registry.authorization.UserAndGroups;
import org.apache.nifi.registry.authorization.UserGroupProvider;
import org.apache.nifi.registry.authorization.UserGroupProviderInitializationContext;
import org.apache.nifi.registry.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.authorization.resource.ResourceType;
import org.apache.nifi.registry.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.model.authorization.AccessPolicy;
import org.apache.nifi.registry.model.authorization.AccessPolicySummary;
import org.apache.nifi.registry.model.authorization.Resource;
import org.apache.nifi.registry.model.authorization.Tenant;
import org.apache.nifi.registry.model.authorization.User;
import org.apache.nifi.registry.model.authorization.UserGroup;
import org.apache.nifi.registry.service.params.QueryParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    public static final String MSG_NON_MANAGED_AUTHORIZER = "This NiFi Registry is not configured to internally manage users, groups, or policies. Please contact your system administrator.";
    public static final String MSG_NON_CONFIGURABLE_POLICIES = "This NiFi Registry is not configured to allow configurable policies. Please contact your system administrator.";
    public static final String MSG_NON_CONFIGURABLE_USERS = "This NiFi Registry is not configured to allow configurable users and groups. Please contact your system administrator.";

    private AuthorizableLookup authorizableLookup;
    private Authorizer authorizer;
    private RegistryService registryService;
    private UserGroupProvider userGroupProvider;
    private AccessPolicyProvider accessPolicyProvider;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    @Autowired
    public AuthorizationService(
            final AuthorizableLookup authorizableLookup,
            final Authorizer authorizer,
            final RegistryService registryService) {
        this.authorizableLookup = authorizableLookup;
        this.authorizer = authorizer;
        this.registryService = registryService;

        if (AuthorizerCapabilityDetection.isManagedAuthorizer(this.authorizer)) {
            this.accessPolicyProvider = ((ManagedAuthorizer) authorizer).getAccessPolicyProvider();
        } else {
            this.accessPolicyProvider = createExceptionThrowingAccessPolicyProvider();
        }
        this.userGroupProvider = accessPolicyProvider.getUserGroupProvider();
    }


    // ---------------------- Authorization methods -------------------------------------

    public void authorizeAccess(final AuthorizeAccess authorizeAccess) {
        authorizeAccess.authorize(authorizableLookup);
    }


    // ---------------------- Tenant methods --------------------------------------------

    public Tenant getTenant(String identifier) {
        this.readLock.lock();
        try {
            org.apache.nifi.registry.authorization.User user = userGroupProvider.getUser(identifier);
            if (user != null) {
                return tenantToDTO(user);
            } else {
                org.apache.nifi.registry.authorization.Group group = userGroupProvider.getGroup(identifier);
                return tenantToDTO(group);
            }
        } finally {
            this.readLock.unlock();
        }
    }


    // ---------------------- User methods ----------------------------------------------

    public User createUser(User user) {
        verifyUserGroupProviderIsConfigurable();
        writeLock.lock();
        try {
            final org.apache.nifi.registry.authorization.User createdUser =
                ((ConfigurableUserGroupProvider) userGroupProvider).addUser(userFromDTO(user));
            return userToDTO(createdUser);
        } finally {
            writeLock.unlock();
        }
    }

    public List<User> getUsers() {
        this.readLock.lock();
        try {
            return userGroupProvider.getUsers().stream().map(this::userToDTO).collect(Collectors.toList());
        } finally {
            this.readLock.unlock();
        }
    }

    public User getUser(String identifier) {
        this.readLock.lock();
        try {
            return userToDTO(userGroupProvider.getUser(identifier));
        } finally {
            this.readLock.unlock();
        }
    }

    public User updateUser(User user) {
        verifyUserGroupProviderIsConfigurable();
        this.writeLock.lock();
        try {
            final org.apache.nifi.registry.authorization.User updatedUser =
                    ((ConfigurableUserGroupProvider) userGroupProvider).updateUser(userFromDTO(user));
            return userToDTO(updatedUser);
        } finally {
            this.writeLock.unlock();
        }
    }

    public User deleteUser(String identifier) {
        verifyUserGroupProviderIsConfigurable();
        this.writeLock.lock();
        try {
            User deletedUserDTO = getUser(identifier);
            ((ConfigurableUserGroupProvider) userGroupProvider).deleteUser(identifier);
            return deletedUserDTO;
        } finally {
            this.writeLock.unlock();
        }
    }


    // ---------------------- User Group methods --------------------------------------

    public UserGroup createUserGroup(UserGroup userGroup) {
        verifyUserGroupProviderIsConfigurable();
        writeLock.lock();
        try {
            final org.apache.nifi.registry.authorization.Group createdGroup =
                    ((ConfigurableUserGroupProvider) userGroupProvider).addGroup(userGroupFromDTO(userGroup));
            return userGroupToDTO(createdGroup);
        } finally {
            writeLock.unlock();
        }
    }

    public List<UserGroup> getUserGroups() {
        this.readLock.lock();
        try {
            return userGroupProvider.getGroups().stream().map(this::userGroupToDTO).collect(Collectors.toList());
        } finally {
            this.readLock.unlock();
        }
    }

    public List<UserGroup> getUserGroupsForUser(String userIdentifier) {
        this.readLock.lock();
        try {
            return userGroupProvider.getGroups()
                    .stream()
                    .filter(group -> group.getUsers().contains(userIdentifier))
                    .map(this::userGroupToDTO)
                    .collect(Collectors.toList());
        } finally {
            this.readLock.unlock();
        }
    }

    public UserGroup getUserGroup(String identifier) {
        this.readLock.lock();
        try {
            return userGroupToDTO(userGroupProvider.getGroup(identifier));
        } finally {
            this.readLock.unlock();
        }
    }

    public UserGroup updateUserGroup(UserGroup userGroup) {
        verifyUserGroupProviderIsConfigurable();
        writeLock.lock();
        try {
            final org.apache.nifi.registry.authorization.Group updatedGroup =
                    ((ConfigurableUserGroupProvider) userGroupProvider).updateGroup(userGroupFromDTO(userGroup));
            return userGroupToDTO(updatedGroup);
        } finally {
            writeLock.unlock();
        }
    }

    public UserGroup deleteUserGroup(String identifier) {
        verifyUserGroupProviderIsConfigurable();
        writeLock.lock();
        try {
            final UserGroup userGroupDTO = getUserGroup(identifier);
            ((ConfigurableUserGroupProvider) userGroupProvider).deleteGroup(identifier);
            return userGroupDTO;
        } finally {
            writeLock.unlock();
        }
    }


    // ---------------------- Access Policy methods ----------------------------------------

    public AccessPolicy createAccessPolicy(AccessPolicy accessPolicy) {
        verifyAccessPolicyProviderIsConfigurable();
        writeLock.lock();
        try {
            org.apache.nifi.registry.authorization.AccessPolicy createdAccessPolicy =
                    ((ConfigurableAccessPolicyProvider) accessPolicyProvider).addAccessPolicy(accessPolicyFromDTO(accessPolicy));
            return accessPolicyToDTO(createdAccessPolicy);
        } finally {
            writeLock.unlock();
        }
    }

    public AccessPolicy getAccessPolicy(String identifier) {
        readLock.lock();
        try {
            return accessPolicyToDTO(accessPolicyProvider.getAccessPolicy(identifier));
        } finally {
            readLock.unlock();
        }
    }

    public AccessPolicy getAccessPolicy(String resource, RequestAction action) {
        readLock.lock();
        try {
            return accessPolicyToDTO(accessPolicyProvider.getAccessPolicy(resource, action));
        } finally {
            readLock.unlock();
        }
    }

    public List<AccessPolicy> getAccessPolicies() {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream().map(this::accessPolicyToDTO).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<AccessPolicySummary> getAccessPolicySummaries() {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream().map(this::accessPolicyToSummaryDTO).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<AccessPolicy> getAccessPoliciesForUser(String userIdentifier) {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream()
                    .filter(accessPolicy -> accessPolicy.getUsers().contains(userIdentifier))
                    .map(this::accessPolicyToDTO)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<AccessPolicySummary> getAccessPolicySummariesForUser(String userIdentifier) {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream()
                    .filter(accessPolicy -> accessPolicy.getUsers().contains(userIdentifier))
                    .map(this::accessPolicyToSummaryDTO)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public List<AccessPolicySummary> getAccessPolicySummariesForUserGroup(String userGroupIdentifier) {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream()
                    .filter(accessPolicy -> accessPolicy.getGroups().contains(userGroupIdentifier))
                    .map(this::accessPolicyToSummaryDTO)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public AccessPolicy updateAccessPolicy(AccessPolicy accessPolicy) {
        verifyAccessPolicyProviderIsConfigurable();

        // Don't allow changing action or resource of existing policy (should only be adding/removing users/groups)
        org.apache.nifi.registry.authorization.AccessPolicy currentAccessPolicy =
                accessPolicyProvider.getAccessPolicy(accessPolicy.getIdentifier());
        accessPolicy.setResource(currentAccessPolicy.getResource());
        accessPolicy.setAction(currentAccessPolicy.getAction().toString());

        writeLock.lock();
        try {
            org.apache.nifi.registry.authorization.AccessPolicy updateAccessPolicy =
                    ((ConfigurableAccessPolicyProvider) accessPolicyProvider).updateAccessPolicy(accessPolicyFromDTO(accessPolicy));
            return accessPolicyToDTO(updateAccessPolicy);
        } finally {
            writeLock.unlock();
        }
    }

    public AccessPolicy deleteAccessPolicy(String identifier) {
        verifyAccessPolicyProviderIsConfigurable();
        writeLock.lock();
        try {
            AccessPolicy deletedAccessPolicyDTO = getAccessPolicy(identifier);
            ((ConfigurableAccessPolicyProvider) accessPolicyProvider).deleteAccessPolicy(identifier);
            return deletedAccessPolicyDTO;
        } finally {
            writeLock.unlock();
        }
    }


    // ---------------------- Resource Lookup methods --------------------------------------

    public List<Resource> getAuthorizedResources(RequestAction actionType, ResourceType resourceType) {
        final List<Resource> authorizedResources =
                getAllAuthorizableResources()
                        .stream()
                        .filter(resource -> {
                            String resourceId = resource.getIdentifier();
                            if (resourceType != null) {
                                if (!resourceId.startsWith(resourceType.getValue())) {
                                    return false;
                                }
                            }
                            try {
                                authorizableLookup
                                        .getAuthorizableByResource(resource.getIdentifier())
                                        .authorize(authorizer, actionType, NiFiUserUtils.getNiFiUser());
                            } catch (AccessDeniedException e) {
                                return false;
                            }
                            return true;

                        })
                        .map(AuthorizationService::resourceToDTO)
                        .collect(Collectors.toList());

        return authorizedResources;
    }

    public List<Resource> getAuthorizedResources(RequestAction actionType) {
        return getAuthorizedResources(actionType, null);
    }

    public List<Resource> getResources() {
        final List<Resource> dtoResources =
                getAllAuthorizableResources()
                        .stream()
                        .map(AuthorizationService::resourceToDTO)
                        .collect(Collectors.toList());
        return dtoResources;
    }


    // ---------------------- Private Helper methods --------------------------------------

    private void verifyUserGroupProviderIsConfigurable() {
        if (!(userGroupProvider instanceof ConfigurableUserGroupProvider)) {
            throw new IllegalStateException(MSG_NON_CONFIGURABLE_USERS);
        }
    }

    private void verifyAccessPolicyProviderIsConfigurable() {
        if (!(accessPolicyProvider instanceof ConfigurableAccessPolicyProvider)) {
            throw new IllegalStateException(MSG_NON_CONFIGURABLE_POLICIES);
        }
    }

    private List<org.apache.nifi.registry.authorization.Resource> getAllAuthorizableResources() {
        final List<org.apache.nifi.registry.authorization.Resource> resources = new ArrayList<>();
        resources.add(ResourceFactory.getPoliciesResource());
        resources.add(ResourceFactory.getTenantResource());
        resources.add(ResourceFactory.getProxyResource());
        resources.add(ResourceFactory.getResourceResource());

        // add all buckets
        resources.add(ResourceFactory.getBucketsResource());
        for (final Bucket bucket : registryService.getBuckets(QueryParameters.EMPTY_PARAMETERS)) {
            resources.add(ResourceFactory.getChildResource(ResourceType.Bucket, bucket.getIdentifier(), bucket.getName()));
        }

        return resources;
    }

    private org.apache.nifi.registry.model.authorization.User userToDTO(
            final org.apache.nifi.registry.authorization.User user) {
        if (user == null) {
            return null;
        }
        String userIdentifier = user.getIdentifier();
        Collection<UserGroup> userGroups = getUserGroupsForUser(userIdentifier);
        Collection<AccessPolicySummary> accessPolicySummaries = getAccessPolicySummariesForUser(userIdentifier);

        return userToDTO(user, userGroups, accessPolicySummaries);
    }

    private org.apache.nifi.registry.model.authorization.UserGroup userGroupToDTO(
            final org.apache.nifi.registry.authorization.Group userGroup) {
        if (userGroup == null) {
            return null;
        }
        Collection<Tenant> userTenants = userGroup.getUsers() != null
                ? userGroup.getUsers().stream().map(this::getTenant).collect(Collectors.toSet()) : null;
        Collection<AccessPolicySummary> accessPolicySummaries = getAccessPolicySummariesForUserGroup(userGroup.getIdentifier());

        return userGroupToDTO(userGroup, userTenants, accessPolicySummaries);
    }

    private org.apache.nifi.registry.model.authorization.AccessPolicy accessPolicyToDTO(
            final org.apache.nifi.registry.authorization.AccessPolicy accessPolicy) {
        if (accessPolicy == null) {
            return null;
        }

        Collection<Tenant> users = accessPolicy.getUsers() != null
                ? accessPolicy.getUsers().stream().map(this::getTenant).collect(Collectors.toList()) : null;
        Collection<Tenant> userGroups = accessPolicy.getGroups() != null
                ? accessPolicy.getGroups().stream().map(this::getTenant).collect(Collectors.toList()) : null;

        Boolean isConfigurable = AuthorizerCapabilityDetection.isAccessPolicyConfigurable(authorizer, accessPolicy);

        return accessPolicyToDTO(accessPolicy, userGroups, users, isConfigurable);
    }

    private org.apache.nifi.registry.model.authorization.AccessPolicySummary accessPolicyToSummaryDTO(
            final org.apache.nifi.registry.authorization.AccessPolicy accessPolicy) {
        if (accessPolicy == null) {
            return null;
        }

        Boolean isConfigurable = AuthorizerCapabilityDetection.isAccessPolicyConfigurable(authorizer, accessPolicy);

        final AccessPolicySummary accessPolicySummaryDTO = new AccessPolicySummary();
        accessPolicySummaryDTO.setIdentifier(accessPolicy.getIdentifier());
        accessPolicySummaryDTO.setAction(accessPolicy.getAction().toString());
        accessPolicySummaryDTO.setResource(accessPolicy.getResource());
        accessPolicySummaryDTO.setConfigurable(isConfigurable);
        return accessPolicySummaryDTO;
    }

    private static Resource resourceToDTO(org.apache.nifi.registry.authorization.Resource resource) {
        if (resource == null) {
            return null;
        }
        Resource resourceDto = new Resource();
        resourceDto.setIdentifier(resource.getIdentifier());
        resourceDto.setName(resource.getName());
        return resourceDto;
    }

    private static Tenant tenantToDTO(org.apache.nifi.registry.authorization.User user) {
        if (user == null) {
            return null;
        }
        return new Tenant(user.getIdentifier(), user.getIdentity());
    }

    private static Tenant tenantToDTO(org.apache.nifi.registry.authorization.Group group) {
        if (group == null) {
            return null;
        }
        return new Tenant(group.getIdentifier(), group.getName());
    }

    private static org.apache.nifi.registry.authorization.User userFromDTO(
            final org.apache.nifi.registry.model.authorization.User userDTO) {
        if (userDTO == null) {
            return null;
        }
        return new org.apache.nifi.registry.authorization.User.Builder()
                .identifier(userDTO.getIdentifier() != null ? userDTO.getIdentifier() : UUID.randomUUID().toString())
                .identity(userDTO.getIdentity())
                .build();
    }

    private static org.apache.nifi.registry.model.authorization.User userToDTO(
            final org.apache.nifi.registry.authorization.User user,
            final Collection<? extends Tenant> userGroups,
            final Collection<? extends AccessPolicySummary> accessPolicies) {

        if (user == null) {
            return null;
        }
        User userDTO = new User(user.getIdentifier(), user.getIdentity());
        userDTO.addUserGroups(userGroups);
        userDTO.addAccessPolicies(accessPolicies);
        return userDTO;
    }

    private static org.apache.nifi.registry.authorization.Group userGroupFromDTO(
            final org.apache.nifi.registry.model.authorization.UserGroup userGroupDTO) {
        if (userGroupDTO == null) {
            return null;
        }
        org.apache.nifi.registry.authorization.Group.Builder groupBuilder = new org.apache.nifi.registry.authorization.Group.Builder()
                .identifier(userGroupDTO.getIdentifier() != null ? userGroupDTO.getIdentifier() : UUID.randomUUID().toString())
                .name(userGroupDTO.getIdentity());
        Set<Tenant> users = userGroupDTO.getUsers();
        if (users != null) {
            groupBuilder.addUsers(users.stream().map(Tenant::getIdentifier).collect(Collectors.toSet()));
        }
        return groupBuilder.build();
    }

    private static org.apache.nifi.registry.model.authorization.UserGroup userGroupToDTO(
            final org.apache.nifi.registry.authorization.Group userGroup,
            final Collection<? extends Tenant> users,
            final Collection<? extends AccessPolicySummary> accessPolicies) {
        if (userGroup == null) {
            return null;
        }
        UserGroup userGroupDTO = new UserGroup(userGroup.getIdentifier(), userGroup.getName());
        userGroupDTO.addUsers(users);
        userGroupDTO.addAccessPolicies(accessPolicies);
        return userGroupDTO;
    }

    private static org.apache.nifi.registry.authorization.AccessPolicy accessPolicyFromDTO(
            final org.apache.nifi.registry.model.authorization.AccessPolicy accessPolicyDTO) {
        org.apache.nifi.registry.authorization.AccessPolicy.Builder accessPolicyBuilder =
                new org.apache.nifi.registry.authorization.AccessPolicy.Builder()
                        .identifier(accessPolicyDTO.getIdentifier() != null ? accessPolicyDTO.getIdentifier() : UUID.randomUUID().toString())
                        .resource(accessPolicyDTO.getResource())
                        .action(RequestAction.valueOfValue(accessPolicyDTO.getAction()));

        Set<Tenant> dtoUsers = accessPolicyDTO.getUsers();
        if (accessPolicyDTO.getUsers() != null) {
            accessPolicyBuilder.addUsers(dtoUsers.stream().map(Tenant::getIdentifier).collect(Collectors.toSet()));
        }

        Set<Tenant> dtoUserGroups = accessPolicyDTO.getUserGroups();
        if (dtoUserGroups != null) {
            accessPolicyBuilder.addGroups(dtoUserGroups.stream().map(Tenant::getIdentifier).collect(Collectors.toSet()));
        }

        return accessPolicyBuilder.build();
    }

    private static org.apache.nifi.registry.model.authorization.AccessPolicy accessPolicyToDTO(
            final org.apache.nifi.registry.authorization.AccessPolicy accessPolicy,
            final Collection<? extends Tenant> userGroups,
            final Collection<? extends Tenant> users,
            final Boolean isConfigurable) {

        if (accessPolicy == null) {
            return null;
        }

        final AccessPolicy accessPolicyDTO = new AccessPolicy();
        accessPolicyDTO.setIdentifier(accessPolicy.getIdentifier());
        accessPolicyDTO.setAction(accessPolicy.getAction().toString());
        accessPolicyDTO.setResource(accessPolicy.getResource());
        accessPolicyDTO.setConfigurable(isConfigurable);
        accessPolicyDTO.addUsers(users);
        accessPolicyDTO.addUserGroups(userGroups);
        return accessPolicyDTO;
    }

    private static AccessPolicyProvider createExceptionThrowingAccessPolicyProvider() {

        return new AccessPolicyProvider() {
            @Override
            public Set<org.apache.nifi.registry.authorization.AccessPolicy> getAccessPolicies() throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public org.apache.nifi.registry.authorization.AccessPolicy getAccessPolicy(String identifier) throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public org.apache.nifi.registry.authorization.AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action) throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public UserGroupProvider getUserGroupProvider() {
                return new UserGroupProvider() {
                    @Override
                    public Set<org.apache.nifi.registry.authorization.User> getUsers() throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public org.apache.nifi.registry.authorization.User getUser(String identifier) throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public org.apache.nifi.registry.authorization.User getUserByIdentity(String identity) throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public Set<Group> getGroups() throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public Group getGroup(String identifier) throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public UserAndGroups getUserAndGroups(String identity) throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public void initialize(UserGroupProviderInitializationContext initializationContext) throws AuthorizerCreationException {

                    }

                    @Override
                    public void onConfigured(AuthorizerConfigurationContext configurationContext) throws AuthorizerCreationException {

                    }

                    @Override
                    public void preDestruction() throws AuthorizerDestructionException {

                    }
                };
            }

            @Override
            public void initialize(AccessPolicyProviderInitializationContext initializationContext) throws AuthorizerCreationException {
            }

            @Override
            public void onConfigured(AuthorizerConfigurationContext configurationContext) throws AuthorizerCreationException {
            }

            @Override
            public void preDestruction() throws AuthorizerDestructionException {
            }
        };

    }

}
