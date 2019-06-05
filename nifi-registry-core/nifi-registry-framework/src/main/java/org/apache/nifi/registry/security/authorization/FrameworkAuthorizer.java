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
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.security.authorization.exception.AccessDeniedException;
import org.apache.nifi.registry.security.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.security.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.security.authorization.resource.ResourceType;
import org.apache.nifi.registry.security.authorization.user.NiFiUser;
import org.apache.nifi.registry.security.authorization.user.StandardNiFiUser;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.security.exception.SecurityProviderDestructionException;
import org.apache.nifi.registry.service.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Wraps an Authorizer and adds framework level logic for authorizing proxies, public resources, and anything else
 * that needs to be done on top of the regular Authorizer.
 */
public class FrameworkAuthorizer implements Authorizer {

    public static Logger LOGGER = LoggerFactory.getLogger(FrameworkAuthorizer.class);

    private static final Authorizable PROXY_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getProxyResource();
        }
    };

    private final Authorizer wrappedAuthorizer;
    private final RegistryService registryService;

    public FrameworkAuthorizer(final Authorizer wrappedAuthorizer, final RegistryService registryService) {
        this.wrappedAuthorizer = Objects.requireNonNull(wrappedAuthorizer);
        this.registryService = Objects.requireNonNull(registryService);
    }

    @Override
    public void initialize(final AuthorizerInitializationContext initializationContext) throws SecurityProviderCreationException {
        wrappedAuthorizer.initialize(initializationContext);
    }

    @Override
    public void onConfigured(final AuthorizerConfigurationContext configurationContext) throws SecurityProviderCreationException {
        wrappedAuthorizer.onConfigured(configurationContext);
    }

    @Override
    public AuthorizationResult authorize(final AuthorizationRequest request) throws AuthorizationAccessException {
        final Resource resource = request.getResource();
        final RequestAction requestAction = request.getAction();

        /**
         * If the request is for a resource that has been made public and action is READ, then it should automatically be authorized.
         *
         * This needs to be checked before the proxy authorizations b/c access to a public resource should always be allowed.
         */

        final boolean allowPublicAccess = isPublicAccessAllowed(resource, requestAction);
        if (allowPublicAccess) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Authorizing access to public resource '{}'", new Object[]{resource.getIdentifier()});
            }
            return AuthorizationResult.approved();
        }

        /**
         * Deny an anonymous user access to anything else, they should only have access to publicly readable resources checked above
         */

        if (request.isAnonymous()) {
            return AuthorizationResult.denied("Anonymous access is not authorized");
        }

        /*
        * If the request has a proxy chain, ensure each identity in the chain is an authorized proxy for the given action.
        *
        * The action comes from the original request. For example, if user1 is proxied by proxy1, and it is a WRITE request
        * to /buckets/12345, then we need to determine if proxy1 is authorized to proxy WRITE requests.
        */

        final List<String> proxyChainIdentities = request.getProxyIdentities();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found {} proxy identities", new Object[]{proxyChainIdentities.size()});
        }

        for (final String proxyIdentity : proxyChainIdentities) {
            final NiFiUser proxyNiFiUser = createProxyNiFiUser(proxyIdentity);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Authorizing proxy [{}] for {}", new Object[]{proxyIdentity, requestAction});
            }

            try {
                PROXY_AUTHORIZABLE.authorize(wrappedAuthorizer, requestAction, proxyNiFiUser);
            } catch (final AccessDeniedException e) {
                final String actionString = requestAction.toString();
                return AuthorizationResult.denied(String.format("Untrusted proxy [%s] for %s operation.", proxyIdentity, actionString));
            }
        }

        /**
         * All other authorization decisions need to be delegated to the original wrapped Authorizer.
         */

        return wrappedAuthorizer.authorize(request);
    }

    /**
     * Determines if the given Resource is considered public for the action being performed.
     *
     * @param resource a Resource being authorized
     * @param action the action being performed
     * @return true if the resource is public for the given action, false otherwise
     */
    private boolean isPublicAccessAllowed(final Resource resource, final RequestAction action) {
        if (resource == null || action == null) {
            return false;
        }

        final String resourceIdentifier = resource.getIdentifier();
        if (resourceIdentifier == null || !resourceIdentifier.startsWith(ResourceType.Bucket.getValue() + "/")) {
            return false;
        }

        final int lastSlashIndex = resourceIdentifier.lastIndexOf("/");
        if (lastSlashIndex < 0 || lastSlashIndex >= resourceIdentifier.length() - 1) {
            return false;
        }

        final String bucketId = resourceIdentifier.substring(lastSlashIndex + 1);
        try {
            final Bucket bucket = registryService.getBucket(bucketId);
            return bucket.isAllowPublicRead() && action == RequestAction.READ;
        } catch (ResourceNotFoundException rnfe) {
            // if not found then we can't determine public access, so return false to delegate to regular authorizer
            LOGGER.debug("Cannot determine public access, bucket not found with id [{}]", new Object[]{bucketId});
            return false;
        } catch (Exception e) {
            LOGGER.error("Error checking public access to bucket with id [{}]", new Object[]{bucketId}, e);
            return false;
        }
    }

    /**
     * Creates a NiFiUser for the given proxy identity.
     *
     * This is only intended to be used for authorizing the given proxy identity against the /proxy resource, so we
     * don't need to populate the rest of the info on this user.
     *
     * @param proxyIdentity the proxy identity
     * @return the NiFiUser
     */
    private NiFiUser createProxyNiFiUser(final String proxyIdentity) {
        return new StandardNiFiUser.Builder().identity(proxyIdentity).build();
    }

    @Override
    public void preDestruction() throws SecurityProviderDestructionException {
        wrappedAuthorizer.preDestruction();
    }

}
