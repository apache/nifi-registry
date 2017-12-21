<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the 'License'); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an 'AS IS' BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType='text/html' pageEncoding='UTF-8' session='false' %>
<!DOCTYPE html>
<html>
<head>
    <title>NiFi Registry</title>
    <base href='/'>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1'>
    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>
    <link rel='shortcut icon' href='nifi-registry/images/registry-favicon.png' type='image/png'>
    <link rel='icon' href='nifi-registry/images/registry-favicon.png' type='image/png'>
    ${nf.registry.style.tags}
    <link rel='stylesheet' href='nifi-registry/node_modules/font-awesome/css/font-awesome.css'/>
</head>
<body>
<nf-registry-app></nf-registry-app>
</body>
${nf.registry.script.tags}
</html>