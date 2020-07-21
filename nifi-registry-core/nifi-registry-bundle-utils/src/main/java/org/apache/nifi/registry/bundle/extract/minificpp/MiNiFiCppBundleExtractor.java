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
package org.apache.nifi.registry.bundle.extract.minificpp;

import org.apache.nifi.registry.bundle.extract.nar.NarBundleExtractor;
import org.apache.nifi.registry.bundle.model.BundleDetails;
import org.apache.nifi.registry.extension.component.manifest.Extension;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

/**
 * Description: Layers a header location stream to locate the JAR entries near the end of the file, which
 * is the expected location of the file format.
 *
 * This can and may be adjusted for differing binary types; however, the expectation is that we locate
 * a zip/jar of some sort that we can extract via the base class.
 *
 * Purpose: Provides BundleDetails for MiNiFi CPP binaries
 *
 *
 * Design:
 *
 * The specification of the input files are expected to have a zip archive at the end. As a result,
 * the binary is still executable, but can carry a payload as needed.
 *
 */
public class MiNiFiCppBundleExtractor extends NarBundleExtractor {

    /**
     * Zip magic bytes.
     */
    public static final byte [] MAGIC_HEADER = new byte[] {(byte) 0x50, (byte) 0x4B,(byte) 0x03, (byte) 0x04};

    @Override
    protected long getBuildTime(final String timeStamp) throws ParseException {
        try{
            // still want to support opening NARs as we will be delivering some binaries
            // as NAR files.
            return super.getBuildTime(timeStamp);
        }catch(ParseException pe){

        }
        try {
            return Long.valueOf(timeStamp);
        }catch(NumberFormatException nfe){
            throw new ParseException("Could not parse " + timeStamp + " as a valid long",0);
        }
    }

    @Override
    public BundleDetails extract(final InputStream inputStream) throws IOException {

        // for now we will disable reverselookup to maintain backwards compatibility with NARS and keep the
        // door open other archive types.
        return super.extract(new HeaderLocationInputStream(inputStream,MAGIC_HEADER,false));
    }

}
