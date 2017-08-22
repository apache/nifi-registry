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
package org.apache.nifi.registry.web.api;

import org.apache.nifi.registry.flow.FlowPersistenceProvider;
import org.apache.nifi.registry.metadata.MetadataProvider;
import org.apache.nifi.registry.web.response.TestEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test")
public class TestResource {

    private final MetadataProvider metadataProvider;

    private final FlowPersistenceProvider flowPersistenceProvider;

    public TestResource(final MetadataProvider metadataProvider, final FlowPersistenceProvider flowPersistenceProvider) {
        this.metadataProvider = metadataProvider;
        this.flowPersistenceProvider = flowPersistenceProvider;

        if (this.metadataProvider == null) {
            throw new IllegalStateException("MetadataProvider cannot be null");
        }

        if (this.flowPersistenceProvider == null) {
            throw new IllegalStateException("FlowPersistenceProvider cannot be null");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTest() {
        final TestEntity testEntity = new TestEntity("testing");
        return Response.ok(testEntity).build();
    }

}
