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
package org.apache.nifi.registry.provider;

import org.apache.nifi.registry.extension.ExtensionBundleContext;
import org.apache.nifi.registry.extension.ExtensionBundlePersistenceException;
import org.apache.nifi.registry.extension.ExtensionBundlePersistenceProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class MockExtensionBundlePersistenceProvider implements ExtensionBundlePersistenceProvider {

    private Map<String,String> properties;

    @Override
    public void saveBundleVersion(ExtensionBundleContext context, InputStream contentStream)
            throws ExtensionBundlePersistenceException {

    }

    @Override
    public void getBundleVersion(ExtensionBundleContext context, OutputStream outputStream) throws ExtensionBundlePersistenceException {

    }

    @Override
    public void deleteBundleVersion(ExtensionBundleContext context) throws ExtensionBundlePersistenceException {

    }

    @Override
    public void deleteAllBundleVersions(String bucketId, String bucketName, String groupId, String artifactId) throws ExtensionBundlePersistenceException {

    }

    @Override
    public void onConfigured(ProviderConfigurationContext configurationContext)
            throws ProviderCreationException {
        properties = configurationContext.getProperties();
    }

    public Map<String,String> getProperties() {
        return properties;
    }

}
