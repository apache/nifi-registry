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
# Apache NiFi Registry Toolkit

This submodule is a landing zone for command line utilities that can be used for maintenance/automation of registry actions.

## Build

```
mvn clean install
```

## Flow Persistence Provider migrator usage

1. Shutdown registry
1. (Optional but recommended) Backup your registry by zipping/tarring the directory up
1. Copy providers.xml -> providers-to.xml
1. Edit providers-to.xml to reflect what you'd like to migrate to (e.g. git)
1. In registry home as working directory, run persistence-toolkit.sh -t providers-to.xml
1. Rename providers-to.xml -> providers.xml
1. Start registry back up


## Registry rebase (EXPERIMENTAL)

Initial support for rebasing a branched registry's changes on top of the upstream.

### Workflow:

#### Generate diff:
1. "Branch" the upstream registry by shutting it down, copying directory to new location.
1. Start upstream registry back up
1. Configure branched registry with new hostname, registry-aliases.xml to account for new url
1. Point NiFi at the branched registry, make some changes, decide you want to push them upstream
1. Configure conf/branch-nifi-registry-client.properties and conf/upstream-nifi-registry-client.properties to point at the branched, upstream registries
1. Generate a rebase diff
```
bin/rebase-toolkit.sh diff --upstreamPropertiesFile conf/upstream-nifi-registry-client.properties --branchPropertiesFile conf/branch-nifi-registry-client.properties --bucketId BUCKET_UUID --flowId FLOW_UUID --output output.yaml
```

#### Apply upstream:
1. Review output.yaml, opportunity for peer review as well
1. Apply rebase diff
```
bin/rebase-toolkit.sh apply --upstreamPropertiesFile conf/upstream-nifi-registry-client.properties --branchPropertiesFile conf/branch-nifi-registry-client.properties --input output.yaml
```