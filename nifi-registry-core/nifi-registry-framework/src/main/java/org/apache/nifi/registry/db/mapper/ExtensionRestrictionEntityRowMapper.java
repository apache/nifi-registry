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

import org.apache.nifi.registry.db.entity.ExtensionRestrictionEntity;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExtensionRestrictionEntityRowMapper implements RowMapper<ExtensionRestrictionEntity> {

    @Override
    public ExtensionRestrictionEntity mapRow(final ResultSet rs, final int i) throws SQLException {
        final ExtensionRestrictionEntity entity = new ExtensionRestrictionEntity();
        entity.setId(rs.getString("ID"));
        entity.setExtensionId(rs.getString("EXTENSION_ID"));
        entity.setRequiredPermission(rs.getString("REQUIRED_PERMISSION"));
        entity.setExplanation(rs.getString("EXPLANATION"));
        return entity;
    }

}
