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
# Apache NiFi Registry [![Build Status](https://travis-ci.org/apache/nifi-registry.svg?branch=master)](https://travis-ci.org/apache/nifi-registry)

Registry—a subproject of Apache NiFi—is a complementary application that provides a central location for storage and management of shared resources across one or more instances of NiFi and/or MiNiFi.

## Table of Contents

- [Getting Started](#getting-started)
- [License](#license)

## Getting Started

### Requirements
    
* Java 1.8 (above 1.8.0_45)
* Maven 3.1.0, or newer
* Recent git client

1) Clone the repo

        git clone https://git-wip-us.apache.org/repos/asf/nifi-registry.git
        git checkout master

2) Build the project

        cd nifi-registry
        mvn clean install

    If you wish to enable style and license checks, specify the contrib-check profile:
    
        mvn clean install -Pcontrib-check
        
    If you wish to run integration tests and contrib-check, specify both profiles:
    
        mvn clean install -Pcontrib-check,integration-tests

3) Start the application

        cd nifi-registry-assembly/target/nifi-registry-<VERSION>-bin/nifi-registry-<VERSION>/
        ./bin/nifi-registry.sh start
   
   Note that the application web server can take a while to load before it is accessible.   

4) Accessing the application web UI
 
    With the default settings, the application UI will be available at [http://localhost:18080/nifi-registry](http://localhost:18080/nifi-registry) 
   
5) Accessing the application REST API

    If you wish to test against the application REST API, you can access the REST API directly. With the default settings, the base URL of the REST API will be at `http://localhost:18080/nifi-registry-api`. A UI for testing the REST API will be available at [http://localhost:18080/nifi-registry-api/swagger/ui.html](http://localhost:18080/nifi-registry-api/swagger/ui.html) 

6) Accessing the application logs

    Logs will be available in `logs/nifi-registry-app.log`

## License

Except as otherwise noted this software is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

