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
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for NiFiRegistry.
 */
public class NiFiRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(NiFiRegistry.class);
    private static final String KEY_FILE_FLAG = "-K";

    public static final String BOOTSTRAP_PORT_PROPERTY = "nifi.registry.bootstrap.listen.port";

    private final JettyServer server;
    private final BootstrapListener bootstrapListener;
    private volatile boolean shutdown = false;

    public NiFiRegistry(final NiFiRegistryProperties properties)
            throws ClassNotFoundException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                LOGGER.error("An Unknown Error Occurred in Thread {}: {}", t, e.toString());
                LOGGER.error("", e);
            }
        });

        // register the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // shutdown the jetty server
                shutdownHook();
            }
        }));

        final String bootstrapPort = System.getProperty(BOOTSTRAP_PORT_PROPERTY);
        if (bootstrapPort != null) {
            try {
                final int port = Integer.parseInt(bootstrapPort);

                if (port < 1 || port > 65535) {
                    throw new RuntimeException("Failed to start NiFi Registry because system property '" + BOOTSTRAP_PORT_PROPERTY + "' is not a valid integer in the range 1 - 65535");
                }

                bootstrapListener = new BootstrapListener(this, port);
                bootstrapListener.start();
            } catch (final NumberFormatException nfe) {
                throw new RuntimeException("Failed to start NiFi Registry because system property '" + BOOTSTRAP_PORT_PROPERTY + "' is not a valid integer in the range 1 - 65535");
            }
        } else {
            LOGGER.info("NiFi Registry started without Bootstrap Port information provided; will not listen for requests from Bootstrap");
            bootstrapListener = null;
        }

        // delete the web working dir - if the application does not start successfully
        // the web app directories might be in an invalid state. when this happens
        // jetty will not attempt to re-extract the war into the directory. by removing
        // the working directory, we can be assured that it will attempt to extract the
        // war every time the application starts.
        File webWorkingDir = properties.getWebWorkingDirectory();
        FileUtils.deleteFilesInDirectory(webWorkingDir, null, LOGGER, true, true);
        FileUtils.deleteFile(webWorkingDir, LOGGER, 3);

        // redirect JUL log events
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final long startTime = System.nanoTime();
        server = new JettyServer(properties);

        if (shutdown) {
            LOGGER.info("NiFi Registry has been shutdown via NiFi Registry Bootstrap. Will not start Controller");
        } else {
            server.start();

            if (bootstrapListener != null) {
                bootstrapListener.sendStartedStatus(true);
            }

            final long duration = System.nanoTime() - startTime;
            LOGGER.info("Registry initialization took " + duration + " nanoseconds "
                    + "(" + (int) TimeUnit.SECONDS.convert(duration, TimeUnit.NANOSECONDS) + " seconds).");
        }
    }

    protected void shutdownHook() {
        try {
            this.shutdown = true;

            LOGGER.info("Initiating shutdown of Jetty web server...");
            if (server != null) {
                server.stop();
            }
            if (bootstrapListener != null) {
                bootstrapListener.stop();
            }
            LOGGER.info("Jetty web server shutdown completed (nicely or otherwise).");
        } catch (final Throwable t) {
            LOGGER.warn("Problem occurred ensuring Jetty web server was properly terminated due to " + t);
        }
    }

    /**
     * Main entry point of the application.
     *
     * @param args things which are ignored
     */
    public static void main(String[] args) {
        LOGGER.info("Launching NiFi Registry...");

        final NiFiRegistryProperties properties = new NiFiRegistryProperties();
        try (final FileReader reader = new FileReader("conf/nifi-registry.properties")) {
            properties.load(reader);
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to load properties: " + ioe, ioe);
        }

        try {
            new NiFiRegistry(properties);
        } catch (final Throwable t) {
            LOGGER.error("Failure to launch NiFi Registry due to " + t, t);
        }
    }

    private static String loadFormattedKey(String[] args) {
        String key = null;
        List<String> parsedArgs = parseArgs(args);
        // Check if args contain protection key
        if (parsedArgs.contains(KEY_FILE_FLAG)) {
            key = getKeyFromKeyFileAndPrune(parsedArgs);
            // Format the key (check hex validity and remove spaces)
            key = formatHexKey(key);
        }

        if (null == key) {
            return "";
        } else if (!isHexKeyValid(key)) {
            throw new IllegalArgumentException("The key was not provided in valid hex format and of the correct length");
        } else {
            return key;
        }
    }

    private static String getKeyFromKeyFileAndPrune(List<String> parsedArgs) {
        String key = null;
        LOGGER.debug("The bootstrap process provided the " + KEY_FILE_FLAG + " flag");
        int i = parsedArgs.indexOf(KEY_FILE_FLAG);
        if (parsedArgs.size() <= i + 1) {
            LOGGER.error("The bootstrap process passed the {} flag without a filename", KEY_FILE_FLAG);
            throw new IllegalArgumentException("The bootstrap process provided the " + KEY_FILE_FLAG + " flag but no key");
        }
        try {
            String passwordfile_path = parsedArgs.get(i + 1);
            // Slurp in the contents of the file:
            byte[] encoded = Files.readAllBytes(Paths.get(passwordfile_path));
            key = new String(encoded, StandardCharsets.UTF_8);
            if (0 == key.length())
                throw new IllegalArgumentException("Key in keyfile " + passwordfile_path + " yielded an empty key");

            LOGGER.info("Now overwriting file in "+passwordfile_path);

            // Overwrite the contents of the file (to avoid littering file system
            // unlinked with key material):
            File password_file = new File(passwordfile_path);
            FileWriter overwriter = new FileWriter(password_file,false);

            // Construe a random pad:
            Random r = new Random();
            StringBuffer sb = new StringBuffer();
            // Note on correctness: this pad is longer, but equally sufficient.
            while(sb.length() < encoded.length){
                sb.append(Integer.toHexString(r.nextInt()));
            }
            String pad = sb.toString();
            LOGGER.info("Overwriting key material with pad: "+pad);
            overwriter.write(pad);
            overwriter.close();

            LOGGER.info("Removing/unlinking file: "+passwordfile_path);
            password_file.delete();

        } catch (IOException e) {
            LOGGER.error("Caught IOException while retrieving the "+KEY_FILE_FLAG+"-passed keyfile; aborting: "+e.toString());
            System.exit(1);
        }

        LOGGER.info("Read property protection key from key file provided by bootstrap process");
        return key;
    }

    private static List<String> parseArgs(String[] args) {
        List<String> parsedArgs = new ArrayList<>(Arrays.asList(args));
        for (int i = 0; i < parsedArgs.size(); i++) {
            if (parsedArgs.get(i).startsWith(KEY_FILE_FLAG + " ")) {
                String[] split = parsedArgs.get(i).split(" ", 2);
                parsedArgs.set(i, split[0]);
                parsedArgs.add(i + 1, split[1]);
                break;
            }
        }
        return parsedArgs;
    }

    private static String formatHexKey(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        return input.replaceAll("[^0-9a-fA-F]", "").toLowerCase();
    }

    private static boolean isHexKeyValid(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        // Key length is in "nibbles" (i.e. one hex char = 4 bits)
        return Arrays.asList(128, 196, 256).contains(key.length() * 4) && key.matches("^[0-9a-fA-F]*$");
    }
}
