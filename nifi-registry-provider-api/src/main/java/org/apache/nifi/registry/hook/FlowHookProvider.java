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
package org.apache.nifi.registry.hook;

import org.apache.nifi.registry.flow.FlowSnapshotContext;
import org.apache.nifi.registry.provider.Provider;

/**
 * A service that defines post event action hook
 *
 * NOTE: Although this interface is intended to be an extension point, it is not yet considered stable and thus may
 * change across releases until the registry matures.
 */
public interface FlowHookProvider extends Provider {

    /**
     * @param bucketId
     * @throws FlowHookException
     */
    default void postCreateBucket(String bucketId) throws FlowHookException { }

    /**
     * @param bucketId
     * @param flowId
     * @throws FlowHookException
     */
    default void postCreateFlow(String bucketId, String flowId) throws FlowHookException { }

    /**
     * @param flowSnapshotContext
     * @throws FlowHookException
     */
    default void postCreateFlowVersion(FlowSnapshotContext flowSnapshotContext) throws FlowHookException { }

    /**
     * @param bucketId
     * @throws FlowHookException
     */
    default void postDeleteBucket(String bucketId) throws FlowHookException { }

    /**
     * @param bucketId
     * @param flowId
     * @throws FlowHookException
     */
    default void postDeleteFlow(String bucketId, String flowId) throws FlowHookException { }

    /**
     * @param flowSnapshotContext
     * @throws FlowHookException
     */
    default void postDeleteFlowVersion(String bucketIdentifier, String flowIdentifier, int version) throws FlowHookException { }

    /**
     * @param bucketId
     * @throws FlowHookException
     */
    default void postUpdateBucket(String bucketId) throws FlowHookException { }

    /**
     * @param bucketId
     * @param flowId
     * @throws FlowHookException
     */
    default void postUpdateFlow(String bucketId, String flowId) throws FlowHookException { }

}
