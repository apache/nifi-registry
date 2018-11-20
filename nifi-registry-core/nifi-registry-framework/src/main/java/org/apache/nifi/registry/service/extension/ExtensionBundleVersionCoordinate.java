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
package org.apache.nifi.registry.service.extension;

import org.apache.commons.lang3.Validate;

/**
 * The unique coordinate for a version of an extension bundle.
 */
public class ExtensionBundleVersionCoordinate extends ExtensionBundleCoordinate {

    private final String version;

    public ExtensionBundleVersionCoordinate(final String bucketId, final String groupId, final String artifactId, final String version) {
        super(bucketId, groupId, artifactId);
        this.version = version;
        Validate.notBlank(this.version, "Version cannot be null or blank");
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + version;
    }
}
