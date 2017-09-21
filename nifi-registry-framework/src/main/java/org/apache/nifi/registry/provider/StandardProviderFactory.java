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
import org.apache.nifi.registry.provider.generated.Property;
import org.apache.nifi.registry.provider.generated.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standard implementation of ProviderFactory.
 */
@Configuration
public class StandardProviderFactory implements ProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardProviderFactory.class);

    private static final String PROVIDERS_XSD = "/providers.xsd";
    private static final String JAXB_GENERATED_PATH = "org.apache.nifi.registry.provider.generated";
    private static final JAXBContext JAXB_CONTEXT = initializeJaxbContext();

    /**
     * Load the JAXBContext.
     */
    private static JAXBContext initializeJaxbContext() {
        try {
            return JAXBContext.newInstance(JAXB_GENERATED_PATH, StandardProviderFactory.class.getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.", e);
        }
    }

    private final NiFiRegistryProperties properties;
    private final AtomicReference<Providers> providersHolder = new AtomicReference<>(null);

    private FlowPersistenceProvider flowPersistenceProvider;

    @Autowired
    public StandardProviderFactory(final NiFiRegistryProperties properties) {
        this.properties = properties;

        if (this.properties == null) {
            throw new IllegalStateException("NiFiRegistryProperties cannot be null");
        }
    }

    @PostConstruct
    @Override
    public synchronized void initialize() throws ProviderFactoryException {
        if (providersHolder.get() == null) {
            final File providersConfigFile = properties.getProvidersConfigurationFile();
            if (providersConfigFile.exists()) {
                try {
                    // find the schema
                    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    final Schema schema = schemaFactory.newSchema(StandardProviderFactory.class.getResource(PROVIDERS_XSD));

                    // attempt to unmarshal
                    final Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                    unmarshaller.setSchema(schema);

                    // set the holder for later use
                    final JAXBElement<Providers> element = unmarshaller.unmarshal(new StreamSource(providersConfigFile), Providers.class);
                    providersHolder.set(element.getValue());
                } catch (SAXException | JAXBException e) {
                    throw new ProviderFactoryException("Unable to load the providers configuration file at: " + providersConfigFile.getAbsolutePath(), e);
                }
            } else {
                throw new ProviderFactoryException("Unable to find the providers configuration file at " + providersConfigFile.getAbsolutePath());
            }
        }
    }

    @Bean
    @Override
    public synchronized FlowPersistenceProvider getFlowPersistenceProvider() {
        if (flowPersistenceProvider == null) {
            if (providersHolder.get() == null) {
                throw new ProviderFactoryException("ProviderFactory must be initialized before obtaining a Provider");
            }

            final Providers providers = providersHolder.get();
            final org.apache.nifi.registry.provider.generated.Provider jaxbFlowProvider = providers.getFlowPersistenceProvider();
            final String flowProviderClassName = jaxbFlowProvider.getClazz();

            try {
                final Class<?> rawFlowProviderClass = Class.forName(flowProviderClassName, true, StandardProviderFactory.class.getClassLoader());
                final Class<? extends FlowPersistenceProvider> flowProviderClass = rawFlowProviderClass.asSubclass(FlowPersistenceProvider.class);

                final Constructor constructor = flowProviderClass.getConstructor();
                flowPersistenceProvider = (FlowPersistenceProvider) constructor.newInstance();

                LOGGER.info("Instantiated FlowPersistenceProvider with class name {}", new Object[] {flowProviderClassName});
            } catch (Exception e) {
                throw new ProviderFactoryException("Error creating FlowPersistenceProvider with class name: " + flowProviderClassName, e);
            }

            final ProviderConfigurationContext configurationContext = createConfigurationContext(jaxbFlowProvider.getProperty());
            flowPersistenceProvider.onConfigured(configurationContext);
            LOGGER.info("Configured FlowPersistenceProvider with class name {}", new Object[] {flowProviderClassName});
        }

        return flowPersistenceProvider;
    }

    private ProviderConfigurationContext createConfigurationContext(final List<Property> configProperties) {
        final Map<String,String> properties = new HashMap<>();

        if (configProperties != null) {
            configProperties.stream().forEach(p -> properties.put(p.getName(), p.getValue()));
        }

        return new StandardProviderConfigurationContext(properties);
    }

}
