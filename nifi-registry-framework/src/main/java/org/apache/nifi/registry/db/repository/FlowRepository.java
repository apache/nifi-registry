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
package org.apache.nifi.registry.db.repository;

import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Set;

/**
 * Spring Data Repository for FlowEntity.
 */
public interface FlowRepository extends PagingAndSortingRepository<FlowEntity,String> {

    List<FlowEntity> findByNameIgnoreCase(String name);

    /**
     * Find all flows by buckets.
     *
     * @param buckets the buckets to find items for
     * @return the list of items for the buckets
     */
    List<FlowEntity> findByBucketIn(Set<BucketEntity> buckets);

    /**
     * Find all flows by buckets with sorting.
     *
     * @param buckets the buckets to find flows for
     * @param sort the sort params
     * @return the list of flows for the buckets
     */
    List<FlowEntity> findByBucketIn(Set<BucketEntity> buckets, Sort sort);

    /**
     * Find all flows by buckets with paging/sorting.
     *
     * @param buckets the buckets to find flows for
     * @param pageable the pageable params
     * @return the list of flows for the buckets based on the pageable params
     */
    List<FlowEntity> findByBucketIn(Set<BucketEntity> buckets, Pageable pageable);

}
