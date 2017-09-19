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
import java.util.Properties;
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
    public static final String SECURITY_AUTHORIZED_USERS = "nifi.registry.security.authorized.users";

    public static final String PROVIDERS_CONFIGURATION_FILE = "nifi.registry.providers.configuration.file";

    public static final String DATABASE_DIRECTORY = "nifi.registry.db.directory";
    public static final String DATABASE_URL_APPEND = "nifi.registry.db.url.append";

    // Defaults
    public static final String DEFAULT_WEB_WORKING_DIR = "./work/jetty";
    public static final String DEFAULT_WAR_DIR = "./lib";
    public static final String DEFAULT_PROVIDERS_CONFIGURATION_FILE = "./conf/providers.xml";

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
        final String rawPort = getProperty(WEB_HTTP_PORT);
        if (StringUtils.isBlank(rawPort)) {
            return null;
        }
        try {
            return Integer.parseInt(rawPort);
        } catch (final NumberFormatException nfe) {
            throw new IllegalStateException(String.format("%s must be an integer value.", WEB_HTTP_PORT));
        }
    }

    public String getHttpHost() {
        return getProperty(WEB_HTTP_HOST);
    }

    public Integer getSslPort() {
        final String rawPort = getProperty(WEB_HTTPS_PORT);
        if (StringUtils.isBlank(rawPort)) {
            return null;
        }
        try {
            return Integer.parseInt(rawPort);
        } catch (final NumberFormatException nfe) {
            throw new IllegalStateException(String.format("%s must be an integer value.", WEB_HTTPS_PORT));
        }
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

    public File getAuthorizedUsersFile() {
        final String authorizedUsersFile = getProperty(SECURITY_AUTHORIZED_USERS);
        if (StringUtils.isBlank(authorizedUsersFile)) {
            return null;
        }
        return new File(authorizedUsersFile);
    }

    public File getProvidersConfigurationFile() {
        final String value = getProperty(PROVIDERS_CONFIGURATION_FILE);
        if (StringUtils.isBlank(value)) {
            return new File(DEFAULT_PROVIDERS_CONFIGURATION_FILE);
        } else {
            return new File(value);
        }
    }

    public String getDatabaseDirectory() {
        return getProperty(DATABASE_DIRECTORY);
    }

    public String getDatabaseUrlAppend() {
        return getProperty(DATABASE_URL_APPEND);
    }

}
