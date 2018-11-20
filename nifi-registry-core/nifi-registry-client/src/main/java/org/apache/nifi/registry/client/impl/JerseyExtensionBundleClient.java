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
package org.apache.nifi.registry.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.client.ExtensionBundleClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.extension.ExtensionBundle;

import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jersey implementation of ExtensionBundleClient.
 */
public class JerseyExtensionBundleClient extends AbstractJerseyClient implements ExtensionBundleClient {

    private final WebTarget bucketExtensionBundlesTarget;
    private final WebTarget extensionBundlesTarget;

    public JerseyExtensionBundleClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyExtensionBundleClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.bucketExtensionBundlesTarget = baseTarget.path("buckets/{bucketId}/extensions/bundles");
        this.extensionBundlesTarget = baseTarget.path("extensions/bundles");
    }

    @Override
    public List<ExtensionBundle> getAll() throws IOException, NiFiRegistryException {
        return executeAction("Error getting extension bundles", () -> {
            WebTarget target = extensionBundlesTarget;

            final ExtensionBundle[] bundles = getRequestBuilder(target).get(ExtensionBundle[].class);
            return  bundles == null ? Collections.emptyList() : Arrays.asList(bundles);
        });
    }

    @Override
    public List<ExtensionBundle> getByBucket(final String bucketId) throws IOException, NiFiRegistryException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket id cannot be null or blank");
        }

        return executeAction("Error getting extension bundles for bucket", () -> {
            WebTarget target = bucketExtensionBundlesTarget.resolveTemplate("bucketId", bucketId);

            final ExtensionBundle[] bundles = getRequestBuilder(target).get(ExtensionBundle[].class);
            return  bundles == null ? Collections.emptyList() : Arrays.asList(bundles);
        });
    }

    @Override
    public ExtensionBundle get(final String bundleId) throws IOException, NiFiRegistryException {
        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        return executeAction("Error getting extension bundle", () -> {
            WebTarget target = extensionBundlesTarget
                    .path("{bundleId}")
                    .resolveTemplate("bundleId", bundleId);

            return getRequestBuilder(target).get(ExtensionBundle.class);
        });
    }

    @Override
    public ExtensionBundle delete(final String bundleId) throws IOException, NiFiRegistryException {
        if (StringUtils.isBlank(bundleId)) {
            throw new IllegalArgumentException("Bundle id cannot be null or blank");
        }

        return executeAction("Error deleting extension bundle", () -> {
            WebTarget target = extensionBundlesTarget
                    .path("{bundleId}")
                    .resolveTemplate("bundleId", bundleId);

            return getRequestBuilder(target).delete(ExtensionBundle.class);
        });
    }

}
