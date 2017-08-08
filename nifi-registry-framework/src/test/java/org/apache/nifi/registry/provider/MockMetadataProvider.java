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

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
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
    public Bucket createBucket(Bucket bucket) {
        return null;
    }

    @Override
    public Bucket getBucket(String bucketIdentifier) {
        return null;
    }

    @Override
    public Set<Bucket> getBuckets() {
        return null;
    }

    @Override
    public Bucket updateBucket(Bucket bucket) {
        return null;
    }

    @Override
    public void deleteBucket(String bucketIdentifier) {

    }

    @Override
    public VersionedFlow createFlow(String bucketIdentifier, VersionedFlow flow) {
        return null;
    }

    @Override
    public VersionedFlow getFlow(String flowIdentifier) {
        return null;
    }

    @Override
    public Set<VersionedFlow> getFlows() {
        return null;
    }

    @Override
    public Set<VersionedFlow> getFlows(String bucketId) {
        return null;
    }

    @Override
    public VersionedFlow updateFlow(VersionedFlow versionedFlow) {
        return null;
    }

    @Override
    public void deleteFlow(String flowIdentifier) {

    }

    @Override
    public VersionedFlowSnapshot createFlowSnapshot(VersionedFlowSnapshot flowSnapshot) {
        return null;
    }

    @Override
    public VersionedFlowSnapshot getFlowSnapshot(String flowIdentifier, Integer version) {
        return null;
    }

    @Override
    public void deleteFlowSnapshot(String flowIdentifier, Integer version) {

    }
}
