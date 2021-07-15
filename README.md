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
[<img src="https://nifi.apache.org/assets/images/registry-logo.png" width="360" height="126" alt="Apache NiFi Registry" />][nifi-registry]

[![Docker pulls](https://img.shields.io/docker/pulls/apache/nifi-registry.svg)](https://hub.docker.com/r/apache/nifi-registry/)
[![Version](https://img.shields.io/maven-central/v/org.apache.nifi.registry/nifi-registry-framework.svg)](https://nifi.apache.org/registry)
[![Slack](https://img.shields.io/badge/chat-on%20Slack-brightgreen.svg)](https://s.apache.org/nifi-community-slack)

Registry—a subproject of Apache NiFi—is a complementary application that provides a central location for storage and management of shared resources across one or more instances of NiFi and/or MiNiFi.

## NOTICE: This subproject has been moved under the [Apache NiFi](https://github.com/apache/nifi) codebase as nifi-registry. See [NIFI-8528](https://issues.apache.org/jira/browse/NIFI-8528) for details

This repository is read-only and serves as an archive. All existing Pull Requests (PRs) should be resubmitted against the NiFi repository's main branch. Pull Requests can be ported 
to the NiFi repo by adding ".patch" to the URL of your PR, downloading the patch, and applying it to a branch in your fork of the NiFi repo,
for example:

`git checkout -b NIFIREG-406`

`git apply --directory='nifi-registry' ~/289.patch`

[nifi-registry]: https://nifi.apache.org/registry