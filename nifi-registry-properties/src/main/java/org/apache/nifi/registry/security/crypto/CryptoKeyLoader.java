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
package org.apache.nifi.registry.security.crypto;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class CryptoKeyLoader {

    private static final Logger logger = LoggerFactory.getLogger(CryptoKeyLoader.class);

    private static final String BOOTSTRAP_KEY_PREFIX = "nifi.registry.bootstrap.sensitive.key=";

    /**
     * Returns the key (if any) used to encrypt sensitive properties.
     * The key extracted from the bootstrap.conf file at the specified location.
     *
     * @param bootstrapPath the path to the bootstrap file
     * @return the key in hexadecimal format, or {@link CryptoKeyProvider#EMPTY_KEY} if the key is null or empty
     * @throws IOException if the file is not readable
     */
    public static String extractKeyFromBootstrapFile(String bootstrapPath) throws IOException {
        File expectedBootstrapFile;
        if (StringUtils.isBlank(bootstrapPath)) {
            logger.error("Cannot read from bootstrap.conf file to extract encryption key; location not specified");
            throw new IOException("Cannot read from bootstrap.conf without file location");
        } else {
            expectedBootstrapFile = new File(bootstrapPath);
        }

        if (expectedBootstrapFile.exists() && expectedBootstrapFile.canRead()) {
            try (Stream<String> stream = Files.lines(Paths.get(expectedBootstrapFile.getAbsolutePath()))) {
                Optional<String> keyLine = stream.filter(l -> l.startsWith(BOOTSTRAP_KEY_PREFIX)).findFirst();
                if (keyLine.isPresent()) {
                    String keyValue = keyLine.get().split("=", 2)[1];
                    return checkHexKey(keyValue);
                } else {
                    logger.warn("No encryption key present in the bootstrap.conf file at {}", expectedBootstrapFile.getAbsolutePath());
                    return CryptoKeyProvider.EMPTY_KEY;
                }
            } catch (IOException e) {
                logger.error("Cannot read from bootstrap.conf file at {} to extract encryption key", expectedBootstrapFile.getAbsolutePath());
                throw new IOException("Cannot read from bootstrap.conf", e);
            }
        } else {
            logger.error("Cannot read from bootstrap.conf file at {} to extract encryption key -- file is missing or permissions are incorrect", expectedBootstrapFile.getAbsolutePath());
            throw new IOException("Cannot read from bootstrap.conf");
        }
    }

    /**
     * Returns the key (if any) used to encrypt sensitive properties.
     * The key extracted from the key file at the specified location.
     *
     * @param keyfilePath the path to the file containing the password/key
     * @param securelyDeleteKeyfileOnSuccess If true, this method has the additional
     *                                       side-effect of overwriting the contents
     *                                       of the key file (with random bytes) and
     *                                       deleting it after successfully reading the key.
     *                                       If the key is not read from the file, secure
     *                                       deletion is not attempted even if this flag is set to true.
     * @return the key in hexadecimal format, or {@link CryptoKeyProvider#EMPTY_KEY} if the key is null or empty
     * @throws IOException if the file is not readable
     */
    public static String extractKeyFromKeyFile(String keyfilePath, boolean securelyDeleteKeyfileOnSuccess)
            throws IOException, IllegalArgumentException {

        if (StringUtils.isBlank(keyfilePath)) {
            logger.error("Cannot read from password file to extract encryption key; location not specified");
            throw new IOException("Cannot read from password file without file location");
        }

        // Slurp in the contents of the file:
        logger.info("Loading crypto key from file: {}", keyfilePath);
        byte[] encoded = Files.readAllBytes(Paths.get(keyfilePath));
        String key = new String(encoded, StandardCharsets.UTF_8);
        if (0 == key.length())
            throw new IllegalArgumentException("Key in keyfile " + keyfilePath + " yielded an empty key");

        if (securelyDeleteKeyfileOnSuccess) {
            // Overwrite the contents of the file (to avoid littering file system unlinked with key material):
            logger.info("Now overwriting file '{}' with random bytes ", keyfilePath);
            File password_file = new File(keyfilePath);
            FileWriter overwriter = new FileWriter(password_file,false);

            // Construe a random pad:
            Random r = new Random();
            StringBuffer sb = new StringBuffer();
            // Note on correctness: this pad is longer, but equally sufficient.
            while(sb.length() < encoded.length){
                sb.append(Integer.toHexString(r.nextInt()));
            }
            String pad = sb.toString();
            logger.debug("Overwriting key material with pad: " + pad);
            overwriter.write(pad);
            overwriter.close();

            logger.info("Removing/unlinking file: "+ keyfilePath);
            password_file.delete();
        }

        return checkHexKey(key);
    }

    private static String checkHexKey(String input) {
        if (input == null || input.trim().isEmpty()) {
            return CryptoKeyProvider.EMPTY_KEY;
        }
        return input;
    }



}
