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
import org.apache.nifi.registry.exception.ResourceNotFoundException;
import org.apache.nifi.registry.security.authorization.resource.Authorizable;
import org.apache.nifi.registry.security.authorization.resource.InheritingAuthorizable;
import org.apache.nifi.registry.security.authorization.resource.ResourceFactory;
import org.apache.nifi.registry.security.authorization.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StandardAuthorizableLookup implements AuthorizableLookup {

    private static final Logger logger = LoggerFactory.getLogger(StandardAuthorizableLookup.class);

    private static final Authorizable TENANTS_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getTenantsResource();
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

    private static final Authorizable ACTUATOR_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getActuatorResource();
        }
    };

    private static final Authorizable SWAGGER_AUTHORIZABLE = new Authorizable() {
        @Override
        public Authorizable getParentAuthorizable() {
            return null;
        }

        @Override
        public Resource getResource() {
            return ResourceFactory.getSwaggerResource();
        }
    };

    @Override
    public Authorizable getActuatorAuthorizable() {
        return ACTUATOR_AUTHORIZABLE;
    }

    @Override
    public Authorizable getSwaggerAuthorizable() {
        return SWAGGER_AUTHORIZABLE;
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
        // Note - this returns a special Authorizable type that inherits permissions from the parent Authorizable
        return new InheritingAuthorizable() {

            @Override
            public Authorizable getParentAuthorizable() {
                return getBucketsAuthorizable();
            }

            @Override
            public Resource getResource() {
                return ResourceFactory.getBucketResource(bucketIdentifier, "Bucket with ID " + bucketIdentifier);
            }
        };
    }

    @Override
    public Authorizable getAuthorizableByResource(String resource) {
        ResourceType resourceType = ResourceType.mapFullResourcePathToResourceType(resource);

        if (resourceType == null) {
            throw new ResourceNotFoundException("Unrecognized resource: " + resource);
        }

        return getAuthorizableByResource(resourceType, resource);
    }

    private Authorizable getAuthorizableByResource(final ResourceType resourceType, final String resource) {
        Authorizable authorizable = null;
        switch (resourceType) {

            /* Access to these resources are always authorized by the top-level resource */
            case Policy:
                authorizable = getPoliciesAuthorizable();
                break;
            case Tenant:
                authorizable = getTenantsAuthorizable();
                break;
            case Proxy:
                authorizable = getProxyAuthorizable();
                break;
            case Actuator:
                authorizable = getActuatorAuthorizable();
                break;
            case Swagger:
                authorizable = getSwaggerAuthorizable();
                break;

            /* Access to buckets can be authorized by the top-level /buckets resource or an individual /buckets/{id} resource */
            case Bucket:
                final String childResourceId = StringUtils.substringAfter(resource, resourceType.getValue());
                if (childResourceId.startsWith("/")) {
                    authorizable = getAuthorizableByChildResource(resourceType, childResourceId);
                } else {
                    authorizable = getBucketsAuthorizable();
                }
        }

        if (authorizable == null) {
            logger.debug("Could not determine the Authorizable for resource type='{}', path='{}', ", resourceType.getValue(), resource);
            throw new IllegalArgumentException("This an unexpected type of authorizable resource: " + resourceType.getValue());
        }

        return authorizable;
    }

    private Authorizable getAuthorizableByChildResource(final ResourceType baseResourceType, final String childResourceId) {
        Authorizable authorizable;
        switch (baseResourceType) {
            case Bucket:
                String[] childResourcePathParts = childResourceId.split("/");
                if (childResourcePathParts.length >= 1) {
                    final String bucketId = childResourcePathParts[1];
                    authorizable = getBucketAuthorizable(bucketId);
                    break;
                }
            default:
                throw new IllegalArgumentException("Unexpected lookup for child resource authorizable for base resource type " + baseResourceType.getValue());
        }

        return authorizable;
    }

}
