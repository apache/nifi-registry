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
package org.apache.nifi.registry.security.authentication;

import org.apache.nifi.registry.security.authentication.exception.IdentityAccessException;
import org.apache.nifi.registry.security.authentication.exception.InvalidLoginCredentialsException;
import org.apache.nifi.registry.security.authentication.exception.ProviderCreationException;
import org.apache.nifi.registry.security.authentication.exception.ProviderDestructionException;

/**
 * Identity provider that is able to authentication a user with username/password credentials.
 */
public interface LoginIdentityProvider {

    /**
     * Authenticates the specified login credentials.
     *
     * @param credentials the credentials
     * @return The authentication response
     * @throws InvalidLoginCredentialsException The login credentials were invalid
     * @throws IdentityAccessException Unable to register the user due to an issue accessing the underlying storage
     */
    AuthenticationResponse authenticate(LoginCredentials credentials) throws InvalidLoginCredentialsException, IdentityAccessException;

    /**
     * Called immediately after instance creation for implementers to perform additional setup
     *
     * @param initializationContext in which to initialize
     * @throws ProviderCreationException Unable to initialize
     */
    void initialize(LoginIdentityProviderInitializationContext initializationContext) throws ProviderCreationException;

    /**
     * Called to configure the AuthorityProvider.
     *
     * @param configurationContext at the time of configuration
     * @throws ProviderCreationException for any issues configuring the provider
     */
    void onConfigured(LoginIdentityProviderConfigurationContext configurationContext) throws ProviderCreationException;

    /**
     * Called immediately before instance destruction for implementers to release resources.
     *
     * @throws ProviderDestructionException If pre-destruction fails.
     */
    void preDestruction() throws ProviderDestructionException;
}
