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

import org.apache.nifi.registry.security.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.security.exception.SecurityProviderDestructionException;

import java.util.Set;

/**
 * Provides access to AccessPolicies and the configured UserGroupProvider.
 *
 * NOTE: Extensions will be called often and frequently. Because of this, if the underlying implementation needs to
 * make remote calls or expensive calculations those should probably be done asynchronously and/or cache the results.
 *
 * Additionally, extensions need to be thread safe.
 */
public interface AccessPolicyProvider {

    /**
     * Retrieves all access policies. Must be non null
     *
     * @return a list of policies
     * @throws AuthorizationAccessException if there was an unexpected error performing the operation
     */
    Set<AccessPolicy> getAccessPolicies() throws AuthorizationAccessException;

    /**
     * Retrieves the policy with the given identifier.
     *
     * @param identifier the id of the policy to retrieve
     * @return the policy with the given id, or null if no matching policy exists
     * @throws AuthorizationAccessException if there was an unexpected error performing the operation
     */
    AccessPolicy getAccessPolicy(String identifier) throws AuthorizationAccessException;

    /**
     * Gets the access policies for the specified resource identifier and request action.
     *
     * @param resourceIdentifier the resource identifier
     * @param action the request action
     * @return the policy matching the resouce and action, or null if no matching policy exists
     * @throws AuthorizationAccessException if there was any unexpected error performing the operation
     */
    AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action) throws AuthorizationAccessException;

    /**
     * Returns the UserGroupProvider for this managed Authorizer. Must be non null
     *
     * @return the UserGroupProvider
     */
    UserGroupProvider getUserGroupProvider();

    /**
     * Called immediately after instance creation for implementers to perform additional setup
     *
     * @param initializationContext in which to initialize
     */
    void initialize(AccessPolicyProviderInitializationContext initializationContext) throws SecurityProviderCreationException;

    /**
     * Called to configure the Authorizer.
     *
     * @param configurationContext at the time of configuration
     * @throws SecurityProviderCreationException for any issues configuring the provider
     */
    void onConfigured(AuthorizerConfigurationContext configurationContext) throws SecurityProviderCreationException;

    /**
     * Called immediately before instance destruction for implementers to release resources.
     *
     * @throws SecurityProviderDestructionException If pre-destruction fails.
     */
    void preDestruction() throws SecurityProviderDestructionException;
}
