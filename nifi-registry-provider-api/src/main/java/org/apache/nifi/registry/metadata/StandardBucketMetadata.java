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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class StandardBucketMetadata implements BucketMetadata {

    private final String identifier;
    private final String name;
    private final long createdTimestamp;
    private final String description;
    private final Set<FlowMetadata> flowMetadata;

    private StandardBucketMetadata(final Builder builder) {
        this.identifier = builder.identifier;
        this.name = builder.name;
        this.createdTimestamp = builder.createdTimestamp;
        this.description = builder.description;
        this.flowMetadata = Collections.unmodifiableSet(
                builder.flowMetadata == null
                        ? Collections.emptySet() : new LinkedHashSet<>(builder.flowMetadata)
        );
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Set<FlowMetadata> getFlowMetadata() {
        return flowMetadata;
    }

    public static class Builder {

        private String identifier;
        private String name;
        private long createdTimestamp;
        private String description;
        private Set<FlowMetadata> flowMetadata = new LinkedHashSet<>();

        public Builder() {

        }

        public Builder(BucketMetadata bucketMetadata) {
            identifier(bucketMetadata.getIdentifier());
            name(bucketMetadata.getName());
            created(bucketMetadata.getCreatedTimestamp());
            description(bucketMetadata.getDescription());
            addFlows(bucketMetadata.getFlowMetadata());
        }

        public Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder created(final long createdTimestamp) {
            this.createdTimestamp = createdTimestamp;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder addFlow(final FlowMetadata flowMetadata) {
            if (flowMetadata != null) {
                this.flowMetadata.add(flowMetadata);
            }
            return this;
        }

        public Builder addFlows(final Collection<FlowMetadata> flows) {
            if (flows != null) {
                this.flowMetadata.addAll(flows);
            }
            return this;
        }

        public Builder clearFlows() {
            this.flowMetadata.clear();
            return this;
        }

        public StandardBucketMetadata build() {
            return new StandardBucketMetadata(this);
        }
    }
}
