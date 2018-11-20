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
package org.apache.nifi.registry.extension.nar;

import org.apache.nifi.registry.extension.BundleCoordinate;
import org.apache.nifi.registry.extension.BundleDetails;
import org.apache.nifi.registry.extension.BundleExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Implementation of ExtensionBundleExtractor for NAR bundles.
 */
public class NarBundleExtractor implements BundleExtractor {

    @Override
    public BundleDetails extract(final InputStream inputStream) throws IOException {
        try (final JarInputStream jarInputStream = new JarInputStream(inputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            if (manifest == null) {
                throw new IllegalArgumentException("NAR bundles must contain a valid MANIFEST");
            }

            final Attributes attributes = manifest.getMainAttributes();

            final String groupId = attributes.getValue(NarManifestEntry.NAR_GROUP.getManifestName());
            final String artifactId = attributes.getValue(NarManifestEntry.NAR_ID.getManifestName());
            final String version = attributes.getValue(NarManifestEntry.NAR_VERSION.getManifestName());

            final BundleCoordinate bundleCoordinate = new BundleCoordinate(groupId, artifactId, version);

            final String dependencyGroupId = attributes.getValue(NarManifestEntry.NAR_DEPENDENCY_GROUP.getManifestName());
            final String dependencyArtifactId = attributes.getValue(NarManifestEntry.NAR_DEPENDENCY_ID.getManifestName());
            final String dependencyVersion = attributes.getValue(NarManifestEntry.NAR_DEPENDENCY_VERSION.getManifestName());

            final BundleCoordinate dependencyCoordinate;
            if (dependencyArtifactId != null) {
                dependencyCoordinate = new BundleCoordinate(dependencyGroupId, dependencyArtifactId, dependencyVersion);
            } else {
                dependencyCoordinate = null;
            }

            // TODO figure out what to do with build info
            final String buildBranch = attributes.getValue(NarManifestEntry.BUILD_BRANCH.getManifestName());
            final String buildTag = attributes.getValue(NarManifestEntry.BUILD_TAG.getManifestName());
            final String buildRevision = attributes.getValue(NarManifestEntry.BUILD_REVISION.getManifestName());
            final String buildTimestamp = attributes.getValue(NarManifestEntry.BUILD_TIMESTAMP.getManifestName());
            final String buildJdk = attributes.getValue(NarManifestEntry.BUILD_JDK.getManifestName());
            final String builtBy = attributes.getValue(NarManifestEntry.BUILT_BY.getManifestName());

            final BundleDetails.Builder builder = new BundleDetails.Builder()
                    .coordinate(bundleCoordinate)
                    .dependencyCoordinate(dependencyCoordinate);

            return builder.build();
        }
    }

}
