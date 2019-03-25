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
import org.springframework.test.context.jdbc.Sql;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SyncWithoutGitProviderIT extends UnsecuredITBase {

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:db/clearDB.sql",
            "classpath:db/BucketsIT.sql"
    })
    public void testSyncReturnsExistingBucketsWhenGitFlowRepositoryIsNotActive() {
        final Bucket[] buckets = client
                .target(createURL("sync"))
                .path("metadata")
                .request()
                .post(Entity.entity("", MediaType.WILDCARD_TYPE), Bucket[].class);

        assertNotNull(buckets);
        assertEquals(3, buckets.length);
        for (int i = 0; i < buckets.length; i++) {
            assertEquals(String.valueOf(i + 1), buckets[i].getIdentifier());
        }
    }

    @Test(expected = InternalServerErrorException.class)
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:db/clearDB.sql",
            "classpath:db/BucketsIT.sql"
    })
    public void testSyncBucketsByResettingGitRepositoryThrowsIOExceptionWhenSyncIsNotImplemented() {
        client
                .target(createURL("sync"))
                .path("remote")
                .request()
                .put(Entity.entity("https://gitrepository.com/fancy", MediaType.WILDCARD_TYPE), Bucket[].class);
    }

    @Test
    public void testGetStatusReturnsValidSyncState() {
        final org.apache.nifi.registry.sync.RepositorySyncStatus statusDto = client
                .target(createURL("sync"))
                .request()
                .get(org.apache.nifi.registry.sync.RepositorySyncStatus.class);

        assertEquals(statusDto.getIsClean(), true);
        assertEquals(statusDto.getHasChanges(), false);
        assertEquals(statusDto.getChanges().isEmpty(), true);
    }
}
