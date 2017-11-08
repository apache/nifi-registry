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
package org.apache.nifi.registry.web.security.authentication.x509;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * Extracts client certificates from Http requests.
 */
@Component
public class X509CertificateValidator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Extract the client certificate from the specified HttpServletRequest or null if none is specified.
     *
     * @param certificates the client certificates
     * @throws CertificateExpiredException cert is expired
     * @throws CertificateNotYetValidException cert is not yet valid
     */
    public void validateClientCertificate(final X509Certificate[] certificates)
            throws CertificateExpiredException, CertificateNotYetValidException {

        // ensure the cert is valid
        certificates[0].checkValidity();
    }

}
