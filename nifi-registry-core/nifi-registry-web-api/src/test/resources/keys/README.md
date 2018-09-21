<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Integration Test Keys

The integration tests that run a secure NiFi require keystores and truststores for the server and client in order
to establish a two-way TLS connection.

The keys/certs for these tests were generated with the tls-toolkit included with NiFi Toolkit v1.4.0.

The steps for generating replacements are:

    # use NiFi tls-toolkit to generate CA, server key/cert, client key/cert
    ./nifi-toolkit-1.4.0/bin/tls-toolkit.sh standalone --certificateAuthorityHostname localhost --hostnames localhost --nifiDnSuffix ", OU=nifi" --keyStorePassword localhostKeystorePassword --trustStorePassword localhostTruststorePassword --clientCertDn "CN=user1, OU=nifi" --clientCertPassword u1Pass --days 3650 --outputDirectory nifireg-integrationtest

    # change to tls-toolkit output directory
    cd ./nifireg-integrationtest

    # copy server's key/trust stores
    mkdir keys
    cp localhost/keystore.jks keys/localhost-ks.jks
    cp localhost/truststore.jks keys/localhost-ts.jks

    # create a Java Key Store (JKS) from the client key
    keytool -importkeystore -destkeystore keys/client-ks.jks -deststorepass clientKeystorePassword -destkeypass u1Pass -srckeystore CN=user1_OU=nifi.p12 -srcstorepass u1Pass -srcstoretype PKCS12


You should now have a directory with the following contents:

    keys/
     +-- client-ks.jks      # client keystore: keystorePass=clientKeystorePassword, keyPass=u1Pass
     +-- localhost-ks.jks   # server keystore: keystorePass=localhostKeystorePassword, keyPass=localhostKeystorePassword
     +-- localhost-ts.jks   # server/client truststore (contains CA): truststorePass=localhostTruststorePassword

Copy these files to the test/resources/keys/ directory.

