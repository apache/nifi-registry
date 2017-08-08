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
package org.apache.nifi.registry.metadata;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.metadata.generated.Buckets;
import org.apache.nifi.registry.metadata.generated.Flow;
import org.apache.nifi.registry.metadata.generated.Flows;
import org.apache.nifi.registry.metadata.generated.Metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides atomic access to the Metadata object.
 */
public class MetadataHolder {

    private final Metadata metadata;
    private final Map<String,Bucket> bucketsById;
    private final Map<String,VersionedFlow> flowsById;
    private final Map<String,Set<VersionedFlow>> flowsByBucket;

    public MetadataHolder(final Metadata metadata) {
        this.metadata = metadata;
        this.bucketsById = Collections.unmodifiableMap(createBucketsBydId(metadata));
        this.flowsByBucket = Collections.unmodifiableMap(createFlowsByBucket(metadata));
        this.flowsById = Collections.unmodifiableMap(createFlowsById(flowsByBucket));
    }

    private Map<String,Bucket> createBucketsBydId(final Metadata metadata) {
        final Map<String,Bucket> bucketsById = new HashMap<>();

        final Buckets buckets = metadata.getBuckets();
        if (buckets != null) {
            buckets.getBucket().stream().forEach(b -> bucketsById.put(b.getIdentifier(), createBucket(b)));
        }

        return bucketsById;
    }

    private Bucket createBucket(final org.apache.nifi.registry.metadata.generated.Bucket jaxbBucket) {
        final Bucket bucket = new Bucket();
        bucket.setIdentifier(jaxbBucket.getIdentifier());
        bucket.setName(jaxbBucket.getName());
        bucket.setDescription(jaxbBucket.getDescription());
        bucket.setCreatedTimestamp(jaxbBucket.getCreatedTimestamp());
        return bucket;
    }

    private Map<String,Set<VersionedFlow>> createFlowsByBucket(final Metadata metadata) {
        final Map<String,Set<VersionedFlow>> flowsByBucket = new HashMap<>();

        final Flows flows = metadata.getFlows();
        if (flows != null) {
            flows.getFlow().stream().forEach(f -> {
                Set<VersionedFlow> bucketFLows = flowsByBucket.get(f.getBucketIdentifier());
                if (bucketFLows == null) {
                    bucketFLows = new HashSet<>();
                    flowsByBucket.put(f.getBucketIdentifier(), bucketFLows);
                }
                bucketFLows.add(createFlow(f));
            });
        }

        return flowsByBucket;
    }

    private VersionedFlow createFlow(final Flow flow) {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setIdentifier(flow.getIdentifier());
        versionedFlow.setName(flow.getName());
        versionedFlow.setDescription(flow.getDescription());
        versionedFlow.setCreatedTimestamp(flow.getCreatedTimestamp());
        versionedFlow.setModifiedTimestamp(flow.getModifiedTimestamp());

        if (flow.getSnapshot() != null) {
            flow.getSnapshot().stream().forEach(s -> versionedFlow.addVersionedFlowSnapshot(createSnapshot(flow, s)));
        }

        return versionedFlow;
    }

    private VersionedFlowSnapshot createSnapshot(final Flow flow, final Flow.Snapshot snapshot) {
        final VersionedFlowSnapshot versionedFlowSnapshot = new VersionedFlowSnapshot();
        versionedFlowSnapshot.setFlowIdentifier(flow.getIdentifier());
        versionedFlowSnapshot.setFlowName(flow.getName());
        versionedFlowSnapshot.setVersion(snapshot.getVersion());
        versionedFlowSnapshot.setComments(snapshot.getComments());
        versionedFlowSnapshot.setTimestamp(snapshot.getCreatedTimestamp());
        return versionedFlowSnapshot;
    }

    private Map<String,VersionedFlow> createFlowsById(final Map<String,Set<VersionedFlow>> flowsByBucket) {
        final Map<String,VersionedFlow> flowsBdId = new HashMap<>();

        for (final Map.Entry<String,Set<VersionedFlow>> entry : flowsByBucket.entrySet()) {
            for (final VersionedFlow flow : entry.getValue()) {
                flowsBdId.put(flow.getIdentifier(), flow);
            }
        }

        return flowsBdId;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Map<String,Bucket> getBucketsBydId() {
        return bucketsById;
    }

    public Map<String,VersionedFlow> getFlowsById() {
        return flowsById;
    }

    public Map<String,Set<VersionedFlow>> getFlowsByBucket() {
        return flowsByBucket;
    }

}
