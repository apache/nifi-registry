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
package org.apache.nifi.registry.web.link.builder;

import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * LinkBuilder that builds "self" links for VersionedFlowSnapshotMetadata.
 */
public class VersionedFlowSnapshotLinkBuilder implements LinkBuilder<VersionedFlowSnapshotMetadata> {

    private static final String PATH = "buckets/{bucketId}/flows/{flowId}/versions/{versionNumber}";

    @Override
    public Link createLink(final VersionedFlowSnapshotMetadata snapshotMetadata) {
        if (snapshotMetadata == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(PATH)
                .resolveTemplate("bucketId", snapshotMetadata.getBucketIdentifier())
                .resolveTemplate("flowId", snapshotMetadata.getFlowIdentifier())
                .resolveTemplate("versionNumber", snapshotMetadata.getVersion())
                .build();

        return Link.fromUri(uri).rel("content").build();
    }

}
