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
import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.provider.flow.git.GitFlowPersistenceProvider;
import org.apache.nifi.registry.provider.flow.git.GitFlowPersistenceTestDataFactory;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@ActiveProfiles("WithGitProvider")
public class SyncIT extends UnsecuredITBase {
    //TODO thread safety with parallel test execution?
    private static GitFlowPersistenceProvider gitFlowPersistenceProviderMock;


    @Configuration
    @Profile({"WithGitProvider"})
    public static class MockFlowPersistenceProvider {

        @Primary
        @Bean
        public FlowPersistenceProvider getGitFlowPersistenceProvider() {
            if (gitFlowPersistenceProviderMock == null)
                gitFlowPersistenceProviderMock = mock(GitFlowPersistenceProvider.class);
            return gitFlowPersistenceProviderMock;
        }
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:db/clearDB.sql",
            "classpath:db/BucketsIT.sql"
    })
    public void testSyncDeletesExistingBucketsWhenGitRepositoryIsEmpty() {
        final Bucket[] buckets = client
                .target(createURL("sync"))
                .path("metadata")
                .request()
                .post(Entity.entity("", MediaType.WILDCARD_TYPE), Bucket[].class);

        assertNotNull(buckets);
        assertEquals(0, buckets.length);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:db/clearDB.sql",
            "classpath:db/BucketsIT.sql"
    })
    public void testSyncBucketsWithFilledGitRepository() {
        Collection<VersionedFlowSnapshot> snapshots = GitFlowPersistenceTestDataFactory.createSampleFlowSnapshots();
        when(gitFlowPersistenceProviderMock.getFlowSnapshots()).thenReturn(snapshots);

        final Bucket[] buckets = client
                .target(createURL("sync"))
                .path("metadata")
                .request()
                .post(Entity.entity("", MediaType.WILDCARD_TYPE), Bucket[].class);

        assertNotNull(buckets);
        assertEquals(10, buckets.length);
        assertBuckets(
                GitFlowPersistenceTestDataFactory.createExpectedBuckets(buckets.length),
                buckets);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:db/clearDB.sql",
            "classpath:db/BucketsIT.sql"
    })
    public void testSyncBucketsByResettingGitRepository() throws URISyntaxException, IOException {
        Collection<VersionedFlowSnapshot> snapshots = GitFlowPersistenceTestDataFactory.createSampleFlowSnapshots();
        when(gitFlowPersistenceProviderMock.getFlowSnapshots()).thenReturn(snapshots);
        when(gitFlowPersistenceProviderMock.canBeSynchronized()).thenReturn(true);

        final Bucket[] buckets = client
                .target(createURL("sync"))
                .path("remote")
                .request()
                .put(Entity.entity("https://gitrepository.com/fancy", MediaType.WILDCARD_TYPE), Bucket[].class);

        verify(gitFlowPersistenceProviderMock).resetRepository(new URI("https://gitrepository.com/fancy"));
        assertNotNull(buckets);
        assertEquals(10, buckets.length);
        assertBuckets(
                GitFlowPersistenceTestDataFactory.createExpectedBuckets(buckets.length),
                buckets);
    }

    private void assertBuckets(Bucket[] expectedBuckets, Bucket[] actual) {
        for (int i = 0; i < expectedBuckets.length; i++) {
            assertEquals(expectedBuckets[i].getIdentifier(), actual[i].getIdentifier());
            assertEquals(expectedBuckets[i].getName(), actual[i].getName());
        }
    }
}
