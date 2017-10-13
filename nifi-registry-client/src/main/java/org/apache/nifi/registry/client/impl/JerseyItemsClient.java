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
import org.apache.nifi.registry.bucket.BucketItem;
import org.apache.nifi.registry.client.ItemsClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.field.Fields;
import org.apache.nifi.registry.params.SortParameter;

import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Jersey implementation of ItemsClient.
 */
public class JerseyItemsClient extends AbstractJerseyClient implements ItemsClient {

    private final WebTarget itemsTarget;

    public JerseyItemsClient(final WebTarget baseTarget) {
        this.itemsTarget = baseTarget.path("/items");
    }

    @Override
    public List<BucketItem> getAll() throws NiFiRegistryException, IOException {
        return getAll(Collections.emptyList());
    }

    @Override
    public List<BucketItem> getAll(final List<SortParameter> sorts) throws NiFiRegistryException, IOException {
        if (sorts == null) {
            throw new IllegalArgumentException("Sort Parameters cannot be null");
        }

        return executeAction("", () -> {
            WebTarget target = itemsTarget;
            for (final SortParameter sortParam : sorts) {
                target = target.queryParam("sort", sortParam.toString());
            }

            return target.request().get(List.class);
        });
    }

    @Override
    public List<BucketItem> getByBucket(final String bucketId) throws NiFiRegistryException, IOException {
        return getByBucket(bucketId, Collections.emptyList());
    }

    @Override
    public List<BucketItem> getByBucket(final String bucketId, final List<SortParameter> sorts)
            throws NiFiRegistryException, IOException {
        if (StringUtils.isBlank(bucketId)) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (sorts == null) {
            throw new IllegalArgumentException("Sort Parameters cannot be null");
        }

        return executeAction("", () -> {
            WebTarget target = itemsTarget
                    .path("/{bucketId}")
                    .resolveTemplate("bucketId", bucketId);

            for (final SortParameter sortParam : sorts) {
                target = target.queryParam("sort", sortParam.toString());
            }

            return target.request().get(List.class);
        });
    }

    @Override
    public Fields getFields() throws NiFiRegistryException, IOException {
        return executeAction("", () -> {
            return itemsTarget
                    .path("/fields")
                    .request()
                    .get(Fields.class);

        });
    }

}
