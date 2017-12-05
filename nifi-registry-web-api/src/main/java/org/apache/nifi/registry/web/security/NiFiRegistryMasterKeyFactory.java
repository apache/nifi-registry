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
package org.apache.nifi.registry.web.security;

import org.apache.nifi.registry.NiFiRegistryApiApplication;
import org.apache.nifi.registry.security.crypto.BootstrapFileCryptoKeyProvider;
import org.apache.nifi.registry.security.crypto.CryptoKeyProvider;
import org.apache.nifi.registry.security.crypto.MissingCryptoKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

@Configuration
public class NiFiRegistryMasterKeyFactory implements ServletContextAttributeListener, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistryMasterKeyFactory.class);

    private CryptoKeyProvider masterKeyProvider = MISSING_KEY_PROVIDER;

    @Bean
    public CryptoKeyProvider getNiFiRegistryMasterKeyProvider() {
        return masterKeyProvider;
    }

    // -- ServletContextAttributeListener methods

    @Override
    public void attributeAdded(ServletContextAttributeEvent servletContextAttributeEvent) {
        updateMasterKeyProvider(servletContextAttributeEvent);
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent servletContextAttributeEvent) {
        updateMasterKeyProvider(servletContextAttributeEvent);
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent servletContextAttributeEvent) {
        String attributeName = servletContextAttributeEvent.getName();
        if (attributeName != null && attributeName.equals(NiFiRegistryApiApplication.NIFI_REGISTRY_MASTER_KEY_ATTRIBUTE)) {
            clearMasterKeyProvider();
        }
    }

    // -- ApplicationListener<ContextRefreshedEvent> methods

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (masterKeyProvider != null) {
            if (!(masterKeyProvider instanceof BootstrapFileCryptoKeyProvider)) {
                // If the Key Holder is not backed by the bootstrap.conf file,
                // if might be holding the key material in memory, and in scope.
                // If we receive this event, the ApplicationContext has finished initializing,
                // so all sensitive props should be loaded and the master key is no longer needed.
                // De-reference the holder so that GC can purge the key material from memory.
                logger.info("Received {} indicating the ApplicationContext is done initializing. Now clearing the CryptoKeyProvider " +
                        "Bean holding the master key from ApplicationContext, as it should no longer be needed.", contextRefreshedEvent);
                clearMasterKeyProvider();
            } else {
                logger.debug("Received {} indicating the ApplicationContext is done initializing. CryptoKeyProvider instance is backed " +
                        "by bootstrap.conf file and will remain available in the ApplicationContext to load master key on demand if needed.");
            }
        }

    }

    private void updateMasterKeyProvider(ServletContextAttributeEvent servletContextAttributeEvent) {
        String attributeName = servletContextAttributeEvent.getName();
        if (attributeName != null && attributeName.equals(NiFiRegistryApiApplication.NIFI_REGISTRY_MASTER_KEY_ATTRIBUTE)) {
            Object attributeValue = servletContextAttributeEvent.getValue();
            if (attributeValue == null || !(attributeValue instanceof CryptoKeyProvider)) {
                clearMasterKeyProvider();
            } else {
                logger.debug("Received {}, updating CryptoKeyProvider Bean containing the master key.", servletContextAttributeEvent);
                masterKeyProvider = (CryptoKeyProvider)attributeValue;
            }
        }
    }

    private void clearMasterKeyProvider() {
        logger.debug("Clearing CryptoKeyProvider Bean containing the master key. Master key will no longer be accessible in ApplicationContext.");
        masterKeyProvider = MISSING_KEY_PROVIDER;
        // actual master key holder is now out of scope and can be reaped from memory at next GC run
    }

    private static final CryptoKeyProvider MISSING_KEY_PROVIDER = new CryptoKeyProvider() {
        @Override
        public String getKey() throws MissingCryptoKeyException {
            throw new MissingCryptoKeyException("The actual CryptoKeyProvider used for ApplicationContext " +
                    "loading has been intentionally destroyed and is no longer accessible. Something is " +
                    "trying to access the master key after ApplicationContext has finished loading.");
        }
    };

}
