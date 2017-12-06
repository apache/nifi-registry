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

import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotCount;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntityKey;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Repository for FlowSnapshotEntity.
 */
public interface FlowSnapshotRepository extends PagingAndSortingRepository<FlowSnapshotEntity, FlowSnapshotEntityKey> {

    @Query("select new org.apache.nifi.registry.db.entity.FlowSnapshotCount(fs.id.flowId, count(*)) from FlowSnapshotEntity as fs group by fs.id.flowId")
    List<FlowSnapshotCount> countByFlow();

    FlowSnapshotEntity findFirstByFlowOrderByIdVersionDesc(FlowEntity flowEntity);

}
