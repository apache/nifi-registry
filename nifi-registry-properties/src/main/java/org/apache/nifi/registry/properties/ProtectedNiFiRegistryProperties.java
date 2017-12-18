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
package org.apache.nifi.registry.properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Wrapper class of {@link NiFiRegistryProperties} for intermediate phase when
 * {@link NiFiRegistryPropertiesLoader} loads the raw properties file and performs
 * unprotection activities before returning an instance of {@link NiFiRegistryProperties}.
 */
class ProtectedNiFiRegistryProperties {
    private static final Logger logger = LoggerFactory.getLogger(ProtectedNiFiRegistryProperties.class);

    private NiFiRegistryProperties properties;

    private Map<String, SensitivePropertyProvider> localProviderCache = new HashMap<>();

    // Additional "sensitive" property key
    public static final String ADDITIONAL_SENSITIVE_PROPERTIES_KEY = "nifi.registry.sensitive.props.additional.keys";

    // Default list of "sensitive" property keys
    public static final List<String> DEFAULT_SENSITIVE_PROPERTIES = new ArrayList<>(asList(
            NiFiRegistryProperties.SECURITY_KEY_PASSWD,
            NiFiRegistryProperties.SECURITY_KEYSTORE_PASSWD,
            NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD));

    public ProtectedNiFiRegistryProperties() {
        this(null);
    }

    /**
     * Creates an instance containing the provided {@link NiFiRegistryProperties}.
     *
     * @param props the NiFiProperties to contain
     */
    public ProtectedNiFiRegistryProperties(NiFiRegistryProperties props) {
        if (props == null) {
            props = new NiFiRegistryProperties();
        }
        this.properties = props;
        logger.debug("Loaded {} properties (including {} protection schemes) into ProtectedNiFiProperties",
                getPropertyKeysIncludingProtectionSchemes().size(), getProtectedPropertyKeys().size());
    }

    /**
     * Retrieves the property value for the given property key.
     *
     * @param key the key of property value to lookup
     * @return value of property at given key or null if not found
     */
    // @Override
    public String getProperty(String key) {
        return getInternalNiFiProperties().getProperty(key);
    }

    /**
     * Returns the internal representation of the {@link NiFiRegistryProperties} -- protected
     * or not as determined by the current state. No guarantee is made to the
     * protection state of these properties. If the internal reference is null, a new
     * {@link NiFiRegistryProperties} instance is created.
     *
     * @return the internal properties
     */
    NiFiRegistryProperties getInternalNiFiProperties() {
        if (this.properties == null) {
            this.properties = new NiFiRegistryProperties();
        }

        return this.properties;
    }

    /**
     * Returns the number of properties in the NiFiRegistryProperties,
     * excluding protection scheme properties.
     *
     * <p>
     * Example:
     * <p>
     * key: E(value, key)
     * key.protected: aes/gcm/256
     * key2: value2
     * <p>
     * would return size 2
     *
     * @return the count of real properties
     */
    int size() {
        return getPropertyKeysExcludingProtectionSchemes().size();
    }

    /**
     * Returns the complete set of property keys in the NiFiRegistryProperties,
     * including any protection keys (i.e. 'x.y.z.protected').
     *
     * @return the set of property keys
     */
    Set<String> getPropertyKeysIncludingProtectionSchemes() {
        return getInternalNiFiProperties().getPropertyKeys();
    }

    /**
     * Returns the set of property keys in the NiFiRegistryProperties,
     * excluding any protection keys (i.e. 'x.y.z.protected').
     *
     * @return the set of property keys
     */
    Set<String> getPropertyKeysExcludingProtectionSchemes() {
        Set<String> filteredKeys = getPropertyKeysIncludingProtectionSchemes();
        filteredKeys.removeIf(p -> p.endsWith(".protected"));
        return filteredKeys;
    }

    /**
     * Splits a single string containing multiple property keys into a List.
     *
     * Delimited by ',' or ';' and ignores leading and trailing whitespace around delimiter.
     *
     * @param multipleProperties a single String containing multiple properties, i.e.
     *                           "nifi.registry.property.1; nifi.registry.property.2, nifi.registry.property.3"
     * @return a List containing the split and trimmed properties
     */
    private static List<String> splitMultipleProperties(String multipleProperties) {
        if (multipleProperties == null || multipleProperties.trim().isEmpty()) {
            return new ArrayList<>(0);
        } else {
            List<String> properties = new ArrayList<>(asList(multipleProperties.split("\\s*[,;]\\s*")));
            for (int i = 0; i < properties.size(); i++) {
                properties.set(i, properties.get(i).trim());
            }
            return properties;
        }
    }

    /**
     * Returns a list of the keys identifying "sensitive" properties.
     *
     * There is a default list, and additional keys can be provided in the
     * {@code nifi.registry.sensitive.props.additional.keys} property in {@code nifi-registry.properties}.
     *
     * @return the list of sensitive property keys
     */
    public List<String> getSensitivePropertyKeys() {
        String additionalPropertiesString = getProperty(ADDITIONAL_SENSITIVE_PROPERTIES_KEY);
        if (additionalPropertiesString == null || additionalPropertiesString.trim().isEmpty()) {
            return DEFAULT_SENSITIVE_PROPERTIES;
        } else {
            List<String> additionalProperties = splitMultipleProperties(additionalPropertiesString);
            /* Remove this key if it was accidentally provided as a sensitive key
             * because we cannot protect it and read from it
            */
            if (additionalProperties.contains(ADDITIONAL_SENSITIVE_PROPERTIES_KEY)) {
                logger.warn("The key '{}' contains itself. This is poor practice and should be removed", ADDITIONAL_SENSITIVE_PROPERTIES_KEY);
                additionalProperties.remove(ADDITIONAL_SENSITIVE_PROPERTIES_KEY);
            }
            additionalProperties.addAll(DEFAULT_SENSITIVE_PROPERTIES);
            return additionalProperties;
        }
    }

    /**
     * Returns a list of the keys identifying "sensitive" properties. There is a default list,
     * and additional keys can be provided in the {@code nifi.sensitive.props.additional.keys} property in {@code nifi.properties}.
     *
     * @return the list of sensitive property keys
     */
    public List<String> getPopulatedSensitivePropertyKeys() {
        List<String> allSensitiveKeys = getSensitivePropertyKeys();
        return allSensitiveKeys.stream().filter(k -> StringUtils.isNotBlank(getProperty(k))).collect(Collectors.toList());
    }

    /**
     * Returns true if any sensitive keys are protected.
     *
     * @return true if any key is protected; false otherwise
     */
    public boolean hasProtectedKeys() {
        List<String> sensitiveKeys = getSensitivePropertyKeys();
        for (String k : sensitiveKeys) {
            if (isPropertyProtected(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a Map of the keys identifying "sensitive" properties that are currently protected and the "protection" key for each.
     *
     * This may or may not include all properties marked as sensitive.
     *
     * @return the Map of protected property keys and the protection identifier for each
     */
    public Map<String, String> getProtectedPropertyKeys() {
        List<String> sensitiveKeys = getSensitivePropertyKeys();

        Map<String, String> traditionalProtectedProperties = new HashMap<>();
        for (String key : sensitiveKeys) {
            String protection = getProperty(getProtectionKey(key));
            if (StringUtils.isNotBlank(protection) && StringUtils.isNotBlank(getProperty(key))) {
                traditionalProtectedProperties.put(key, protection);
            }
        }

        return traditionalProtectedProperties;
    }

    /**
     * Returns the unique set of all protection schemes currently in use for this instance.
     *
     * @return the set of protection schemes
     */
    public Set<String> getProtectionSchemes() {
        return new HashSet<>(getProtectedPropertyKeys().values());
    }

    /**
     * Returns a percentage of the total number of populated properties marked as sensitive that are currently protected.
     *
     * @return the percent of sensitive properties marked as protected
     */
    public int getPercentOfSensitivePropertiesProtected() {
        return (int) Math.round(getProtectedPropertyKeys().size() / ((double) getPopulatedSensitivePropertyKeys().size()) * 100);
    }

    /**
     * Returns true if the property identified by this key is considered sensitive in this instance of {@code NiFiProperties}.
     * Some properties are sensitive by default, while others can be specified by
     * {@link ProtectedNiFiRegistryProperties#ADDITIONAL_SENSITIVE_PROPERTIES_KEY}.
     *
     * @param key the key
     * @return true if it is sensitive
     * @see ProtectedNiFiRegistryProperties#getSensitivePropertyKeys()
     */
    public boolean isPropertySensitive(String key) {
        // If the explicit check for ADDITIONAL_SENSITIVE_PROPERTIES_KEY is not here, this could loop infinitely
        return key != null && !key.equals(ADDITIONAL_SENSITIVE_PROPERTIES_KEY) && getSensitivePropertyKeys().contains(key.trim());
    }

    /**
     * Returns true if the property identified by this key is considered protected in this instance of {@code NiFiProperties}.
     * The property value is protected if the key is sensitive and the sibling key of key.protected is present.
     *
     * @param key the key
     * @return true if it is currently marked as protected
     * @see ProtectedNiFiRegistryProperties#getSensitivePropertyKeys()
     */
    public boolean isPropertyProtected(String key) {
        return key != null && isPropertySensitive(key) && !StringUtils.isBlank(getProperty(getProtectionKey(key)));
    }

    /**
     * Returns the sibling property key which specifies the protection scheme for this key.
     * <p>
     * Example:
     * <p>
     * nifi.registry.sensitive.key=ABCXYZ
     * nifi.registry.sensitive.key.protected=aes/gcm/256
     * <p>
     * nifi.registry.sensitive.key -> nifi.sensitive.key.protected
     *
     * @param key the key identifying the sensitive property
     * @return the key identifying the protection scheme for the sensitive property
     */
    public static String getProtectionKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Cannot find protection key for null key");
        }

        return key + ".protected";
    }

    /**
     * Returns the unprotected {@link NiFiRegistryProperties} instance. If none of the
     * properties loaded are marked as protected, it will simply pass through the
     * internal instance. If any are protected, it will drop the protection scheme keys
     * and translate each protected value (encrypted, HSM-retrieved, etc.) into the raw
     * value and store it under the original key.
     * <p>
     * If any property fails to unprotect, it will save that key and continue. After
     * attempting all properties, it will throw an exception containing all failed
     * properties. This is necessary because the order is not enforced, so all failed
     * properties should be gathered together.
     *
     * @return the NiFiRegistryProperties instance with all raw values
     * @throws SensitivePropertyProtectionException if there is a problem unprotecting one or more keys
     */
    public NiFiRegistryProperties getUnprotectedProperties() throws SensitivePropertyProtectionException {
        if (hasProtectedKeys()) {
            logger.debug("There are {} protected properties of {} sensitive properties ({}%)",
                    getProtectedPropertyKeys().size(),
                    getPopulatedSensitivePropertyKeys().size(),
                    getPercentOfSensitivePropertiesProtected());

            NiFiRegistryProperties unprotectedProperties = new NiFiRegistryProperties();

            Set<String> failedKeys = new HashSet<>();

            for (String key : getPropertyKeysExcludingProtectionSchemes()) {
                /* Three kinds of keys
                 * 1. protection schemes -- skip
                 * 2. protected keys -- unprotect and copy
                 * 3. normal keys -- copy over
                 */
                if (key.endsWith(".protected")) {
                    // Do nothing
                } else if (isPropertyProtected(key)) {
                    try {
                        unprotectedProperties.setProperty(key, unprotectValue(key, getProperty(key)));
                    } catch (SensitivePropertyProtectionException e) {
                        logger.warn("Failed to unprotect '{}'", key, e);
                        failedKeys.add(key);
                    }
                } else {
                    unprotectedProperties.setProperty(key, getProperty(key));
                }
            }

            if (!failedKeys.isEmpty()) {
                if (failedKeys.size() > 1) {
                    logger.warn("Combining {} failed keys [{}] into single exception", failedKeys.size(), StringUtils.join(failedKeys, ", "));
                    throw new MultipleSensitivePropertyProtectionException("Failed to unprotect keys", failedKeys);
                } else {
                    throw new SensitivePropertyProtectionException("Failed to unprotect key " + failedKeys.iterator().next());
                }
            }

            return unprotectedProperties;
        } else {
            logger.debug("No protected properties");
            return getInternalNiFiProperties();
        }
    }

    /**
     * Registers a new {@link SensitivePropertyProvider}. This method will throw a {@link UnsupportedOperationException} if a provider is already registered for the protection scheme.
     *
     * @param sensitivePropertyProvider the provider
     */
    void addSensitivePropertyProvider(SensitivePropertyProvider sensitivePropertyProvider) {
        if (sensitivePropertyProvider == null) {
            throw new IllegalArgumentException("Cannot add null SensitivePropertyProvider");
        }

        if (getSensitivePropertyProviders().containsKey(sensitivePropertyProvider.getIdentifierKey())) {
            throw new UnsupportedOperationException("Cannot overwrite existing sensitive property provider registered for " + sensitivePropertyProvider.getIdentifierKey());
        }

        getSensitivePropertyProviders().put(sensitivePropertyProvider.getIdentifierKey(), sensitivePropertyProvider);
    }

    private String getDefaultProtectionScheme() {
        if (!getSensitivePropertyProviders().isEmpty()) {
            List<String> schemes = new ArrayList<>(getSensitivePropertyProviders().keySet());
            Collections.sort(schemes);
            return schemes.get(0);
        } else {
            throw new IllegalStateException("No registered protection schemes");
        }
    }

    /**
     * Returns a new instance of {@link NiFiRegistryProperties} with all populated sensitive values protected by the default protection scheme.
     *
     * Plain non-sensitive values are copied directly.
     *
     * @return the protected properties in a {@link NiFiRegistryProperties} object
     * @throws IllegalStateException if no protection schemes are registered
     */
    NiFiRegistryProperties protectPlainProperties() {
        try {
            return protectPlainProperties(getDefaultProtectionScheme());
        } catch (IllegalStateException e) {
            final String msg = "Cannot protect properties with default scheme if no protection schemes are registered";
            logger.warn(msg);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Returns a new instance of {@link NiFiRegistryProperties} with all populated sensitive values protected by the provided protection scheme.
     *
     * Plain non-sensitive values are copied directly.
     *
     * @param protectionScheme the identifier key of the {@link SensitivePropertyProvider} to use
     * @return the protected properties in a {@link NiFiRegistryProperties} object
     */
    NiFiRegistryProperties protectPlainProperties(String protectionScheme) {
        SensitivePropertyProvider spp = getSensitivePropertyProvider(protectionScheme);

        NiFiRegistryProperties protectedProperties = new NiFiRegistryProperties();

        // Copy over the plain keys
        Set<String> plainKeys = getPropertyKeysExcludingProtectionSchemes();
        plainKeys.removeAll(getSensitivePropertyKeys());
        for (String key : plainKeys) {
            protectedProperties.setProperty(key, getInternalNiFiProperties().getProperty(key));
        }

        // Add the protected keys and the protection schemes
        for (String key : getSensitivePropertyKeys()) {
            final String plainValue = getProperty(key);
            if (plainValue != null && !plainValue.trim().isEmpty()) {
                final String protectedValue = spp.protect(plainValue);
                protectedProperties.setProperty(key, protectedValue);
                protectedProperties.setProperty(getProtectionKey(key), protectionScheme);
            }
        }

        return protectedProperties;
    }

    /**
     * Returns the number of properties that are marked as protected in the provided {@link NiFiRegistryProperties} instance
     * without requiring external creation of a {@link ProtectedNiFiRegistryProperties} instance.
     *
     * @param plainProperties the instance to count protected properties
     * @return the number of protected properties
     */
    public static int countProtectedProperties(NiFiRegistryProperties plainProperties) {
        return new ProtectedNiFiRegistryProperties(plainProperties).getProtectedPropertyKeys().size();
    }

    /**
     * Returns the number of properties that are marked as sensitive in the provided {@link NiFiRegistryProperties} instance
     * without requiring external creation of a {@link ProtectedNiFiRegistryProperties} instance.
     *
     * @param plainProperties the instance to count sensitive properties
     * @return the number of sensitive properties
     */
    public static int countSensitiveProperties(NiFiRegistryProperties plainProperties) {
        return new ProtectedNiFiRegistryProperties(plainProperties).getSensitivePropertyKeys().size();
    }

    @Override
    public String toString() {
        final Set<String> providers = getSensitivePropertyProviders().keySet();
        return new StringBuilder("ProtectedNiFiProperties instance with ")
                .append(getPropertyKeysIncludingProtectionSchemes().size())
                .append(" properties (")
                .append(getProtectedPropertyKeys().size())
                .append(" protected) and ")
                .append(providers.size())
                .append(" sensitive property providers: ")
                .append(StringUtils.join(providers, ", "))
                .toString();
    }

    /**
     * Returns the local provider cache (null-safe) as a Map of protection schemes -> implementations.
     *
     * @return the map
     */
    private Map<String, SensitivePropertyProvider> getSensitivePropertyProviders() {
        if (localProviderCache == null) {
            localProviderCache = new HashMap<>();
        }

        return localProviderCache;
    }

    private SensitivePropertyProvider getSensitivePropertyProvider(String protectionScheme) {
        if (isProviderAvailable(protectionScheme)) {
            return getSensitivePropertyProviders().get(protectionScheme);
        } else {
            throw new SensitivePropertyProtectionException("No provider available for " + protectionScheme);
        }
    }

    private boolean isProviderAvailable(String protectionScheme) {
        return getSensitivePropertyProviders().containsKey(protectionScheme);
    }

    /**
     * If the value is protected, unprotects it and returns it. If not, returns the original value.
     *
     * @param key            the retrieved property key
     * @param retrievedValue the retrieved property value
     * @return the unprotected value
     */
    private String unprotectValue(String key, String retrievedValue) {
        // Checks if the key is sensitive and marked as protected
        if (isPropertyProtected(key)) {
            final String protectionScheme = getProperty(getProtectionKey(key));

            // No provider registered for this scheme, so just return the value
            if (!isProviderAvailable(protectionScheme)) {
                logger.warn("No provider available for {} so passing the protected {} value back", protectionScheme, key);
                return retrievedValue;
            }

            try {
                SensitivePropertyProvider sensitivePropertyProvider = getSensitivePropertyProvider(protectionScheme);
                return sensitivePropertyProvider.unprotect(retrievedValue);
            } catch (SensitivePropertyProtectionException e) {
                throw new SensitivePropertyProtectionException("Error unprotecting value for " + key, e.getCause());
            }
        }
        return retrievedValue;
    }
}
