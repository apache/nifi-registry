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
package org.apache.nifi.registry.db

import org.apache.nifi.registry.db.entity.KeyEntity
import org.apache.nifi.registry.db.repository.KeyRepository
import org.apache.nifi.registry.security.key.Key
import spock.lang.Specification

class DatabaseKeyServiceSpec extends Specification {

    def keyRepository = Mock(KeyRepository)

    DatabaseKeyService keyService

    def setup() {
        keyService = new DatabaseKeyService(keyRepository)
    }

    def "get key"() {

        given: "a record exists for id=key1"
        keyRepository.findOne("key1") >> new KeyEntity([id: "key1", tenantIdentity: "user1", keyValue: "keyValue1"])

        when: "getKey is called with an existing id"
        Key existingKey = keyService.getKey("key1")

        then: "the existing key is returned as model object"
        with(existingKey) {
            id == "key1"
            identity == "user1"
            key == "keyValue1"
        }

        when: "getKey is called for a nonexistent id"
        Key nonexistentKey = keyService.getKey("key2")

        then: "null is returned"
        nonexistentKey == null

    }

    def "get or create key"() {

        given: "a record exists for identity=user1"
        keyRepository.findOneByTenantIdentity("user1") >> new KeyEntity([id: "key1", tenantIdentity: "user1", keyValue: "keyValue1"])
        keyRepository.findOneByTenantIdentity("user2") >> null
        keyRepository.save(_ as KeyEntity) >> { KeyEntity ke -> new KeyEntity([id: ke.id, tenantIdentity: ke.tenantIdentity, keyValue: ke.keyValue])}

        when: "getOrCreateKey is called with an existing identity"
        Key existingKey = keyService.getOrCreateKey("user1")

        then: "the existing key is returned as model object"
        with(existingKey) {
            id == "key1"
            identity == "user1"
            key == "keyValue1"
        }

        when: "getOrCreateKey is called for a nonexistent identity"
        Key newKey = keyService.getOrCreateKey("user2")

        then: "a new key is returned"
        with(newKey) {
            id != null
            identity == "user2"
            key != null
        }
    }

}
