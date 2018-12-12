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
package org.apache.nifi.registry.bundle.model;


import org.apache.nifi.registry.bundle.extract.BundleExtractor;
import org.apache.nifi.registry.bundle.util.BundleUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Details for a given bundle which are obtained from a given {@link BundleExtractor}.
 */
public class BundleDetails {

    private final BundleCoordinate bundleCoordinate;

    private final Set<BundleCoordinate> dependencyBundleCoordinates;

    private final String systemApiVersion;

    private final Set<ExtensionDetails> extensionDetails;

    private final Map<String,String> additionalDetails;

    private final BuildDetails buildDetails;

    private final String docsContent;

    private BundleDetails(final Builder builder) {
        this.bundleCoordinate = builder.bundleCoordinate;
        this.dependencyBundleCoordinates = Collections.unmodifiableSet(new HashSet<>(builder.dependencyBundleCoordinates));
        this.extensionDetails = Collections.unmodifiableSet(new HashSet<>(builder.extensionDetails));
        this.additionalDetails = Collections.unmodifiableMap(new HashMap<>(builder.additionalDetails));
        this.systemApiVersion = builder.systemApiVersion;
        this.buildDetails = builder.buildDetails;
        this.docsContent = builder.docsContent;

        BundleUtils.validateNotNull("Bundle Coordinate", this.bundleCoordinate);
        BundleUtils.validateNotNull("Dependency Coordinates", this.dependencyBundleCoordinates);
        BundleUtils.validateNotNull("Extension Details", this.extensionDetails);
        BundleUtils.validateNotNull("System API Version", this.systemApiVersion);
        BundleUtils.validateNotNull("Build Details", this.buildDetails);
        BundleUtils.validateNotBlank("Docs Content", this.docsContent);
    }

    public BundleCoordinate getBundleCoordinate() {
        return bundleCoordinate;
    }

    public Set<BundleCoordinate> getDependencyBundleCoordinates() {
        return dependencyBundleCoordinates;
    }

    public String getSystemApiVersion() {
        return systemApiVersion;
    }

    public Set<ExtensionDetails> getExtensionDetails() {
        return extensionDetails;
    }

    public Map<String, String> getAdditionalDetails() {
        return additionalDetails;
    }

    public BuildDetails getBuildDetails() {
        return buildDetails;
    }

    public String getDocsContent() {
        return docsContent;
    }

    /**
     * Builder for creating instances of BundleDetails.
     */
    public static class Builder {

        private BundleCoordinate bundleCoordinate;
        private Set<BundleCoordinate> dependencyBundleCoordinates = new HashSet<>();
        private Set<ExtensionDetails> extensionDetails = new HashSet<>();
        private Map<String,String> additionalDetails = new HashMap<>();
        private BuildDetails buildDetails;
        private String systemApiVersion;
        private String docsContent;

        public Builder coordinate(final BundleCoordinate bundleCoordinate) {
            this.bundleCoordinate = bundleCoordinate;
            return this;
        }

        public Builder addDependencyCoordinate(final BundleCoordinate dependencyCoordinate) {
            if (dependencyCoordinate != null) {
                this.dependencyBundleCoordinates.add(dependencyCoordinate);
            }
            return this;
        }

        public Builder systemApiVersion(final String systemApiVersion) {
            this.systemApiVersion = systemApiVersion;
            return this;
        }

        public Builder addExtensionDetails(final ExtensionDetails extensionDetails) {
            if (extensionDetails != null) {
                this.extensionDetails.add(extensionDetails);
            }
            return this;
        }

        public Builder addExtensionDetails(final Set<ExtensionDetails> extensionDetails) {
            if (extensionDetails != null) {
                this.extensionDetails.addAll(extensionDetails);
            }
            return this;
        }

        public Builder addAdditionalDetails(final String extensionName, final String additionalDetails) {
            this.additionalDetails.put(extensionName, additionalDetails);
            return this;
        }

        public Builder buildDetails(final BuildDetails buildDetails) {
            this.buildDetails = buildDetails;
            return this;
        }

        public Builder docsContent(final String docsContent) {
            this.docsContent = docsContent;
            return this;
        }

        public BundleDetails build() {
            return new BundleDetails(this);
        }
    }

}
