-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- test data for buckets

insert into bucket (id, name, description, created)
  values ('1', 'Bucket 1', 'This is test bucket 1', parsedatetime('2017-09-11 12:51:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));

insert into bucket (id, name, description, created)
  values ('2', 'Bucket 2', 'This is test bucket 2', parsedatetime('2017-09-11 12:52:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));

insert into bucket (id, name, description, created)
  values ('3', 'Bucket 3', 'This is test bucket 3', parsedatetime('2017-09-11 12:53:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));

insert into bucket (id, name, description, created)
  values ('4', 'Bucket 4', 'This is test bucket 4', parsedatetime('2017-09-11 12:54:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));

insert into bucket (id, name, description, created)
  values ('5', 'Bucket 5', 'This is test bucket 5', parsedatetime('2017-09-11 12:55:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));

insert into bucket (id, name, description, created)
  values ('6', 'Bucket 6', 'This is test bucket 6', parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'));


-- test data for flows

insert into bucket_item (id, name, description, created, modified, item_type, bucket_id)
  values ('1', 'Flow 1', 'This is flow 1 bucket 1', parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'FLOW', '1');

insert into flow (id) values ('1');

insert into bucket_item (id, name, description, created, modified, item_type, bucket_id)
  values ('2', 'Flow 2', 'This is flow 2 bucket 1', parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'FLOW', '1');

insert into flow (id) values ('2');

insert into bucket_item (id, name, description, created, modified, item_type, bucket_id)
  values ('3', 'Flow 1', 'This is flow 1 bucket 2', parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), parsedatetime('2017-09-11 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'FLOW', '2');

insert into flow (id) values ('3');


-- test data for flow snapshots

insert into flow_snapshot (flow_id, version, created, created_by, comments)
  values ('1', 1, parsedatetime('2017-09-11 12:57:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'user1', 'This is flow 1 snapshot 1');

insert into flow_snapshot (flow_id, version, created, created_by, comments)
  values ('1', 2, parsedatetime('2017-09-11 12:58:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'user1', 'This is flow 1 snapshot 2');

insert into flow_snapshot (flow_id, version, created, created_by, comments)
  values ('1', 3, parsedatetime('2017-09-11 12:59:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'), 'user1', 'This is flow 1 snapshot 3');


-- test data for signing keys

insert into signing_key (id, tenant_identity, key_value)
  values ('1', 'unit_test_tenant_identity', '0123456789abcdef');

-- test data for extension bundles

-- processors bundle, depends on service api bundle
insert into bucket_item (
  id,
  name,
  description,
  created,
  modified,
  item_type,
  bucket_id
) values (
  'eb1',
  'nifi-example-processors-nar',
  'Example processors bundle',
  parsedatetime('2018-11-02 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  parsedatetime('2018-11-02 12:56:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'BUNDLE',
  '3'
);

insert into bundle (
  id,
  bucket_id,
  bundle_type,
  group_id,
  artifact_id
) values (
  'eb1',
  '3',
  'NIFI_NAR',
  'org.apache.nifi',
  'nifi-example-processors-nar'
);

insert into bundle_version (
  id,
  bundle_id,
  version,
  created,
  created_by,
  description,
  sha_256_hex,
  sha_256_supplied,
  content_size
) values (
  'eb1-v1',
  'eb1',
  '1.0.0',
  parsedatetime('2018-11-02 13:00:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'user1',
  'First version of eb1',
  '123456789',
  '1',
  1024
);

insert into bundle_version_dependency (
  id,
  bundle_version_id,
  group_id,
  artifact_id,
  version
) values (
  'eb1-v1-dep1',
  'eb1-v1',
  'org.apache.nifi',
  'nifi-example-service-api-nar',
  '2.0.0'
);

-- service impl bundle, depends on service api bundle
insert into bucket_item (
  id,
  name,
  description,
  created,
  modified,
  item_type,
  bucket_id
) values (
  'eb2',
  'nifi-example-services-nar',
  'Example services bundle',
  parsedatetime('2018-11-02 12:57:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  parsedatetime('2018-11-02 12:57:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'BUNDLE',
  '3'
);

insert into bundle (
  id,
  bucket_id,
  bundle_type,
  group_id,
  artifact_id
) values (
  'eb2',
  '3',
  'NIFI_NAR',
  'com.foo',
  'nifi-example-services-nar'
);

insert into bundle_version (
  id,
  bundle_id,
  version,
  created,
  created_by,
  description,
  sha_256_hex,
  sha_256_supplied,
  content_size
) values (
  'eb2-v1',
  'eb2',
  '1.0.0',
  parsedatetime('2018-11-02 13:00:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'user1',
  'First version of eb2',
  '123456789',
  '1',
  1024
);

insert into bundle_version_dependency (
  id,
  bundle_version_id,
  group_id,
  artifact_id,
  version
) values (
  'eb2-v1-dep1',
  'eb2-v1',
  'org.apache.nifi',
  'nifi-example-service-api-nar',
  '2.0.0'
);

-- service api bundle
insert into bucket_item (
  id,
  name,
  description,
  created,
  modified,
  item_type,
  bucket_id
) values (
  'eb3',
  'nifi-example-service-api-nar',
  'Example service API bundle',
  parsedatetime('2018-11-02 12:58:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  parsedatetime('2017-11-02 12:58:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'BUNDLE',
  '3'
);

insert into bundle (
  id,
  bucket_id,
  bundle_type,
  group_id,
  artifact_id
) values (
  'eb3',
  '3',
  'NIFI_NAR',
  'org.apache.nifi',
  'nifi-example-service-api-nar'
);

insert into bundle_version (
  id,
  bundle_id,
  version,
  created,
  created_by,
  description,
  sha_256_hex,
  sha_256_supplied,
  content_size
) values (
  'eb3-v1',
  'eb3',
  '2.0.0',
  parsedatetime('2018-11-02 13:00:00.000 UTC', 'yyyy-MM-dd hh:mm:ss.SSS z'),
  'user1',
  'First version of eb3',
  '123456789',
  '1',
  1024
);

-- test data for extensions

insert into extension (
  id, bundle_version_id, name, display_name, type, content, has_additional_details
) values (
  'e1', 'eb1-v1', 'org.apache.nifi.ExampleProcessor', 'ExampleProcessor', 'PROCESSOR', '{ "name" : "org.apache.nifi.ExampleProcessor", "type" : "PROCESSOR" }', 0
);

insert into extension (
  id, bundle_version_id, name, display_name, type, content, has_additional_details
) values (
  'e2', 'eb1-v1', 'org.apache.nifi.ExampleProcessorRestricted', 'ExampleProcessorRestricted', 'PROCESSOR', '{ "name" : "org.apache.nifi.ExampleProcessorRestricted", "type" : "PROCESSOR" }', 0
);

insert into extension (
  id, bundle_version_id, name, display_name, type, content, additional_details, has_additional_details
) values (
  'e3', 'eb2-v1', 'org.apache.nifi.ExampleService', 'ExampleService', 'CONTROLLER_SERVICE', '{ "name" : "org.apache.nifi.ExampleService", "type" : "CONTROLLER_SERVICE" }', 'extra docs', 1
);

-- test data for extension restrictions

insert into extension_restriction (
  id, extension_id, required_permission, explanation
) values (
  'er1', 'e2', 'write filesystem', 'This writes to the filesystem'
);

-- test data for extension provided service apis

insert into extension_provided_service_api (
  id, extension_id, class_name, group_id, artifact_id, version
) values (
  'epapi1', 'e3', 'org.apache.nifi.ExampleServiceAPI', 'org.apache.nifi', 'nifi-example-service-api-nar', '2.0.0'
);

-- test data for extension tags

insert into extension_tag (extension_id, tag) values ('e1', 'example');
insert into extension_tag (extension_id, tag) values ('e1', 'processor');

insert into extension_tag (extension_id, tag) values ('e2', 'example');
insert into extension_tag (extension_id, tag) values ('e2', 'processor');
insert into extension_tag (extension_id, tag) values ('e2', 'restricted');

insert into extension_tag (extension_id, tag) values ('e3', 'example');
insert into extension_tag (extension_id, tag) values ('e3', 'service');