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
package org.apache.nifi.registry.web.security.authentication;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.security.authentication.LoginIdentityProvider;
import org.apache.nifi.registry.security.authentication.LoginIdentityProviderConfigurationContext;
import org.apache.nifi.registry.security.authentication.LoginIdentityProviderLookup;
import org.apache.nifi.registry.security.authentication.annotation.LoginIdentityProviderContext;
import org.apache.nifi.registry.extension.ExtensionManager;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.security.authentication.generated.IdentityProviders;
import org.apache.nifi.registry.security.authentication.generated.Property;
import org.apache.nifi.registry.security.authentication.generated.Provider;
import org.apache.nifi.registry.security.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LoginIdentityProviderFactory implements LoginIdentityProviderLookup, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(LoginIdentityProviderFactory.class);
    private static final String LOGIN_IDENTITY_PROVIDERS_XSD = "/identity-providers.xsd";
    private static final String JAXB_GENERATED_PATH = "org.apache.nifi.registry.security.authentication.generated";
    private static final JAXBContext JAXB_CONTEXT = initializeJaxbContext();

    private static JAXBContext initializeJaxbContext() {
        try {
            return JAXBContext.newInstance(JAXB_GENERATED_PATH, LoginIdentityProviderFactory.class.getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.");
        }
    }

    private NiFiRegistryProperties properties;
    private ExtensionManager extensionManager;
    private LoginIdentityProvider loginIdentityProvider;
    private final Map<String, LoginIdentityProvider> loginIdentityProviders = new HashMap<>();

    @Autowired
    public LoginIdentityProviderFactory(final NiFiRegistryProperties properties, final ExtensionManager extensionManager) {
        this.properties = properties;
        this.extensionManager = extensionManager;

        if (this.properties == null) {
            throw new IllegalStateException("NiFiRegistryProperties cannot be null");
        }

        if (this.extensionManager == null) {
            throw new IllegalStateException("ExtensionManager cannot be null");
        }
    }

    @Override
    public LoginIdentityProvider getLoginIdentityProvider(String identifier) {
        return loginIdentityProviders.get(identifier);
    }

    @Bean
    public LoginIdentityProvider getLoginIdentityProvider() throws Exception {
        if (loginIdentityProvider == null) {
            // look up the login identity provider to use
            final String loginIdentityProviderIdentifier = properties.getProperty(NiFiRegistryProperties.SECURITY_IDENTITY_PROVIDER);

            // ensure the login identity provider class name was specified
            if (StringUtils.isNotBlank(loginIdentityProviderIdentifier)) {
                final IdentityProviders loginIdentityProviderConfiguration = loadLoginIdentityProvidersConfiguration();

                // create each login identity provider
                for (final Provider provider : loginIdentityProviderConfiguration.getProvider()) {
                    loginIdentityProviders.put(provider.getIdentifier(), createLoginIdentityProvider(provider.getIdentifier(), provider.getClazz()));
                }

                // configure each login identity provider
                for (final Provider provider : loginIdentityProviderConfiguration.getProvider()) {
                    final LoginIdentityProvider instance = loginIdentityProviders.get(provider.getIdentifier());
                    instance.onConfigured(loadLoginIdentityProviderConfiguration(provider));
                }

                // get the login identity provider instance
                loginIdentityProvider = getLoginIdentityProvider(loginIdentityProviderIdentifier);

                // ensure it was found
                if (loginIdentityProvider == null) {
                    throw new Exception(String.format("The specified login identity provider '%s' could not be found.", loginIdentityProviderIdentifier));
                }
            }
        }

        return loginIdentityProvider;
    }

    @Override
    public void destroy() throws Exception {
        if (loginIdentityProviders != null) {
            loginIdentityProviders.entrySet().stream().forEach(e -> e.getValue().preDestruction());
        }
    }

    private IdentityProviders loadLoginIdentityProvidersConfiguration() throws Exception {
        final File loginIdentityProvidersConfigurationFile = properties.getIdentityProviderConfigurationFile();

        // load the users from the specified file
        if (loginIdentityProvidersConfigurationFile.exists()) {
            try {
                // find the schema
                final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                final Schema schema = schemaFactory.newSchema(IdentityProviders.class.getResource(LOGIN_IDENTITY_PROVIDERS_XSD));

                // attempt to unmarshal
                XMLStreamReader xsr = XmlUtils.createSafeReader(new StreamSource(loginIdentityProvidersConfigurationFile));
                final Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                unmarshaller.setSchema(schema);
                final JAXBElement<IdentityProviders> element = unmarshaller.unmarshal(xsr, IdentityProviders.class);
                return element.getValue();
            } catch (SAXException | JAXBException e) {
                throw new Exception("Unable to load the login identity provider configuration file at: " + loginIdentityProvidersConfigurationFile.getAbsolutePath());
            }
        } else {
            throw new Exception("Unable to find the login identity provider configuration file at " + loginIdentityProvidersConfigurationFile.getAbsolutePath());
        }
    }

    private LoginIdentityProvider createLoginIdentityProvider(final String identifier, final String loginIdentityProviderClassName) throws Exception {
        final LoginIdentityProvider instance;

        final ClassLoader classLoader = extensionManager.getExtensionClassLoader(loginIdentityProviderClassName);
        if (classLoader == null) {
            throw new IllegalStateException("Extension not found in any of the configured class loaders: " + loginIdentityProviderClassName);
        }

        // attempt to load the class
        Class<?> rawLoginIdentityProviderClass = Class.forName(loginIdentityProviderClassName, true, classLoader);
        Class<? extends LoginIdentityProvider> loginIdentityProviderClass = rawLoginIdentityProviderClass.asSubclass(LoginIdentityProvider.class);

        // otherwise create a new instance
        Constructor constructor = loginIdentityProviderClass.getConstructor();
        instance = (LoginIdentityProvider) constructor.newInstance();

        // method injection
        performMethodInjection(instance, loginIdentityProviderClass);

        // field injection
        performFieldInjection(instance, loginIdentityProviderClass);

        // call post construction lifecycle event
        instance.initialize(new StandardLoginIdentityProviderInitializationContext(identifier, this));

        return instance;
    }

    private LoginIdentityProviderConfigurationContext loadLoginIdentityProviderConfiguration(final Provider provider) {
        final Map<String, String> providerProperties = new HashMap<>();

        for (final Property property : provider.getProperty()) {
            providerProperties.put(property.getName(), property.getValue());
        }

        return new StandardLoginIdentityProviderConfigurationContext(provider.getIdentifier(), providerProperties);
    }

    private void performMethodInjection(final LoginIdentityProvider instance, final Class loginIdentityProviderClass)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        for (final Method method : loginIdentityProviderClass.getMethods()) {
            if (method.isAnnotationPresent(LoginIdentityProviderContext.class)) {
                // make the method accessible
                final boolean isAccessible = method.isAccessible();
                method.setAccessible(true);

                try {
                    final Class<?>[] argumentTypes = method.getParameterTypes();

                    // look for setters (single argument)
                    if (argumentTypes.length == 1) {
                        final Class<?> argumentType = argumentTypes[0];

                        // look for well known types
                        if (NiFiRegistryProperties.class.isAssignableFrom(argumentType)) {
                            // nifi properties injection
                            method.invoke(instance, properties);
                        }
                    }
                } finally {
                    method.setAccessible(isAccessible);
                }
            }
        }

        final Class parentClass = loginIdentityProviderClass.getSuperclass();
        if (parentClass != null && LoginIdentityProvider.class.isAssignableFrom(parentClass)) {
            performMethodInjection(instance, parentClass);
        }
    }

    private void performFieldInjection(final LoginIdentityProvider instance, final Class loginIdentityProviderClass) throws IllegalArgumentException, IllegalAccessException {
        for (final Field field : loginIdentityProviderClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(LoginIdentityProviderContext.class)) {
                // make the method accessible
                final boolean isAccessible = field.isAccessible();
                field.setAccessible(true);

                try {
                    // get the type
                    final Class<?> fieldType = field.getType();

                    // only consider this field if it isn't set yet
                    if (field.get(instance) == null) {
                        // look for well known types
                        if (NiFiRegistryProperties.class.isAssignableFrom(fieldType)) {
                            // nifi properties injection
                            field.set(instance, properties);
                        }
                    }

                } finally {
                    field.setAccessible(isAccessible);
                }
            }
        }

        final Class parentClass = loginIdentityProviderClass.getSuperclass();
        if (parentClass != null && LoginIdentityProvider.class.isAssignableFrom(parentClass)) {
            performFieldInjection(instance, parentClass);
        }
    }

}
