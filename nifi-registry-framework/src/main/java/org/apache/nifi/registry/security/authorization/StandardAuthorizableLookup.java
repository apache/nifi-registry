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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.security.authorization.Resource;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.security.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.security.authorization.resource.ResourceType;
import org.apache.nifi.registry.security.authorization.resource.AccessPolicyAuthorizable;
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class StandardAuthorizableLookup implements AuthorizableLookup {

    private static final Authorizable TENANTS_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getTenantResource();
        }
    };

    private static final Authorizable POLICIES_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getPoliciesResource();
        }
    };

    private static final Authorizable RESOURCES_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getResourceResource();
        }
    };

    private static final Authorizable BUCKETS_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getBucketsResource();
        }
    };

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

    @Override
    public Authorizable getResourcesAuthorizable() {
        return RESOURCES_AUTHORIZABLE;
    }

    @Override
    public Authorizable getProxyAuthorizable() {
        return PROXY_AUTHORIZABLE;
    }

    @Override
    public Authorizable getTenantsAuthorizable() {
        return TENANTS_AUTHORIZABLE;
    }

    @Override
    public Authorizable getPoliciesAuthorizable() {
        return POLICIES_AUTHORIZABLE;
    }

    @Override
    public Authorizable getBucketsAuthorizable() {
        return BUCKETS_AUTHORIZABLE;
    }

    @Override
    public Authorizable getBucketAuthorizable(String bucketIdentifier) {
        return new Authorizable() {

            @Override
            public Authorizable getParentAuthorizable() {
                return getBucketsAuthorizable();
            }

            @Override
            public Resource getResource() {
                return ResourceFactory.getBucketResource(bucketIdentifier, null);
            }
        };
    }

    @Override
    public Authorizable getAccessPolicyByResource(final String resource) {
        try {
            return new AccessPolicyAuthorizable(getAuthorizableByResource(resource));
        } catch (final ResourceNotFoundException e) {
            // the underlying component has been removed or resource is invalid... require /policies permissions
            return POLICIES_AUTHORIZABLE;
        }
    }

    @Override
    public Authorizable getAuthorizableByResource(String resource) {
        // parse the resource type
        ResourceType resourceType = null;
        for (ResourceType type : ResourceType.values()) {
            if (resource.equals(type.getValue()) || resource.startsWith(type.getValue() + "/")) {
                resourceType = type;
            }
        }

        if (resourceType == null) {
            throw new ResourceNotFoundException("Unrecognized resource: " + resource);
        }

        // if this is a policy resource, there should be another resource type
        if (ResourceType.Policy.equals(resourceType)) {
            final ResourceType primaryResourceType = resourceType;

            // get the resource type
            resource = StringUtils.substringAfter(resource, resourceType.getValue());

            for (ResourceType type : ResourceType.values()) {
                if (resource.equals(type.getValue()) || resource.startsWith(type.getValue() + "/")) {
                    resourceType = type;
                }
            }

            if (resourceType == null) {
                throw new ResourceNotFoundException("Unrecognized resource: " + resource);
            }

            return new AccessPolicyAuthorizable(getAccessPolicy(resourceType, resource));
        } else {
            return getAccessPolicy(resourceType, resource);
        }
    }

    private Authorizable getAccessPolicy(final ResourceType resourceType, final String resource) {
        final String slashComponentId = StringUtils.substringAfter(resource, resourceType.getValue());
        if (slashComponentId.startsWith("/")) {
            return getAccessPolicyByResource(resourceType, slashComponentId.substring(1));
        } else {
            return getAccessPolicyByResource(resourceType);
        }
    }

    private Authorizable getAccessPolicyByResource(final ResourceType resourceType, final String childResourceId) {
        Authorizable authorizable = null;
        switch (resourceType) {
            case Bucket:
                authorizable = getBucketAuthorizable(childResourceId);
        }

        if (authorizable == null) {
            throw new IllegalArgumentException("An unexpected type of resource in this policy " + resourceType.getValue());
        }

        return authorizable;
    }

    private Authorizable getAccessPolicyByResource(final ResourceType resourceType) {
        Authorizable authorizable = null;
        switch (resourceType) {

            case Bucket:
                authorizable = getBucketsAuthorizable();
                break;
            case Policy:
                authorizable = getPoliciesAuthorizable();
                break;
            case Resource:
                authorizable = new Authorizable() {
                    @Override
                    public Authorizable getParentAuthorizable() {
                        return null;
                    }

                    @Override
                    public Resource getResource() {
                        return ResourceFactory.getResourceResource();
                    }
                };
                break;
            case Tenant:
                authorizable = getTenantsAuthorizable();
                break;
            case Proxy:
                authorizable = getProxyAuthorizable();
        }

        if (authorizable == null) {
            throw new IllegalArgumentException("An unexpected type of resource in this policy " + resourceType.getValue());
        }

        return authorizable;
    }

}
