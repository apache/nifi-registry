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
package org.apache.nifi.registry.toolkit.rebase.merge;

import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedFlowCoordinates;
import org.apache.nifi.registry.toolkit.rebase.RebaseApplicationContext;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BranchCoordinatesOperationTest {
    private RebaseApplicationContext rebaseApplicationContext;
    private VersionedComponentCollectionTest.PgMocker pgMocker;
    private VersionedFlowCoordinates versionedFlowCoordinates;

    @Before
    public void setup() {
        rebaseApplicationContext = mock(RebaseApplicationContext.class);
        pgMocker = new VersionedComponentCollectionTest.PgMocker();

        versionedFlowCoordinates = mock(VersionedFlowCoordinates.class);
        when(pgMocker.getGrandChildProcessGroup().getVersionedFlowCoordinates()).thenReturn(versionedFlowCoordinates);
    }

    @Test
    public void testApply() {
        String testBucketId = "testBucketId";
        String testFlowId = "testFlowId";
        int testVersion = 9;

        when(rebaseApplicationContext.getReconciledVersion(testBucketId, testFlowId)).thenReturn(testVersion);

        RoundTrip.testRoundTrip(MergeOperation.class, new BranchCoordinatesOperation(testBucketId, testFlowId)).apply(rebaseApplicationContext, pgMocker.getRootProcessGroup(),
                pgMocker.getChildProcessGroup().getIdentifier(), ComponentType.PROCESS_GROUP, pgMocker.getGrandChildProcessGroup().getIdentifier());

        verify(versionedFlowCoordinates).setVersion(testVersion);
    }

    @Test(expected = IllegalStateException.class)
    public void testApplyCantFindPg() {
        String testBucketId = "testBucketId";
        String testFlowId = "testFlowId";
        int testVersion = 9;

        when(rebaseApplicationContext.getReconciledVersion(testBucketId, testFlowId)).thenReturn(testVersion);

        RoundTrip.testRoundTrip(MergeOperation.class, new BranchCoordinatesOperation(testBucketId, testFlowId)).apply(rebaseApplicationContext, pgMocker.getRootProcessGroup(),
                pgMocker.getChildProcessGroup().getIdentifier(), ComponentType.PROCESS_GROUP, "wrong-component-id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongComponentType() {
        String testBucketId = "testBucketId";
        String testFlowId = "testFlowId";

        RoundTrip.testRoundTrip(MergeOperation.class, new BranchCoordinatesOperation(testBucketId, testFlowId)).apply(rebaseApplicationContext, pgMocker.getRootProcessGroup(),
                pgMocker.getChildProcessGroup().getIdentifier(), ComponentType.LABEL, pgMocker.getGrandChildProcessGroup().getIdentifier());
    }

    @Test
    public void testRoundTrip() {
        RoundTrip.testRoundTrip(MergeOperation.class, new BranchCoordinatesOperation("testBucketId", "testFlowId"));
    }
}
