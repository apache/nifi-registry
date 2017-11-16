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

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NiFiRegistryProperties extends Properties {

    private static final Logger logger = LoggerFactory.getLogger(NiFiRegistryProperties.class);

    // Keys
    public static final String WEB_WAR_DIR = "nifi.registry.web.war.directory";
    public static final String WEB_HTTP_PORT = "nifi.registry.web.http.port";
    public static final String WEB_HTTP_HOST = "nifi.registry.web.http.host";
    public static final String WEB_HTTPS_PORT = "nifi.registry.web.https.port";
    public static final String WEB_HTTPS_HOST = "nifi.registry.web.https.host";
    public static final String WEB_WORKING_DIR = "nifi.registry.web.jetty.working.directory";
    public static final String WEB_THREADS = "nifi.registry.web.jetty.threads";

    public static final String SECURITY_KEYSTORE = "nifi.registry.security.keystore";
    public static final String SECURITY_KEYSTORE_TYPE = "nifi.registry.security.keystoreType";
    public static final String SECURITY_KEYSTORE_PASSWD = "nifi.registry.security.keystorePasswd";
    public static final String SECURITY_KEY_PASSWD = "nifi.registry.security.keyPasswd";
    public static final String SECURITY_TRUSTSTORE = "nifi.registry.security.truststore";
    public static final String SECURITY_TRUSTSTORE_TYPE = "nifi.registry.security.truststoreType";
    public static final String SECURITY_TRUSTSTORE_PASSWD = "nifi.registry.security.truststorePasswd";
    public static final String SECURITY_NEED_CLIENT_AUTH = "nifi.registry.security.needClientAuth";
    public static final String SECURITY_AUTHORIZERS_CONFIGURATION_FILE = "nifi.registry.security.authorizers.configuration.file";
    public static final String SECURITY_AUTHORIZER = "nifi.registry.security.authorizer";
    public static final String SECURITY_IDENTITY_PROVIDERS_CONFIGURATION_FILE = "nifi.registry.security.identity.providers.configuration.file";
    public static final String SECURITY_IDENTITY_PROVIDER = "nifi.registry.security.identity.provider";
    public static final String SECURITY_IDENTITY_MAPPING_PATTERN_PREFIX = "nifi.registry.security.identity.mapping.pattern.";
    public static final String SECURITY_IDENTITY_MAPPING_VALUE_PREFIX = "nifi.registry.security.identity.mapping.value.";

    public static final String EXTENSION_DIR_PREFIX = "nifi.registry.extension.dir.";

    public static final String PROVIDERS_CONFIGURATION_FILE = "nifi.registry.providers.configuration.file";

    public static final String DATABASE_DIRECTORY = "nifi.registry.db.directory";
    public static final String DATABASE_URL_APPEND = "nifi.registry.db.url.append";

    // Kerberos properties
    public static final String KERBEROS_KRB5_FILE = "nifi.registry.kerberos.krb5.file";
    public static final String KERBEROS_SPNEGO_PRINCIPAL = "nifi.registry.kerberos.spnego.principal";
    public static final String KERBEROS_SPNEGO_KEYTAB_LOCATION = "nifi.registry.kerberos.spnego.keytab.location";
    public static final String KERBEROS_SPNEGO_AUTHENTICATION_EXPIRATION = "nifi.registry.kerberos.spnego.authentication.expiration";

    // Defaults
    public static final String DEFAULT_WEB_WORKING_DIR = "./work/jetty";
    public static final String DEFAULT_WAR_DIR = "./lib";
    public static final String DEFAULT_PROVIDERS_CONFIGURATION_FILE = "./conf/providers.xml";
    public static final String DEFAULT_SECURITY_AUTHORIZERS_CONFIGURATION_FILE = "./conf/authorizers.xml";
    public static final String DEFAULT_SECURITY_IDENTITY_PROVIDER_CONFIGURATION_FILE = "./conf/identity-providers.xml";
    public static final String DEFAULT_AUTHENTICATION_EXPIRATION = "12 hours";

    public int getWebThreads() {
        int webThreads = 200;
        try {
            webThreads = Integer.parseInt(getProperty(WEB_THREADS));
        } catch (final NumberFormatException nfe) {
            logger.warn(String.format("%s must be an integer value. Defaulting to %s", WEB_THREADS, webThreads));
        }
        return webThreads;
    }

    public Integer getPort() {
        return getPropertyAsInteger(WEB_HTTP_PORT);
    }

    public String getHttpHost() {
        return getProperty(WEB_HTTP_HOST);
    }

    public Integer getSslPort() {
        return getPropertyAsInteger(WEB_HTTPS_PORT);
    }

    public String getHttpsHost() {
        return getProperty(WEB_HTTPS_HOST);
    }

    public boolean getNeedClientAuth() {
        boolean needClientAuth = true;
        String rawNeedClientAuth = getProperty(SECURITY_NEED_CLIENT_AUTH);
        if ("false".equalsIgnoreCase(rawNeedClientAuth)) {
            needClientAuth = false;
        }
        return needClientAuth;
    }

    public String getKeyStorePath() {
        return getProperty(SECURITY_KEYSTORE);
    }

    public String getKeyStoreType() {
        return getProperty(SECURITY_KEYSTORE_TYPE);
    }

    public String getKeyStorePassword() {
        return getProperty(SECURITY_KEYSTORE_PASSWD);
    }

    public String getKeyPassword() {
        return getProperty(SECURITY_KEY_PASSWD);
    }

    public String getTrustStorePath() {
        return getProperty(SECURITY_TRUSTSTORE);
    }

    public String getTrustStoreType() {
        return getProperty(SECURITY_TRUSTSTORE_TYPE);
    }

    public String getTrustStorePassword() {
        return getProperty(SECURITY_TRUSTSTORE_PASSWD);
    }

    public File getWarLibDirectory() {
        return new File(getProperty(WEB_WAR_DIR, DEFAULT_WAR_DIR));
    }

    public File getWebWorkingDirectory() {
        return new File(getProperty(WEB_WORKING_DIR, DEFAULT_WEB_WORKING_DIR));
    }

    public File getProvidersConfigurationFile() {
        return getPropertyAsFile(PROVIDERS_CONFIGURATION_FILE, DEFAULT_PROVIDERS_CONFIGURATION_FILE);
    }

    public String getDatabaseDirectory() {
        return getProperty(DATABASE_DIRECTORY);
    }

    public String getDatabaseUrlAppend() {
        return getProperty(DATABASE_URL_APPEND);
    }

    public File getAuthorizersConfigurationFile() {
        return getPropertyAsFile(SECURITY_AUTHORIZERS_CONFIGURATION_FILE, DEFAULT_SECURITY_AUTHORIZERS_CONFIGURATION_FILE);
    }

    public File getIdentityProviderConfigurationFile() {
        return getPropertyAsFile(SECURITY_IDENTITY_PROVIDERS_CONFIGURATION_FILE, DEFAULT_SECURITY_IDENTITY_PROVIDER_CONFIGURATION_FILE);
    }

    public File getKerberosConfigurationFile() {
        return getPropertyAsFile(KERBEROS_KRB5_FILE);
    }

    public String getKerberosSpnegoAuthenticationExpiration() {
        return getProperty(KERBEROS_SPNEGO_AUTHENTICATION_EXPIRATION, DEFAULT_AUTHENTICATION_EXPIRATION);
    }

    public String getKerberosSpnegoPrincipal() {
        return getPropertyAsTrimmedString(KERBEROS_SPNEGO_PRINCIPAL);
    }

    public String getKerberosSpnegoKeytabLocation() {
        return getPropertyAsTrimmedString(KERBEROS_SPNEGO_KEYTAB_LOCATION);
    }

    public boolean isKerberosSpnegoSupportEnabled() {
        return !StringUtils.isBlank(getKerberosSpnegoPrincipal()) && !StringUtils.isBlank(getKerberosSpnegoKeytabLocation());
    }

    public Set<String> getExtensionsDirs() {
        final Set<String> extensionDirs = new HashSet<>();
        stringPropertyNames().stream().filter(key -> key.startsWith(EXTENSION_DIR_PREFIX)).forEach(key -> extensionDirs.add(getProperty(key)));
        return extensionDirs;
    }

    /**
     * Retrieves all known property keys.
     *
     * @return all known property keys
     */
    public Set<String> getPropertyKeys() {
        Set<String> propertyNames = new HashSet<>();
        Enumeration e = this.propertyNames();
        for (; e.hasMoreElements(); ){
            propertyNames.add((String) e.nextElement());
        }

        return propertyNames;
    }

    // Helper functions for common ways of interpreting property values

    private String getPropertyAsTrimmedString(String key) {
        final String value = getProperty(key);
        if (!StringUtils.isBlank(value)) {
            return value.trim();
        } else {
            return null;
        }
    }

    private Integer getPropertyAsInteger(String key) {
        final String value = getProperty(key);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException nfe) {
            throw new IllegalStateException(String.format("%s must be an integer value.", key));
        }
    }

    private File getPropertyAsFile(String key) {
        final String filePath = getProperty(key);
        if (filePath != null && filePath.trim().length() > 0) {
            return new File(filePath.trim());
        } else {
            return null;
        }
    }

    private File getPropertyAsFile(String propertyKey, String defaultFileLocation) {
        final String value = getProperty(propertyKey);
        if (StringUtils.isBlank(value)) {
            return new File(defaultFileLocation);
        } else {
            return new File(value);
        }
    }

}
