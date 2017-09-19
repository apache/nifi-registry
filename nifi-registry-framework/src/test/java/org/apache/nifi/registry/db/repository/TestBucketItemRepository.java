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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestBucketItemRepository extends RepositoryBaseTest {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private BucketItemRepository bucketItemRepository;

    @Test
    public void testFindAllPageable() {
        final Page<BucketItemEntity> page = bucketItemRepository.findAll(new PageRequest(0, 10));
        assertNotNull(page);
        assertEquals(1, page.getTotalPages());
        assertEquals(3, page.getTotalElements());

        final List<BucketItemEntity> entities = page.getContent();
        assertNotNull(entities);
        assertEquals(3, entities.size());
    }

    @Test
    public void testFindAll() {
        final Iterable<BucketItemEntity> entities = bucketItemRepository.findAll();
        assertNotNull(entities);

        int count = 0;
        for (BucketItemEntity entity : entities) {
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testFindByBucket() {
        final BucketEntity bucket = bucketRepository.findOne("1");
        assertNotNull(bucket);

        final List<BucketItemEntity> entities = bucketItemRepository.findByBucket(bucket);
        assertNotNull(entities);
        assertEquals(2, entities.size());
    }

    @Test
    public void testFindByBucketPageable() {
        final BucketEntity bucket = bucketRepository.findOne("1");
        assertNotNull(bucket);

        final List<BucketItemEntity> entities = bucketItemRepository.findByBucket(bucket, new PageRequest(0, 2, new Sort(Sort.Direction.ASC, "id")));
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertEquals("1", entities.get(0).getId());
        assertEquals("2", entities.get(1).getId());
    }

    @Test
    public void testFindByBucketSort() {
        final BucketEntity bucket = bucketRepository.findOne("1");
        assertNotNull(bucket);

        final List<BucketItemEntity> entities = bucketItemRepository.findByBucket(bucket, new Sort(Sort.Direction.DESC, "id"));
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertEquals("2", entities.get(0).getId());
        assertEquals("1", entities.get(1).getId());
    }

}
