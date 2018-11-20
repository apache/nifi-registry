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
package org.apache.nifi.registry.web.link;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleVersion;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.repo.ExtensionRepoArtifact;
import org.apache.nifi.registry.extension.repo.ExtensionRepoBucket;
import org.apache.nifi.registry.extension.repo.ExtensionRepoGroup;
import org.apache.nifi.registry.extension.repo.ExtensionRepoVersionSummary;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.link.LinkableEntity;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class LinkService {

    private static final String BUCKET_PATH = "buckets/{id}";

    private static final String FLOW_PATH = "buckets/{bucketId}/flows/{flowId}";
    private static final String FLOW_SNAPSHOT_PATH = "buckets/{bucketId}/flows/{flowId}/versions/{versionNumber}";

    private static final String EXTENSION_BUNDLE_PATH = "extensions/bundles/{bundleId}";
    private static final String EXTENSION_BUNDLE_VERSION_PATH = "extensions/bundles/{bundleId}/versions/{version}";
    private static final String EXTENSION_BUNDLE_VERSION_CONTENT_PATH = "extensions/bundles/{bundleId}/versions/{version}/content";

    private static final String EXTENSION_REPO_BUCKET_PATH = "extensions/repo/{bucketName}";
    private static final String EXTENSION_REPO_GROUP_PATH = "extensions/repo/{bucketName}/{groupId}";
    private static final String EXTENSION_REPO_ARTIFACT_PATH = "extensions/repo/{bucketName}/{groupId}/{artifactId}";
    private static final String EXTENSION_REPO_VERSION_PATH = "extensions/repo/{bucketName}/{groupId}/{artifactId}/{version}";


    private static final LinkBuilder<Bucket> BUCKET_LINK_BUILDER = (bucket) -> {
        if (bucket == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(BUCKET_PATH)
                .resolveTemplate("id", bucket.getIdentifier())
                .build();

        return Link.fromUri(uri).rel("self").build();
    };

    private static final LinkBuilder<VersionedFlow> FLOW_LINK_BUILDER = (versionedFlow -> {
        if (versionedFlow == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(FLOW_PATH)
                .resolveTemplate("bucketId", versionedFlow.getBucketIdentifier())
                .resolveTemplate("flowId", versionedFlow.getIdentifier())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<VersionedFlowSnapshotMetadata> FLOW_SNAPSHOT_LINK_BUILDER = (snapshotMetadata) -> {
        if (snapshotMetadata == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(FLOW_SNAPSHOT_PATH)
                .resolveTemplate("bucketId", snapshotMetadata.getBucketIdentifier())
                .resolveTemplate("flowId", snapshotMetadata.getFlowIdentifier())
                .resolveTemplate("versionNumber", snapshotMetadata.getVersion())
                .build();

        return Link.fromUri(uri).rel("content").build();
    };

    private static final LinkBuilder<ExtensionBundle> EXTENSION_BUNDLE_LINK_BUILDER = (extensionBundle -> {
        if (extensionBundle == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_BUNDLE_PATH)
                .resolveTemplate("bundleId", extensionBundle.getIdentifier())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionBundleVersionMetadata> EXTENSION_BUNDLE_VERSION_LINK_BUILDER = (bundleVersion -> {
        if (bundleVersion == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_BUNDLE_VERSION_PATH)
                .resolveTemplate("bundleId", bundleVersion.getExtensionBundleId())
                .resolveTemplate("version", bundleVersion.getVersion())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionBundleVersion> EXTENSION_BUNDLE_VERSION_CONTENT_LINK_BUILDER = (bundleVersion -> {
        if (bundleVersion == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_BUNDLE_VERSION_CONTENT_PATH)
                .resolveTemplate("bundleId", bundleVersion.getExtensionBundle().getIdentifier())
                .resolveTemplate("version", bundleVersion.getVersionMetadata().getVersion())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionRepoBucket> EXTENSION_REPO_BUCKET_LINK_BUILDER = (extensionRepoBucket -> {
        if (extensionRepoBucket == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_REPO_BUCKET_PATH)
                .resolveTemplate("bucketName", extensionRepoBucket.getBucketName())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionRepoGroup> EXTENSION_REPO_GROUP_LINK_BUILDER = (extensionRepoGroup -> {
        if (extensionRepoGroup == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_REPO_GROUP_PATH)
                .resolveTemplate("bucketName", extensionRepoGroup.getBucketName())
                .resolveTemplate("groupId", extensionRepoGroup.getGroupId())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionRepoArtifact> EXTENSION_REPO_ARTIFACT_LINK_BUILDER = (extensionRepoArtifact -> {
        if (extensionRepoArtifact == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_REPO_ARTIFACT_PATH)
                .resolveTemplate("bucketName", extensionRepoArtifact.getBucketName())
                .resolveTemplate("groupId", extensionRepoArtifact.getGroupId())
                .resolveTemplate("artifactId", extensionRepoArtifact.getArtifactId())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });

    private static final LinkBuilder<ExtensionRepoVersionSummary> EXTENSION_REPO_VERSION_LINK_BUILDER = (extensionRepoVersion -> {
        if (extensionRepoVersion == null) {
            return null;
        }

        final URI uri = UriBuilder.fromPath(EXTENSION_REPO_VERSION_PATH)
                .resolveTemplate("bucketName", extensionRepoVersion.getBucketName())
                .resolveTemplate("groupId", extensionRepoVersion.getGroupId())
                .resolveTemplate("artifactId", extensionRepoVersion.getArtifactId())
                .resolveTemplate("version", extensionRepoVersion.getVersion())
                .build();

        return Link.fromUri(uri).rel("self").build();
    });


    private static final Map<Class,LinkBuilder> LINK_BUILDERS;
    static {
        final Map<Class,LinkBuilder> builderMap = new HashMap<>();
        builderMap.put(Bucket.class, BUCKET_LINK_BUILDER);
        builderMap.put(VersionedFlow.class, FLOW_LINK_BUILDER);
        builderMap.put(VersionedFlowSnapshotMetadata.class, FLOW_SNAPSHOT_LINK_BUILDER);
        builderMap.put(ExtensionBundle.class, EXTENSION_BUNDLE_LINK_BUILDER);
        builderMap.put(ExtensionBundleVersionMetadata.class, EXTENSION_BUNDLE_VERSION_LINK_BUILDER);
        builderMap.put(ExtensionBundleVersion.class, EXTENSION_BUNDLE_VERSION_CONTENT_LINK_BUILDER);
        builderMap.put(ExtensionRepoBucket.class, EXTENSION_REPO_BUCKET_LINK_BUILDER);
        builderMap.put(ExtensionRepoGroup.class, EXTENSION_REPO_GROUP_LINK_BUILDER);
        builderMap.put(ExtensionRepoArtifact.class, EXTENSION_REPO_ARTIFACT_LINK_BUILDER);
        builderMap.put(ExtensionRepoVersionSummary.class, EXTENSION_REPO_VERSION_LINK_BUILDER);
        LINK_BUILDERS = Collections.unmodifiableMap(builderMap);
    }

    public <E extends LinkableEntity> void populateLinks(final E entity) {
        final LinkBuilder linkBuilder = LINK_BUILDERS.get(entity.getClass());
        if (linkBuilder == null) {
            throw new IllegalArgumentException("No LinkBuilder found for " + entity.getClass().getCanonicalName());
        }

        final Link link = linkBuilder.createLink(entity);
        entity.setLink(link);
    }

    public <E extends LinkableEntity> void populateLinks(final Iterable<E> entities) {
        if (entities == null) {
            return;
        }

        entities.forEach(e -> populateLinks(e));
    }

    public <E extends LinkableEntity> void populateFullLinks(final E entity, final URI baseUri) {
        final LinkBuilder linkBuilder = LINK_BUILDERS.get(entity.getClass());
        if (linkBuilder == null) {
            throw new IllegalArgumentException("No LinkBuilder found for " + entity.getClass().getCanonicalName());
        }

        if (baseUri == null) {
            throw new IllegalArgumentException("Base URI cannot be null");
        }

        final Link relativeLink = linkBuilder.createLink(entity);
        final URI relativeUri = relativeLink.getUri();

        final URI fullUri = UriBuilder.fromUri(baseUri)
                .path(relativeUri.getPath())
                .build();

        final Link fullLink = Link.fromUri(fullUri)
                .rel(relativeLink.getRel())
                .build();

        entity.setLink(fullLink);
    }

    public <E extends LinkableEntity> void populateFullLinks(final Iterable<E> entities, final URI baseUri) {
        if (entities == null) {
            return;
        }

        entities.forEach(e -> populateFullLinks(e, baseUri));
    }

}
