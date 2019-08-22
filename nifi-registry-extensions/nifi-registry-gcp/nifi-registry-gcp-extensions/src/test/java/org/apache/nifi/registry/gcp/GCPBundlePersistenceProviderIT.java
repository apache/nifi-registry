/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.nifi.registry.extension.BundleCoordinate;
import org.apache.nifi.registry.extension.BundlePersistenceContext;
import org.apache.nifi.registry.extension.BundlePersistenceProvider;
import org.apache.nifi.registry.extension.BundleVersionCoordinate;
import org.apache.nifi.registry.extension.BundleVersionType;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;

public class GCPBundlePersistenceProviderIT {

    private Storage storageClient;
    private BundlePersistenceProvider provider;
    private ProviderConfigurationContext configurationContext;
    private String bucketName;

    @Before
    public void setup() throws IOException {
        bucketName = "integration-test-" + System.currentTimeMillis();

        // Create config context and provider, and call onConfigured
        final Map<String, String> properties = new HashMap<>();
        properties.put(GCPBundlePersistenceProvider.BUCKET_NAME_PROP, bucketName);
        properties.put(GCPBundlePersistenceProvider.SERVICE_ACCOUNT_PROP, "/path/to/service/account.json");
        properties.put(GCPBundlePersistenceProvider.KEY_PREFIX_PROP, "prefix");

        configurationContext = mock(ProviderConfigurationContext.class);
        when(configurationContext.getProperties()).thenReturn(properties);

        provider = new GCPBundlePersistenceProvider();
        provider.onConfigured(configurationContext);

        // Create a separate client just for the IT test so we can setup a new bucket
        storageClient = ((GCPBundlePersistenceProvider) provider).getClient(configurationContext);

        storageClient.create(BucketInfo.of(bucketName));
    }

    @After
    public void teardown() {
        try {
            provider.preDestruction();
        } catch (Exception e) {
            e.printStackTrace();
        }

        storageClient.delete(bucketName);
    }

    @Test
    @Ignore // Remove to run this against GCP, assumes you have setup credentials
    public void testGCPPersistenceProvider() throws IOException {
        final File narFile = new File("src/test/resources/nars/nifi-foo-nar-1.0.0.nar");

        final UUID bucketId = UUID.randomUUID();

        // Save bundle version #1
        final BundleVersionCoordinate versionCoordinate1 = mock(BundleVersionCoordinate.class);
        when(versionCoordinate1.getBucketId()).thenReturn(bucketId.toString());
        when(versionCoordinate1.getGroupId()).thenReturn("org.apache.nifi");
        when(versionCoordinate1.getArtifactId()).thenReturn("nifi-foo-nar");
        when(versionCoordinate1.getVersion()).thenReturn("1.0.0");
        when(versionCoordinate1.getType()).thenReturn(BundleVersionType.NIFI_NAR);

        final BundlePersistenceContext context1 = mock(BundlePersistenceContext.class);
        when(context1.getCoordinate()).thenReturn(versionCoordinate1);
        when(context1.getSize()).thenReturn(narFile.length());

        try (final InputStream in = new FileInputStream(narFile)) {
            provider.createBundleVersion(context1, in);
        }

        // Save bundle version #2
        final BundleVersionCoordinate versionCoordinate2 = mock(BundleVersionCoordinate.class);
        when(versionCoordinate2.getBucketId()).thenReturn(bucketId.toString());
        when(versionCoordinate2.getGroupId()).thenReturn("org.apache.nifi");
        when(versionCoordinate2.getArtifactId()).thenReturn("nifi-foo-nar");
        when(versionCoordinate2.getVersion()).thenReturn("2.0.0");
        when(versionCoordinate2.getType()).thenReturn(BundleVersionType.NIFI_NAR);

        final BundlePersistenceContext context2 = mock(BundlePersistenceContext.class);
        when(context2.getCoordinate()).thenReturn(versionCoordinate2);
        when(context2.getSize()).thenReturn(narFile.length());

        try (final InputStream in = new FileInputStream(narFile)) {
            provider.createBundleVersion(context2, in);
        }

        // Verify we can retrieve version #1
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        provider.getBundleVersionContent(versionCoordinate1, outputStream);
        assertEquals(context1.getSize(), outputStream.size());

        // Delete version #1
        provider.deleteBundleVersion(versionCoordinate1);

        // Verify we can no longer retrieve version #1
        final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        try {
            provider.getBundleVersionContent(versionCoordinate1, outputStream2);
            fail("Should have thrown exception");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Call delete all bundle versions which should leave an empty bucket
        final BundleCoordinate bundleCoordinate = mock(BundleCoordinate.class);
        when(bundleCoordinate.getBucketId()).thenReturn(bucketId.toString());
        when(bundleCoordinate.getGroupId()).thenReturn("org.apache.nifi");
        when(bundleCoordinate.getArtifactId()).thenReturn("nifi-foo-nar");

        provider.deleteAllBundleVersions(bundleCoordinate);

        Page<Blob> list = storageClient.list(bucketName);
        assertFalse(list.iterateAll().iterator().hasNext());
    }

}
