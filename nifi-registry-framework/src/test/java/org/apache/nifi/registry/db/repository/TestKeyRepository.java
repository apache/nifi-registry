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
import org.apache.nifi.registry.db.entity.KeyEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestKeyRepository extends DatabaseBaseTest {

    @Autowired
    private KeyRepository keyRepository;

    @Test
    public void testCreate() {
        final KeyEntity key = new KeyEntity();
        key.setId(UUID.randomUUID().toString());
        key.setTenantIdentity("user");
        key.setKeyValue(UUID.randomUUID().toString());

        final KeyEntity createdKey = keyRepository.save(key);
        assertNotNull(createdKey);
        assertEquals(key.getId(), createdKey.getId());
        assertEquals(key.getTenantIdentity(), createdKey.getTenantIdentity());
        assertEquals(key.getKeyValue(), createdKey.getKeyValue());
    }

    @Test
    public void testUpdate() {
        final String prepopulatedKeyId = "1";  // see test-setup.sql

        final KeyEntity existingKey = keyRepository.findById(prepopulatedKeyId).orElse(null);
        assertNotNull(existingKey);

        final String updatedKeyValue = existingKey.getKeyValue() + " UPDATED";
        existingKey.setKeyValue(updatedKeyValue);

        keyRepository.save(existingKey);

        final KeyEntity updatedKey = keyRepository.findById(prepopulatedKeyId).orElse(null);
        assertNotNull(updatedKey);
        assertEquals(updatedKeyValue, updatedKey.getKeyValue());
    }

    @Test
    @Transactional
    public void testDelete() {
        final String id = "1";

        final KeyEntity existingKey = keyRepository.findById("1").orElse(null);
        assertNotNull(existingKey);

        keyRepository.delete(existingKey);

        final KeyEntity deletedKey = keyRepository.findById("1").orElse(null);
        assertNull(deletedKey);
    }

    @Test
    public void testFindOneByTenantIdentity() {
        final String prepopulatedKeyTenantIdentity = "unit_test_tenant_identity";  // see test-setup.sql

        final KeyEntity existingKey = keyRepository.findOneByTenantIdentity(prepopulatedKeyTenantIdentity);
        assertNotNull(existingKey);
        assertEquals("1", existingKey.getId());
    }

    @Test
    @Transactional
    public void testDeleteByTenantIdentity() {
        final String prepopulatedKeyTenantIdentity = "unit_test_tenant_identity";  // see test-setup.sql

        final KeyEntity existingKey = keyRepository.findOneByTenantIdentity(prepopulatedKeyTenantIdentity);
        assertNotNull(existingKey);

        keyRepository.delete(existingKey);

        KeyEntity deletedKey = keyRepository.findOneByTenantIdentity(prepopulatedKeyTenantIdentity);
        assertNull(deletedKey);
        deletedKey = keyRepository.findById("1").orElse(null);
        assertNull(deletedKey);
    }

}
