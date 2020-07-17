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

import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.nifi.registry.toolkit.rebase.TestFlowSnapshot.createAndAddProcessor;
import static org.junit.Assert.assertEquals;

public class PropertyOperationTest {
    private Map<String, String> valueA;
    private Map<String, String> valueB;

    @Before
    public void setup() {
        valueA = new HashMap<>();
        valueA.put("key1", "val1");
        valueA.put("key2", "val2");
        valueA.put("key3", "val3");

        valueB = new HashMap<>(valueA);
    }

    @Test
    public void testSetExistingKey() {
        valueB.put("key1", "newVal");
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void testSetNewKey() {
        valueB.put("key4", "newVal");
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void testRemoveKey() {
        valueB.remove("key1");
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void testSetNewKeyNull() {
        valueB.put("key4", null);
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void testSetExistingKeyNull() {
        valueB.put("key1", null);
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void testClearProperties() {
        valueB.clear();
        testApplyMakesAEqualBRemovingNulls();
    }

    @Test
    public void flowTest() throws IOException, NiFiRegistryException {
        VersionedFlowSnapshot base = TestFlowSnapshot.createBase();

        String id = "test-id";
        VersionedProcessGroup flowContents = base.getFlowContents();

        VersionedProcessor baseProcessor = createAndAddProcessor(id, flowContents);
        baseProcessor.getProperties().put("key1", "value1");

        TestFlowSnapshot testFlowSnapshot = new TestFlowSnapshot(base);

        VersionedProcessor branchProcessor = testFlowSnapshot.getBranch().getFlowContents().getProcessors().iterator().next();
        branchProcessor.getProperties().put("key1", "value1a");
        branchProcessor.getProperties().put("key2", "value2");

        VersionedProcessor upstreamProcessor = testFlowSnapshot.getUpstream().getFlowContents().getProcessors().iterator().next();
        upstreamProcessor.getProperties().put("key1", "value1a");
        upstreamProcessor.getProperties().put("key3", "value3");

        VersionedProcessor expected = TestFlowSnapshot.deepCopy(branchProcessor);
        // Check that master value is preserved, key1 and key2 should still be present
        expected.getProperties().put("key3", "value3");

        VersionedFlowSnapshot reconciled = testFlowSnapshot.reconcile();

        testFlowSnapshot.assertEqualsWithNormalizedRootPg(expected, reconciled.getFlowContents().getProcessors().iterator().next());
    }

    private void testApplyMakesAEqualBRemovingNulls() {
        Map<String, String> result = new HashMap<>(valueA);
        ((PropertyOperation) RoundTrip.testRoundTrip(MergeOperation.class, new PropertyOperation(valueA, valueB))).apply(result);
        Map<String, String> valueBNoNulls = valueB.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(valueBNoNulls, result);
    }
}
