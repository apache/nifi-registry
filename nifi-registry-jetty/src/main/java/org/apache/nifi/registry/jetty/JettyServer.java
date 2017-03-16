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
package org.apache.nifi.registry.jetty;

import org.apache.nifi.registry.security.AuthorizationProvider;
import org.apache.nifi.registry.security.AuthorizedUserFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class JettyServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private static final int HEADER_BUFFER_SIZE = 16 * 1024; // 16kb

    private static final FileFilter WAR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            final String nameToTest = pathname.getName().toLowerCase();
            return nameToTest.endsWith(".war") && pathname.isFile();
        }
    };

    private final NiFiRegistryProperties properties;
    private final Server server;

    private WebAppContext webUiContext;
    private WebAppContext webApiContext;

    public JettyServer(final NiFiRegistryProperties properties) {
        final QueuedThreadPool threadPool = new QueuedThreadPool(properties.getWebThreads());
        threadPool.setName("NiFi Registry Web Server");

        this.properties = properties;
        this.server = new Server(threadPool);
        configureConnectors();
        loadWars();
    }

    private void configureConnectors() {
        // create the http configuration
        final HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setRequestHeaderSize(HEADER_BUFFER_SIZE);
        httpConfiguration.setResponseHeaderSize(HEADER_BUFFER_SIZE);

        if (properties.getPort() != null) {
            final Integer port = properties.getPort();
            if (port < 0 || (int) Math.pow(2, 16) <= port) {
                throw new IllegalStateException("Invalid HTTP port: " + port);
            }

            logger.info("Configuring Jetty for HTTP on port: " + port);

            // create the connector
            final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));

            // set host and port
            if (StringUtils.isNotBlank(properties.getHttpHost())) {
                http.setHost(properties.getHttpHost());
            }
            http.setPort(port);

            // add this connector
            server.addConnector(http);
        } else if (properties.getSslPort() != null) {
            final Integer port = properties.getSslPort();
            if (port < 0 || (int) Math.pow(2, 16) <= port) {
                throw new IllegalStateException("Invalid HTTPs port: " + port);
            }

            logger.info("Configuring Jetty for HTTPs on port: " + port);

            // add some secure config
            final HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.setSecureScheme("https");
            httpsConfiguration.setSecurePort(properties.getSslPort());
            httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

            // build the connector
            final ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(createSslContextFactory(), "http/1.1"),
                    new HttpConnectionFactory(httpsConfiguration));

            // set host and port
            if (StringUtils.isNotBlank(properties.getHttpsHost())) {
                https.setHost(properties.getHttpsHost());
            }
            https.setPort(port);

            // add this connector
            server.addConnector(https);
        }
    }

    private SslContextFactory createSslContextFactory() {
        final SslContextFactory contextFactory = new SslContextFactory();

        logger.error("" + properties.getNeedClientAuth());

        // need client auth
        contextFactory.setNeedClientAuth(properties.getNeedClientAuth());

        /* below code sets JSSE system properties when values are provided */
        // keystore properties
        if (StringUtils.isNotBlank(properties.getKeyStorePath())) {
            contextFactory.setKeyStorePath(properties.getKeyStorePath());
        }
        if (StringUtils.isNotBlank(properties.getKeyStoreType())) {
            contextFactory.setKeyStoreType(properties.getKeyStoreType());
        }
        final String keystorePassword = properties.getKeyStorePassword();
        final String keyPassword = properties.getKeyPassword();
        if (StringUtils.isNotBlank(keystorePassword)) {
            // if no key password was provided, then assume the keystore password is the same as the key password.
            final String defaultKeyPassword = (StringUtils.isBlank(keyPassword)) ? keystorePassword : keyPassword;
            contextFactory.setKeyManagerPassword(keystorePassword);
            contextFactory.setKeyStorePassword(defaultKeyPassword);
        } else if (StringUtils.isNotBlank(keyPassword)) {
            // since no keystore password was provided, there will be no keystore integrity check
            contextFactory.setKeyStorePassword(keyPassword);
        }

        // truststore properties
        if (StringUtils.isNotBlank(properties.getTrustStorePath())) {
            contextFactory.setTrustStorePath(properties.getTrustStorePath());
        }
        if (StringUtils.isNotBlank(properties.getTrustStoreType())) {
            contextFactory.setTrustStoreType(properties.getTrustStoreType());
        }
        if (StringUtils.isNotBlank(properties.getTrustStorePassword())) {
            contextFactory.setTrustStorePassword(properties.getTrustStorePassword());
        }

        return contextFactory;
    }

    private void loadWars() {
        final File warDirectory = properties.getWarLibDirectory();
        final File[] wars = warDirectory.listFiles(WAR_FILTER);

        if (wars == null) {
            throw new RuntimeException("Unable to access war lib directory: " + warDirectory);
        }

        File webUiWar = null;
        File webApiWar = null;
        for (final File war : wars) {
            if (war.getName().startsWith("nifi-registry-web-ui")) {
                webUiWar = war;
            } else if (war.getName().startsWith("nifi-registry-web-api")) {
                webApiWar = war;
            }
        }

        if (webUiWar == null) {
            throw new IllegalStateException("Unable to locate NiFi Registry Web UI");
        } else if (webApiWar == null) {
            throw new IllegalStateException("Unable to locate NiFi Registry Web API");
        }

        webUiContext = loadWar(webUiWar, "/nifi-registry");
        webApiContext = loadWar(webApiWar, "/nifi-registry-api");
        webApiContext.setAttribute("nifi-registry.properties", properties);

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(webUiContext);
        handlers.addHandler(webApiContext);
        server.setHandler(handlers);
    }

    private WebAppContext loadWar(final File warFile, final String contextPath) {
        final WebAppContext webappContext = new WebAppContext(warFile.getPath(), contextPath);
        webappContext.setContextPath(contextPath);
        webappContext.setDisplayName(contextPath);

        // remove slf4j server class to allow WAR files to have slf4j dependencies in WEB-INF/lib
        List<String> serverClasses = new ArrayList<>(Arrays.asList(webappContext.getServerClasses()));
        serverClasses.remove("org.slf4j.");
        webappContext.setServerClasses(serverClasses.toArray(new String[0]));

        // get the temp directory for this webapp
        final File webWorkingDirectory = properties.getWebWorkingDirectory();
        final File tempDir = new File(webWorkingDirectory, warFile.getName());
        if (tempDir.exists() && !tempDir.isDirectory()) {
            throw new RuntimeException(tempDir.getAbsolutePath() + " is not a directory");
        } else if (!tempDir.exists()) {
            final boolean made = tempDir.mkdirs();
            if (!made) {
                throw new RuntimeException(tempDir.getAbsolutePath() + " could not be created");
            }
        }
        if (!(tempDir.canRead() && tempDir.canWrite())) {
            throw new RuntimeException(tempDir.getAbsolutePath() + " directory does not have read/write privilege");
        }

        // configure the temp dir
        webappContext.setTempDirectory(tempDir);

        // configure the max form size (3x the default)
        webappContext.setMaxFormContentSize(600000);

//        try {
//            webappContext.setClassLoader(new WebAppClassLoader(ClassLoader.getSystemClassLoader(), webappContext));
//        } catch (final IOException ioe) {
//            throw new RuntimeException(ioe);
//        }
        logger.info("Loading WAR: " + warFile.getAbsolutePath() + " with context path set to " + contextPath);
        return webappContext;
    }

    public void start() {
        try {
            // start the server
            server.start();

            // ensure everything started successfully
            for (Handler handler : server.getChildHandlers()) {
                // see if the handler is a web app
                if (handler instanceof WebAppContext) {
                    WebAppContext context = (WebAppContext) handler;

                    // see if this webapp had any exceptions that would
                    // cause it to be unavailable
                    if (context.getUnavailableException() != null) {
                        throw context.getUnavailableException();
                    }
                }
            }

            // add the authorization filter
            final Filter authorizationFilter = new AuthorizedUserFilter(new AuthorizationProvider(properties));
            webUiContext.addFilter(new FilterHolder(authorizationFilter), "/*", EnumSet.allOf(DispatcherType.class));
            webApiContext.addFilter(new FilterHolder(authorizationFilter), "/*", EnumSet.allOf(DispatcherType.class));

            dumpUrls();
        } catch (final Throwable t) {
            throw new RuntimeException("Unable to start up: " + t, t);
        }
    }

    private void dumpUrls() throws SocketException {
        final List<String> urls = new ArrayList<>();

        for (Connector connector : server.getConnectors()) {
            if (connector instanceof ServerConnector) {
                final ServerConnector serverConnector = (ServerConnector) connector;

                Set<String> hosts = new HashSet<>();

                // determine the hosts
                if (StringUtils.isNotBlank(serverConnector.getHost())) {
                    hosts.add(serverConnector.getHost());
                } else {
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    if (networkInterfaces != null) {
                        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                            for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                                hosts.add(inetAddress.getHostAddress());
                            }
                        }
                    }
                }

                // ensure some hosts were found
                if (!hosts.isEmpty()) {
                    String scheme = "http";
                    if (properties.getSslPort() != null && serverConnector.getPort() == properties.getSslPort()) {
                        scheme = "https";
                    }

                    // dump each url
                    for (String host : hosts) {
                        urls.add(String.format("%s://%s:%s", scheme, host, serverConnector.getPort()));
                    }
                }
            }
        }

        if (urls.isEmpty()) {
            logger.warn("NiFi Registry has started, but the UI is not available on any hosts. Please verify the host properties.");
        } else {
            // log the ui location
            logger.info("NiFi Registry has started. The UI is available at the following URLs:");
            for (final String url : urls) {
                logger.info(String.format("%s/nifi-registry", url));
            }
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception ex) {
            logger.warn("Failed to stop web server", ex);
        }
    }
}
