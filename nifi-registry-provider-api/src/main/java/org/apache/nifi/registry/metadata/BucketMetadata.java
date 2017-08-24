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

import java.util.Set;

/**
 * The metadata for a bucket, along with the metadata about any objects stored in the bucket, such as flows.
 */
public interface BucketMetadata {

    /**
     * @return the identifier of this bucket
     */
    String getIdentifier();

    /**
     * @return the name of this bucket
     */
    String getName();

    /**
     * @return the timestamp of when this bucket was created
     */
    long getCreatedTimestamp();

    /**
     * @return the description of this bucket
     */
    String getDescription();

    /**
     * @return the metadata about the flows that are part of this bucket
     */
    Set<FlowMetadata> getFlowMetadata();

}
