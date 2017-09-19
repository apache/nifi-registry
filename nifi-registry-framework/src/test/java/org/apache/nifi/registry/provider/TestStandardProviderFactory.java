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

import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestStandardProviderFactory {

    @Test
    public void testGetProvidersSuccess() {
        final NiFiRegistryProperties props = new NiFiRegistryProperties();
        props.setProperty(NiFiRegistryProperties.PROVIDERS_CONFIGURATION_FILE, "src/test/resources/provider/providers-good.xml");

        final ProviderFactory providerFactory = new StandardProviderFactory(props);
        providerFactory.initialize();

        final FlowPersistenceProvider flowPersistenceProvider = providerFactory.getFlowPersistenceProvider();
        assertNotNull(flowPersistenceProvider);

        final MockFlowPersistenceProvider mockFlowProvider = (MockFlowPersistenceProvider) flowPersistenceProvider;
        assertNotNull(mockFlowProvider.getProperties());
        assertEquals("flow foo", mockFlowProvider.getProperties().get("Flow Property 1"));
        assertEquals("flow bar", mockFlowProvider.getProperties().get("Flow Property 2"));
    }

    @Test(expected = ProviderFactoryException.class)
    public void testGetFlowProviderBeforeInitializingShouldThrowException() {
        final NiFiRegistryProperties props = new NiFiRegistryProperties();
        props.setProperty(NiFiRegistryProperties.PROVIDERS_CONFIGURATION_FILE, "src/test/resources/provider/providers-good.xml");

        final ProviderFactory providerFactory = new StandardProviderFactory(props);
        providerFactory.getFlowPersistenceProvider();
    }

    @Test(expected = ProviderFactoryException.class)
    public void testProvidersConfigDoesNotExist() {
        final NiFiRegistryProperties props = new NiFiRegistryProperties();
        props.setProperty(NiFiRegistryProperties.PROVIDERS_CONFIGURATION_FILE, "src/test/resources/provider/providers-does-not-exist.xml");

        final ProviderFactory providerFactory = new StandardProviderFactory(props);
        providerFactory.initialize();
    }

    @Test(expected = ProviderFactoryException.class)
    public void testFlowProviderClassNotFound() {
        final NiFiRegistryProperties props = new NiFiRegistryProperties();
        props.setProperty(NiFiRegistryProperties.PROVIDERS_CONFIGURATION_FILE, "src/test/resources/provider/providers-class-not-found.xml");

        final ProviderFactory providerFactory = new StandardProviderFactory(props);
        providerFactory.initialize();

        providerFactory.getFlowPersistenceProvider();
    }

}
