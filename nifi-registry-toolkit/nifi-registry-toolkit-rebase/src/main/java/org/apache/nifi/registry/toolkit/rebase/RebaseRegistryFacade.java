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
package org.apache.nifi.registry.toolkit.rebase;

import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.service.alias.RegistryUrlAliasService;

import java.io.IOException;

public class RebaseRegistryFacade {
    public static final String INTERNAL_REPO_TOKEN = "LOCAL_NIFI_REGISTRY";

    private final NiFiRegistryClient niFiRegistryClient;
    private final RegistryUrlAliasService aliasService;

    public RebaseRegistryFacade(NiFiRegistryClient niFiRegistryClient, RegistryUrlAliasService aliasService) {
        this.niFiRegistryClient = niFiRegistryClient;
        this.aliasService = aliasService;
    }

    public VersionedFlowSnapshot getLatestSnapshot(String bucketId, String flowId) throws IOException, NiFiRegistryException {
        VersionedFlowSnapshot latest = niFiRegistryClient.getFlowSnapshotClient().getLatest(bucketId, flowId);
        aliasService.setInternal(latest.getFlowContents());
        return latest;
    }

    public VersionedFlowSnapshot getVersionSnapshot(String bucketId, String flowId, Integer version) throws IOException, NiFiRegistryException {
        VersionedFlowSnapshot snapshot = niFiRegistryClient.getFlowSnapshotClient().get(bucketId, flowId, version);
        aliasService.setInternal(snapshot.getFlowContents());
        return snapshot;
    }

    public void updateSnapshot(VersionedFlowSnapshot versionedFlowSnapshot) throws IOException, NiFiRegistryException {
        aliasService.setExternal(versionedFlowSnapshot.getFlowContents());
        niFiRegistryClient.getFlowSnapshotClient().create(versionedFlowSnapshot);
    }

    public boolean isInternal(String url) {
        return url.startsWith(INTERNAL_REPO_TOKEN);
    }
}
