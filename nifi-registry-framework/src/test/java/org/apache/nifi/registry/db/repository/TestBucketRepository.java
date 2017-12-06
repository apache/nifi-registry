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

import org.apache.nifi.registry.db.DatabaseBaseTest;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestBucketRepository extends DatabaseBaseTest {

    @Autowired
    private BucketRepository bucketRepository;

    @Test
    public void testCreate() {
        final BucketEntity bucket = new BucketEntity();
        bucket.setId(UUID.randomUUID().toString());
        bucket.setName("Some new bucket");
        bucket.setDescription("This is some new bucket");
        bucket.setCreated(new Date());

        final BucketEntity createdBucket = bucketRepository.save(bucket);
        assertNotNull(createdBucket);
        assertEquals(bucket.getId(), createdBucket.getId());
        assertEquals(bucket.getName(), createdBucket.getName());
        assertEquals(bucket.getDescription(), createdBucket.getDescription());
        assertEquals(bucket.getCreated(), createdBucket.getCreated());
    }

    @Test
    public void testUpdate() {
        final String id = "1";

        final BucketEntity existingBucket = bucketRepository.findById(id).orElse(null);
        assertNotNull(existingBucket);

        final String updatedDescription = existingBucket.getDescription() + " UPDATED";
        existingBucket.setDescription(updatedDescription);

        bucketRepository.save(existingBucket);

        final BucketEntity updatedBucket = bucketRepository.findById(id).orElse(null);
        assertNotNull(updatedBucket);
        assertEquals(updatedDescription, updatedBucket.getDescription());

        // create date should not have changed
        assertEquals(existingBucket.getCreated(), updatedBucket.getCreated());
    }

    @Test
    public void testDelete() {
        final String id = "6";

        final BucketEntity existingBucket = bucketRepository.findById(id).orElse(null);
        assertNotNull(existingBucket);

        bucketRepository.delete(existingBucket);

        final BucketEntity updatedBucket = bucketRepository.findById(id).orElse(null);
        assertNull(updatedBucket);
    }

    @Test
    public void testOneToManyWithBucketItems() {
        final String id = "1";

        final BucketEntity existingBucket = bucketRepository.findById(id).orElse(null);
        assertNotNull(existingBucket);

        final Set<BucketItemEntity> items = existingBucket.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());
    }

    @Test
    public void testFindByNameCaseInsensitive() {
        final String bucketName = "bUcKEt 1";

        final List<BucketEntity> buckets = bucketRepository.findByNameIgnoreCase(bucketName);
        assertNotNull(buckets);
        assertEquals(1, buckets.size());

        final BucketEntity bucket = buckets.get(0);
        assertEquals(bucketName.toLowerCase(), bucket.getName().toLowerCase());
    }

    @Test
    public void testFindAllWithPaging() {
        final Sort sort = new Sort(Sort.Direction.ASC, "id");

        int pageIndex = 0;
        int pageSize = 2;

        // query for first page
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Page<BucketEntity> page = bucketRepository.findAll(pageable);
        assertNotNull(page);
        assertEquals(6, page.getTotalElements());
        assertEquals(3, page.getTotalPages());

        Iterable<BucketEntity> buckets = page.getContent();
        assertNotNull(buckets);

        List<String> ids = getIds(buckets);
        assertEquals(2, ids.size());
        assertEquals("1", ids.get(0));
        assertEquals("2", ids.get(1));

        // query for second page
        pageIndex++;
        pageable = PageRequest.of(pageIndex, pageSize, sort);
        buckets = bucketRepository.findAll(pageable);
        assertNotNull(buckets);

        ids = getIds(buckets);
        assertEquals(2, ids.size());
        assertEquals("3", ids.get(0));
        assertEquals("4", ids.get(1));

        // query for third page
        pageIndex++;
        pageable = PageRequest.of(pageIndex, pageSize, sort);
        buckets = bucketRepository.findAll(pageable);
        assertNotNull(buckets);

        ids = getIds(buckets);
        assertEquals(2, ids.size());
        assertEquals("5", ids.get(0));
        assertEquals("6", ids.get(1));

        // query for fourth page
        pageIndex++;
        pageable = PageRequest.of(pageIndex, pageSize, sort);
        buckets = bucketRepository.findAll(pageable);
        assertNotNull(buckets);

        ids = getIds(buckets);
        assertEquals(0, ids.size());
    }

    private List<String> getIds(final Iterable<BucketEntity> buckets) {
        List<String> ids = new ArrayList<>();
        buckets.forEach(b -> ids.add(b.getId()));
        return ids;
    }

}
