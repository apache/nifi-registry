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
package org.apache.nifi.registry.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jersey implementation of FlowSnapshotClient.
 */
public class JerseyFlowSnapshotClient extends AbstractJerseyClient implements FlowSnapshotClient {

    final WebTarget flowSnapshotTarget;

    public JerseyFlowSnapshotClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyFlowSnapshotClient(final WebTarget baseTarget, final Map<String,String> headers) {
        super(headers);
        this.flowSnapshotTarget = baseTarget.path("/buckets/{bucketId}/flows/{flowId}/versions");
    }


    @Override
    public VersionedFlowSnapshot create(final VersionedFlowSnapshot snapshot)
            throws NiFiRegistryException, IOException {
        if (snapshot.getSnapshotMetadata() == null) {
            throw new IllegalArgumentException("Snapshot Metadata cannot be null");
        }

        final String bucketId = snapshot.getSnapshotMetadata().getBucketIdentifier();
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        final String flowId = snapshot.getSnapshotMetadata().getFlowIdentifier();
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow Identifier cannot be blank");
        }

        return executeAction("Error creating snapshot", () -> {
            final WebTarget target = flowSnapshotTarget
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("flowId", flowId);

            return  getRequestBuilder(target)
                    .post(
                            Entity.entity(snapshot, MediaType.APPLICATION_JSON),
                            VersionedFlowSnapshot.class
                    );
        });
    }

    @Override
    public VersionedFlowSnapshot get(final String bucketId, final String flowId, final int version)
            throws NiFiRegistryException, IOException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow Identifier cannot be blank");
        }

        if (version < 1) {
            throw new IllegalArgumentException("Version must be greater than 1");
        }

        return executeAction("Error retrieving flow snapshot", () -> {
            final WebTarget target = flowSnapshotTarget
                    .path("/{version}")
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("flowId", flowId)
                    .resolveTemplate("version", version);

            return getRequestBuilder(target).get(VersionedFlowSnapshot.class);
        });
    }

    @Override
    public VersionedFlowSnapshot getLatest(final String bucketId, final String flowId)
            throws NiFiRegistryException, IOException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow Identifier cannot be blank");
        }

        return executeAction("Error retrieving latest snapshot", () -> {
            final WebTarget target = flowSnapshotTarget
                    .path("/latest")
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("flowId", flowId);

            return getRequestBuilder(target).get(VersionedFlowSnapshot.class);
        });
    }

    @Override
    public VersionedFlowSnapshotMetadata getLatestMetadata(String bucketId, String flowId) throws NiFiRegistryException, IOException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow Identifier cannot be blank");
        }

        return executeAction("Error retrieving latest snapshot metadata", () -> {
            final WebTarget target = flowSnapshotTarget
                    .path("/latest/metadata")
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("flowId", flowId);

            return getRequestBuilder(target).get(VersionedFlowSnapshotMetadata.class);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VersionedFlowSnapshotMetadata> getSnapshotMetadata(final String bucketId, final String flowId)
            throws NiFiRegistryException, IOException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow Identifier cannot be blank");
        }

        return executeAction("Error retrieving snapshot metadata", () -> {
            final WebTarget target = flowSnapshotTarget
                    .resolveTemplate("bucketId", bucketId)
                    .resolveTemplate("flowId", flowId);

            final VersionedFlowSnapshotMetadata[] snapshots = getRequestBuilder(target)
                    .get(VersionedFlowSnapshotMetadata[].class);

            return snapshots == null ? Collections.emptyList() : Arrays.asList(snapshots);
        });
    }

}
