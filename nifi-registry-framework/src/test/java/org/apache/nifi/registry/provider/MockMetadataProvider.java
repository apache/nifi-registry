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
package org.apache.nifi.registry.provider;

import org.apache.nifi.registry.metadata.BucketMetadata;
import org.apache.nifi.registry.metadata.FlowMetadata;
import org.apache.nifi.registry.metadata.FlowSnapshotMetadata;
import org.apache.nifi.registry.metadata.MetadataProvider;

import java.util.Map;
import java.util.Set;

public class MockMetadataProvider implements MetadataProvider {

    private Map<String,String> properties;

    @Override
    public void onConfigured(ProviderConfigurationContext configurationContext) throws ProviderCreationException {
        this.properties = configurationContext.getProperties();
    }

    public Map<String,String> getProperties() {
        return properties;
    }

    @Override
    public BucketMetadata createBucket(BucketMetadata bucket) {
        return null;
    }

    @Override
    public BucketMetadata getBucket(String bucketIdentifier) {
        return null;
    }

    @Override
    public BucketMetadata updateBucket(BucketMetadata bucket) {
        return null;
    }

    @Override
    public void deleteBucket(String bucketIdentifier) {

    }

    @Override
    public Set<BucketMetadata> getBuckets() {
        return null;
    }

    @Override
    public FlowMetadata createFlow(String bucketIdentifier, FlowMetadata flow) {
        return null;
    }

    @Override
    public FlowMetadata getFlow(String flowIdentifier) {
        return null;
    }

    @Override
    public FlowMetadata updateFlow(FlowMetadata versionedFlow) {
        return null;
    }

    @Override
    public void deleteFlow(String flowIdentifier) {

    }

    @Override
    public Set<FlowMetadata> getFlows() {
        return null;
    }

    @Override
    public Set<FlowMetadata> getFlows(String bucketId) {
        return null;
    }

    @Override
    public FlowSnapshotMetadata createFlowSnapshot(FlowSnapshotMetadata flowSnapshot) {
        return null;
    }

    @Override
    public FlowSnapshotMetadata getFlowSnapshot(String flowIdentifier, Integer version) {
        return null;
    }

    @Override
    public void deleteFlowSnapshot(String flowIdentifier, Integer version) {

    }
}
