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
package org.apache.nifi.registry;

import org.apache.nifi.registry.jetty.JettyServer;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;

/**
 * Main entry point for NiFiRegistry.
 */
public class NiFiRegistry {

    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private static JettyServer server;

    public static void main(final String[] args) {
        // register the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // shutdown the jetty server
                shutdownHook();
            }
        }));

        // load the properties
        final NiFiRegistryProperties properties = new NiFiRegistryProperties();
        try (final FileReader reader = new FileReader("conf/nifi-registry.properties")) {
            properties.load(reader);
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to load properties: " + ioe, ioe);
        }

        // start the server
        server = new JettyServer(properties);
        server.start();
    }

    private static void shutdownHook() {
        try {
            logger.info("Initiating shutdown of Jetty web server...");
            if (server != null) {
                server.stop();
            }
            logger.info("Jetty web server shutdown completed (nicely or otherwise).");
        } catch (final Throwable t) {
            logger.warn("Problem occurred ensuring Jetty web server was properly terminated due to " + t);
        }
    }
}
