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
package org.apache.nifi.registry.db;

import org.apache.nifi.registry.db.entity.KeyEntity;
import org.apache.nifi.registry.db.repository.KeyRepository;
import org.apache.nifi.registry.security.key.Key;
import org.apache.nifi.registry.security.key.KeyService;
import org.apache.nifi.registry.service.DataModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class DatabaseKeyService implements KeyService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseKeyService.class);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private KeyRepository keyRepository;

    @Autowired
    public DatabaseKeyService(KeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Override
    public Key getKey(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        Key key = null;
        readLock.lock();
        try {
            KeyEntity keyEntity = keyRepository.findById(id).orElse(null);
            if (keyEntity != null) {
                key = DataModelMapper.map(keyEntity);
            } else {
                logger.debug("No signing key found with id='" + id + "'");
            }
        } finally {
            readLock.unlock();
        }
        return key;
    }

    @Override
    public Key getOrCreateKey(String identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity cannot be null");
        }

        Key key;
        writeLock.lock();
        try {
            final KeyEntity existingKeyEntity = keyRepository.findOneByTenantIdentity(identity);
            if (existingKeyEntity == null) {
                logger.debug("No key found with identity='" + identity + "'. Creating new key.");

                final KeyEntity newKeyEntity = new KeyEntity();
                newKeyEntity.setId(UUID.randomUUID().toString());
                newKeyEntity.setTenantIdentity(identity);
                newKeyEntity.setKeyValue(UUID.randomUUID().toString());

                final KeyEntity savedKeyEntity = keyRepository.save(newKeyEntity);

                key = DataModelMapper.map(savedKeyEntity);
            } else {
                key = DataModelMapper.map(existingKeyEntity);
            }
        } finally {
            writeLock.unlock();
        }
        return key;
    }

    @Override
    public void deleteKey(String identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity cannot be null");
        }

        Key key;
        writeLock.lock();
        try {
            logger.debug("Deleting key with identity='" + identity + "'.");
            keyRepository.deleteByTenantIdentity(identity);
        } finally {
            writeLock.unlock();
        }

    }

}
