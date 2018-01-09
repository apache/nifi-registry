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

import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.mapper.BucketEntityRowMapper;
import org.apache.nifi.registry.db.mapper.BucketItemEntityRowMapper;
import org.apache.nifi.registry.db.mapper.FlowEntityRowMapper;
import org.apache.nifi.registry.db.mapper.FlowSnapshotEntityRowMapper;
import org.apache.nifi.registry.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class DatabaseMetadataService implements MetadataService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseMetadataService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //----------------- Buckets ---------------------------------

    @Override
    public BucketEntity createBucket(final BucketEntity b) {
        final String sql = "INSERT INTO bucket (ID, NAME, DESCRIPTION, CREATED) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, b.getId(), b.getName(), b.getDescription(), b.getCreated());
        return b;
    }

    @Override
    public BucketEntity getBucketById(final String bucketIdentifier) {
        final String sql = "SELECT * FROM bucket WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new BucketEntityRowMapper(), bucketIdentifier);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<BucketEntity> getBucketsByName(final String name) {
        final String sql = "SELECT * FROM bucket WHERE name = ? ORDER BY name ASC";
        return jdbcTemplate.query(sql, new Object[] {name} , new BucketEntityRowMapper());
    }

    @Override
    public BucketEntity updateBucket(final BucketEntity bucket) {
        final String sql = "UPDATE bucket SET name = ?, description = ? WHERE id = ?";
        jdbcTemplate.update(sql, bucket.getName(), bucket.getDescription(), bucket.getId());
        return bucket;
    }

    @Override
    public void deleteBucket(final BucketEntity bucket) {
        final String snapshotDeleteSql = "DELETE FROM flow_snapshot WHERE flow_id IN ( " +
                    "SELECT f.id FROM flow f, bucket_item item WHERE f.id = item.id AND item.bucket_id = ?" +
                ")";
        jdbcTemplate.update(snapshotDeleteSql, bucket.getId());

        final String flowDeleteSql = "DELETE FROM flow WHERE id IN ( " +
                    "SELECT f.id FROM flow f, bucket_item item WHERE f.id = item.id AND item.bucket_id = ?" +
                ")";
        jdbcTemplate.update(flowDeleteSql, bucket.getId());

        final String itemDeleteSql = "DELETE FROM bucket_item WHERE bucket_id = ?";
        jdbcTemplate.update(itemDeleteSql, bucket.getId());

        final String sql = "DELETE FROM bucket WHERE id = ?";
        jdbcTemplate.update(sql, bucket.getId());
    }

    @Override
    public List<BucketEntity> getBuckets(final Set<String> bucketIds) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM bucket WHERE id IN (");
        for (int i=0; i < bucketIds.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("?");
        }
        sqlBuilder.append(") ");
        sqlBuilder.append("ORDER BY name ASC");

        return jdbcTemplate.query(sqlBuilder.toString(), bucketIds.toArray(), new BucketEntityRowMapper());
    }

    @Override
    public List<BucketEntity> getAllBuckets() {
        final String sql = "SELECT * FROM bucket ORDER BY name ASC";
        return jdbcTemplate.query(sql, new BucketEntityRowMapper());
    }

    //----------------- BucketItems ---------------------------------

    @Override
    public List<BucketItemEntity> getBucketItems(final String bucketIdentifier) {
        final String sql =
                "SELECT " +
                    "item.id as ID, " +
                    "item.name as NAME, " +
                    "item.description as DESCRIPTION, " +
                    "item.created as CREATED, " +
                    "item.modified as MODIFIED, " +
                    "item.item_type as ITEM_TYPE, " +
                    "b.id as BUCKET_ID, " +
                    "b.name as BUCKET_NAME " +
                "FROM " +
                        "bucket_item item, bucket b " +
                "WHERE " +
                        "item.bucket_id = b.id " +
                "AND " +
                        "item.bucket_id = ?";

        final List<BucketItemEntity> items = jdbcTemplate.query(sql, new Object[] { bucketIdentifier }, new BucketItemEntityRowMapper());
        return getItemsWithCounts(items);
    }

    @Override
    public List<BucketItemEntity> getBucketItems(final Set<String> bucketIds) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sqlBuilder = new StringBuilder(
                "SELECT " +
                        "item.id as ID, " +
                        "item.name as NAME, " +
                        "item.description as DESCRIPTION, " +
                        "item.created as CREATED, " +
                        "item.modified as MODIFIED, " +
                        "item.item_type as ITEM_TYPE, " +
                        "b.id as BUCKET_ID, " +
                        "b.name as BUCKET_NAME " +
                "FROM " +
                        "bucket_item item, bucket b " +
                "WHERE " +
                        "item.bucket_id = b.id " +
                "AND " +
                        "item.bucket_id IN (");

        for (int i=0; i < bucketIds.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("?");
        }
        sqlBuilder.append(")");

        final List<BucketItemEntity> items = jdbcTemplate.query(sqlBuilder.toString(), bucketIds.toArray(), new BucketItemEntityRowMapper());
        return getItemsWithCounts(items);
    }

    private List<BucketItemEntity> getItemsWithCounts(final Iterable<BucketItemEntity> items) {
        final Map<String,Long> snapshotCounts = getFlowSnapshotCounts();

        final List<BucketItemEntity> itemWithCounts = new ArrayList<>();
        for (final BucketItemEntity item : items) {
            if (item.getType() == BucketItemEntityType.FLOW) {
                final Long snapshotCount = snapshotCounts.get(item.getId());
                if (snapshotCount != null) {
                    final FlowEntity flowEntity = (FlowEntity) item;
                    flowEntity.setSnapshotCount(snapshotCount);
                }
            }

            itemWithCounts.add(item);
        }

        return itemWithCounts;
    }

    private Map<String,Long> getFlowSnapshotCounts() {
        final String sql = "SELECT flow_id, count(*) FROM flow_snapshot GROUP BY flow_id";

        final Map<String,Long> results = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            results.put(rs.getString(1), rs.getLong(2));
        });
        return results;
    }

    private Long getFlowSnapshotCount(final String flowIdentifier) {
        final String sql = "SELECT count(*) FROM flow_snapshot WHERE flow_id = ?";

        return jdbcTemplate.queryForObject(sql, new Object[] {flowIdentifier}, (rs, num) -> {
            return rs.getLong(1);
        });
    }

    //----------------- Flows ---------------------------------

    @Override
    public FlowEntity createFlow(final FlowEntity flow) {
        final String itemSql = "INSERT INTO bucket_item (ID, NAME, DESCRIPTION, CREATED, MODIFIED, ITEM_TYPE, BUCKET_ID) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(itemSql,
                flow.getId(),
                flow.getName(),
                flow.getDescription(),
                flow.getCreated(),
                flow.getModified(),
                flow.getType().toString(),
                flow.getBucketId());

        final String flowSql = "INSERT INTO flow (ID) VALUES (?)";

        jdbcTemplate.update(flowSql, flow.getId());

        return flow;
    }

    @Override
    public FlowEntity getFlowById(final String flowIdentifier) {
        final String sql = "SELECT * FROM flow f, bucket_item item WHERE f.id = ? AND item.id = f.id";
        try {
            return jdbcTemplate.queryForObject(sql, new FlowEntityRowMapper(), flowIdentifier);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public FlowEntity getFlowByIdWithSnapshotCounts(final String flowIdentifier) {
        final FlowEntity flowEntity = getFlowById(flowIdentifier);
        if (flowEntity == null) {
            return flowEntity;
        }

        final Long snapshotCount = getFlowSnapshotCount(flowIdentifier);
        if (snapshotCount != null) {
            flowEntity.setSnapshotCount(snapshotCount);
        }

        return flowEntity;
    }

    @Override
    public List<FlowEntity> getFlowsByName(final String name) {
        final String sql = "SELECT * FROM flow f, bucket_item item WHERE item.name = ? AND item.id = f.id";
        return jdbcTemplate.query(sql, new Object[] {name}, new FlowEntityRowMapper());
    }

    @Override
    public List<FlowEntity> getFlowsByName(final String bucketIdentifier, final String name) {
        final String sql = "SELECT * FROM flow f, bucket_item item WHERE item.name = ? AND item.id = f.id AND item.bucket_id = ?";
        return jdbcTemplate.query(sql, new Object[] {name, bucketIdentifier}, new FlowEntityRowMapper());
    }

    @Override
    public List<FlowEntity> getFlowsByBucket(final String bucketIdentifier) {
        final String sql = "SELECT * FROM flow f, bucket_item item WHERE item.bucket_id = ? AND item.id = f.id";
        final List<FlowEntity> flows = jdbcTemplate.query(sql, new Object[] {bucketIdentifier}, new FlowEntityRowMapper());

        final Map<String,Long> snapshotCounts = getFlowSnapshotCounts();
        for (final FlowEntity flowEntity : flows) {
            final Long snapshotCount = snapshotCounts.get(flowEntity.getId());
            if (snapshotCount != null) {
                flowEntity.setSnapshotCount(snapshotCount);
            }
        }

        return flows;
    }

    @Override
    public FlowEntity updateFlow(final FlowEntity flow) {
        flow.setModified(new Date());

        final String sql = "UPDATE bucket_item SET name = ?, description = ?, modified = ? WHERE id = ?";
        jdbcTemplate.update(sql, flow.getName(), flow.getDescription(), flow.getModified(), flow.getId());
        return flow;
    }

    @Override
    public void deleteFlow(final FlowEntity flow) {
        final String snapshotDeleteSql = "DELETE FROM flow_snapshot WHERE flow_id = ?";
        jdbcTemplate.update(snapshotDeleteSql, flow.getId());

        final String flowDeleteSql = "DELETE FROM flow WHERE id = ?";
        jdbcTemplate.update(flowDeleteSql, flow.getId());

        final String itemDeleteSql = "DELETE FROM bucket_item WHERE id = ?";
        jdbcTemplate.update(itemDeleteSql, flow.getId());
    }

    //----------------- Flow Snapshots ---------------------------------

    @Override
    public FlowSnapshotEntity createFlowSnapshot(final FlowSnapshotEntity flowSnapshot) {
        final String sql = "INSERT INTO flow_snapshot (FLOW_ID, VERSION, CREATED, CREATED_BY, COMMENTS) VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                flowSnapshot.getFlowId(),
                flowSnapshot.getVersion(),
                flowSnapshot.getCreated(),
                flowSnapshot.getCreatedBy(),
                flowSnapshot.getComments());

        return flowSnapshot;
    }

    @Override
    public FlowSnapshotEntity getFlowSnapshot(final String flowIdentifier, final Integer version) {
        final String sql =
                "SELECT " +
                        "fs.flow_id, " +
                        "fs.version, " +
                        "fs.created, " +
                        "fs.created_by, " +
                        "fs.comments " +
                "FROM " +
                        "flow_snapshot fs, " +
                        "flow f, " +
                        "bucket_item item " +
                "WHERE " +
                        "item.id = f.id AND " +
                        "f.id = ? AND " +
                        "f.id = fs.flow_id AND " +
                        "fs.version = ?";

        try {
            return jdbcTemplate.queryForObject(sql, new FlowSnapshotEntityRowMapper(),
                    flowIdentifier, version);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public FlowSnapshotEntity getLatestSnapshot(final String flowIdentifier) {
        final String sql = "SELECT * FROM flow_snapshot WHERE flow_id = ? ORDER BY version DESC LIMIT 1";

        try {
            return jdbcTemplate.queryForObject(sql, new FlowSnapshotEntityRowMapper(), flowIdentifier);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<FlowSnapshotEntity> getSnapshots(final String flowIdentifier) {
        final String sql =
                "SELECT " +
                        "fs.flow_id, " +
                        "fs.version, " +
                        "fs.created, " +
                        "fs.created_by, " +
                        "fs.comments " +
                "FROM " +
                        "flow_snapshot fs, " +
                        "flow f, " +
                        "bucket_item item " +
                "WHERE " +
                        "item.id = f.id AND " +
                        "f.id = ? AND " +
                        "f.id = fs.flow_id";

        final Object[] args = new Object[] { flowIdentifier };
        return jdbcTemplate.query(sql, args, new FlowSnapshotEntityRowMapper());
    }

    @Override
    public void deleteFlowSnapshot(final FlowSnapshotEntity flowSnapshot) {
        final String sql = "DELETE FROM flow_snapshot WHERE flow_id = ? AND version = ?";
        jdbcTemplate.update(sql, flowSnapshot.getFlowId(), flowSnapshot.getVersion());
    }

    //----------------- BucketItems ---------------------------------

    @Override
    public Set<String> getBucketFields() {
        final Set<String> fields = new LinkedHashSet<>();
        fields.add("ID");
        fields.add("NAME");
        fields.add("DESCRIPTION");
        fields.add("CREATED");
        return fields;
    }

    @Override
    public Set<String> getBucketItemFields() {
        final Set<String> fields = new LinkedHashSet<>();
        fields.add("ID");
        fields.add("NAME");
        fields.add("DESCRIPTION");
        fields.add("CREATED");
        fields.add("MODIFIED");
        fields.add("ITEM_TYPE");
        fields.add("BUCKET_ID");
        return fields;
    }

    @Override
    public Set<String> getFlowFields() {
        final Set<String> fields = new LinkedHashSet<>();
        fields.add("ID");
        fields.add("NAME");
        fields.add("DESCRIPTION");
        fields.add("CREATED");
        fields.add("MODIFIED");
        fields.add("ITEM_TYPE");
        fields.add("BUCKET_ID");
        return fields;
    }
}
