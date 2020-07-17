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
package org.apache.nifi.registry.toolkit.rebase.merge;

import java.io.IOException;

import static org.apache.nifi.registry.toolkit.rebase.RecursiveRebase.OBJECT_MAPPER;
import static org.junit.Assert.assertEquals;

public class RoundTrip {
    public static <T> T testRoundTrip(Class<T> clazz, T obj) {
        String writtenYaml = null;
        try {
            writtenYaml = OBJECT_MAPPER.writeValueAsString(obj);
            T value = OBJECT_MAPPER.readValue(writtenYaml, clazz);
            assertEquals(writtenYaml, OBJECT_MAPPER.writeValueAsString(value));
            return value;
        } catch (IOException e) {
            String message = "Got JSON Exception.";
            if (writtenYaml != null) {
                message = message + "\nWritten yaml:\n" + writtenYaml;
            }
            throw new AssertionError(message, e);
        }
    }
}
