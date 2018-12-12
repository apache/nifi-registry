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

import org.apache.nifi.registry.bundle.util.BundleUtils;

import java.util.Objects;

/**
 * Represents a service API that is implemented by a service.
 */
public class ProvidedServiceApiDetails {

    private final String className;
    private final BundleCoordinate bundleCoordinate;

    private ProvidedServiceApiDetails(final Builder builder) {
        this.className = builder.className;
        this.bundleCoordinate = new BundleCoordinate(builder.groupId, builder.artifactId, builder.version);
        BundleUtils.validateNotBlank("Class Name", this.className);
        BundleUtils.validateNotNull("Bundle Coordinate", this.bundleCoordinate);
    }

    public String getClassName() {
        return className;
    }

    public BundleCoordinate getBundleCoordinate() {
        return bundleCoordinate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, bundleCoordinate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ProvidedServiceApiDetails)) {
            return false;
        }

        final ProvidedServiceApiDetails other = (ProvidedServiceApiDetails) obj;

        return Objects.equals(this.className, other.className)
                && Objects.equals(this.bundleCoordinate, other.bundleCoordinate);
    }

    public static class Builder {

        private String className;
        private String groupId;
        private String artifactId;
        private String version;

        public Builder className(final String className) {
            this.className = className;
            return this;
        }

        public Builder groupId(final String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(final String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public ProvidedServiceApiDetails build() {
            return new ProvidedServiceApiDetails(this);
        }

    }
}
