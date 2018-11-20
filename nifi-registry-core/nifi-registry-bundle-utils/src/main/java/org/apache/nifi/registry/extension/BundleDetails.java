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
package org.apache.nifi.registry.extension;


public class BundleDetails {

    private final BundleCoordinate bundleCoordinate;

    // Can be null when there is no dependent bundle
    private final BundleCoordinate dependencyBundleCoordinate;

    private BundleDetails(final Builder builder) {
        this.bundleCoordinate = builder.bundleCoordinate;
        this.dependencyBundleCoordinate = builder.dependencyBundleCoordinate;
        if (this.bundleCoordinate == null) {
            throw new IllegalStateException("A bundle coordinate is required");
        }
    }

    public BundleCoordinate getBundleCoordinate() {
        return bundleCoordinate;
    }

    public BundleCoordinate getDependencyBundleCoordinate() {
        return dependencyBundleCoordinate;
    }

    /**
     * Builder for creating instances of BundleDetails.
     */
    public static class Builder {

        private BundleCoordinate bundleCoordinate;
        private BundleCoordinate dependencyBundleCoordinate;

        public Builder coordinate(final BundleCoordinate bundleCoordinate) {
            this.bundleCoordinate = bundleCoordinate;
            return this;
        }

        public Builder dependencyCoordinate(final BundleCoordinate dependencyCoordinate) {
            this.dependencyBundleCoordinate = dependencyCoordinate;
            return this;
        }

        public BundleDetails build() {
            return new BundleDetails(this);
        }
    }

}
