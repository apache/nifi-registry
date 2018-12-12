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
package org.apache.nifi.registry.bundle.extract.nar.docs;

import org.apache.nifi.registry.bundle.model.ExtensionDetails;
import org.apache.nifi.registry.bundle.util.BundleUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExtensionDocs {

    private final String systemApiVersion;

    private final Set<ExtensionDetails> extensionDetails;

    public ExtensionDocs(final String systemApiVersion, final Set<ExtensionDetails> extensionDetails) {
        this.systemApiVersion = systemApiVersion;
        this.extensionDetails = Collections.unmodifiableSet(
                extensionDetails == null ? Collections.emptySet() : new HashSet<>(extensionDetails));
        BundleUtils.validateNotBlank("System API Version", this.systemApiVersion);
        BundleUtils.validateNotNull("Extension Details", this.extensionDetails);
    }

    public String getSystemApiVersion() {
        return systemApiVersion;
    }

    public Set<ExtensionDetails> getExtensionDetails() {
        return extensionDetails;
    }

}
