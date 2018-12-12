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
package org.apache.nifi.registry.bundle.extract.nar;

import org.apache.nifi.registry.bundle.extract.BundleException;
import org.apache.nifi.registry.bundle.extract.BundleExtractor;
import org.apache.nifi.registry.bundle.model.BuildDetails;
import org.apache.nifi.registry.bundle.model.BundleCoordinate;
import org.apache.nifi.registry.bundle.model.BundleDetails;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestNarBundleExtractor {

    private BundleExtractor extractor;

    @Before
    public void setup() {
        this.extractor = new NarBundleExtractor();
    }

    @Test
    public void testExtractFromGoodNarNoDependencies() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-framework-nar.nar")) {
            final BundleDetails bundleDetails = extractor.extract(in);
            assertNotNull(bundleDetails);
            assertNotNull(bundleDetails.getBundleCoordinate());
            assertNotNull(bundleDetails.getDependencyBundleCoordinates());
            assertEquals(0, bundleDetails.getDependencyBundleCoordinates().size());

            final BundleCoordinate bundleCoordinate = bundleDetails.getBundleCoordinate();
            assertEquals("org.apache.nifi", bundleCoordinate.getGroupId());
            assertEquals("nifi-framework-nar", bundleCoordinate.getArtifactId());
            assertEquals("1.8.0", bundleCoordinate.getVersion());

            assertNotNull(bundleDetails.getExtensionDetails());
            assertEquals(0, bundleDetails.getExtensionDetails().size());
            assertEquals("1.8.0", bundleDetails.getSystemApiVersion());

            assertNotNull(bundleDetails.getDocsContent());
            assertTrue(bundleDetails.getDocsContent().startsWith("<extensions>"));
            assertTrue(bundleDetails.getDocsContent().trim().endsWith("</extensions>"));
        }
    }

    @Test
    public void testExtractFromGoodNarWithDependencies() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar")) {
            final BundleDetails bundleDetails = extractor.extract(in);
            assertNotNull(bundleDetails);
            assertNotNull(bundleDetails.getBundleCoordinate());
            assertNotNull(bundleDetails.getDependencyBundleCoordinates());
            assertEquals(1, bundleDetails.getDependencyBundleCoordinates().size());

            final BundleCoordinate bundleCoordinate = bundleDetails.getBundleCoordinate();
            assertEquals("org.apache.nifi", bundleCoordinate.getGroupId());
            assertEquals("nifi-foo-nar", bundleCoordinate.getArtifactId());
            assertEquals("1.8.0", bundleCoordinate.getVersion());

            final BundleCoordinate dependencyCoordinate = bundleDetails.getDependencyBundleCoordinates().stream().findFirst().get();
            assertEquals("org.apache.nifi", dependencyCoordinate.getGroupId());
            assertEquals("nifi-bar-nar", dependencyCoordinate.getArtifactId());
            assertEquals("2.0.0", dependencyCoordinate.getVersion());

            final Map<String,String> additionalDetails = bundleDetails.getAdditionalDetails();
            assertNotNull(additionalDetails);
            assertEquals(0, additionalDetails.size());
        }
    }

    @Test(expected = BundleException.class)
    public void testExtractFromNarMissingRequiredManifestEntries() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-missing-manifest-entries.nar")) {
            extractor.extract(in);
            fail("Should have thrown exception");
        }
    }

    @Test(expected = BundleException.class)
    public void testExtractFromNarMissingManifest() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-missing-manifest.nar")) {
            extractor.extract(in);
            fail("Should have thrown exception");
        }
    }

    @Test(expected = BundleException.class)
    public void testExtractFromNarMissingExtensionDescriptor() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-foo-nar-missing-extension-descriptor.nar")) {
            extractor.extract(in);
            fail("Should have thrown exception");
        }
    }

    @Test
    public void testExtractFromNarWithDescriptorAndAdditionalDetails() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-hadoop-nar.nar")) {
            final BundleDetails bundleDetails = extractor.extract(in);
            assertNotNull(bundleDetails);
            assertNotNull(bundleDetails.getBundleCoordinate());
            assertNotNull(bundleDetails.getDependencyBundleCoordinates());
            assertEquals(1, bundleDetails.getDependencyBundleCoordinates().size());

            final BundleCoordinate bundleCoordinate = bundleDetails.getBundleCoordinate();
            assertEquals("org.apache.nifi", bundleCoordinate.getGroupId());
            assertEquals("nifi-hadoop-nar", bundleCoordinate.getArtifactId());
            assertEquals("1.9.0-SNAPSHOT", bundleCoordinate.getVersion());

            final BuildDetails buildDetails = bundleDetails.getBuildDetails();
            assertNotNull(buildDetails);
            assertEquals("1.8.0_162", buildDetails.getTool());
            assertEquals(BuildDetails.NA, buildDetails.getFlags());
            assertEquals("master", buildDetails.getBranch());
            assertEquals("HEAD", buildDetails.getTag());
            assertEquals("1a937b6", buildDetails.getRevision());
            assertEquals("jsmith", buildDetails.getBuiltBy());
            assertNotNull(buildDetails.getBuilt());

            assertEquals("1.9.0-SNAPSHOT", bundleDetails.getSystemApiVersion());
            assertNotNull(bundleDetails.getExtensionDetails());
            assertEquals(10, bundleDetails.getExtensionDetails().size());

            final Map<String,String> additionalDetails = bundleDetails.getAdditionalDetails();
            assertNotNull(additionalDetails);
            assertEquals(3, additionalDetails.size());

            final String listHdfsKey = "org.apache.nifi.processors.hadoop.ListHDFS";
            assertTrue(additionalDetails.containsKey(listHdfsKey));
            assertTrue(additionalDetails.get(listHdfsKey).startsWith("<!DOCTYPE html>"));
            assertTrue(additionalDetails.get(listHdfsKey).trim().endsWith("</html>"));
        }
    }

}
