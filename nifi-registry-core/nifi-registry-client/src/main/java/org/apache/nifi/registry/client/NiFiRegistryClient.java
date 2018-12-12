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
package org.apache.nifi.registry.client;

import java.io.Closeable;

/**
 * A client for interacting with the REST API of a NiFi registry instance.
 */
public interface NiFiRegistryClient extends Closeable {

    /**
     * @return the client for interacting with buckets
     */
    BucketClient getBucketClient();

    /**
     * @return the client for interacting with buckets on behalf of the given proxied entities
     */
    BucketClient getBucketClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with flows
     */
    FlowClient getFlowClient();

    /**
     * @return the client for interacting with flows on behalf of the given proxied entities
     */
    FlowClient getFlowClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with flows/snapshots
     */
    FlowSnapshotClient getFlowSnapshotClient();

    /**
     * @return the client for interacting with flows/snapshots on behalf of the given proxied entities
     */
    FlowSnapshotClient getFlowSnapshotClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with bucket items
     */
    ItemsClient getItemsClient();

    /**
     * @return the client for interacting with bucket items on behalf of the given proxied entities
     */
    ItemsClient getItemsClient(String ... proxiedEntity);

    /**
     * @return the client for obtaining information about the current user
     */
    UserClient getUserClient();

    /**
     * @return the client for obtaining information about the current user based on the given proxied entities
     */
    UserClient getUserClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with extension bundles
     */
    BundleClient getBundleClient();

    /**
     * @return the client for interacting with extension bundles on behalf of the given proxied entities
     */
    BundleClient getBundleClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with extension bundle versions
     */
    BundleVersionClient getBundleVersionClient();

    /**
     * @return the client for interacting with extension bundle versions on behalf of the given proxied entities
     */
    BundleVersionClient getBundleVersionClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with the extension repository
     */
    ExtensionRepoClient getExtensionRepoClient();

    /**
     * @return the client for interacting with the extension repository on behalf of the given proxied entities
     */
    ExtensionRepoClient getExtensionRepoClient(String ... proxiedEntity);

    /**
     * @return the client for interacting with extensions
     */
    ExtensionClient getExtensionClient();

    /**
     * @return the client for interacting with extensions on behalf of the given proxied entities
     */
    ExtensionClient getExtensionClient(String ... proxiedEntity);

    /**
     * The builder interface that implementations should provide for obtaining the client.
     */
    interface Builder {

        Builder config(NiFiRegistryClientConfig clientConfig);

        NiFiRegistryClientConfig getConfig();

        NiFiRegistryClient build();

    }

}
