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
package org.apache.nifi.registry.web;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class TestRestAPI {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestRestAPI.class);

    public static final String REGISTRY_API_URL = "http://localhost:8080/nifi-registry-api";
    public static final String REGISTRY_API_BUCKETS_URL = REGISTRY_API_URL + "/buckets";
    public static final String REGISTRY_API_FLOWS_URL = REGISTRY_API_URL + "/flows";

    public static void main(String[] args) {
        try {
            final Client client = ClientBuilder.newClient();

            // create some buckets
            final int numBuckets = 20;
            final List<Bucket> createdBuckets = new ArrayList<>();

            for (int i=0; i < numBuckets; i++) {
                final Bucket createdBucket = createBucket(client, i);
                System.out.println("Created bucket # " + i + " with id " + createdBucket.getIdentifier());
                createdBuckets.add(createdBucket);
            }

            final Bucket createdBucket = createdBuckets.get(0);

            // create some flows
            final int numFlows = 20;
            final List<VersionedFlow> createdFlows = new ArrayList<>();

            for (int i=0; i < numFlows; i++) {
                final VersionedFlow createdFlow = createFlow(client, createdBucket, i);
                System.out.println("Created flow # " + i + " with id " + createdFlow.getIdentifier());
                createdFlows.add(createdFlow);
            }

            final VersionedFlow createdFlow = createdFlows.get(0);

            // Create first snapshot for the flow

            final VersionedFlowSnapshotMetadata snapshotMetadata1 = new VersionedFlowSnapshotMetadata();
            snapshotMetadata1.setBucketIdentifier(createdBucket.getIdentifier());
            snapshotMetadata1.setFlowIdentifier(createdFlow.getIdentifier());
            snapshotMetadata1.setFlowName(createdFlow.getName());
            snapshotMetadata1.setVersion(1);
            snapshotMetadata1.setComments("This is snapshot #1.");

            final VersionedProcessGroup snapshotContents1 = new VersionedProcessGroup();
            snapshotContents1.setIdentifier("pg1");
            snapshotContents1.setName("Process Group 1");

            final VersionedFlowSnapshot snapshot1 = new VersionedFlowSnapshot();
            snapshot1.setSnapshotMetadata(snapshotMetadata1);
            snapshot1.setFlowContents(snapshotContents1);

            final VersionedFlowSnapshot createdSnapshot1 = client.target(REGISTRY_API_FLOWS_URL)
                    .path("/{flowId}/versions")
                    .resolveTemplate("flowId", createdFlow.getIdentifier())
                    .request()
                    .post(
                            Entity.entity(snapshot1, MediaType.APPLICATION_JSON_TYPE),
                            VersionedFlowSnapshot.class
                    );

            System.out.println("Created snapshot with version: " + createdSnapshot1.getSnapshotMetadata().getVersion());

            // Create second snapshot for the flow

            final VersionedFlowSnapshotMetadata snapshotMetadata2 = new VersionedFlowSnapshotMetadata();
            snapshotMetadata2.setBucketIdentifier(createdBucket.getIdentifier());
            snapshotMetadata2.setFlowIdentifier(createdFlow.getIdentifier());
            snapshotMetadata2.setFlowName(createdFlow.getName());
            snapshotMetadata2.setVersion(2);
            snapshotMetadata2.setComments("This is snapshot #2.");

            final VersionedProcessGroup snapshotContents2 = new VersionedProcessGroup();
            snapshotContents2.setIdentifier("pg1");
            snapshotContents2.setName("Process Group 1 New Name");

            final VersionedFlowSnapshot snapshot2 = new VersionedFlowSnapshot();
            snapshot2.setSnapshotMetadata(snapshotMetadata2);
            snapshot2.setFlowContents(snapshotContents2);

            final VersionedFlowSnapshot createdSnapshot2 = client.target(REGISTRY_API_FLOWS_URL)
                    .path("/{flowId}/versions")
                    .resolveTemplate("flowId", createdFlow.getIdentifier())
                    .request()
                    .post(
                            Entity.entity(snapshot2, MediaType.APPLICATION_JSON_TYPE),
                            VersionedFlowSnapshot.class
                    );

            System.out.println("Created snapshot with version: " + createdSnapshot2.getSnapshotMetadata().getVersion());

            // Retrieve the flow by id

            final Response flowResponse = client.target(REGISTRY_API_FLOWS_URL)
                    .path("/{flowId}")
                    .resolveTemplate("flowId", createdFlow.getIdentifier())
                    .request()
                    .get();

            final String flowJson = flowResponse.readEntity(String.class);
            System.out.println("Flow: " + flowJson);
        } catch (WebApplicationException e) {
            LOGGER.error(e.getMessage(), e);

            final Response response = e.getResponse();
            LOGGER.error(response.readEntity(String.class));
        }
    }

    private static Bucket createBucket(Client client, int num) {
        final Bucket bucket = new Bucket();
        bucket.setName("Bucket #" + num);
        bucket.setDescription("This is bucket #" + num);

        final Bucket createdBucket = client.target(REGISTRY_API_BUCKETS_URL)
                .request()
                .post(
                        Entity.entity(bucket, MediaType.APPLICATION_JSON),
                        Bucket.class
                );

        return createdBucket;
    }

    private static VersionedFlow createFlow(Client client, Bucket bucket, int num) {
        final VersionedFlow versionedFlow = new VersionedFlow();
        versionedFlow.setName("Flow #" + num);
        versionedFlow.setDescription("This is flow #" + num);

        final VersionedFlow createdFlow = client.target(REGISTRY_API_BUCKETS_URL)
                .path("/{bucketId}/flows")
                .resolveTemplate("bucketId", bucket.getIdentifier())
                .request()
                .post(
                        Entity.entity(versionedFlow, MediaType.APPLICATION_JSON),
                        VersionedFlow.class
                );

        return createdFlow;
    }

}
