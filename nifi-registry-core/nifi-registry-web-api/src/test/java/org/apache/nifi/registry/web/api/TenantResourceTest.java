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
package org.apache.nifi.registry.web.api;

import org.apache.nifi.registry.authorization.User;
import org.apache.nifi.registry.authorization.UserGroup;
import org.apache.nifi.registry.event.EventFactory;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.security.authorization.AuthorizableLookup;
import org.apache.nifi.registry.security.authorization.Authorizer;
import org.apache.nifi.registry.security.authorization.ConfigurableAccessPolicyProvider;
import org.apache.nifi.registry.security.authorization.ConfigurableUserGroupProvider;
import org.apache.nifi.registry.security.authorization.ManagedAuthorizer;
import org.apache.nifi.registry.service.AuthorizationService;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantResourceTest {

    private TenantResource tenantResource;
    private AuthorizationService authorizationService;
    private Authorizer authorizer;
    private ConfigurableAccessPolicyProvider accessPolicyProvider;
    private EventService eventService;
    private ConfigurableUserGroupProvider userGroupProvider;
    private AuthorizableLookup authorizableLookup;

    @Before
    public void setUp() {
        authorizationService = mock(AuthorizationService.class);
        authorizer = mock(ManagedAuthorizer.class);
        authorizableLookup = mock(AuthorizableLookup.class);
        accessPolicyProvider = mock(ConfigurableAccessPolicyProvider.class);
        userGroupProvider = mock(ConfigurableUserGroupProvider.class);
        eventService = mock(EventService.class);

        when(accessPolicyProvider.getUserGroupProvider()).thenReturn(userGroupProvider);
        when(((ManagedAuthorizer) authorizer).getAccessPolicyProvider()).thenReturn(accessPolicyProvider);
        when(authorizationService.getAuthorizer()).thenReturn(authorizer);
        when(authorizationService.getAuthorizableLookup()).thenReturn(authorizableLookup);

        tenantResource = new TenantResource(authorizationService, eventService) {

            @Override
            protected URI getBaseUri() {
                try {
                    return new URI("http://registry.nifi.apache.org/nifi-registry");
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testCreateUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User(null, "identity");
        User result = new User("identifier", user.getIdentity());

        when(authorizationService.createUser(user)).thenReturn(result);

        tenantResource.createUser(request, user);

        verify(authorizationService).createUser(user);
        verify(eventService).publish(eq(EventFactory.userCreated(result)));
    }

    @Test
    public void testUpdateUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("identifier", "new-identity");

        when(authorizationService.updateUser(user)).thenReturn(user);

        tenantResource.updateUser(request, user.getIdentifier(), user);

        verify(authorizationService).updateUser(user);
        verify(eventService).publish(eq(EventFactory.userUpdated(user)));
    }

    @Test
    public void testDeleteUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("identifier", "identity");

        when(authorizationService.deleteUser(user.getIdentifier())).thenReturn(user);

        tenantResource.removeUser(request, user.getIdentifier());

        verify(authorizationService).deleteUser(user.getIdentifier());
        verify(eventService).publish(eq(EventFactory.userDeleted(user)));
    }

    @Test
    public void testCreateUserGroup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UserGroup userGroup = new UserGroup(null, "identity");
        UserGroup result = new UserGroup("identifier", userGroup.getIdentity());

        when(authorizationService.createUserGroup(userGroup)).thenReturn(result);

        tenantResource.createUserGroup(request, userGroup);

        verify(authorizationService).createUserGroup(userGroup);
        verify(eventService).publish(eq(EventFactory.userGroupCreated(result)));
    }

    @Test
    public void testUpdateUserGroup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UserGroup userGroup = new UserGroup("identifier", "new-identity");

        when(authorizationService.updateUserGroup(userGroup)).thenReturn(userGroup);

        tenantResource.updateUserGroup(request, userGroup.getIdentifier(), userGroup);

        verify(authorizationService).updateUserGroup(userGroup);
        verify(eventService).publish(eq(EventFactory.userGroupUpdated(userGroup)));
    }

    @Test
    public void testDeleteUserGroup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UserGroup userGroup = new UserGroup("identifier", "identity");

        when(authorizationService.deleteUserGroup(userGroup.getIdentifier())).thenReturn(userGroup);

        tenantResource.removeUserGroup(request, userGroup.getIdentifier());

        verify(authorizationService).deleteUserGroup(userGroup.getIdentifier());
        verify(eventService).publish(eq(EventFactory.userGroupDeleted(userGroup)));
    }
}
