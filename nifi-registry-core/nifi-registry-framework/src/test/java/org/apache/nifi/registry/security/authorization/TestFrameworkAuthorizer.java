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
package org.apache.nifi.registry.security.authorization;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.security.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.security.authorization.user.StandardNiFiUser;
import org.apache.nifi.registry.service.RegistryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestFrameworkAuthorizer {

    private Authorizer frameworkAuthorizer;
    private Authorizer wrappedAuthorizer;
    private RegistryService registryService;

    private Bucket bucketPublic;
    private Bucket bucketNotPublic;

    @Before
    public void setup() {
        wrappedAuthorizer = mock(Authorizer.class);
        registryService = mock(RegistryService.class);
        frameworkAuthorizer = new FrameworkAuthorizer(wrappedAuthorizer, registryService);

        bucketPublic = new Bucket();
        bucketPublic.setIdentifier(UUID.randomUUID().toString());
        bucketPublic.setName("Public Bucket");
        bucketPublic.setAllowPublicRead(true);

        bucketNotPublic = new Bucket();
        bucketNotPublic.setIdentifier(UUID.randomUUID().toString());
        bucketNotPublic.setName("Non Public Bucket");
        bucketNotPublic.setAllowPublicRead(false);

        when(registryService.getBucket(bucketPublic.getIdentifier())).thenReturn(bucketPublic);
        when(registryService.getBucket(bucketNotPublic.getIdentifier())).thenReturn(bucketNotPublic);
    }

    @Test
    public void testReadPublicBucketWhenAnonymous() {
        final Resource resource = ResourceFactory.getBucketResource(bucketPublic.getIdentifier(), bucketPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("anonymous")
                .anonymous(true)
                .build();

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Approved, result.getResult());

        // should never make it to wrapped authorizer
        verify(wrappedAuthorizer, times(0)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testReadNonPublicBucketWhenAnonymous() {
        final Resource resource = ResourceFactory.getBucketResource(bucketNotPublic.getIdentifier(), bucketNotPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("anonymous")
                .anonymous(true)
                .build();

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Denied, result.getResult());

        // should be denied before making it to the wrapped authorizer since the user is anonymous
        verify(wrappedAuthorizer, times(0)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testWritePublicBucketWhenAnonymous() {
        final Resource resource = ResourceFactory.getBucketResource(bucketPublic.getIdentifier(), bucketPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.WRITE)
                .accessAttempt(true)
                .identity("anonymous")
                .anonymous(true)
                .build();

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Denied, result.getResult());

        // should be denied before making it to wrapped authorizer since request is anonymous
        verify(wrappedAuthorizer, times(0)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testReadPublicBucketWhenNotAnonymous() {
        final Resource resource = ResourceFactory.getBucketResource(bucketPublic.getIdentifier(), bucketPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("user1")
                .anonymous(false)
                .proxyNiFiUsers(Arrays.asList(
                        new StandardNiFiUser.Builder().identity("proxy1").build(),
                        new StandardNiFiUser.Builder().identity("proxy2").build()))
                .build();

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Approved, result.getResult());

        // should never make it to wrapped authorizer
        verify(wrappedAuthorizer, times(0)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testReadNonPublicBucketWhenNotAnonymousAndAuthorizedProxies() {
        final Resource resource = ResourceFactory.getBucketResource(bucketNotPublic.getIdentifier(), bucketNotPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("user1")
                .anonymous(false)
                .proxyNiFiUsers(Arrays.asList(
                        new StandardNiFiUser.Builder().identity("proxy1").build(),
                        new StandardNiFiUser.Builder().identity("proxy2").build()))
                .build();

        // since the bucket is not public it will fall through to the wrapped authorizer
        when(wrappedAuthorizer.authorize(any(AuthorizationRequest.class)))
                .thenReturn(AuthorizationResult.approved());

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Approved, result.getResult());

        // should make 3 calls to the wrapped authorizer to authorize user1, proxy1, proxy2
        verify(wrappedAuthorizer, times(3)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testReadNonPublicBucketWhenNotAnonymousAndUnauthorizedProxy() {
        final Resource resource = ResourceFactory.getBucketResource(bucketNotPublic.getIdentifier(), bucketNotPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("user1")
                .anonymous(false)
                .proxyNiFiUsers(Arrays.asList(
                        new StandardNiFiUser.Builder().identity("proxy1").build(),
                        new StandardNiFiUser.Builder().identity("proxy2").build()))
                .build();

        // since the bucket is not public and the user is not anonymous, it will continue to proxy authorization

        // simulate the first proxy being authorized for READ actions
        final AuthorizationRequestMatcher proxy1Matcher = new AuthorizationRequestMatcher(
                "proxy1", ResourceFactory.getProxyResource(), request.getAction());
        when(wrappedAuthorizer.authorize(argThat(proxy1Matcher))).thenReturn(AuthorizationResult.approved());

        // simulate the second proxy being unauthorized for READ actions
        final AuthorizationRequestMatcher proxy2Matcher = new AuthorizationRequestMatcher(
                "proxy2", ResourceFactory.getProxyResource(), request.getAction());
        when(wrappedAuthorizer.authorize(argThat(proxy2Matcher))).thenReturn(AuthorizationResult.denied("denied"));

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Denied, result.getResult());

        // should make 2 calls to the wrapped authorizer for the two proxies
        verify(wrappedAuthorizer, times(2)).authorize(any(AuthorizationRequest.class));
    }

    @Test
    public void testReadNonPublicBucketWhenNotAnonymousAndUnauthorizedEndUser() {
        final Resource resource = ResourceFactory.getBucketResource(bucketNotPublic.getIdentifier(), bucketNotPublic.getName());

        final AuthorizationRequest request = new AuthorizationRequest.Builder()
                .resource(resource)
                .requestedResource(resource)
                .action(RequestAction.READ)
                .accessAttempt(true)
                .identity("user1")
                .anonymous(false)
                .proxyNiFiUsers(Arrays.asList(
                        new StandardNiFiUser.Builder().identity("proxy1").build(),
                        new StandardNiFiUser.Builder().identity("proxy2").build()))
                .build();

        // since the bucket is not public and the user is not anonymous, it will continue to proxy authorization

        // simulate the first proxy being authorized for READ actions
        final AuthorizationRequestMatcher proxy1Matcher = new AuthorizationRequestMatcher(
                "proxy1", ResourceFactory.getProxyResource(), request.getAction());
        when(wrappedAuthorizer.authorize(argThat(proxy1Matcher))).thenReturn(AuthorizationResult.approved());

        // simulate the second proxy being authorized for READ actions
        final AuthorizationRequestMatcher proxy2Matcher = new AuthorizationRequestMatcher(
                "proxy2", ResourceFactory.getProxyResource(), request.getAction());
        when(wrappedAuthorizer.authorize(argThat(proxy2Matcher))).thenReturn(AuthorizationResult.approved());

        // simulate the end user being unauthorized for READ actions
        final AuthorizationRequestMatcher user1Matcher = new AuthorizationRequestMatcher(
                "user1", resource, request.getAction());
        when(wrappedAuthorizer.authorize(argThat(user1Matcher))).thenReturn(AuthorizationResult.denied("denied"));

        final AuthorizationResult result = frameworkAuthorizer.authorize(request);
        assertNotNull(result);
        assertEquals(AuthorizationResult.Result.Denied, result.getResult());

        // should make 3 calls to the wrapped authorizer for the two proxies and end user
        verify(wrappedAuthorizer, times(3)).authorize(any(AuthorizationRequest.class));
    }


    /**
     * Matcher for matching Authorization requests.
     */
    private static class AuthorizationRequestMatcher implements ArgumentMatcher<AuthorizationRequest> {

        private final String identity;
        private final Resource resource;
        private final RequestAction action;

        public AuthorizationRequestMatcher(final String identity, final Resource resource, final RequestAction action) {
            this.identity = identity;
            this.resource = resource;
            this.action = action;
        }

        @Override
        public boolean matches(final AuthorizationRequest request) {
            if (request == null) {
                return false;
            }

            return identity.equals(request.getIdentity())
                    && resource.getIdentifier().equals(request.getResource().getIdentifier())
                    && action == request.getAction();
        }
    }
}
