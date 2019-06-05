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
import org.apache.nifi.registry.security.authorization.exception.UninheritableAuthorizationsException;
import org.apache.nifi.registry.service.RegistryService;

/**
 * Similar to FrameworkAuthorizer, but specifically for wrapping a ManagedAuthorizer.
 */
public class FrameworkManagedAuthorizer extends FrameworkAuthorizer implements ManagedAuthorizer {

    private final ManagedAuthorizer wrappedManagedAuthorizer;

    public FrameworkManagedAuthorizer(final ManagedAuthorizer wrappedManagedAuthorizer, final RegistryService registryService) {
        super(wrappedManagedAuthorizer, registryService);
        this.wrappedManagedAuthorizer = wrappedManagedAuthorizer;
    }

    @Override
    public String getFingerprint() throws AuthorizationAccessException {
        return wrappedManagedAuthorizer.getFingerprint();
    }

    @Override
    public void inheritFingerprint(final String fingerprint) throws AuthorizationAccessException {
        wrappedManagedAuthorizer.inheritFingerprint(fingerprint);
    }

    @Override
    public void checkInheritability(final String proposedFingerprint) throws AuthorizationAccessException, UninheritableAuthorizationsException {
        wrappedManagedAuthorizer.checkInheritability(proposedFingerprint);
    }

    @Override
    public AccessPolicyProvider getAccessPolicyProvider() {
        return wrappedManagedAuthorizer.getAccessPolicyProvider();
    }
}
