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
package org.apache.nifi.registry.aws;

import org.apache.nifi.registry.extension.BundleContext;
import org.apache.nifi.registry.extension.BundlePersistenceProvider;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class S3BundlePersistenceProviderIT {

    private S3Client s3Client;

    private BundlePersistenceProvider provider;
    private ProviderConfigurationContext configurationContext;

    @Before
    public void setup() {
        final Region region = Region.US_EAST_1;
        final String bucketName = "integration-test-" + System.currentTimeMillis();

        // Create a separate client just for the IT test so we can setup a new bucket
        s3Client = S3Client.builder().region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        final CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();

        s3Client.createBucket(createBucketRequest);

        // Create config context and provider, and call onConfigured
        final Map<String,String> properties = new HashMap<>();
        properties.put(S3BundlePersistenceProvider.REGION_PROP, region.id());
        properties.put(S3BundlePersistenceProvider.BUCKET_NAME_PROP, bucketName);
        properties.put(S3BundlePersistenceProvider.CREDENTIALS_PROVIDER_PROP,
                S3BundlePersistenceProvider.CredentialProvider.DEFAULT_CHAIN.name());

        configurationContext = mock(ProviderConfigurationContext.class);
        when(configurationContext.getProperties()).thenReturn(properties);

        provider = new S3BundlePersistenceProvider();
        provider.onConfigured(configurationContext);
    }

    @After
    public void teardown() {
        try {
            provider.preDestruction();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            s3Client.close();
        } catch (Exception e) {
            e.printStackTrace();;
        }
    }

    @Test
    @Ignore // Remove to run this against S3, assumes you have setup external credentials
    public void testS3PersistenceProvider() throws IOException {
        final File narFile = new File("src/test/resources/nars/nifi-foo-nar-1.0.0.nar");

        // Save bundle version #1
        final BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundleGroupId()).thenReturn("org.apache.nifi");
        when(bundleContext.getBundleArtifactId()).thenReturn("nifi-foo-nar");
        when(bundleContext.getBundleVersion()).thenReturn("1.0.0");
        when(bundleContext.getBundleSize()).thenReturn(narFile.length());
        when(bundleContext.getBundleType()).thenReturn(BundleContext.BundleType.NIFI_NAR);

        try (final InputStream in = new FileInputStream(narFile)) {
            provider.saveBundleVersion(bundleContext, in, true);
        }

        // Save bundle version #2
        final BundleContext bundleContext2 = mock(BundleContext.class);
        when(bundleContext2.getBundleGroupId()).thenReturn("org.apache.nifi");
        when(bundleContext2.getBundleArtifactId()).thenReturn("nifi-foo-nar");
        when(bundleContext2.getBundleVersion()).thenReturn("2.0.0");
        when(bundleContext2.getBundleSize()).thenReturn(narFile.length());
        when(bundleContext2.getBundleType()).thenReturn(BundleContext.BundleType.NIFI_NAR);

        try (final InputStream in = new FileInputStream(narFile)) {
            provider.saveBundleVersion(bundleContext2, in, true);
        }

        // Verify we can retrieve version #1
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        provider.getBundleVersion(bundleContext, outputStream);
        assertEquals(bundleContext.getBundleSize(), outputStream.size());

        // Delete version #1
        provider.deleteBundleVersion(bundleContext);

        // Verify we can no longer retrieve version #1
        final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        try {
            provider.getBundleVersion(bundleContext, outputStream2);
            fail("Should have thrown exception");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Call delete all bundle versions which should leave an empty bucket
        provider.deleteAllBundleVersions("", "", bundleContext.getBundleGroupId(), bundleContext.getBundleArtifactId());
    }

}
