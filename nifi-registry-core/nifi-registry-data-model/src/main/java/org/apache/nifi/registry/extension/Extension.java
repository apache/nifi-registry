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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Extension {

    private ExtensionBundle bundle;

    private ExtensionBundleVersionMetadata bundleVersion;

    private ExtensionMetadata extensionMetadata;

    @ApiModelProperty(value = "The extension bundle that this extension belongs to.")
    public ExtensionBundle getBundle() {
        return bundle;
    }

    public void setBundle(ExtensionBundle bundle) {
        this.bundle = bundle;
    }

    @ApiModelProperty(value = "The extension bundle version that this extension belongs to.")
    public ExtensionBundleVersionMetadata getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(ExtensionBundleVersionMetadata bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    @ApiModelProperty(value = "The metadata for this extension.")
    public ExtensionMetadata getExtensionMetadata() {
        return extensionMetadata;
    }

    public void setExtensionMetadata(ExtensionMetadata extensionMetadata) {
        this.extensionMetadata = extensionMetadata;
    }

}
