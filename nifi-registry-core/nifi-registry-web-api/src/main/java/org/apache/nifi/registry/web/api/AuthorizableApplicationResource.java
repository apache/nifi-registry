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

import org.apache.nifi.registry.authorization.Resource;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.security.authorization.AuthorizableLookup;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.security.authorization.resource.ResourceType;
import org.apache.nifi.registry.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizableApplicationResource extends ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizableApplicationResource.class);

    protected final AuthorizationService authorizationService;
    protected final AuthorizableLookup authorizableLookup;

    protected AuthorizableApplicationResource(
            AuthorizationService authorizationService,
            EventService eventService) {
        super(eventService);
        this.authorizationService = authorizationService;
        this.authorizableLookup = authorizationService.getAuthorizableLookup();
    }

    protected void authorizeBucketAccess(RequestAction actionType, String bucketIdentifier) {
        final Authorizable bucketAuthorizable = authorizableLookup.getBucketAuthorizable(bucketIdentifier);
        authorizationService.authorize(bucketAuthorizable, actionType);
    }

    protected void authorizeBucketItemAccess(RequestAction actionType, BucketItem bucketItem) {
        authorizeBucketAccess(actionType, bucketItem.getBucketIdentifier());
    }

    protected Set<String> getAuthorizedBucketIds(RequestAction actionType) {
        return authorizationService
                .getAuthorizedResources(actionType, ResourceType.Bucket)
                .stream()
                .map(AuthorizableApplicationResource::extractBucketIdFromResource)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
    }

    private static String extractBucketIdFromResource(Resource resource) {

        if (resource == null || resource.getIdentifier() == null || !resource.getIdentifier().startsWith("/buckets/")) {
            return null;
        }

        String[] pathComponents = resource.getIdentifier().split("/");
        if (pathComponents.length < 3) {
            return null;
        }
        return pathComponents[2];
    }

}
