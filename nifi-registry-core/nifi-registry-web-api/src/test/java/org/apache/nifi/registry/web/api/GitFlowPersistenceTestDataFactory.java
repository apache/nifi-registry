package org.apache.nifi.registry.provider.flow.git;

import org.apache.commons.io.FilenameUtils;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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

    private static Collection<Bucket> createSampleBuckets() {
        ArrayList<Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Bucket b = new Bucket("bucket" + i);
            b.setBucketDirName("bucket" + i);
            Flow flow = b.getFlowOrCreate(b.getBucketId() + "_flowpointer_" + i);
            createSampleFlow(flow);
            buckets.add(b);
        }
        return buckets;
    }

    private static Flow createSampleFlow(Flow flow) {
        Flow.FlowPointer pointer = new Flow.FlowPointer(flow.getFlowId() + 1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 12, 12, 4, 2);
        pointer.setTimestamp(calendar.getTime());
        pointer.setAuthor("author");

        flow.putVersion(1, pointer);
        return flow;
    }

    public static Collection<VersionedFlowSnapshot> createSampleFlowSnapshots() {
        Collection<VersionedFlowSnapshot> flows = new ArrayList<>();
        Collection<org.apache.nifi.registry.provider.flow.git.Bucket> buckets = createSampleBuckets();
        for (org.apache.nifi.registry.provider.flow.git.Bucket existingBucket : buckets) {
            org.apache.nifi.registry.bucket.Bucket modelBucket = new org.apache.nifi.registry.bucket.Bucket();
            modelBucket.setCreatedTimestamp(Date.from(Instant.now()).getTime());
            modelBucket.setName(existingBucket.getBucketDirName());
            modelBucket.setIdentifier(existingBucket.getBucketId());
            modelBucket.setDescription("synced with git repository");

            for (Flow flow : existingBucket.getFlows()) {
                VersionedFlow modelFlow = new VersionedFlow();
                modelFlow.setIdentifier(flow.getFlowId());
                modelFlow.setBucketIdentifier(existingBucket.getBucketId());
                modelFlow.setCreatedTimestamp(new java.util.Date().getTime());
                modelFlow.setModifiedTimestamp(new java.util.Date().getTime());
                modelFlow.setName("unknown");
                //TODO get flow description
                modelFlow.setDescription("lost by sync because unavailable atm");

                for (Map.Entry<Integer, Flow.FlowPointer> version : flow.getVersions()) {
                    Integer versionNumber = version.getKey();
                    Flow.FlowPointer flowPointer = version.getValue();

                    if (flow.getLatestVersion().isPresent() && flow.getLatestVersion().get() == versionNumber) {
                        modelFlow.setName(FilenameUtils.getBaseName(flowPointer.getFileName()));
                    }
                }

                for (Map.Entry<Integer, Flow.FlowPointer> version : flow.getVersions()) {
                    Integer versionNumber = version.getKey();
                    Flow.FlowPointer flowPointer = version.getValue();

                    modelFlow.setVersionCount(Math.max(versionNumber, modelFlow.getVersionCount()));

                    VersionedFlowSnapshotMetadata metadata = new VersionedFlowSnapshotMetadata();
                    metadata.setVersion(versionNumber);
                    metadata.setAuthor(flowPointer.getAuthor());
                    metadata.setComments(flowPointer.getComments());
                    metadata.setTimestamp(flowPointer.getTimestamp().getTime());

                    VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
                    snapshot.setFlow(modelFlow);
                    snapshot.setBucket(modelBucket);
                    snapshot.setSnapshotMetadata(metadata);

                    flows.add(snapshot);
                }
            }
        }

        return flows;
    }
}
