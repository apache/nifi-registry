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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionDependencyEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionEntity;
import org.apache.nifi.registry.db.entity.ExtensionEntity;
import org.apache.nifi.registry.db.entity.ExtensionEntityCategory;
import org.apache.nifi.registry.db.entity.ExtensionProvidedServiceApiEntity;
import org.apache.nifi.registry.db.entity.ExtensionRestrictionEntity;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.mapper.BucketEntityRowMapper;
import org.apache.nifi.registry.db.mapper.BucketItemEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionBundleEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionBundleEntityWithBucketNameRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionBundleVersionDependencyEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionBundleVersionEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionProvidedServiceApiEntityRowMapper;
import org.apache.nifi.registry.db.mapper.ExtensionRestrictionEntityRowMapper;
import org.apache.nifi.registry.db.mapper.FlowEntityRowMapper;
import org.apache.nifi.registry.db.mapper.FlowSnapshotEntityRowMapper;
import org.apache.nifi.registry.extension.filter.ExtensionBundleFilterParams;
import org.apache.nifi.registry.extension.filter.ExtensionBundleVersionFilterParams;
import org.apache.nifi.registry.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
        final String sql = "INSERT INTO bucket (ID, NAME, DESCRIPTION, CREATED, ALLOW_EXTENSION_BUNDLE_REDEPLOY) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                b.getId(),
                b.getName(),
                b.getDescription(),
                b.getCreated(),
                b.isAllowExtensionBundleRedeploy() ? 1 : 0);
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
        final String sql = "UPDATE bucket SET name = ?, description = ?, allow_extension_bundle_redeploy = ? WHERE id = ?";
        jdbcTemplate.update(sql, bucket.getName(), bucket.getDescription(), bucket.isAllowExtensionBundleRedeploy() ? 1 : 0, bucket.getId());
        return bucket;
    }

    @Override
    public void deleteBucket(final BucketEntity bucket) {
        // NOTE: Cascading deletes will delete from all child tables
        final String sql = "DELETE FROM bucket WHERE id = ?";
        jdbcTemplate.update(sql, bucket.getId());
    }

    @Override
    public List<BucketEntity> getBuckets(final Set<String> bucketIds) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM bucket WHERE ");
        addIdentifiersInClause(sqlBuilder, "id", bucketIds);
        sqlBuilder.append("ORDER BY name ASC");

        return jdbcTemplate.query(sqlBuilder.toString(), bucketIds.toArray(), new BucketEntityRowMapper());
    }

    @Override
    public List<BucketEntity> getAllBuckets() {
        final String sql = "SELECT * FROM bucket ORDER BY name ASC";
        return jdbcTemplate.query(sql, new BucketEntityRowMapper());
    }

    //----------------- BucketItems ---------------------------------

    private static final String BASE_BUCKET_ITEMS_SQL =
            "SELECT " +
                "item.id as ID, " +
                "item.name as NAME, " +
                "item.description as DESCRIPTION, " +
                "item.created as CREATED, " +
                "item.modified as MODIFIED, " +
                "item.item_type as ITEM_TYPE, " +
                "b.id as BUCKET_ID, " +
                "b.name as BUCKET_NAME ," +
                "eb.bundle_type as BUNDLE_TYPE, " +
                "eb.group_id as BUNDLE_GROUP_ID, " +
                "eb.artifact_id as BUNDLE_ARTIFACT_ID " +
            "FROM bucket_item item " +
            "INNER JOIN bucket b ON item.bucket_id = b.id " +
            "LEFT JOIN extension_bundle eb ON item.id = eb.id ";

    @Override
    public List<BucketItemEntity> getBucketItems(final String bucketIdentifier) {
        final String sql = BASE_BUCKET_ITEMS_SQL + " WHERE item.bucket_id = ?";
        final List<BucketItemEntity> items = jdbcTemplate.query(sql, new Object[] { bucketIdentifier }, new BucketItemEntityRowMapper());
        return getItemsWithCounts(items);
    }

    @Override
    public List<BucketItemEntity> getBucketItems(final Set<String> bucketIds) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sqlBuilder = new StringBuilder(BASE_BUCKET_ITEMS_SQL + " WHERE item.bucket_id IN (");
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
        final Map<String,Long> extensionBundleVersionCounts = getExtensionBundleVersionCounts();

        final List<BucketItemEntity> itemWithCounts = new ArrayList<>();
        for (final BucketItemEntity item : items) {
            if (item.getType() == BucketItemEntityType.FLOW) {
                final Long snapshotCount = snapshotCounts.get(item.getId());
                if (snapshotCount != null) {
                    final FlowEntity flowEntity = (FlowEntity) item;
                    flowEntity.setSnapshotCount(snapshotCount);
                }
            } else if (item.getType() == BucketItemEntityType.EXTENSION_BUNDLE) {
                final Long versionCount = extensionBundleVersionCounts.get(item.getId());
                if (versionCount != null) {
                    final ExtensionBundleEntity extensionBundleEntity = (ExtensionBundleEntity) item;
                    extensionBundleEntity.setVersionCount(versionCount);
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

    private Map<String,Long> getExtensionBundleVersionCounts() {
        final String sql = "SELECT extension_bundle_id, count(*) FROM extension_bundle_version GROUP BY extension_bundle_id";

        final Map<String,Long> results = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            results.put(rs.getString(1), rs.getLong(2));
        });
        return results;
    }

    private Long getExtensionBundleVersionCount(final String extensionBundleIdentifier) {
        final String sql = "SELECT count(*) FROM extension_bundle_version WHERE extension_bundle_id = ?";

        return jdbcTemplate.queryForObject(sql, new Object[] {extensionBundleIdentifier}, (rs, num) -> {
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
        // NOTE: Cascading deletes will delete from child tables
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

    //----------------- Extension Bundles ---------------------------------

    @Override
    public ExtensionBundleEntity createExtensionBundle(final ExtensionBundleEntity extensionBundle) {
        final String itemSql =
                "INSERT INTO bucket_item (" +
                    "ID, " +
                    "NAME, " +
                    "DESCRIPTION, " +
                    "CREATED, " +
                    "MODIFIED, " +
                    "ITEM_TYPE, " +
                    "BUCKET_ID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(itemSql,
                extensionBundle.getId(),
                extensionBundle.getName(),
                extensionBundle.getDescription(),
                extensionBundle.getCreated(),
                extensionBundle.getModified(),
                extensionBundle.getType().toString(),
                extensionBundle.getBucketId());

        final String bundleSql =
                "INSERT INTO extension_bundle (" +
                    "ID, " +
                    "BUCKET_ID, " +
                    "BUNDLE_TYPE, " +
                    "GROUP_ID, " +
                    "ARTIFACT_ID) " +
                "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(bundleSql,
                extensionBundle.getId(),
                extensionBundle.getBucketId(),
                extensionBundle.getBundleType().toString(),
                extensionBundle.getGroupId(),
                extensionBundle.getArtifactId());

        return extensionBundle;
    }

    @Override
    public ExtensionBundleEntity getExtensionBundle(final String extensionBundleId) {
        final String sql =
                "SELECT * " +
                "FROM extension_bundle eb, bucket_item item " +
                "WHERE eb.id = ? AND item.id = eb.id";
        try {
            final ExtensionBundleEntity entity = jdbcTemplate.queryForObject(sql, new ExtensionBundleEntityRowMapper(), extensionBundleId);

            final Long versionCount = getExtensionBundleVersionCount(extensionBundleId);
            if (versionCount != null) {
                entity.setVersionCount(versionCount);
            }

            return entity;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public ExtensionBundleEntity getExtensionBundle(final String bucketId, final String groupId, final String artifactId) {
        final String sql =
                "SELECT * " +
                "FROM " +
                        "extension_bundle eb, " +
                        "bucket_item item " +
                "WHERE " +
                        "item.id = eb.id AND " +
                        "eb.bucket_id = ? AND " +
                        "eb.group_id = ? AND " +
                        "eb.artifact_id = ?";
        try {
            final ExtensionBundleEntity entity = jdbcTemplate.queryForObject(sql, new ExtensionBundleEntityRowMapper(), bucketId, groupId, artifactId);

            final Long versionCount = getExtensionBundleVersionCount(entity.getId());
            if (versionCount != null) {
                entity.setVersionCount(versionCount);
            }

            return entity;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<ExtensionBundleEntity> getExtensionBundles(final Set<String> bucketIds, final ExtensionBundleFilterParams filterParams) {
        if (bucketIds == null || bucketIds.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Object> args = new ArrayList<>();

        final StringBuilder sqlBuilder = new StringBuilder(
                "SELECT " +
                        "item.id as ID, " +
                        "item.name as NAME, " +
                        "item.description as DESCRIPTION, " +
                        "item.created as CREATED, " +
                        "item.modified as MODIFIED, " +
                        "item.item_type as ITEM_TYPE, " +
                        "b.id as BUCKET_ID, " +
                        "b.name as BUCKET_NAME ," +
                        "eb.bundle_type as BUNDLE_TYPE, " +
                        "eb.group_id as BUNDLE_GROUP_ID, " +
                        "eb.artifact_id as BUNDLE_ARTIFACT_ID " +
                "FROM " +
                    "extension_bundle eb, " +
                    "bucket_item item, " +
                    "bucket b " +
                "WHERE " +
                    "item.id = eb.id AND " +
                    "b.id = item.bucket_id");

        if (filterParams != null) {
            final String bucketName = filterParams.getBucketName();
            if (!StringUtils.isBlank(bucketName)) {
                sqlBuilder.append(" AND b.name LIKE ? ");
                args.add(bucketName);
            }

            final String groupId = filterParams.getGroupId();
            if (!StringUtils.isBlank(groupId)) {
                sqlBuilder.append(" AND eb.group_id LIKE ? ");
                args.add(groupId);
            }

            final String artifactId = filterParams.getArtifactId();
            if (!StringUtils.isBlank(artifactId)) {
                sqlBuilder.append(" AND eb.artifact_id LIKE ? ");
                args.add(artifactId);
            }
        }

        sqlBuilder.append(" AND ");
        addIdentifiersInClause(sqlBuilder, "item.bucket_id", bucketIds);
        sqlBuilder.append("ORDER BY eb.group_id ASC, eb.artifact_id ASC");

        args.addAll(bucketIds);

        final List<ExtensionBundleEntity> bundleEntities = jdbcTemplate.query(sqlBuilder.toString(), args.toArray(), new ExtensionBundleEntityWithBucketNameRowMapper());
        return populateVersionCounts(bundleEntities);
    }

    @Override
    public List<ExtensionBundleEntity> getExtensionBundlesByBucket(final String bucketId) {
        final String sql =
                "SELECT * " +
                "FROM " +
                    "extension_bundle eb, " +
                    "bucket_item item " +
                "WHERE " +
                    "item.id = eb.id AND " +
                    "item.bucket_id = ? " +
                    "ORDER BY eb.group_id ASC, eb.artifact_id ASC";

        final List<ExtensionBundleEntity> bundles = jdbcTemplate.query(sql, new Object[]{bucketId}, new ExtensionBundleEntityRowMapper());
        return populateVersionCounts(bundles);
    }

    @Override
    public List<ExtensionBundleEntity> getExtensionBundlesByBucketAndGroup(String bucketId, String groupId) {
        final String sql =
                "SELECT * " +
                    "FROM " +
                        "extension_bundle eb, " +
                        "bucket_item item " +
                    "WHERE " +
                        "item.id = eb.id AND " +
                        "item.bucket_id = ? AND " +
                        "eb.group_id = ?" +
                    "ORDER BY eb.group_id ASC, eb.artifact_id ASC";

        final List<ExtensionBundleEntity> bundles = jdbcTemplate.query(sql, new Object[]{bucketId, groupId}, new ExtensionBundleEntityRowMapper());
        return populateVersionCounts(bundles);
    }

    private List<ExtensionBundleEntity> populateVersionCounts(final List<ExtensionBundleEntity> bundles) {
        if (!bundles.isEmpty()) {
            final Map<String, Long> versionCounts = getExtensionBundleVersionCounts();
            for (final ExtensionBundleEntity entity : bundles) {
                final Long versionCount = versionCounts.get(entity.getId());
                if (versionCount != null) {
                    entity.setVersionCount(versionCount);
                }
            }
        }

        return bundles;
    }

    @Override
    public void deleteExtensionBundle(final ExtensionBundleEntity extensionBundle) {
        deleteExtensionBundle(extensionBundle.getId());
    }

    @Override
    public void deleteExtensionBundle(final String extensionBundleId) {
        // NOTE: All of the foreign key constraints for extension related tables are set to cascade on delete
        final String itemDeleteSql = "DELETE FROM bucket_item WHERE id = ?";
        jdbcTemplate.update(itemDeleteSql, extensionBundleId);
    }

    //----------------- Extension Bundle Versions ---------------------------------

    @Override
    public ExtensionBundleVersionEntity createExtensionBundleVersion(final ExtensionBundleVersionEntity extensionBundleVersion) {
        final String sql =
                "INSERT INTO extension_bundle_version (" +
                    "ID, " +
                    "EXTENSION_BUNDLE_ID, " +
                    "VERSION, " +
                    "CREATED, " +
                    "CREATED_BY, " +
                    "DESCRIPTION, " +
                    "SHA_256_HEX, " +
                    "SHA_256_SUPPLIED," +
                    "CONTENT_SIZE, " +
                    "SYSTEM_API_VERSION, " +
                    "BUILD_TOOL, " +
                    "BUILD_FLAGS, " +
                    "BUILD_BRANCH, " +
                    "BUILD_TAG, " +
                    "BUILD_REVISION, " +
                    "BUILT, " +
                    "BUILT_BY," +
                    "DOCS_CONTENT " +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                extensionBundleVersion.getId(),
                extensionBundleVersion.getExtensionBundleId(),
                extensionBundleVersion.getVersion(),
                extensionBundleVersion.getCreated(),
                extensionBundleVersion.getCreatedBy(),
                extensionBundleVersion.getDescription(),
                extensionBundleVersion.getSha256Hex(),
                extensionBundleVersion.getSha256Supplied() ? 1 : 0,
                extensionBundleVersion.getContentSize(),
                extensionBundleVersion.getSystemApiVersion(),
                extensionBundleVersion.getBuildTool(),
                extensionBundleVersion.getBuildFlags(),
                extensionBundleVersion.getBuildBranch(),
                extensionBundleVersion.getBuildTag(),
                extensionBundleVersion.getBuildRevision(),
                extensionBundleVersion.getBuilt(),
                extensionBundleVersion.getBuiltBy(),
                extensionBundleVersion.getDocsContent());

        return extensionBundleVersion;
    }

    private static final String BASE_EXTENSION_BUNDLE_VERSION_SQL =
            "SELECT " +
                "ebv.id AS ID," +
                "ebv.extension_bundle_id AS EXTENSION_BUNDLE_ID, " +
                "ebv.version AS VERSION, " +
                "ebv.created AS CREATED, " +
                "ebv.created_by AS CREATED_BY, " +
                "ebv.description AS DESCRIPTION, " +
                "ebv.sha_256_hex AS SHA_256_HEX, " +
                "ebv.sha_256_supplied AS SHA_256_SUPPLIED ," +
                "ebv.content_size AS CONTENT_SIZE, " +
                "ebv.system_api_version AS SYSTEM_API_VERSION, " +
                "ebv.build_tool AS BUILD_TOOL, " +
                "ebv.build_flags AS BUILD_FLAGS, " +
                "ebv.build_branch AS BUILD_BRANCH, " +
                "ebv.build_tag AS BUILD_TAG, " +
                "ebv.build_revision AS BUILD_REVISION, " +
                "ebv.built AS BUILT, " +
                "ebv.built_by AS BUILT_BY, " +
                "eb.bucket_id AS BUCKET_ID " +
            "FROM extension_bundle eb, extension_bundle_version ebv " +
            "WHERE eb.id = ebv.extension_bundle_id ";

    @Override
    public ExtensionBundleVersionEntity getExtensionBundleVersion(final String extensionBundleId, final String version) {
        final String sql = BASE_EXTENSION_BUNDLE_VERSION_SQL +
                " AND ebv.extension_bundle_id = ? AND ebv.version = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new ExtensionBundleVersionEntityRowMapper(), extensionBundleId, version);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public ExtensionBundleVersionEntity getExtensionBundleVersion(final String bucketId, final String groupId, final String artifactId, final String version) {
        final String sql = BASE_EXTENSION_BUNDLE_VERSION_SQL +
                    "AND eb.bucket_id = ? " +
                    "AND eb.group_id = ? " +
                    "AND eb.artifact_id = ? " +
                    "AND ebv.version = ?";

        try {
            return jdbcTemplate.queryForObject(sql, new ExtensionBundleVersionEntityRowMapper(), bucketId, groupId, artifactId, version);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<ExtensionBundleVersionEntity> getExtensionBundleVersions(final Set<String> bucketIdentifiers, final ExtensionBundleVersionFilterParams filterParams) {
        if (bucketIdentifiers == null || bucketIdentifiers.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Object> args = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(BASE_EXTENSION_BUNDLE_VERSION_SQL);

        if (filterParams != null) {
            final String groupId = filterParams.getGroupId();
            if (!StringUtils.isBlank(groupId)) {
                sqlBuilder.append(" AND eb.group_id LIKE ? ");
                args.add(groupId);
            }

            final String artifactId = filterParams.getArtifactId();
            if (!StringUtils.isBlank(artifactId)) {
                sqlBuilder.append(" AND eb.artifact_id LIKE ? ");
                args.add(artifactId);
            }

            final String version = filterParams.getVersion();
            if (!StringUtils.isBlank(version)) {
                sqlBuilder.append(" AND ebv.version LIKE ? ");
                args.add(version);
            }
        }

        sqlBuilder.append(" AND ");
        addIdentifiersInClause(sqlBuilder, "eb.bucket_id", bucketIdentifiers);
        args.addAll(bucketIdentifiers);

        final List<ExtensionBundleVersionEntity> bundleVersionEntities = jdbcTemplate.query(
                sqlBuilder.toString(), args.toArray(), new ExtensionBundleVersionEntityRowMapper());

        return bundleVersionEntities;
    }

    private void addIdentifiersInClause(StringBuilder sqlBuilder, String idFieldName, Set<String> identifiers) {
        sqlBuilder.append(idFieldName).append(" IN (");
        for (int i = 0; i < identifiers.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("?");
        }
        sqlBuilder.append(") ");
    }

    @Override
    public List<ExtensionBundleVersionEntity> getExtensionBundleVersions(final String extensionBundleId) {
        final String sql = BASE_EXTENSION_BUNDLE_VERSION_SQL + " AND ebv.extension_bundle_id = ?";
        return jdbcTemplate.query(sql, new Object[]{extensionBundleId}, new ExtensionBundleVersionEntityRowMapper());
    }

    @Override
    public List<ExtensionBundleVersionEntity> getExtensionBundleVersions(final String bucketId, final String groupId, final String artifactId) {
        final String sql = BASE_EXTENSION_BUNDLE_VERSION_SQL +
                    "AND eb.bucket_id = ? " +
                    "AND eb.group_id = ? " +
                    "AND eb.artifact_id = ? ";

        final Object[] args = {bucketId, groupId, artifactId};
        return jdbcTemplate.query(sql, args, new ExtensionBundleVersionEntityRowMapper());
    }

    @Override
    public List<ExtensionBundleVersionEntity> getExtensionBundleVersionsGlobal(final String groupId, final String artifactId, final String version) {
        final String sql = BASE_EXTENSION_BUNDLE_VERSION_SQL +
                "AND eb.group_id = ? " +
                "AND eb.artifact_id = ? " +
                "AND ebv.version = ?";

        final Object[] args = {groupId, artifactId, version};
        return jdbcTemplate.query(sql, args, new ExtensionBundleVersionEntityRowMapper());
    }

    @Override
    public void deleteExtensionBundleVersion(final ExtensionBundleVersionEntity extensionBundleVersion) {
        deleteExtensionBundleVersion(extensionBundleVersion.getId());
    }

    @Override
    public void deleteExtensionBundleVersion(final String extensionBundleVersionId) {
        // NOTE: All of the foreign key constraints for extension related tables are set to cascade on delete
        final String sql = "DELETE FROM extension_bundle_version WHERE id = ?";
        jdbcTemplate.update(sql, extensionBundleVersionId);
    }

    //------------ Extension Bundle Version Dependencies ------------

    @Override
    public ExtensionBundleVersionDependencyEntity createDependency(final ExtensionBundleVersionDependencyEntity dependencyEntity) {
        final String dependencySql =
                "INSERT INTO extension_bundle_version_dependency (" +
                    "ID, " +
                    "EXTENSION_BUNDLE_VERSION_ID, " +
                    "GROUP_ID, " +
                    "ARTIFACT_ID, " +
                    "VERSION " +
                ") VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(dependencySql,
                dependencyEntity.getId(),
                dependencyEntity.getExtensionBundleVersionId(),
                dependencyEntity.getGroupId(),
                dependencyEntity.getArtifactId(),
                dependencyEntity.getVersion());

        return dependencyEntity;
    }

    @Override
    public List<ExtensionBundleVersionDependencyEntity> getDependenciesForBundleVersion(final String extensionBundleVersionId) {
        final String sql = "SELECT * FROM extension_bundle_version_dependency WHERE extension_bundle_version_id = ?";
        final Object[] args = {extensionBundleVersionId};
        return jdbcTemplate.query(sql, args, new ExtensionBundleVersionDependencyEntityRowMapper());
    }


    //----------------- Extensions ---------------------------------

    private static String BASE_EXTENSION_SQL =
            "SELECT " +
                "e.id AS ID, " +
                "e.extension_bundle_version_id AS EXTENSION_BUNDLE_VERSION_ID, " +
                "e.name AS NAME, " +
                "e.description AS DESCRIPTION, " +
                "e.general_restriction AS GENERAL_RESTRICTION, " +
                "e.category AS CATEGORY, " +
                "e.tags AS TAGS " +
            "FROM extension e";

    @Override
    public ExtensionEntity createExtension(final ExtensionEntity extension) {
        final String insertExtensionSql =
                "INSERT INTO extension (" +
                    "ID, " +
                    "EXTENSION_BUNDLE_VERSION_ID, " +
                    "NAME, " +
                    "DESCRIPTION, " +
                    "GENERAL_RESTRICTION, " +
                    "CATEGORY, " +
                    "TAGS, " +
                    "ADDITIONAL_DETAILS " +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(insertExtensionSql,
                extension.getId(),
                extension.getExtensionBundleVersionId(),
                extension.getName(),
                extension.getDescription(),
                extension.getGeneralRestriction(),
                extension.getCategory().toString(),
                extension.getTags(),
                extension.getAdditionalDetails()
        );

        // insert tags...

        final String insertTagSql = "INSERT INTO extension_tag (EXTENSION_ID, TAG) VALUES (?, ?);";

        if (extension.getTags() != null) {
            final String tags[] = extension.getTags().split("[,]");
            for (final String tag : tags) {
                if (tag != null) {
                    jdbcTemplate.update(insertTagSql, extension.getId(), tag.trim().toLowerCase());
                }
            }
        }

        // insert provided service APIs...

        final Set<ExtensionProvidedServiceApiEntity> providedServiceApis = extension.getProvidedServiceApis();
        if (providedServiceApis != null) {
            providedServiceApis.forEach(p -> createProvidedServiceApi(p));
        }

        // insert restrictions...

        final Set<ExtensionRestrictionEntity> restrictions = extension.getRestrictions();
        if (restrictions != null) {
            restrictions.forEach(r -> createRestriction(r));
        }

        return extension;
    }

    @Override
    public ExtensionEntity getExtensionById(final String id) {
        final String selectSql = BASE_EXTENSION_SQL + " WHERE e.id = ?";
        try {
            final ExtensionEntity extension = jdbcTemplate.queryForObject(selectSql, new ExtensionEntityRowMapper(), id);
            populateExtensionAssociations(Collections.singletonList(extension));
            return extension;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<ExtensionEntity> getAllExtensions() {
        final String selectSql = BASE_EXTENSION_SQL +  " ORDER BY e.name ASC";

        final List<ExtensionEntity> extensions = jdbcTemplate.query(selectSql, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionEntity> getExtensionsByBundleVersionId(final String extensionBundleVersionId) {
        final String selectSql = BASE_EXTENSION_SQL + " WHERE e.extension_bundle_version_id = ?";

        final Object[] args = { extensionBundleVersionId };
        final List<ExtensionEntity> extensions = jdbcTemplate.query(selectSql, args, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionEntity> getExtensionsByBundleCoordinate(final String bucketId, final String groupId, final String artifactId, final String version) {
        final String sql =
                BASE_EXTENSION_SQL +
                ", extension_bundle eb, extension_bundle_version ebv " +
                "WHERE eb.id = ebv.extension_bundle_id " +
                    "AND ebv.id = e.extension_bundle_version_id " +
                    "AND eb.bucket_id = ? " +
                    "AND eb.group_id = ? " +
                    "AND eb.artifact_id = ? " +
                    "AND ebv.version = ?";

        final Object[] args = { bucketId, groupId, artifactId, version };
        final List<ExtensionEntity> extensions = jdbcTemplate.query(sql, args, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionEntity> getExtensionsByCategory(final ExtensionEntityCategory category) {
        final String selectSql = BASE_EXTENSION_SQL + " WHERE e.category = ?";

        final Object[] args = { category.toString() };
        final List<ExtensionEntity> extensions = jdbcTemplate.query(selectSql, args, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionEntity> getExtensionsByProvidedServiceApi(final String className, final String groupId, final String artifactId, final String version) {
        final String selectSql =
                BASE_EXTENSION_SQL +
                    ", extension_provided_service_api ep " +
                    "WHERE e.id = ep.extension_id " +
                        "AND ep.class_name = ? " +
                        "AND ep.group_id = ? " +
                        "AND ep.artifact_id = ? " +
                        "AND ep.version = ?";

        final Object[] args = {className, groupId, artifactId, version};
        final List<ExtensionEntity> extensions = jdbcTemplate.query(selectSql, args, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionEntity> getExtensionsByTag(final String tag) {
        final String selectSql =
                BASE_EXTENSION_SQL +
                ", extension_tag et " +
                "WHERE e.id = et.extension_id AND et.tag = ?";

        final Object[] args = { tag.trim().toLowerCase() };
        final List<ExtensionEntity> extensions = jdbcTemplate.query(selectSql, args, new ExtensionEntityRowMapper());
        populateExtensionAssociations(extensions);
        return extensions;
    }

    @Override
    public Set<String> getAllExtensionTags() {
        final String selectSql = "SELECT DISTINCT tag FROM extension_tag ORDER BY tag ASC";

        final Set<String> tags = new LinkedHashSet<>();
        final RowCallbackHandler handler = (rs) -> tags.add(rs.getString(1));
        jdbcTemplate.query(selectSql, handler);
        return tags;
    }

    @Override
    public void deleteExtension(final ExtensionEntity extension) {
        // NOTE: All of the foreign key constraints for extension related tables are set to cascade on delete
        final String deleteSql = "DELETE FROM extension WHERE id = ?";
        jdbcTemplate.update(deleteSql, extension.getId());
    }

    private void populateExtensionAssociations(final List<ExtensionEntity> extensions) {
        final Set<String> extensionIds = new HashSet<>();
        extensions.forEach(e -> (extensionIds).add(e.getId()));

        final Map<String,Set<ExtensionProvidedServiceApiEntity>> providedServiceApis = getProvidedServiceApisByExtensions(extensionIds);
        final Map<String,Set<ExtensionRestrictionEntity>> restrictions = getRestrictionsByExtensions(extensionIds);
        for (final ExtensionEntity extension : extensions) {
            extension.setProvidedServiceApis(providedServiceApis.computeIfAbsent(extension.getId(), (k) -> new HashSet<>()));
            extension.setRestrictions(restrictions.computeIfAbsent(extension.getId(), (k) -> new HashSet<>()));
        }
    }

    //----------------- Extension Provided Service APIs --------------------

    private ExtensionProvidedServiceApiEntity createProvidedServiceApi(final ExtensionProvidedServiceApiEntity providedServiceApi) {
        final String sql =
                "INSERT INTO extension_provided_service_api (" +
                    "ID, " +
                    "EXTENSION_ID, " +
                    "CLASS_NAME, " +
                    "GROUP_ID, " +
                    "ARTIFACT_ID, " +
                    "VERSION) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                providedServiceApi.getId(),
                providedServiceApi.getExtensionId(),
                providedServiceApi.getClassName(),
                providedServiceApi.getGroupId(),
                providedServiceApi.getArtifactId(),
                providedServiceApi.getVersion()
        );

        return providedServiceApi;
    }

    private List<ExtensionProvidedServiceApiEntity> getProvidedServiceApisByExtension(final String extensionId) {
        final String sql = "SELECT * FROM extension_provided_service_api WHERE EXTENSION_ID = ?";
        final Object[] args = {extensionId};
        return jdbcTemplate.query(sql, args, new ExtensionProvidedServiceApiEntityRowMapper());
    }

    private Map<String, Set<ExtensionProvidedServiceApiEntity>> getProvidedServiceApisByExtensions(final Set<String> extensionIds) {
        final Map<String,Set<ExtensionProvidedServiceApiEntity>> results = new HashMap<>();

        final StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM extension_provided_service_api WHERE ");
        addIdentifiersInClause(sqlBuilder, "extension_id", extensionIds);

        final Object[] args = extensionIds.toArray();
        final List<ExtensionProvidedServiceApiEntity> providedServiceApis = jdbcTemplate.query(
                sqlBuilder.toString(), args, new ExtensionProvidedServiceApiEntityRowMapper());

        for (final ExtensionProvidedServiceApiEntity entity : providedServiceApis) {
            final Set<ExtensionProvidedServiceApiEntity> entities = results.computeIfAbsent(
                    entity.getExtensionId(), (key) -> new HashSet<>());
            entities.add(entity);
        }

        return results;
    }

    //----------------- Extension Provided Service APIs --------------------

    private ExtensionRestrictionEntity createRestriction(final ExtensionRestrictionEntity restriction) {
        final String sql =
                "INSERT INTO extension_restriction(" +
                    "ID, " +
                    "EXTENSION_ID, " +
                    "REQUIRED_PERMISSION, " +
                    "EXPLANATION) " +
                "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                restriction.getId(),
                restriction.getExtensionId(),
                restriction.getRequiredPermission(),
                restriction.getExplanation());

        return restriction;
    }

    private List<ExtensionRestrictionEntity> getRestrictionsByExtension(final String extensionId) {
        final String sql = "SELECT * FROM extension_restriction WHERE extension_id = ?";
        final Object[] args = {extensionId};
        return jdbcTemplate.query(sql, args, new ExtensionRestrictionEntityRowMapper());
    }

    private Map<String, Set<ExtensionRestrictionEntity>> getRestrictionsByExtensions(final Set<String> extensionIds) {
        final Map<String,Set<ExtensionRestrictionEntity>> results = new HashMap<>();

        final StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM extension_restriction WHERE ");
        addIdentifiersInClause(sqlBuilder, "extension_id", extensionIds);

        final Object[] args = extensionIds.toArray();
        final List<ExtensionRestrictionEntity> restrictions = jdbcTemplate.query(
                sqlBuilder.toString(), args, new ExtensionRestrictionEntityRowMapper());

        for (final ExtensionRestrictionEntity entity : restrictions) {
            final Set<ExtensionRestrictionEntity> entities = results.computeIfAbsent(
                    entity.getExtensionId(), (key) -> new HashSet<>());
            entities.add(entity);
        }

        return results;
    }

    //----------------- Fields ---------------------------------

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
