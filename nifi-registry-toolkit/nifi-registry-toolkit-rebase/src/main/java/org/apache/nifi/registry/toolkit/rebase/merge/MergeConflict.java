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
package org.apache.nifi.registry.toolkit.rebase.merge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class MergeConflict {
    private final List<MergeOperation> branchOperations;
    private final List<MergeOperation> upstreamOperations;

    public MergeConflict(MergeOperation branchOperation, MergeOperation upstreamOperation) {
        this(Collections.singletonList(branchOperation), Collections.singletonList(upstreamOperation));
    }

    @JsonCreator
    public MergeConflict(@JsonProperty("branchOperations") List<MergeOperation> branchOperations, @JsonProperty("upstreamOperations") List<MergeOperation> upstreamOperations) {
        this.branchOperations = branchOperations;
        this.upstreamOperations = upstreamOperations;
    }

    public List<MergeOperation> getBranchOperations() {
        return branchOperations;
    }

    public List<MergeOperation> getUpstreamOperations() {
        return upstreamOperations;
    }
}
