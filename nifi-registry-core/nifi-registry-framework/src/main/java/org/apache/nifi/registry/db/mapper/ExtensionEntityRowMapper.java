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

import org.apache.nifi.registry.db.entity.ExtensionEntity;
import org.apache.nifi.registry.db.entity.ExtensionEntityCategory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExtensionEntityRowMapper implements RowMapper<ExtensionEntity> {

    @Override
    public ExtensionEntity mapRow(ResultSet rs, int i) throws SQLException {
        final ExtensionEntity entity = new ExtensionEntity();
        entity.setId(rs.getString("ID"));
        entity.setExtensionBundleVersionId(rs.getString("EXTENSION_BUNDLE_VERSION_ID"));
        entity.setType(rs.getString("TYPE"));
        entity.setTypeDescription(rs.getString("TYPE_DESCRIPTION"));
        entity.setRestricted(rs.getInt("IS_RESTRICTED") == 1);
        entity.setCategory(ExtensionEntityCategory.valueOf(rs.getString("CATEGORY")));
        entity.setTags(rs.getString("TAGS"));
        return entity;
    }

}
