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
package org.apache.nifi.registry.security.util;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CertificateUtils {
    private static final Logger logger = LoggerFactory.getLogger(CertificateUtils.class);
    private static final String PEER_NOT_AUTHENTICATED_MSG = "peer not authenticated";
    private static final Map<ASN1ObjectIdentifier, Integer> dnOrderMap = createDnOrderMap();

    public static final String JAVA_8_MAX_SUPPORTED_TLS_PROTOCOL_VERSION = "TLSv1.2";
    public static final String JAVA_11_MAX_SUPPORTED_TLS_PROTOCOL_VERSION = "TLSv1.3";
    public static final String[] JAVA_8_SUPPORTED_TLS_PROTOCOL_VERSIONS = new String[]{JAVA_8_MAX_SUPPORTED_TLS_PROTOCOL_VERSION};
    public static final String[] JAVA_11_SUPPORTED_TLS_PROTOCOL_VERSIONS = new String[]{JAVA_11_MAX_SUPPORTED_TLS_PROTOCOL_VERSION, JAVA_8_MAX_SUPPORTED_TLS_PROTOCOL_VERSION};

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * The time in milliseconds that the last unique serial number was generated
     */
    private static long lastSerialNumberMillis = 0L;

    /**
     * An incrementor to add uniqueness to serial numbers generated in the same millisecond
     */
    private static int serialNumberIncrementor = 0;

    /**
     * BigInteger value to use for the base of the unique serial number
     */
    private static BigInteger millisecondBigInteger;

    private static Map<ASN1ObjectIdentifier, Integer> createDnOrderMap() {
        Map<ASN1ObjectIdentifier, Integer> orderMap = new HashMap<>();
        int count = 0;
        orderMap.put(BCStyle.CN, count++);
        orderMap.put(BCStyle.L, count++);
        orderMap.put(BCStyle.ST, count++);
        orderMap.put(BCStyle.O, count++);
        orderMap.put(BCStyle.OU, count++);
        orderMap.put(BCStyle.C, count++);
        orderMap.put(BCStyle.STREET, count++);
        orderMap.put(BCStyle.DC, count++);
        orderMap.put(BCStyle.UID, count++);
        return Collections.unmodifiableMap(orderMap);
    }

    /**
     * Extracts the username from the specified DN. If the username cannot be extracted because the CN is in an unrecognized format, the entire CN is returned. If the CN cannot be extracted because
     * the DN is in an unrecognized format, the entire DN is returned.
     *
     * @param dn the dn to extract the username from
     * @return the exatracted username
     */
    public static String extractUsername(String dn) {
        String username = dn;

        // ensure the dn is specified
        if (StringUtils.isNotBlank(dn)) {
            // determine the separate
            final String separator = StringUtils.indexOfIgnoreCase(dn, "/cn=") > 0 ? "/" : ",";

            // attempt to locate the cd
            final String cnPattern = "cn=";
            final int cnIndex = StringUtils.indexOfIgnoreCase(dn, cnPattern);
            if (cnIndex >= 0) {
                int separatorIndex = StringUtils.indexOf(dn, separator, cnIndex);
                if (separatorIndex > 0) {
                    username = StringUtils.substring(dn, cnIndex + cnPattern.length(), separatorIndex);
                } else {
                    username = StringUtils.substring(dn, cnIndex + cnPattern.length());
                }
            }
        }

        return username;
    }

    /**
     * Returns a list of subject alternative names. Any name that is represented as a String by X509Certificate.getSubjectAlternativeNames() is converted to lowercase and returned.
     *
     * @param certificate a certificate
     * @return a list of subject alternative names; list is never null
     * @throws CertificateParsingException if parsing the certificate failed
     */
    public static List<String> getSubjectAlternativeNames(final X509Certificate certificate) throws CertificateParsingException {

        final Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
        if (altNames == null) {
            return new ArrayList<>();
        }

        final List<String> result = new ArrayList<>();
        for (final List<?> generalName : altNames) {
            /**
             * generalName has the name type as the first element a String or byte array for the second element. We return any general names that are String types.
             *
             * We don't inspect the numeric name type because some certificates incorrectly put IPs and DNS names under the wrong name types.
             */
            final Object value = generalName.get(1);
            if (value instanceof String) {
                result.add(((String) value).toLowerCase());
            }

        }

        return result;
    }

    /**
     * Returns the DN extracted from the peer certificate (the server DN if run on the client; the client DN (if available) if run on the server).
     * <p>
     * If the client auth setting is WANT or NONE and a client certificate is not present, this method will return {@code null}.
     * If the client auth is NEED, it will throw a {@link CertificateException}.
     *
     * @param socket the SSL Socket
     * @return the extracted DN
     * @throws CertificateException if there is a problem parsing the certificate
     */
    public static String extractPeerDNFromSSLSocket(Socket socket) throws CertificateException {
        String dn = null;
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;

            boolean clientMode = sslSocket.getUseClientMode();
            logger.debug("SSL Socket in {} mode", clientMode ? "client" : "server");
            SslContextFactory.ClientAuth clientAuth = getClientAuthStatus(sslSocket);
            logger.debug("SSL Socket client auth status: {}", clientAuth);

            if (clientMode) {
                logger.debug("This socket is in client mode, so attempting to extract certificate from remote 'server' socket");
                dn = extractPeerDNFromServerSSLSocket(sslSocket);
            } else {
                logger.debug("This socket is in server mode, so attempting to extract certificate from remote 'client' socket");
                dn = extractPeerDNFromClientSSLSocket(sslSocket);
            }
        }

        return dn;
    }

    /**
     * Returns the DN extracted from the client certificate.
     * <p>
     * If the client auth setting is WANT or NONE and a certificate is not present (and {@code respectClientAuth} is {@code true}), this method will return {@code null}.
     * If the client auth is NEED, it will throw a {@link CertificateException}.
     *
     * @param sslSocket the SSL Socket
     * @return the extracted DN
     * @throws CertificateException if there is a problem parsing the certificate
     */
    private static String extractPeerDNFromClientSSLSocket(SSLSocket sslSocket) throws CertificateException {
        String dn = null;

        /** The clientAuth value can be "need", "want", or "none"
         * A client must send client certificates for need, should for want, and will not for none.
         * This method should throw an exception if none are provided for need, return null if none are provided for want, and return null (without checking) for none.
         */

        SslContextFactory.ClientAuth clientAuth = getClientAuthStatus(sslSocket);
        logger.debug("SSL Socket client auth status: {}", clientAuth);

        if (clientAuth != SslContextFactory.ClientAuth.NONE) {
            try {
                final Certificate[] certChains = sslSocket.getSession().getPeerCertificates();
                if (certChains != null && certChains.length > 0) {
                    X509Certificate x509Certificate = convertAbstractX509Certificate(certChains[0]);
                    dn = x509Certificate.getSubjectDN().getName().trim();
                    logger.debug("Extracted DN={} from client certificate", dn);
                }
            } catch (SSLPeerUnverifiedException e) {
                if (e.getMessage().equals(PEER_NOT_AUTHENTICATED_MSG)) {
                    logger.error("The incoming request did not contain client certificates and thus the DN cannot" +
                            " be extracted. Check that the other endpoint is providing a complete client certificate chain");
                }
                if (clientAuth == SslContextFactory.ClientAuth.WANT) {
                    logger.warn("Suppressing missing client certificate exception because client auth is set to 'want'");
                    return dn;
                }
                throw new CertificateException(e);
            }
        }
        return dn;
    }

    /**
     * Returns the DN extracted from the server certificate.
     *
     * @param socket the SSL Socket
     * @return the extracted DN
     * @throws CertificateException if there is a problem parsing the certificate
     */
    private static String extractPeerDNFromServerSSLSocket(Socket socket) throws CertificateException {
        String dn = null;
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            try {
                final Certificate[] certChains = sslSocket.getSession().getPeerCertificates();
                if (certChains != null && certChains.length > 0) {
                    X509Certificate x509Certificate = convertAbstractX509Certificate(certChains[0]);
                    dn = x509Certificate.getSubjectDN().getName().trim();
                    logger.debug("Extracted DN={} from server certificate", dn);
                }
            } catch (SSLPeerUnverifiedException e) {
                if (e.getMessage().equals(PEER_NOT_AUTHENTICATED_MSG)) {
                    logger.error("The server did not present a certificate and thus the DN cannot" +
                            " be extracted. Check that the other endpoint is providing a complete certificate chain");
                }
                throw new CertificateException(e);
            }
        }
        return dn;
    }

    private static SslContextFactory.ClientAuth getClientAuthStatus(SSLSocket sslSocket) {
        return sslSocket.getNeedClientAuth() ? SslContextFactory.ClientAuth.REQUIRED : sslSocket.getWantClientAuth() ? SslContextFactory.ClientAuth.WANT : SslContextFactory.ClientAuth.NONE;
    }

    /**
     * Accepts a legacy {@link javax.security.cert.X509Certificate} and returns an {@link X509Certificate}. The {@code javax.*} package certificate classes are for legacy compatibility and should
     * not be used for new development.
     *
     * @param legacyCertificate the {@code javax.security.cert.X509Certificate}
     * @return a new {@code java.security.cert.X509Certificate}
     * @throws CertificateException if there is an error generating the new certificate
     */
    @SuppressWarnings("deprecation")
    public static X509Certificate convertLegacyX509Certificate(javax.security.cert.X509Certificate legacyCertificate) throws CertificateException {
        if (legacyCertificate == null) {
            throw new IllegalArgumentException("The X.509 certificate cannot be null");
        }

        try {
            return formX509Certificate(legacyCertificate.getEncoded());
        } catch (javax.security.cert.CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Accepts an abstract {@link java.security.cert.Certificate} and returns an {@link X509Certificate}. Because {@code sslSocket.getSession().getPeerCertificates()} returns an array of the
     * abstract certificates, they must be translated to X.509 to replace the functionality of {@code sslSocket.getSession().getPeerCertificateChain()}.
     *
     * @param abstractCertificate the {@code java.security.cert.Certificate}
     * @return a new {@code java.security.cert.X509Certificate}
     * @throws CertificateException if there is an error generating the new certificate
     */
    public static X509Certificate convertAbstractX509Certificate(java.security.cert.Certificate abstractCertificate) throws CertificateException {
        if (abstractCertificate == null || !(abstractCertificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("The certificate cannot be null and must be an X.509 certificate");
        }

        try {
            return formX509Certificate(abstractCertificate.getEncoded());
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    private static X509Certificate formX509Certificate(byte[] encodedCertificate) throws CertificateException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedCertificate);
            return (X509Certificate) cf.generateCertificate(bais);
        } catch (CertificateException e) {
            logger.error("Error converting the certificate", e);
            throw e;
        }
    }

    /**
     * Reorders DN to the order the elements appear in the RFC 2253 table
     * <p>
     * https://www.ietf.org/rfc/rfc2253.txt
     * <p>
     * String  X.500 AttributeType
     * ------------------------------
     * CN      commonName
     * L       localityName
     * ST      stateOrProvinceName
     * O       organizationName
     * OU      organizationalUnitName
     * C       countryName
     * STREET  streetAddress
     * DC      domainComponent
     * UID     userid
     *
     * @param dn a possibly unordered DN
     * @return the ordered dn
     */
    public static String reorderDn(String dn) {
        RDN[] rdNs = new X500Name(dn).getRDNs();
        Arrays.sort(rdNs, new Comparator<RDN>() {
            @Override
            public int compare(RDN o1, RDN o2) {
                AttributeTypeAndValue o1First = o1.getFirst();
                AttributeTypeAndValue o2First = o2.getFirst();

                ASN1ObjectIdentifier o1Type = o1First.getType();
                ASN1ObjectIdentifier o2Type = o2First.getType();

                Integer o1Rank = dnOrderMap.get(o1Type);
                Integer o2Rank = dnOrderMap.get(o2Type);
                if (o1Rank == null) {
                    if (o2Rank == null) {
                        int idComparison = o1Type.getId().compareTo(o2Type.getId());
                        if (idComparison != 0) {
                            return idComparison;
                        }
                        return String.valueOf(o1Type).compareTo(String.valueOf(o2Type));
                    }
                    return 1;
                } else if (o2Rank == null) {
                    return -1;
                }
                return o1Rank - o2Rank;
            }
        });
        return new X500Name(rdNs).toString();
    }

    /**
     * Reverses the X500Name in order make the certificate be in the right order
     * [see http://stackoverflow.com/questions/7567837/attributes-reversed-in-certificate-subject-and-issuer/12645265]
     *
     * @param x500Name the X500Name created with the intended order
     * @return the X500Name reversed
     */
    private static X500Name reverseX500Name(X500Name x500Name) {
        List<RDN> rdns = Arrays.asList(x500Name.getRDNs());
        Collections.reverse(rdns);
        return new X500Name(rdns.toArray(new RDN[rdns.size()]));
    }

    /**
     * Generates a unique serial number by using the current time in milliseconds left shifted 32 bits (to make room for incrementor) with an incrementor added
     *
     * @return a unique serial number (technically unique to this classloader)
     */
    protected static synchronized BigInteger getUniqueSerialNumber() {
        final long currentTimeMillis = System.currentTimeMillis();
        final int incrementorValue;

        if (lastSerialNumberMillis != currentTimeMillis) {
            // We can only get into this block once per millisecond
            millisecondBigInteger = BigInteger.valueOf(currentTimeMillis).shiftLeft(32);
            lastSerialNumberMillis = currentTimeMillis;
            incrementorValue = 0;
            serialNumberIncrementor = 1;
        } else {
            // Already created at least one serial number this millisecond
            incrementorValue = serialNumberIncrementor++;
        }

        return millisecondBigInteger.add(BigInteger.valueOf(incrementorValue));
    }

    /**
     * Generates a self-signed {@link X509Certificate} suitable for use as a Certificate Authority.
     *
     * @param keyPair                 the {@link KeyPair} to generate the {@link X509Certificate} for
     * @param dn                      the distinguished name to user for the {@link X509Certificate}
     * @param signingAlgorithm        the signing algorithm to use for the {@link X509Certificate}
     * @param certificateDurationDays the duration in days for which the {@link X509Certificate} should be valid
     * @return a self-signed {@link X509Certificate} suitable for use as a Certificate Authority
     * @throws CertificateException if there is an generating the new certificate
     */
    public static X509Certificate generateSelfSignedX509Certificate(KeyPair keyPair, String dn, String signingAlgorithm, int certificateDurationDays)
            throws CertificateException {
        try {
            ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(certificateDurationDays));

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    reverseX500Name(new X500Name(dn)),
                    getUniqueSerialNumber(),
                    startDate, endDate,
                    reverseX500Name(new X500Name(dn)),
                    subPubKeyInfo);

            // Set certificate extensions
            // (1) digitalSignature extension
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment
                    | KeyUsage.keyAgreement | KeyUsage.nonRepudiation | KeyUsage.cRLSign | KeyUsage.keyCertSign));

            certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));

            certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

            // (2) extendedKeyUsage extension
            certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

            // Sign the certificate
            X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);
        } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Generates an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     *
     * @param dn               the distinguished name to use
     * @param publicKey        the public key to issue the certificate to
     * @param issuer           the issuer's certificate
     * @param issuerKeyPair    the issuer's keypair
     * @param signingAlgorithm the signing algorithm to use
     * @param days             the number of days it should be valid for
     * @return an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     * @throws CertificateException if there is an error issuing the certificate
     */
    public static X509Certificate generateIssuedCertificate(String dn, PublicKey publicKey, X509Certificate issuer, KeyPair issuerKeyPair, String signingAlgorithm, int days)
            throws CertificateException {
        return generateIssuedCertificate(dn, publicKey, null, issuer, issuerKeyPair, signingAlgorithm, days);
    }

    /**
     * Generates an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     *
     * @param dn               the distinguished name to use
     * @param publicKey        the public key to issue the certificate to
     * @param extensions       extensions extracted from the CSR
     * @param issuer           the issuer's certificate
     * @param issuerKeyPair    the issuer's keypair
     * @param signingAlgorithm the signing algorithm to use
     * @param days             the number of days it should be valid for
     * @return an issued {@link X509Certificate} from the given issuer certificate and {@link KeyPair}
     * @throws CertificateException if there is an error issuing the certificate
     */
    public static X509Certificate generateIssuedCertificate(String dn, PublicKey publicKey, Extensions extensions, X509Certificate issuer, KeyPair issuerKeyPair, String signingAlgorithm, int days)
            throws CertificateException {
        try {
            ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(issuerKeyPair.getPrivate());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(days));

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    reverseX500Name(new X500Name(issuer.getSubjectX500Principal().getName())),
                    getUniqueSerialNumber(),
                    startDate, endDate,
                    reverseX500Name(new X500Name(dn)),
                    subPubKeyInfo);

            certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));

            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerKeyPair.getPublic()));
            // Set certificate extensions
            // (1) digitalSignature extension
            certBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.nonRepudiation));

            certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

            // (2) extendedKeyUsage extension
            certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

            // (3) subjectAlternativeName
            if (extensions != null && extensions.getExtension(Extension.subjectAlternativeName) != null) {
                certBuilder.addExtension(Extension.subjectAlternativeName, false, extensions.getExtensionParsedValue(Extension.subjectAlternativeName));
            }

            X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);
        } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Returns true if the two provided DNs are equivalent, regardless of the order of the elements. Returns false if one or both are invalid DNs.
     * <p>
     * Example:
     * <p>
     * CN=test1, O=testOrg, C=US compared to CN=test1, O=testOrg, C=US -> true
     * CN=test1, O=testOrg, C=US compared to O=testOrg, CN=test1, C=US -> true
     * CN=test1, O=testOrg, C=US compared to CN=test2, O=testOrg, C=US -> false
     * CN=test1, O=testOrg, C=US compared to O=testOrg, CN=test2, C=US -> false
     * CN=test1, O=testOrg, C=US compared to                           -> false
     * compared to                           -> true
     *
     * @param dn1 the first DN to compare
     * @param dn2 the second DN to compare
     * @return true if the DNs are equivalent, false otherwise
     */
    public static boolean compareDNs(String dn1, String dn2) {
        if (dn1 == null) {
            dn1 = "";
        }

        if (dn2 == null) {
            dn2 = "";
        }

        if (StringUtils.isEmpty(dn1) || StringUtils.isEmpty(dn2)) {
            return dn1.equals(dn2);
        }
        try {
            List<Rdn> rdn1 = new LdapName(dn1).getRdns();
            List<Rdn> rdn2 = new LdapName(dn2).getRdns();

            return rdn1.size() == rdn2.size() && rdn1.containsAll(rdn2);
        } catch (InvalidNameException e) {
            logger.warn("Cannot compare DNs: {} and {} because one or both is not a valid DN", dn1, dn2);
            return false;
        }
    }

    /**
     * Extract extensions from CSR object
     */
    public static Extensions getExtensionsFromCSR(JcaPKCS10CertificationRequest csr) {
        Attribute[] attributess = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        for (Attribute attribute : attributess) {
            ASN1Set attValue = attribute.getAttrValues();
            if (attValue != null) {
                ASN1Encodable extension = attValue.getObjectAt(0);
                if (extension instanceof Extensions) {
                    return (Extensions) extension;
                } else if (extension instanceof DERSequence) {
                    return Extensions.getInstance(extension);
                }
            }
        }
        return null;
    }

    /**
     * Returns the JVM Java major version based on the System properties (e.g. {@code JVM 1.8.0.231} -> {code 8}).
     *
     * @return the Java major version
     */
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        return parseJavaVersion(version);
    }

    /**
     * Returns the major version parsed from the provided Java version string (e.g. {@code "1.8.0.231"} -> {@code 8}).
     *
     * @param version the Java version string
     * @return the major version as an int
     */
    public static int parseJavaVersion(String version) {
        String majorVersion;
        if (version.startsWith("1.")) {
            majorVersion = version.substring(2, 3);
        } else {
            Pattern majorVersion9PlusPattern = Pattern.compile("(\\d+).*");
            Matcher m = majorVersion9PlusPattern.matcher(version);
            if (m.find()) {
                majorVersion = m.group(1);
            } else {
                throw new IllegalArgumentException("Could not detect major version of " + version);
            }
        }
        return Integer.parseInt(majorVersion);
    }

    /**
     * Returns a {@code String[]} of supported TLS protocol versions based on the current Java platform version.
     *
     * @return the supported TLS protocol version(s)
     */
    public static String[] getCurrentSupportedTlsProtocolVersions() {
        int javaMajorVersion = getJavaVersion();
        if (javaMajorVersion < 11) {
            return JAVA_8_SUPPORTED_TLS_PROTOCOL_VERSIONS;
        } else {
            return JAVA_11_SUPPORTED_TLS_PROTOCOL_VERSIONS;
        }
    }

    /**
     * Returns the highest supported TLS protocol version based on the current Java platform version.
     *
     * @return the TLS protocol (e.g. {@code "TLSv1.2"})
     */
    public static String getHighestCurrentSupportedTlsProtocolVersion() {
        int javaMajorVersion = getJavaVersion();
        if (javaMajorVersion < 11) {
            return JAVA_8_MAX_SUPPORTED_TLS_PROTOCOL_VERSION;
        } else {
            return JAVA_11_MAX_SUPPORTED_TLS_PROTOCOL_VERSION;
        }
    }

    private CertificateUtils() {
    }
}
