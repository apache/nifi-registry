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
# NiFi Registry AWS extensions

This modules provides AWS related extensions for NiFi Registry.

## Prerequisites

* AWS account credentials and an S3 bucket.

## How to install

### Enable AWS extensions at NiFi Registry build

In order to enable AWS extensions when you build NiFi Registry, specify `include-aws` profile with a maven install command:

```
cd nifi-registry
mvn clean install -Paws
```

Then the extension will be installed at `${NIFI_REG_HOME}/ext/aws` directory.

### Add AWS extensions to existing NiFi Registry

Alternatively, you can add AWS extensions to an existing NiFi Registry.
To do so, build the extension with the following command:

```
cd nifi-registry
mvn clean install -f nifi-registry-extensions/nifi-registry-aws
```

The extension zip will be created as `nifi-registry-extensions/nifi-registry-aws/nifi-registry-aws-assembly/target/nifi-registry-aws-assembly-xxx-bin.zip`.

Unzip the file into arbitrary directory so that NiFi Registry can use, such as `${NIFI_REG_HOME}/ext/aws`.
For example:

```
mkdir -p ${NIFI_REG_HOME}/ext/aws
unzip -d ${NIFI_REG_HOME}/ext/aws nifi-registry-extensions/nifi-registry-aws/nifi-registry-aws-assembly/target/nifi-registry-aws-assembly-xxx-bin.zip
```

## NiFi Registry Configuration

In order to use this extension, following NiFi Registry files need to be configured.

### nifi-registry.properties

```
# Specify AWS extension dir
nifi.registry.extension.dir.aws=./ext/aws/lib
```

### providers.xml

Uncomment the extensionBundlePersistenceProvider for S3:
```
<!-- Example S3 Bundle Persistence Provider
            - Requires nifi-registry-aws-assembly to be added to the classpath via a custom extension dir in nifi-registry.properties
                Example: nifi.registry.extension.dir.aws=./ext/aws/lib
                Where "./ext/aws/lib" contains the extracted contents of nifi-registry-aws-assembly
            - "Region" - The name of the S3 region where the bucket exists
            - "Bucket Name" - The name of an existing bucket to store extension bundles
            - "Key Prefix" - An optional prefix that if specified will be added to the beginning of all S3 keys
            - "Credentials Provider" - Indicates how credentials will be provided, must be a value of DEFAULT_CHAIN or STATIC
                - DEFAULT_CHAIN will consider in order: Java system properties, environment variables, credential profiles (~/.aws/credentials)
                - STATIC requires that "Access Key" and "Secret Access Key" be specified directly in this file
            - "Access Key" - The access key to use when using STATIC credentials provider
            - "Secret Access Key" - The secret access key to use when using STATIC credentials provider
     -->
    <!--
    <extensionBundlePersistenceProvider>
        <class>org.apache.nifi.registry.aws.S3BundlePersistenceProvider</class>
        <property name="Region">us-east-1</property>
        <property name="Bucket Name">my-bundles</property>
        <property name="Key Prefix"></property>
        <property name="Credentials Provider">DEFAULT_CHAIN</property>
        <property name="Access Key"></property>
        <property name="Secret Access Key"></property>
    </extensionBundlePersistenceProvider>
    -->
```

NOTE: Remember to remove, or comment out the FileSystemBundlePersistenceProvider since there can only be one defined.

