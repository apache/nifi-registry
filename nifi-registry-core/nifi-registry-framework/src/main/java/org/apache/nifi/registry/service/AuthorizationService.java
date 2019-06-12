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

import org.apache.nifi.registry.authorization.AccessPolicy;
import org.apache.nifi.registry.authorization.AccessPolicySummary;
import org.apache.nifi.registry.authorization.CurrentUser;
import org.apache.nifi.registry.authorization.Permissions;
import org.apache.nifi.registry.authorization.Resource;
import org.apache.nifi.registry.authorization.ResourcePermissions;
import org.apache.nifi.registry.authorization.Tenant;
import org.apache.nifi.registry.authorization.User;
import org.apache.nifi.registry.authorization.UserGroup;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.security.authorization.AccessPolicyProvider;
import org.apache.nifi.registry.security.authorization.AccessPolicyProviderInitializationContext;
import org.apache.nifi.registry.security.authorization.AuthorizableLookup;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.AuthorizerCapabilityDetection;
import org.apache.nifi.registry.security.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.security.authorization.ConfigurableAccessPolicyProvider;
import org.apache.nifi.registry.security.authorization.ConfigurableUserGroupProvider;
import org.apache.nifi.registry.security.authorization.Group;
import org.apache.nifi.registry.security.authorization.ManagedAuthorizer;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.UserAndGroups;
import org.apache.nifi.registry.security.authorization.UserGroupProvider;
import org.apache.nifi.registry.security.authorization.UserGroupProviderInitializationContext;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.security.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.security.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.security.authorization.resource.ResourceType;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.security.exception.SecurityProviderDestructionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

    public AuthorizableLookup getAuthorizableLookup() {
        return authorizableLookup;
    }

    public Authorizer getAuthorizer() {
        return authorizer;
    }

    public void authorize(Authorizable authorizable, RequestAction action) throws AccessDeniedException {
        authorizable.authorize(authorizer, action, NiFiUserUtils.getNiFiUser());
    }

    // ---------------------- Permissions methods ---------------------------------------

    public CurrentUser getCurrentUser() {
        final NiFiUser user = NiFiUserUtils.getNiFiUser();
        final CurrentUser currentUser = new CurrentUser();
        currentUser.setIdentity(user.getIdentity());
        currentUser.setAnonymous(user.isAnonymous());
        currentUser.setResourcePermissions(getTopLevelPermissions());
        return currentUser;
    }

    public Permissions getPermissionsForResource(Authorizable authorizableResource) {
        NiFiUser user = NiFiUserUtils.getNiFiUser();
        final Permissions permissions = new Permissions();
        permissions.setCanRead(authorizableResource.isAuthorized(authorizer, RequestAction.READ, user));
        permissions.setCanWrite(authorizableResource.isAuthorized(authorizer, RequestAction.WRITE, user));
        permissions.setCanDelete(authorizableResource.isAuthorized(authorizer, RequestAction.DELETE, user));
        return permissions;
    }

    public Permissions getPermissionsForResource(Authorizable authorizableResource, Permissions knownParentAuthorizablePermissions) {
        if (knownParentAuthorizablePermissions == null) {
            return getPermissionsForResource(authorizableResource);
        }

        final Permissions permissions = new Permissions(knownParentAuthorizablePermissions);
        NiFiUser user = NiFiUserUtils.getNiFiUser();

        if (!permissions.getCanRead()) {
            permissions.setCanRead(authorizableResource.isAuthorized(authorizer, RequestAction.READ, user));
        }

        if (!permissions.getCanWrite()) {
            permissions.setCanWrite(authorizableResource.isAuthorized(authorizer, RequestAction.WRITE, user));
        }

        if (!permissions.getCanDelete()) {
            permissions.setCanDelete(authorizableResource.isAuthorized(authorizer, RequestAction.DELETE, user));
        }

        return permissions;
    }

    private ResourcePermissions getTopLevelPermissions() {

        NiFiUser user = NiFiUserUtils.getNiFiUser();
        ResourcePermissions resourcePermissions = new ResourcePermissions();

        final Permissions bucketsPermissions = getPermissionsForResource(authorizableLookup.getBucketsAuthorizable());
        resourcePermissions.setBuckets(bucketsPermissions);

        final Permissions policiesPermissions = getPermissionsForResource(authorizableLookup.getPoliciesAuthorizable());
        resourcePermissions.setPolicies(policiesPermissions);

        final Permissions tenantsPermissions = getPermissionsForResource(authorizableLookup.getTenantsAuthorizable());
        resourcePermissions.setTenants(tenantsPermissions);

        final Permissions proxyPermissions = getPermissionsForResource(authorizableLookup.getProxyAuthorizable());
        resourcePermissions.setProxy(proxyPermissions);

        return resourcePermissions;
    }

    // ---------------------- User methods ----------------------------------------------

    public User createUser(User user) {
        verifyUserGroupProviderIsConfigurable();
        writeLock.lock();
        try {
            final org.apache.nifi.registry.security.authorization.User createdUser =
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

    public User getUserByIdentity(String identity) {
        this.readLock.lock();
        try {
            return userToDTO(userGroupProvider.getUserByIdentity(identity));
        } finally {
            this.readLock.unlock();
        }
    }

    public User updateUser(User user) {
        verifyUserGroupProviderIsConfigurable();
        this.writeLock.lock();
        try {
            final org.apache.nifi.registry.security.authorization.User updatedUser =
                    ((ConfigurableUserGroupProvider) userGroupProvider).updateUser(userFromDTO(user));
            if (updatedUser == null) {
                return null;
            }
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
            if (deletedUserDTO != null) {
                ((ConfigurableUserGroupProvider) userGroupProvider).deleteUser(identifier);
            }
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
            final org.apache.nifi.registry.security.authorization.Group createdGroup =
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
            final org.apache.nifi.registry.security.authorization.Group updatedGroup =
                    ((ConfigurableUserGroupProvider) userGroupProvider).updateGroup(userGroupFromDTO(userGroup));
            if (updatedGroup == null) {
                return null;
            }
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
            if (userGroupDTO != null) {
                ((ConfigurableUserGroupProvider) userGroupProvider).deleteGroup(identifier);
            }
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
            org.apache.nifi.registry.security.authorization.AccessPolicy createdAccessPolicy =
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

    private List<AccessPolicySummary> getAccessPolicySummariesForUser(String userIdentifier) {
        readLock.lock();
        try {
            return accessPolicyProvider.getAccessPolicies().stream()
                    .filter(accessPolicy -> {
                        if (accessPolicy.getUsers().contains(userIdentifier)) {
                            return true;
                        }
                        return accessPolicy.getGroups().stream().anyMatch(g -> {
                            final Group group = userGroupProvider.getGroup(g);
                            return group != null && group.getUsers().contains(userIdentifier);
                        });
                    })
                    .map(this::accessPolicyToSummaryDTO)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    private List<AccessPolicySummary> getAccessPolicySummariesForUserGroup(String userGroupIdentifier) {
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
        writeLock.lock();
        try {
            // Don't allow changing action or resource of existing policy (should only be adding/removing users/groups)
            org.apache.nifi.registry.security.authorization.AccessPolicy currentAccessPolicy =
                    accessPolicyProvider.getAccessPolicy(accessPolicy.getIdentifier());
            if (currentAccessPolicy == null) {
                return null;
            }
            accessPolicy.setResource(currentAccessPolicy.getResource());
            accessPolicy.setAction(currentAccessPolicy.getAction().toString());

            org.apache.nifi.registry.security.authorization.AccessPolicy updatedAccessPolicy =
                    ((ConfigurableAccessPolicyProvider) accessPolicyProvider).updateAccessPolicy(accessPolicyFromDTO(accessPolicy));
            if (updatedAccessPolicy == null) {
                return null;
            }
            return accessPolicyToDTO(updatedAccessPolicy);
        } finally {
            writeLock.unlock();
        }
    }

    public AccessPolicy deleteAccessPolicy(String identifier) {
        verifyAccessPolicyProviderIsConfigurable();
        writeLock.lock();
        try {
            AccessPolicy deletedAccessPolicyDTO = getAccessPolicy(identifier);
            if (deletedAccessPolicyDTO != null) {
                ((ConfigurableAccessPolicyProvider) accessPolicyProvider).deleteAccessPolicy(identifier);
            }
            return deletedAccessPolicyDTO;
        } finally {
            writeLock.unlock();
        }
    }


    // ---------------------- Resource Lookup methods --------------------------------------

    public List<Resource> getResources() {
        final List<Resource> dtoResources =
                getAuthorizableResources()
                        .stream()
                        .map(AuthorizationService::resourceToDTO)
                        .collect(Collectors.toList());
        return dtoResources;
    }

    public List<Resource> getAuthorizedResources(RequestAction actionType) {
        return getAuthorizedResources(actionType, null);
    }

    public List<Resource> getAuthorizedResources(RequestAction actionType, ResourceType resourceType) {
        final List<Resource> authorizedResources =
                getAuthorizableResources(resourceType)
                        .stream()
                        .filter(resource -> {
                            String resourceId = resource.getIdentifier();
                            try {
                                authorizableLookup
                                        .getAuthorizableByResource(resource.getIdentifier())
                                        .authorize(authorizer, actionType, NiFiUserUtils.getNiFiUser());
                                return true;
                            } catch (AccessDeniedException e) {
                                return false;
                            }
                        })
                        .map(AuthorizationService::resourceToDTO)
                        .collect(Collectors.toList());

        return authorizedResources;
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

    private ResourcePermissions getTopLevelPermissions(String tenantIdentifier) {
        ResourcePermissions resourcePermissions = new ResourcePermissions();

        final Permissions bucketsPermissions = getPermissionsForResource(tenantIdentifier, ResourceFactory.getBucketsResource());
        resourcePermissions.setBuckets(bucketsPermissions);

        final Permissions policiesPermissions = getPermissionsForResource(tenantIdentifier, ResourceFactory.getPoliciesResource());
        resourcePermissions.setPolicies(policiesPermissions);

        final Permissions tenantsPermissions = getPermissionsForResource(tenantIdentifier, ResourceFactory.getTenantsResource());
        resourcePermissions.setTenants(tenantsPermissions);

        final Permissions proxyPermissions = getPermissionsForResource(tenantIdentifier, ResourceFactory.getProxyResource());
        resourcePermissions.setProxy(proxyPermissions);

        return resourcePermissions;
    }

    private Permissions getPermissionsForResource(String tenantIdentifier, org.apache.nifi.registry.security.authorization.Resource resource) {

        Permissions permissions = new Permissions();
        permissions.setCanRead(checkTenantBelongsToPolicy(tenantIdentifier, resource, RequestAction.READ));
        permissions.setCanWrite(checkTenantBelongsToPolicy(tenantIdentifier, resource, RequestAction.WRITE));
        permissions.setCanDelete(checkTenantBelongsToPolicy(tenantIdentifier, resource, RequestAction.DELETE));
        return permissions;

    }

    private boolean checkTenantBelongsToPolicy(String tenantIdentifier, org.apache.nifi.registry.security.authorization.Resource resource, RequestAction action) {
        org.apache.nifi.registry.security.authorization.AccessPolicy policy =
                accessPolicyProvider.getAccessPolicy(resource.getIdentifier(), action);

        if (policy == null) {
            return false;
        }

        boolean tenantInPolicy = policy.getUsers().contains(tenantIdentifier) || policy.getGroups().contains(tenantIdentifier);
        return tenantInPolicy;
    }

    private List<org.apache.nifi.registry.security.authorization.Resource> getAuthorizableResources() {
        return getAuthorizableResources(null);
    }

    private List<org.apache.nifi.registry.security.authorization.Resource> getAuthorizableResources(ResourceType includeFilter) {

        final List<org.apache.nifi.registry.security.authorization.Resource> resources = new ArrayList<>();

        if (includeFilter == null || includeFilter.equals(ResourceType.Policy)) {
            resources.add(ResourceFactory.getPoliciesResource());
        }
        if (includeFilter == null || includeFilter.equals(ResourceType.Tenant)) {
            resources.add(ResourceFactory.getTenantsResource());
        }
        if (includeFilter == null || includeFilter.equals(ResourceType.Proxy)) {
            resources.add(ResourceFactory.getProxyResource());
        }
        if (includeFilter == null || includeFilter.equals(ResourceType.Actuator)) {
            resources.add(ResourceFactory.getActuatorResource());
        }
        if (includeFilter == null || includeFilter.equals(ResourceType.Swagger)) {
            resources.add(ResourceFactory.getSwaggerResource());
        }
        if (includeFilter == null || includeFilter.equals(ResourceType.Bucket)) {
            resources.add(ResourceFactory.getBucketsResource());
            // add all buckets
            for (final Bucket bucket : registryService.getBuckets()) {
                resources.add(ResourceFactory.getBucketResource(bucket.getIdentifier(), bucket.getName()));
            }
        }

        return resources;
    }

    private User userToDTO(
            final org.apache.nifi.registry.security.authorization.User user) {
        if (user == null) {
            return null;
        }
        String userIdentifier = user.getIdentifier();

        Collection<Tenant> groupsContainingUser = userGroupProvider.getGroups().stream()
                .filter(group -> group.getUsers().contains(userIdentifier))
                .map(this::tenantToDTO)
                .collect(Collectors.toList());
        Collection<AccessPolicySummary> accessPolicySummaries = getAccessPolicySummariesForUser(userIdentifier);

        User userDTO = new User(user.getIdentifier(), user.getIdentity());
        userDTO.setConfigurable(AuthorizerCapabilityDetection.isUserConfigurable(authorizer, user));
        userDTO.setResourcePermissions(getTopLevelPermissions(userDTO.getIdentifier()));
        userDTO.addUserGroups(groupsContainingUser);
        userDTO.addAccessPolicies(accessPolicySummaries);
        return userDTO;
    }

    private UserGroup userGroupToDTO(
            final org.apache.nifi.registry.security.authorization.Group userGroup) {
        if (userGroup == null) {
            return null;
        }

        Collection<Tenant> userTenants = userGroup.getUsers() != null
                ? userGroup.getUsers().stream().map(this::tenantIdToDTO).filter(Objects::nonNull).collect(Collectors.toSet()) : null;
        Collection<AccessPolicySummary> accessPolicySummaries = getAccessPolicySummariesForUserGroup(userGroup.getIdentifier());

        UserGroup userGroupDTO = new UserGroup(userGroup.getIdentifier(), userGroup.getName());
        userGroupDTO.setConfigurable(AuthorizerCapabilityDetection.isGroupConfigurable(authorizer, userGroup));
        userGroupDTO.setResourcePermissions(getTopLevelPermissions(userGroupDTO.getIdentifier()));
        userGroupDTO.addUsers(userTenants);
        userGroupDTO.addAccessPolicies(accessPolicySummaries);
        return userGroupDTO;
    }

    private AccessPolicy accessPolicyToDTO(
            final org.apache.nifi.registry.security.authorization.AccessPolicy accessPolicy) {
        if (accessPolicy == null) {
            return null;
        }

        Collection<Tenant> users = accessPolicy.getUsers() != null
                ? accessPolicy.getUsers().stream().map(this::tenantIdToDTO).filter(Objects::nonNull).collect(Collectors.toList()) : null;
        Collection<Tenant> userGroups = accessPolicy.getGroups() != null
                ? accessPolicy.getGroups().stream().map(this::tenantIdToDTO).filter(Objects::nonNull).collect(Collectors.toList()) : null;

        Boolean isConfigurable = AuthorizerCapabilityDetection.isAccessPolicyConfigurable(authorizer, accessPolicy);

        return accessPolicyToDTO(accessPolicy, userGroups, users, isConfigurable);
    }

    private Tenant tenantIdToDTO(String identifier) {
        this.readLock.lock();
        try {
            org.apache.nifi.registry.security.authorization.User user = userGroupProvider.getUser(identifier);
            if (user != null) {
                return tenantToDTO(user);
            } else {
                org.apache.nifi.registry.security.authorization.Group group = userGroupProvider.getGroup(identifier);
                return tenantToDTO(group);
            }
        } finally {
            this.readLock.unlock();
        }
    }

    private AccessPolicySummary accessPolicyToSummaryDTO(
            final org.apache.nifi.registry.security.authorization.AccessPolicy accessPolicy) {
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

    private Tenant tenantToDTO(org.apache.nifi.registry.security.authorization.User user) {
        if (user == null) {
            return null;
        }
        Tenant tenantDTO = new Tenant(user.getIdentifier(), user.getIdentity());
        tenantDTO.setConfigurable(AuthorizerCapabilityDetection.isUserConfigurable(authorizer, user));
        return tenantDTO;
    }

    private Tenant tenantToDTO(org.apache.nifi.registry.security.authorization.Group group) {
        if (group == null) {
            return null;
        }
        Tenant tenantDTO = new Tenant(group.getIdentifier(), group.getName());
        tenantDTO.setConfigurable(AuthorizerCapabilityDetection.isGroupConfigurable(authorizer, group));
        return tenantDTO;
    }

    private static Resource resourceToDTO(org.apache.nifi.registry.security.authorization.Resource resource) {
        if (resource == null) {
            return null;
        }
        Resource resourceDto = new Resource();
        resourceDto.setIdentifier(resource.getIdentifier());
        resourceDto.setName(resource.getName());
        return resourceDto;
    }

    private static org.apache.nifi.registry.security.authorization.User userFromDTO(
            final User userDTO) {
        if (userDTO == null) {
            return null;
        }
        return new org.apache.nifi.registry.security.authorization.User.Builder()
                .identifier(userDTO.getIdentifier() != null ? userDTO.getIdentifier() : UUID.randomUUID().toString())
                .identity(userDTO.getIdentity())
                .build();
    }

    private static org.apache.nifi.registry.security.authorization.Group userGroupFromDTO(
            final UserGroup userGroupDTO) {
        if (userGroupDTO == null) {
            return null;
        }
        org.apache.nifi.registry.security.authorization.Group.Builder groupBuilder = new org.apache.nifi.registry.security.authorization.Group.Builder()
                .identifier(userGroupDTO.getIdentifier() != null ? userGroupDTO.getIdentifier() : UUID.randomUUID().toString())
                .name(userGroupDTO.getIdentity());
        Set<Tenant> users = userGroupDTO.getUsers();
        if (users != null) {
            groupBuilder.addUsers(users.stream().map(Tenant::getIdentifier).collect(Collectors.toSet()));
        }
        return groupBuilder.build();
    }

    private static org.apache.nifi.registry.security.authorization.AccessPolicy accessPolicyFromDTO(
            final AccessPolicy accessPolicyDTO) {
        org.apache.nifi.registry.security.authorization.AccessPolicy.Builder accessPolicyBuilder =
                new org.apache.nifi.registry.security.authorization.AccessPolicy.Builder()
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

    private static AccessPolicy accessPolicyToDTO(
            final org.apache.nifi.registry.security.authorization.AccessPolicy accessPolicy,
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
            public Set<org.apache.nifi.registry.security.authorization.AccessPolicy> getAccessPolicies() throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public org.apache.nifi.registry.security.authorization.AccessPolicy getAccessPolicy(String identifier) throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public org.apache.nifi.registry.security.authorization.AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action) throws AuthorizationAccessException {
                throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
            }

            @Override
            public UserGroupProvider getUserGroupProvider() {
                return new UserGroupProvider() {
                    @Override
                    public Set<org.apache.nifi.registry.security.authorization.User> getUsers() throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public org.apache.nifi.registry.security.authorization.User getUser(String identifier) throws AuthorizationAccessException {
                        throw new IllegalStateException(MSG_NON_MANAGED_AUTHORIZER);
                    }

                    @Override
                    public org.apache.nifi.registry.security.authorization.User getUserByIdentity(String identity) throws AuthorizationAccessException {
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
                    public void initialize(UserGroupProviderInitializationContext initializationContext) throws SecurityProviderCreationException {

                    }

                    @Override
                    public void onConfigured(AuthorizerConfigurationContext configurationContext) throws SecurityProviderCreationException {

                    }

                    @Override
                    public void preDestruction() throws SecurityProviderDestructionException {

                    }
                };
            }

            @Override
            public void initialize(AccessPolicyProviderInitializationContext initializationContext) throws SecurityProviderCreationException {
            }

            @Override
            public void onConfigured(AuthorizerConfigurationContext configurationContext) throws SecurityProviderCreationException {
            }

            @Override
            public void preDestruction() throws SecurityProviderDestructionException {
            }
        };

    }

}
