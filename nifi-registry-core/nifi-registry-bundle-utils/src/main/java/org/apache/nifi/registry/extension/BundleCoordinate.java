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

/**
 * The coordinate of an extension bundle (i.e group + artifact + version).
 */
public class BundleCoordinate {

    private final String groupId;
    private final String artifactId;
    private final String version;

    private final String coordinate;


    public BundleCoordinate(final String groupId, final String artifactId, final String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

        if (isBlank(this.groupId) || isBlank(this.artifactId) || isBlank(this.version)) {
            throw new IllegalStateException("Group, Id, and Version are required for BundleCoordinate");
        }

        this.coordinate = this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public final String getCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        return coordinate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof BundleCoordinate)) {
            return false;
        }

        final BundleCoordinate other = (BundleCoordinate) obj;
        return getCoordinate().equals(other.getCoordinate());
    }

    @Override
    public int hashCode() {
        return 37 * this.coordinate.hashCode();
    }

}
