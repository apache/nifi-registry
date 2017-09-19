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
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Repository for BucketItems that exposes only the methods from PagingAndSortingRepository.
 *
 * There should be no CRUD methods performed directly on BucketItems, only general retrieval.
 */
public interface BucketItemRepository extends Repository<BucketItemEntity,String> {

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    Iterable<BucketItemEntity> findAll();

    /**
     * Returns all entities sorted by the given options.
     *
     * @param sort the sort params
     * @return all entities sorted by the given options
     */
    Iterable<BucketItemEntity> findAll(Sort sort);

    /**
     * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
     *
     * @param pageable the pageable params
     * @return a page of entities
     */
    Page<BucketItemEntity> findAll(Pageable pageable);

    /**
     * Find all items by bucket.
     *
     * @param bucket the bucket to find items for
     * @return the list of items for the bucket
     */
    List<BucketItemEntity> findByBucket(BucketEntity bucket);

    /**
     * Find all items by bucket with sorting.
     *
     * @param bucket the bucket to find items for
     * @param sort the sort params
     * @return the list of items for the bucket
     */
    List<BucketItemEntity> findByBucket(BucketEntity bucket, Sort sort);

    /**
     * Find all items by bucket with paging/sorting.
     *
     * @param bucket the bucket to find items for
     * @param pageable the pageable params
     * @return the list of items for the bucket based on the pageable params
     */
    List<BucketItemEntity> findByBucket(BucketEntity bucket, Pageable pageable);

}
