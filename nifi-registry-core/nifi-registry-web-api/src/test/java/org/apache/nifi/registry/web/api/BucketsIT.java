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
package org.apache.nifi.registry.web.api;

import org.apache.nifi.registry.bucket.Bucket;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.jdbc.Sql;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.apache.nifi.registry.web.api.IntegrationTestUtils.assertBucketsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BucketsIT extends UnsecuredITBase {

    @Test
    public void testGetBucketsEmpty() throws Exception {

        // Given: a fresh context server with an empty DB
        // When: the /buckets endpoint is queried

        final Bucket[] buckets = client
                .target(createURL("buckets"))
                .request()
                .get(Bucket[].class);

        // Then: an empty array is returned

        assertNotNull(buckets);
        assertEquals(0, buckets.length);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:db/clearDB.sql", "classpath:db/BucketsIT.sql"})
    public void testGetBuckets() throws Exception {

        // Given: these buckets have been populated in the DB (see BucketsIT.sql)

        String expected = "[" +
                "{\"identifier\":\"1\"," +
                "\"name\":\"Bucket 1\"," +
                "\"createdTimestamp\":1505091060000," +
                "\"description\":\"This is test bucket 1\"," +
                "\"permissions\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"link\":{\"params\":{\"rel\":\"self\"},\"href\":\"buckets/1\"}}," +
                "{\"identifier\":\"2\"," +
                "\"name\":\"Bucket 2\"," +
                "\"createdTimestamp\":1505091120000," +
                "\"description\":\"This is test bucket 2\"," +
                "\"permissions\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"link\":{\"params\":{\"rel\":\"self\"},\"href\":\"buckets/2\"}}," +
                "{\"identifier\":\"3\"," +
                "\"name\":\"Bucket 3\"," +
                "\"createdTimestamp\":1505091180000," +
                "\"description\":\"This is test bucket 3\"," +
                "\"permissions\":{\"canRead\":true,\"canWrite\":true,\"canDelete\":true}," +
                "\"link\":{\"params\":{\"rel\":\"self\"},\"href\":\"buckets/3\"}}" +
                "]";

        // When: the /buckets endpoint is queried

        String bucketsJson = client
                .target(createURL("buckets"))
                .request()
                .get(String.class);

        // Then: the pre-populated list of buckets is returned

        JSONAssert.assertEquals(expected, bucketsJson, false);
        assertTrue(!bucketsJson.contains("null")); // JSON serialization from the server should not include null fields, such as "versionedFlows": null
    }

    @Test
    public void testGetNonexistentBucket() throws Exception {
        // Given: a fresh context server with an empty DB
        // When: any /buckets/{id} endpoint is queried
        Response response = client.target(createURL("buckets/a-nonexistent-identifier")).request().get();

        // Then: a 404 response status is returned
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCreateBucketGetBucket() throws Exception {

        // Given:

        long testStartTime = System.currentTimeMillis();
        final Bucket bucket = new Bucket();
        bucket.setName("Integration Test Bucket");
        bucket.setDescription("A bucket created by an integration test.");

        // When: a bucket is created on the server

        Bucket createdBucket = client
                .target(createURL("buckets"))
                .request()
                .post(Entity.entity(bucket, MediaType.APPLICATION_JSON), Bucket.class);

        // Then: the server returns the created bucket, with server-set fields populated correctly

        assertBucketsEqual(bucket, createdBucket, false);
        assertNotNull(createdBucket.getIdentifier());
        assertTrue(createdBucket.getCreatedTimestamp() - testStartTime > 0L); // both server and client in same JVM, so there shouldn't be skew
        assertNotNull(createdBucket.getLink());
        assertNotNull(createdBucket.getLink().getUri());

        // And when /buckets is queried, then the newly created bucket is returned in the list

        final Bucket[] buckets = client
                .target(createURL("buckets"))
                .request()
                .get(Bucket[].class);
        assertNotNull(buckets);
        assertEquals(1, buckets.length);
        assertBucketsEqual(createdBucket, buckets[0], true);

        // And when the link URI is queried, then the newly created bucket is returned

        final Bucket bucketByLink = client
                .target(createURL(buckets[0].getLink().getUri().toString()))
                .request()
                .get(Bucket.class);
        assertBucketsEqual(createdBucket, bucketByLink, true);

        // And when the bucket is queried by /buckets/ID, then the newly created bucket is returned

        final Bucket bucketById = client
                .target(createURL("buckets/" + createdBucket.getIdentifier()))
                .request()
                .get(Bucket.class);
        assertBucketsEqual(createdBucket, bucketById, true);
    }

    @Test
    public void testUpdateBucket() throws Exception {

        // Given: a bucket exists on the server

        final Bucket bucket = new Bucket();
        bucket.setName("Integration Test Bucket");
        bucket.setDescription("A bucket created by an integration test.");
        Bucket createdBucket = client
                .target(createURL("buckets"))
                .request()
                .post(Entity.entity(bucket, MediaType.APPLICATION_JSON), Bucket.class);

        // When: the bucket is modified by the client and updated on the server

        createdBucket.setName("Renamed Bucket");
        createdBucket.setDescription("This bucket has been updated by an integration test.");

        final Bucket updatedBucket = client
                .target(createURL("buckets/" + createdBucket.getIdentifier()))
                .request()
                .put(Entity.entity(createdBucket, MediaType.APPLICATION_JSON), Bucket.class);

        // Then: the server returns the updated bucket

        assertBucketsEqual(createdBucket, updatedBucket, true);

    }

    @Test
    public void testDeleteBucket() throws Exception {

        // Given: a bucket has been created

        final Bucket bucket = new Bucket();
        bucket.setName("Integration Test Bucket");
        bucket.setDescription("A bucket created by an integration test.");

        Bucket createdBucket = client
                .target(createURL("buckets"))
                .request()
                .post(Entity.entity(bucket, MediaType.APPLICATION_JSON), Bucket.class);

        // When: that bucket deleted

        final Bucket deletedBucket = client
                .target(createURL("buckets/" + createdBucket.getIdentifier()))
                .request()
                .delete(Bucket.class);

        // Then: the body of the server response matches the bucket that was deleted
        //  and: the bucket is no longer accessible (resource not found)

        createdBucket.setPermissions(null); // authorizedActions will not be present in deletedBucket
        createdBucket.setLink(null); // links will not be present in deletedBucket
        assertBucketsEqual(createdBucket, deletedBucket, true);

        final Response response = client
                .target(createURL("buckets/" + createdBucket.getIdentifier()))
                .request()
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void getBucketFields() throws Exception {

        // Given: the server is configured to return this fixed response

        String expected = "{\"fields\":[\"ID\",\"NAME\",\"DESCRIPTION\",\"CREATED\"]}";

        // When: the server is queried

        String bucketFieldsJson = client
                .target(createURL("buckets/fields"))
                .request()
                .get(String.class);

        // Then: the fixed response is returned to the client

        JSONAssert.assertEquals(expected, bucketFieldsJson, false);

    }

}
