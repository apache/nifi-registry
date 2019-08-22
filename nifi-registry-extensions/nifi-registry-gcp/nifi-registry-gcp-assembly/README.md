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
# NiFi Registry GCP extensions

This modules provides GCP related extensions for NiFi Registry.

## Prerequisites

* GCP service account credentials in a JSON file and a GCS (Google Cloud Storage) bucket.

## How to install

### Enable GCP extensions at NiFi Registry build

The GCP extensions will be automatically included when you build NiFi Registry and will be installed at the `${NIFI_REG_HOME}/ext/gcp` directory.

If you wish to build NiFi Registry without including the GCP extensions, specify the `skipGcp` system property:
```
cd nifi-registry
mvn clean install -DskipGcp
```

### Add GCP extensions to existing NiFi Registry

To add GCP extensions to an existing NiFi Registry, build the extension with the following command:

```
cd nifi-registry
mvn clean install -f nifi-registry-extensions/nifi-registry-gcp
```

The extension zip will be created as `nifi-registry-extensions/nifi-registry-gcp/nifi-registry-gcp-assembly/target/nifi-registry-gcp-assembly-xxx-bin.zip`.

Unzip the file into arbitrary directory so that NiFi Registry can use, such as `${NIFI_REG_HOME}/ext/gcp`.
For example:

```
mkdir -p ${NIFI_REG_HOME}/ext/gcp
unzip -d ${NIFI_REG_HOME}/ext/gcp nifi-registry-extensions/nifi-registry-gcp/nifi-registry-gcp-assembly/target/nifi-registry-gcp-assembly-xxx-bin.zip
```

## NiFi Registry Configuration

In order to use this extension, the following NiFi Registry files need to be configured.

### nifi-registry.properties

The extension dir property will be automatically configured when building with the `include-gcp` profile (i.e. when not specifying -DskipGcp).

To manually specify the property when adding the GCP extensions to an existing NiFi registry, configure the following property:
```
# Specify GCP extension dir
nifi.registry.extension.dir.gcp=./ext/gcp/lib
```

### providers.xml

Uncomment the extensionBundlePersistenceProvider for GCP:
```
<!--
<extensionBundlePersistenceProvider>
    <class>org.apache.nifi.registry.gcp.GCPBundlePersistenceProvider</class>
    <property name="Bucket Name">my-bundles</property>
    <property name="Key Prefix"></property>
    <property name="Service Account Credentials"></property>
</extensionBundlePersistenceProvider>
-->
```

NOTE: Remember to remove, or comment out, the FileSystemBundlePersistenceProvider since there can only be one defined.

