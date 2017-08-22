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

import org.apache.nifi.registry.metadata.generated.Bucket;
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
    private final Map<String,Set<FlowMetadata>> flowsByBucket;
    private final Map<String,FlowMetadata> flowsById;
    private final Map<String,FlowMetadata> flowsByName;
    private final Map<String,BucketMetadata> bucketsById;
    private final Map<String,BucketMetadata> bucketsByName;

    public MetadataHolder(final Metadata metadata) {
        this.metadata = metadata;
        this.flowsByBucket = Collections.unmodifiableMap(createFlowsByBucket(metadata));
        this.flowsByName = Collections.unmodifiableMap(createFlowsByName(flowsByBucket));
        this.flowsById = Collections.unmodifiableMap(createFlowsById(flowsByBucket));
        this.bucketsById = Collections.unmodifiableMap(createBucketsBydId(metadata, flowsByBucket));
        this.bucketsByName = Collections.unmodifiableMap(createBucketsByName(bucketsById));
    }

    private Map<String,BucketMetadata> createBucketsBydId(final Metadata metadata, final Map<String,Set<FlowMetadata>> flowsByBucket) {
        final Map<String,BucketMetadata> bucketsById = new HashMap<>();

        final Buckets buckets = metadata.getBuckets();
        if (buckets != null) {
            buckets.getBucket().stream().forEach(b -> {
                    final Set<FlowMetadata> bucketFlows = flowsByBucket.get(b.getIdentifier());
                    final BucketMetadata bucketMetadata = createBucketMetadata(b, bucketFlows);
                    bucketsById.put(b.getIdentifier(), bucketMetadata);
            });
        }

        return bucketsById;
    }

    private Map<String,BucketMetadata> createBucketsByName(Map<String,BucketMetadata> bucketsById) {
        final Map<String,BucketMetadata> bucketsByName = new HashMap<>();
        bucketsById.values().stream().forEach(b -> bucketsByName.put(b.getName().toLowerCase(), b));
        return bucketsByName;
    }

    private BucketMetadata createBucketMetadata(final Bucket jaxbBucket, final Set<FlowMetadata> bucketFlows) {
        return new StandardBucketMetadata.Builder()
                .identifier(jaxbBucket.getIdentifier())
                .name(jaxbBucket.getName())
                .description(jaxbBucket.getDescription())
                .created(jaxbBucket.getCreatedTimestamp())
                .addFlows(bucketFlows)
                .build();
    }

    private Map<String,Set<FlowMetadata>> createFlowsByBucket(final Metadata metadata) {
        final Map<String,Set<FlowMetadata>> flowsByBucket = new HashMap<>();

        final Flows flows = metadata.getFlows();
        if (flows != null) {
            flows.getFlow().stream().forEach(f -> {
                Set<FlowMetadata> bucketFLows = flowsByBucket.get(f.getBucketIdentifier());
                if (bucketFLows == null) {
                    bucketFLows = new HashSet<>();
                    flowsByBucket.put(f.getBucketIdentifier(), bucketFLows);
                }
                bucketFLows.add(createFlowMetadata(f));
            });
        }

        return flowsByBucket;
    }

    private FlowMetadata createFlowMetadata(final Flow jaxbFlow) {
        final StandardFlowMetadata.Builder builder = new StandardFlowMetadata.Builder()
                .identifier(jaxbFlow.getIdentifier())
                .name(jaxbFlow.getName())
                .bucketIdentifier(jaxbFlow.getBucketIdentifier())
                .description(jaxbFlow.getDescription())
                .created(jaxbFlow.getCreatedTimestamp())
                .modified(jaxbFlow.getModifiedTimestamp());

        if (jaxbFlow.getSnapshot() != null) {
            jaxbFlow.getSnapshot().stream().forEach(s -> builder.addSnapshot(createSnapshotMetadata(jaxbFlow, s)));
        }

        return builder.build();
    }

    private FlowSnapshotMetadata createSnapshotMetadata(final Flow jaxbFlow, final Flow.Snapshot jaxbSnapshot) {
        return new StandardFlowSnapshotMetadata.Builder()
                .bucketIdentifier(jaxbFlow.getBucketIdentifier())
                .flowIdentifier(jaxbFlow.getIdentifier())
                .flowName(jaxbFlow.getName())
                .version(jaxbSnapshot.getVersion())
                .comments(jaxbSnapshot.getComments())
                .created(jaxbSnapshot.getCreatedTimestamp())
                .build();
    }

    private Map<String,FlowMetadata> createFlowsById(final Map<String,Set<FlowMetadata>> flowsByBucket) {
        final Map<String,FlowMetadata> flowsById = new HashMap<>();

        for (final Map.Entry<String,Set<FlowMetadata>> entry : flowsByBucket.entrySet()) {
            for (final FlowMetadata flowMetadata : entry.getValue()) {
                flowsById.put(flowMetadata.getIdentifier(), flowMetadata);
            }
        }

        return flowsById;
    }

    private Map<String,FlowMetadata> createFlowsByName(final Map<String,Set<FlowMetadata>> flowsByBucket) {
        final Map<String,FlowMetadata> flowsByName = new HashMap<>();

        for (final Map.Entry<String,Set<FlowMetadata>> entry : flowsByBucket.entrySet()) {
            for (final FlowMetadata flow : entry.getValue()) {
                flowsByName.put(flow.getName().toLowerCase(), flow);
            }
        }

        return flowsByName;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Map<String,BucketMetadata> getBucketsById() {
        return bucketsById;
    }

    public Map<String,BucketMetadata> getBucketsByName() {
        return bucketsByName;
    }

    public Map<String,FlowMetadata> getFlowsById() {
        return flowsById;
    }

    public Map<String,FlowMetadata> getFlowsByName() {
        return flowsByName;
    }

    public Map<String,Set<FlowMetadata>> getFlowsByBucket() {
        return flowsByBucket;
    }

}
