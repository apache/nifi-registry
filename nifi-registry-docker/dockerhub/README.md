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

# Docker Image Quickstart

## Capabilities
This image currently supports running in standalone mode either unsecured or with user authentication provided through:
   * [Two-Way SSL with Client Certificates](https://nifi.apache.org/docs/nifi-registry-docs/html/administration-guide.html#security-configuration)
   * [Lightweight Directory Access Protocol (LDAP)](https://nifi.apache.org/docs/nifi-registry-docs/html/administration-guide.html#ldap_identity_provider)
   
## Building
The Docker image can be built using the following command:

    . ~/Projects/nifi-dev/nifi-registry/nifi-registry-docker/dockerhub/DockerBuild.sh

This will attempt to build and tag an image matching the string in DockerImage.txt

    dockerhub dchaffey$ cat DockerImage.txt
    > apache/nifi-registry:0.1.0
    docker images
    > REPOSITORY               TAG                 IMAGE ID            CREATED             SIZE
    > apache/nifi-registry     0.1.0               751428cbf631        15 minutes ago      342MB
    
**Note**: The default version of NiFi-Registry specified by the Dockerfile is typically that of one that is unreleased if working from source.
To build an image for a prior released version, one can override the `NIFI_REGISTRY_VERSION` build-arg with the following command:
    
    docker build --build-arg=NIFI_REGISRTY_VERSION={Desired NiFi-Registry Version} -t apache/nifi-registry:latest .

There is, however, no guarantee that older versions will work as properties have changed and evolved with subsequent releases.
The configuration scripts are suitable for at least 0.1.0+.

## Running a container

### Standalone Instance, Unsecured
The minimum to run a NiFi Registry instance is as follows:

    . ~/Projects/nifi-dev/nifi-registry/nifi-registry-docker/dockerhub/DockerRun.sh
      
This will provide a running instance, exposing the instance UI to the host system on at port 18080,
viewable at `http://localhost:18080/nifi-registry`.
For a list of the environment variables recognised in this build, look into the .sh/secure.sh and .sh/start.sh scripts
        
### Standalone Instance, Two-Way SSL
In this configuration, the user will need to provide certificates and the associated configuration information.
Of particular note, is the `AUTH` environment variable which is set to `tls`.  Additionally, the user must provide an
the DN as provided by an accessing client certificate in the `INITIAL_ADMIN_IDENTITY` environment variable.
This value will be used to seed the instance with an initial user with administrative privileges.
Finally, this command makes use of a volume to provide certificates on the host system to the container instance.

    docker run --name nifi-registry \
      -v /User/bob/certs/localhost:/opt/certs \
      -p 8443:8443 \
      -e AUTH=tls \
      -e KEYSTORE_PATH=/opt/certs/keystore.jks \
      -e KEYSTORE_TYPE=JKS \
      -e KEYSTORE_PASSWORD=QKZv1hSWAFQYZ+WU1jjF5ank+l4igeOfQRp+OSbkkrs \
      -e TRUSTSTORE_PATH=/opt/certs/truststore.jks \
      -e TRUSTSTORE_PASSWORD=rHkWR1gDNW3R9hgbeRsT3OM3Ue0zwGtQqcFKJD2EXWE \
      -e TRUSTSTORE_TYPE=JKS \
      -e INITIAL_ADMIN_IDENTITY='CN=Random User, O=Apache, OU=NiFiRegistry, C=US' \
      -d \
      apache/nifi-registry:latest

### Standalone Instance, LDAP
In this configuration, the user will need to provide certificates and the associated configuration information.  Optionally,
if the LDAP provider of interest is operating in LDAPS or START_TLS modes, certificates will additionally be needed.
Of particular note, is the `AUTH` environment variable which is set to `ldap`.  Additionally, the user must provide a
DN as provided by the configured LDAP server in the `INITIAL_ADMIN_IDENTITY` environment variable. This value will be 
used to seed the instance with an initial user with administrative privileges.  Finally, this command makes use of a 
volume to provide certificates on the host system to the container instance.

#### For a minimal, connection to an LDAP server using SIMPLE authentication:

    docker run --name nifi-registry \
      -v /User/bob/certs/localhost:/opt/certs \
      -p 8443:8443 \
      -e AUTH=tls \
      -e KEYSTORE_PATH=/opt/certs/keystore.jks \
      -e KEYSTORE_TYPE=JKS \
      -e KEYSTORE_PASSWORD=QKZv1hSWAFQYZ+WU1jjF5ank+l4igeOfQRp+OSbkkrs \
      -e TRUSTSTORE_PATH=/opt/certs/truststore.jks \
      -e TRUSTSTORE_PASSWORD=rHkWR1gDNW3R9hgbeRsT3OM3Ue0zwGtQqcFKJD2EXWE \
      -e TRUSTSTORE_TYPE=JKS \
      -e INITIAL_ADMIN_IDENTITY='cn=admin,dc=example,dc=org' \
      -e LDAP_AUTHENTICATION_STRATEGY='SIMPLE' \
      -e LDAP_MANAGER_DN='cn=admin,dc=example,dc=org' \
      -e LDAP_MANAGER_PASSWORD='password' \
      -e LDAP_USER_SEARCH_BASE='dc=example,dc=org' \
      -e LDAP_USER_SEARCH_FILTER='cn={0}' \
      -e LDAP_IDENTITY_STRATEGY='USE_DN' \
      -e LDAP_URL='ldap://ldap:389' \
      -d \
      apache/nifi-registry:latest

#### The following, optional environment variables may be added to the above command when connecting to a secure  LDAP server configured with START_TLS or LDAPS

    -e LDAP_TLS_KEYSTORE: ''
    -e LDAP_TLS_KEYSTORE_PASSWORD: ''
    -e LDAP_TLS_KEYSTORE_TYPE: ''
    -e LDAP_TLS_TRUSTSTORE: ''
    -e LDAP_TLS_TRUSTSTORE_PASSWORD: ''
    -e LDAP_TLS_TRUSTSTORE_TYPE: ''

## Configuration Information
The following ports are specified by default in Docker for NiFi-Registry operation within the container and 
can be published to the host.

| Function                 | Property                      | Port  |
|--------------------------|-------------------------------|-------|
| HTTP Port                | nifi.web.http.port            | 18080  |
| HTTPS Port               | nifi.web.https.port           | 18443  |

The following base image is used for NiFi-Registry, which differs from the NiFi Docker image:
    
    openjdk:8-jdk-slim