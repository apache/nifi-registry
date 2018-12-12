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
package org.apache.nifi.registry.bundle.model;

import org.apache.nifi.registry.bundle.util.BundleUtils;

import java.util.Date;

/**
 * Details about how the bundle was built.
 */
public class BuildDetails {

    public static final String NA = "N/A";
    public static final String UNKNOWN = "unknown";

    private final String tool;
    private final String flags;

    private final String branch;
    private final String tag;
    private final String revision;

    private final String builtBy;
    private final Date built;

    private BuildDetails(final Builder builder) {
        this.tool = builder.tool == null ? UNKNOWN : builder.tool;
        this.flags = builder.flags == null ? NA : builder.flags;
        this.branch = builder.branch == null ? UNKNOWN : builder.branch;
        this.tag = builder.tag == null ? UNKNOWN : builder.tag;
        this.revision = builder.revision == null ? UNKNOWN : builder.revision;
        this.builtBy = builder.builtBy == null ? UNKNOWN : builder.builtBy;
        this.built = builder.built;

        BundleUtils.validateNotBlank("Tool", this.tool);
        BundleUtils.validateNotBlank("Revision", this.revision);
        BundleUtils.validateNotNull("Built", this.built);
    }

    public String getTool() {
        return tool;
    }

    public String getFlags() {
        return flags;
    }

    public String getBranch() {
        return branch;
    }

    public String getTag() {
        return tag;
    }

    public String getRevision() {
        return revision;
    }

    public String getBuiltBy() {
        return builtBy;
    }

    public Date getBuilt() {
        return built;
    }

    /**
     * Builder for BuildDetails.
     */
    public static class Builder {

        private String tool;
        private String flags;

        private String branch;
        private String tag;
        private String revision;

        private String builtBy;
        private Date built;

        public Builder tool(final String tool) {
            this.tool = tool;
            return this;
        }

        public Builder flags(final String flags) {
            this.flags = flags;
            return this;
        }

        public Builder branch(final String branch) {
            this.branch = branch;
            return this;
        }

        public Builder tag(final String tag) {
            this.tag = tag;
            return this;
        }

        public Builder revision(final String revision) {
            this.revision = revision;
            return this;
        }

        public Builder builtBy(final String builtBy) {
            this.builtBy = builtBy;
            return this;
        }

        public Builder built(final Date built) {
            this.built = built;
            return this;
        }

        public BuildDetails build() {
            return new BuildDetails(this);
        }

    }

}
