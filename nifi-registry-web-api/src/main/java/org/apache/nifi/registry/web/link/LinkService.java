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
import org.apache.nifi.registry.web.link.builder.BucketLinkBuilder;
import org.apache.nifi.registry.web.link.builder.LinkBuilder;
import org.apache.nifi.registry.web.link.builder.VersionedFlowLinkBuilder;
import org.apache.nifi.registry.web.link.builder.VersionedFlowSnapshotLinkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Link;

@Service
public class LinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkService.class);

    private final LinkBuilder<Bucket> bucketLinkBuilder = new BucketLinkBuilder();

    private final LinkBuilder<VersionedFlow> versionedFlowLinkBuilder = new VersionedFlowLinkBuilder();

    private final LinkBuilder<VersionedFlowSnapshotMetadata> snapshotMetadataLinkBuilder = new VersionedFlowSnapshotLinkBuilder();

    // ---- Bucket Links

    public void populateBucketLinks(final Iterable<Bucket> buckets) {
        if (buckets == null) {
            return;
        }

        buckets.forEach(b -> populateBucketLinks(b));
    }

    public void populateBucketLinks(final Bucket bucket) {
        final Link bucketLink = bucketLinkBuilder.createLink(bucket);
        bucket.setLink(bucketLink);
    }

    // ---- Flow Links

    public void populateFlowLinks(final Iterable<VersionedFlow> versionedFlows) {
        if (versionedFlows == null) {
            return;
        }

        versionedFlows.forEach(f  -> populateFlowLinks(f));
    }

    public void populateFlowLinks(final VersionedFlow versionedFlow) {
        final Link flowLink = versionedFlowLinkBuilder.createLink(versionedFlow);
        versionedFlow.setLink(flowLink);
    }

    // ---- Flow Snapshot Links

    public void populateSnapshotLinks(final Iterable<VersionedFlowSnapshotMetadata> snapshotMetadatas) {
        if (snapshotMetadatas == null) {
            return;
        }

        snapshotMetadatas.forEach(s -> populateSnapshotLinks(s));
    }

    public void populateSnapshotLinks(final VersionedFlowSnapshotMetadata snapshotMetadata) {
        final Link snapshotLink = snapshotMetadataLinkBuilder.createLink(snapshotMetadata);
        snapshotMetadata.setLink(snapshotLink);
    }

    // ---- BucketItem Links

    public void populateItemLinks(final Iterable<BucketItem> items) {
        if (items == null) {
            return;
        }

        items.forEach(i -> populateItemLinks(i));
    }

    public void populateItemLinks(final BucketItem bucketItem) {
        if (bucketItem == null) {
            return;
        }

        if (bucketItem instanceof VersionedFlow) {
            populateFlowLinks((VersionedFlow)bucketItem);
        } else {
            LOGGER.error("Unable to create link for BucketItem with type: " + bucketItem.getClass().getCanonicalName());
        }
    }
}
