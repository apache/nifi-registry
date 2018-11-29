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

import org.apache.nifi.registry.db.entity.ExtensionBundleVersionEntity;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExtensionBundleVersionEntityRowMapper implements RowMapper<ExtensionBundleVersionEntity> {

    @Override
    public ExtensionBundleVersionEntity mapRow(final ResultSet rs, final int i) throws SQLException {
        final ExtensionBundleVersionEntity entity = new ExtensionBundleVersionEntity();
        entity.setId(rs.getString("ID"));
        entity.setExtensionBundleId(rs.getString("EXTENSION_BUNDLE_ID"));
        entity.setVersion(rs.getString("VERSION"));
        entity.setSha256Hex(rs.getString("SHA_256_HEX"));
        entity.setSha256Supplied(rs.getInt("SHA_256_SUPPLIED") == 1);

        entity.setCreated(rs.getTimestamp("CREATED"));
        entity.setCreatedBy(rs.getString("CREATED_BY"));
        entity.setDescription(rs.getString("DESCRIPTION"));

        return entity;
    }

}
