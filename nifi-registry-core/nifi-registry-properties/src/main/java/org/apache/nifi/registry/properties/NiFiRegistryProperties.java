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

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
    public static final String SECURITY_IDENTITY_MAPPING_TRANSFORM_PREFIX = "nifi.registry.security.identity.mapping.transform.";
    public static final String SECURITY_GROUP_MAPPING_PATTERN_PREFIX = "nifi.registry.security.group.mapping.pattern.";
    public static final String SECURITY_GROUP_MAPPING_VALUE_PREFIX = "nifi.registry.security.group.mapping.value.";
    public static final String SECURITY_GROUP_MAPPING_TRANSFORM_PREFIX = "nifi.registry.security.group.mapping.transform.";

    public static final String EXTENSION_DIR_PREFIX = "nifi.registry.extension.dir.";

    public static final String PROVIDERS_CONFIGURATION_FILE = "nifi.registry.providers.configuration.file";
    public static final String REGISTRY_ALIAS_CONFIGURATION_FILE = "nifi.registry.registry.alias.configuration.file";

    public static final String EXTENSIONS_WORKING_DIR = "nifi.registry.extensions.working.directory";

    // Original DB properties
    public static final String DATABASE_DIRECTORY = "nifi.registry.db.directory";
    public static final String DATABASE_URL_APPEND = "nifi.registry.db.url.append";

    // New style DB properties
    public static final String DATABASE_URL = "nifi.registry.db.url";
    public static final String DATABASE_DRIVER_CLASS_NAME = "nifi.registry.db.driver.class";
    public static final String DATABASE_DRIVER_DIR = "nifi.registry.db.driver.directory";
    public static final String DATABASE_USERNAME = "nifi.registry.db.username";
    public static final String DATABASE_PASSWORD = "nifi.registry.db.password";
    public static final String DATABASE_MAX_CONNECTIONS = "nifi.registry.db.maxConnections";
    public static final String DATABASE_SQL_DEBUG = "nifi.registry.db.sql.debug";

    // Kerberos properties
    public static final String KERBEROS_KRB5_FILE = "nifi.registry.kerberos.krb5.file";
    public static final String KERBEROS_SPNEGO_PRINCIPAL = "nifi.registry.kerberos.spnego.principal";
    public static final String KERBEROS_SPNEGO_KEYTAB_LOCATION = "nifi.registry.kerberos.spnego.keytab.location";
    public static final String KERBEROS_SPNEGO_AUTHENTICATION_EXPIRATION = "nifi.registry.kerberos.spnego.authentication.expiration";
    public static final String KERBEROS_SERVICE_PRINCIPAL = "nifi.registry.kerberos.service.principal";
    public static final String KERBEROS_SERVICE_KEYTAB_LOCATION = "nifi.registry.kerberos.service.keytab.location";

    // Defaults
    public static final String DEFAULT_WEB_WORKING_DIR = "./work/jetty";
    public static final String DEFAULT_WAR_DIR = "./lib";
    public static final String DEFAULT_PROVIDERS_CONFIGURATION_FILE = "./conf/providers.xml";
    public static final String DEFAULT_REGISTRY_ALIAS_CONFIGURATION_FILE = "./conf/registry-aliases.xml";
    public static final String DEFAULT_SECURITY_AUTHORIZERS_CONFIGURATION_FILE = "./conf/authorizers.xml";
    public static final String DEFAULT_SECURITY_IDENTITY_PROVIDER_CONFIGURATION_FILE = "./conf/identity-providers.xml";
    public static final String DEFAULT_AUTHENTICATION_EXPIRATION = "12 hours";
    public static final String DEFAULT_EXTENSIONS_WORKING_DIR = "./work/extensions";

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

    public File getExtensionsWorkingDirectory() {
        return  new File(getProperty(EXTENSIONS_WORKING_DIR, DEFAULT_EXTENSIONS_WORKING_DIR));
    }

    public File getProvidersConfigurationFile() {
        return getPropertyAsFile(PROVIDERS_CONFIGURATION_FILE, DEFAULT_PROVIDERS_CONFIGURATION_FILE);
    }

    public File getRegistryAliasConfigurationFile() {
        return getPropertyAsFile(REGISTRY_ALIAS_CONFIGURATION_FILE, DEFAULT_REGISTRY_ALIAS_CONFIGURATION_FILE);
    }

    public String getLegacyDatabaseDirectory() {
        return getProperty(DATABASE_DIRECTORY);
    }

    public String getLegacyDatabaseUrlAppend() {
        return getProperty(DATABASE_URL_APPEND);
    }

    public String getDatabaseUrl() {
        return getProperty(DATABASE_URL);
    }

    public String getDatabaseDriverClassName() {
        return getProperty(DATABASE_DRIVER_CLASS_NAME);
    }

    public String getDatabaseDriverDirectory() {
        return getProperty(DATABASE_DRIVER_DIR);
    }

    public String getDatabaseUsername() {
        return getProperty(DATABASE_USERNAME);
    }

    public String getDatabasePassword() {
        return getProperty(DATABASE_PASSWORD);
    }

    public Integer getDatabaseMaxConnections() {
        return getPropertyAsInteger(DATABASE_MAX_CONNECTIONS);
    }

    public boolean getDatabaseSqlDebug() {
        final String value = getProperty(DATABASE_SQL_DEBUG);

        if (StringUtils.isBlank(value)) {
            return false;
        }

        return "true".equalsIgnoreCase(value.trim());
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

    public String getKerberosServicePrincipal() {
        return getPropertyAsTrimmedString(KERBEROS_SERVICE_PRINCIPAL);
    }

    public String getKerberosServiceKeytabLocation() {
        return getPropertyAsTrimmedString(KERBEROS_SERVICE_KEYTAB_LOCATION);
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
