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
package org.apache.nifi.registry.web.link;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestLinkService {

    private LinkService linkService;

    private List<Bucket> buckets;
    private List<VersionedFlow> flows;
    private List<VersionedFlowSnapshotMetadata> snapshots;
    private List<BucketItem> items;

    @Before
    public void setup() {
        linkService = new LinkService();

        // setup buckets
        final Bucket bucket1 = new Bucket();
        bucket1.setIdentifier("b1");
        bucket1.setName("Bucket 1");

        final Bucket bucket2 = new Bucket();
        bucket2.setIdentifier("b2");
        bucket2.setName("Bucket 2");

        buckets = new ArrayList<>();
        buckets.add(bucket1);
        buckets.add(bucket2);

        // setup flows
        final VersionedFlow flow1 = new VersionedFlow();
        flow1.setIdentifier("f1");
        flow1.setName("Flow 1");
        flow1.setBucketIdentifier(bucket1.getIdentifier());

        final VersionedFlow flow2 = new VersionedFlow();
        flow2.setIdentifier("f2");
        flow2.setName("Flow 2");
        flow2.setBucketIdentifier(bucket1.getIdentifier());

        flows = new ArrayList<>();
        flows.add(flow1);
        flows.add(flow2);

        //setup snapshots
        final VersionedFlowSnapshotMetadata snapshotMetadata1 = new VersionedFlowSnapshotMetadata();
        snapshotMetadata1.setFlowIdentifier(flow1.getIdentifier());
        snapshotMetadata1.setVersion(1);

        final VersionedFlowSnapshotMetadata snapshotMetadata2 = new VersionedFlowSnapshotMetadata();
        snapshotMetadata2.setFlowIdentifier(flow1.getIdentifier());
        snapshotMetadata2.setVersion(2);

        snapshots = new ArrayList<>();
        snapshots.add(snapshotMetadata1);
        snapshots.add(snapshotMetadata2);

        // setup items
        items = new ArrayList<>();
        items.add(flow1);
        items.add(flow2);
    }

    @Test
    public void testPopulateBucketLinks() {
        buckets.stream().forEach(b -> Assert.assertNull(b.getLink()));
        linkService.populateBucketLinks(buckets);
        buckets.stream().forEach(b -> Assert.assertEquals("buckets/" + b.getIdentifier(), b.getLink().getUri().toString()));
    }

    @Test
    public void testPopulateFlowLinks() {
        flows.stream().forEach(f -> Assert.assertNull(f.getLink()));
        linkService.populateFlowLinks(flows);
        flows.stream().forEach(f -> Assert.assertEquals("flows/" + f.getIdentifier(), f.getLink().getUri().toString()));
    }

    @Test
    public void testPopulateSnapshotLinks() {
        snapshots.stream().forEach(s -> Assert.assertNull(s.getLink()));
        linkService.populateSnapshotLinks(snapshots);
        snapshots.stream().forEach(s -> Assert.assertEquals(
                "flows/" + s.getFlowIdentifier() + "/versions/" + s.getVersion(), s.getLink().getUri().toString()));
    }

    @Test
    public void testPopulateItemLinks() {
        items.stream().forEach(i -> Assert.assertNull(i.getLink()));
        linkService.populateItemLinks(items);
        items.stream().forEach(i -> Assert.assertEquals("flows/" + i.getIdentifier(), i.getLink().getUri().toString()));
    }

}
