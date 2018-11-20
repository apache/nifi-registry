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
package org.apache.nifi.registry.db.mapper;

import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntityType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExtensionBundleEntityRowMapper implements RowMapper<ExtensionBundleEntity> {

    @Override
    public ExtensionBundleEntity mapRow(final ResultSet rs, final int i) throws SQLException {
        final ExtensionBundleEntity entity = new ExtensionBundleEntity();

        // BucketItemEntity fields
        entity.setId(rs.getString("ID"));
        entity.setName(rs.getString("NAME"));
        entity.setDescription(rs.getString("DESCRIPTION"));
        entity.setCreated(rs.getTimestamp("CREATED"));
        entity.setModified(rs.getTimestamp("MODIFIED"));
        entity.setBucketId(rs.getString("BUCKET_ID"));
        entity.setType(BucketItemEntityType.EXTENSION_BUNDLE);

        // ExtensionBundleEntity fields
        entity.setBundleType(ExtensionBundleEntityType.valueOf(rs.getString("BUNDLE_TYPE")));
        entity.setGroupId(rs.getString("GROUP_ID"));
        entity.setArtifactId(rs.getString("ARTIFACT_ID"));

        return entity;
    }

}
