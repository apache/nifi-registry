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
import org.apache.nifi.registry.bundle.extract.minificpp.HeaderLocationInputStream;
import org.apache.nifi.registry.bundle.extract.minificpp.MiNiFiCppBundleExtractor;
import org.apache.nifi.registry.bundle.model.BundleDetails;
import org.apache.nifi.registry.bundle.model.BundleIdentifier;
import org.apache.nifi.registry.extension.bundle.BuildInfo;
import org.apache.nifi.registry.extension.component.manifest.Extension;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class TestMiNiFiCPPBundleExtractor {

    private BundleExtractor extractor;

    @Before
    public void setup() {
        this.extractor = new MiNiFiCppBundleExtractor();
    }

    @Test
    public void testExtractFromGoodBinaryWithExtensions() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/minificpp/minifi")) {
            final BundleDetails bundleDetails = extractor.extract(in);
            assertNotNull(bundleDetails);
            assertNotNull(bundleDetails.getBundleIdentifier());
            assertNotNull(bundleDetails.getDependencies());
            assertEquals(0, bundleDetails.getDependencies().size());

            final BundleIdentifier bundleIdentifier = bundleDetails.getBundleIdentifier();
            assertEquals("org.apache.nifi", bundleIdentifier.getGroupId());
            assertEquals("minifi", bundleIdentifier.getArtifactId());
            assertEquals("0.7.0", bundleIdentifier.getVersion());

            assertNotNull(bundleDetails.getExtensions());
            assertEquals(14, bundleDetails.getExtensions().size());
            assertEquals("0.7.0", bundleDetails.getSystemApiVersion());

            Set<String> expectedSet = new HashSet<>(
                    Arrays.asList("org.apache.nifi.minifi.processors.ExecuteProcess",
                            "org.apache.nifi.minifi.processors.PutFile",
                            "org.apache.nifi.minifi.processors.ExtractText",
                            "org.apache.nifi.minifi.processors.AppendHostInfo",
                            "org.apache.nifi.minifi.processors.RouteOnAttribute",
                            "org.apache.nifi.minifi.processors.LogAttribute",
                            "org.apache.nifi.minifi.processors.TailFile",
                            "org.apache.nifi.minifi.processors.HashContent",
                            "org.apache.nifi.minifi.processors.GenerateFlowFile",
                            "org.apache.nifi.minifi.processors.ListenHTTP",
                            "org.apache.nifi.minifi.processors.GetFile",
                            "org.apache.nifi.minifi.processors.ListenSyslog",
                            "org.apache.nifi.minifi.processors.GetTCP",
                            "org.apache.nifi.minifi.processors.UpdateAttribute"));
            for(Extension extension : bundleDetails.getExtensions()){
                assertTrue(expectedSet.remove(extension.getName()));
            }
            assertTrue(expectedSet.isEmpty());
        }
    }


    @Test
    public void testExtractNarSuccessfully() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar")) {
            final BundleDetails bundleDetails = extractor.extract(in);
            assertNotNull(bundleDetails);
            assertNotNull(bundleDetails.getBundleIdentifier());
            assertNotNull(bundleDetails.getDependencies());
            assertEquals(1, bundleDetails.getDependencies().size());

            final BundleIdentifier bundleIdentifier = bundleDetails.getBundleIdentifier();
            assertEquals("org.apache.nifi", bundleIdentifier.getGroupId());
            assertEquals("nifi-foo-nar", bundleIdentifier.getArtifactId());
            assertEquals("1.8.0", bundleIdentifier.getVersion());

            final BundleIdentifier dependencyCoordinate = bundleDetails.getDependencies().stream().findFirst().get();
            assertEquals("org.apache.nifi", dependencyCoordinate.getGroupId());
            assertEquals("nifi-bar-nar", dependencyCoordinate.getArtifactId());
            assertEquals("2.0.0", dependencyCoordinate.getVersion());

            final Map<String,String> additionalDetails = bundleDetails.getAdditionalDetails();
            assertNotNull(additionalDetails);
            assertEquals(0, additionalDetails.size());
        }
    }

    @Test
    public void testStreamValidNarReverse() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar"),MiNiFiCppBundleExtractor.MAGIC_HEADER,true)) {
            try(ZipInputStream zi = new ZipInputStream(in)){
                assertNotNull(zi.getNextEntry());
            }
        }
    }

    @Test
    public void testStreamValidNarForward() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar"),MiNiFiCppBundleExtractor.MAGIC_HEADER,false)) {
            try(ZipInputStream zi = new ZipInputStream(in)){
                assertNotNull(zi.getNextEntry());
            }
        }
    }

    @Test
    public void testStreamValidCppReverse() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/minificpp/minifi"),MiNiFiCppBundleExtractor.MAGIC_HEADER,true)) {
            try(ZipInputStream zi = new ZipInputStream(in)){
                assertNotNull(zi.getNextEntry());
            }
        }
    }

    @Test
    public void testStreamValidCppForward() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/minificpp/minifi"),MiNiFiCppBundleExtractor.MAGIC_HEADER,false)) {
            try(ZipInputStream zi = new ZipInputStream(in)){
                assertNotNull(zi.getNextEntry());
            }
        }
    }


    @Test(expected = NullPointerException.class)
    public void testInvalidInputStream() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(null,MiNiFiCppBundleExtractor.MAGIC_HEADER,false)) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidInputArray() throws IOException {
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/minificpp/minifi"),null,false)) {
        }
    }

    @Test
    public void testInvalidBytesReverse() throws IOException {
        byte [] array = new byte[0];
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar"),array,true)) {
            in.read();
        }
    }

    @Test
    public void testInvalidBytesForward() throws IOException {
        byte [] array = new byte[0];
        try (final InputStream in = new HeaderLocationInputStream(new FileInputStream("src/test/resources/nars/nifi-foo-nar.nar"),array,false)) {
            in.read();
        }
    }



    @Test(expected = IOException.class)
    public void testExtractFromNarMissingRequiredManifestEntries() throws IOException {
        try (final InputStream in = new FileInputStream("src/test/resources/minificpp/nonarchive")) {
            extractor.extract(in);
            fail("Should have thrown exception");
        }
    }
}
