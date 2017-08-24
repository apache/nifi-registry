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

/**
 * The metadata for a flow snapshot.
 */
public interface FlowSnapshotMetadata {

    /**
     * @return the identifier of the bucket this snapshot belongs to
     */
    String getBucketIdentifier();

    /**
     * @return the identifier of the flow this snapshot belongs to
     */
    String getFlowIdentifier();

    /**
     * @return the name of the flow this snapshot belongs to
     */
    String getFlowName();

    /**
     * @return the version of this snapshot
     */
    int getVersion();

    /**
     * @return the timestamp of when this snapshot was created
     */
    long getCreatedTimestamp();

    /**
     * @return the comments for this snapshot
     */
    String getComments();

}
