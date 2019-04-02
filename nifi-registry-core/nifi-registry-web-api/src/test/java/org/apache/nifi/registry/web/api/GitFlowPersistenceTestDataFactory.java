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
package org.apache.nifi.registry.provider.flow.git;

import org.apache.nifi.registry.metadata.BucketMetadata;
import org.apache.nifi.registry.metadata.FlowMetadata;
import org.apache.nifi.registry.metadata.FlowSnapshotMetadata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GitFlowPersistenceTestDataFactory {
    public static org.apache.nifi.registry.bucket.Bucket[] createExpectedBuckets(int amount) {
        ArrayList<org.apache.nifi.registry.bucket.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            buckets.add(createExpectedBucket(i));
        }
        org.apache.nifi.registry.bucket.Bucket[] returnArray =
                new org.apache.nifi.registry.bucket.Bucket[buckets.size()];
        return buckets.toArray(returnArray);
    }

    private static org.apache.nifi.registry.bucket.Bucket createExpectedBucket(int i) {
        org.apache.nifi.registry.bucket.Bucket bucket = new org.apache.nifi.registry.bucket.Bucket();
        bucket.setIdentifier("bucket" + i);
        bucket.setName("bucket" + i);

        return bucket;
    }

    private static Collection<Bucket> createSampleGitBuckets() {
        ArrayList<Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Bucket b = new Bucket("bucket" + i);
            b.setBucketDirName("bucket" + i);
            Flow flow = b.getFlowOrCreate(b.getBucketId() + "_flowpointer_" + i);
            createSampleGitFlow(flow);
            buckets.add(b);
        }
        return buckets;
    }

    private static Flow createSampleGitFlow(Flow flow) {
        Flow.FlowPointer pointer = new Flow.FlowPointer(flow.getFlowId() + 1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 12, 12, 4, 2);
        pointer.setCreated(calendar.getTime().getTime());
        pointer.setAuthor("author");

        flow.putVersion(1, pointer);
        return flow;
    }

    public static List<BucketMetadata> createSampleFlowMetadata() {

        List<BucketMetadata> bucketMetadata = new ArrayList<>();
        Collection<org.apache.nifi.registry.provider.flow.git.Bucket> gitBuckets = createSampleGitBuckets();
        for (org.apache.nifi.registry.provider.flow.git.Bucket existingGitBucket : gitBuckets) {
            BucketMetadata bucket = new BucketMetadata();
            bucket.setName(existingGitBucket.getBucketDirName());
            bucket.setIdentifier(existingGitBucket.getBucketId());
            bucket.setDescription("synced with git repository");

            List<FlowMetadata> flows = new ArrayList<>();
            for (Flow flow : existingGitBucket.getFlows().values()) {
                FlowMetadata flowMetadata = new FlowMetadata();
                flowMetadata.setIdentifier(flow.getFlowId());
                flowMetadata.setName("unknown");
                //TODO get flow description
                flowMetadata.setDescription("lost by sync because unavailable atm");

                List<FlowSnapshotMetadata> flowSnapshotMetadata = new ArrayList<>();
                for (Map.Entry<Integer, Flow.FlowPointer> version : flow.getVersions().entrySet()) {
                    Integer versionNumber = version.getKey();
                    Flow.FlowPointer flowPointer = version.getValue();
                    FlowSnapshotMetadata snapshotMetadata = new FlowSnapshotMetadata();


                    snapshotMetadata.setVersion(versionNumber);
                    snapshotMetadata.setAuthor(flowPointer.getAuthor());
                    snapshotMetadata.setComments(flowPointer.getComment());
                    snapshotMetadata.setCreated(flowPointer.getCreated());

                    flowSnapshotMetadata.add(snapshotMetadata);
                }

                flowMetadata.setFlowSnapshotMetadata(flowSnapshotMetadata);
                flows.add(flowMetadata);
            }
            bucket.setFlowMetadata(flows);
            bucketMetadata.add(bucket);
        }

        return bucketMetadata;
    }
}
