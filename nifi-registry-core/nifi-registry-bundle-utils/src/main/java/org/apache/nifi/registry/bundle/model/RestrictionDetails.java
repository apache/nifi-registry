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

public class RestrictionDetails {

    private final String requiredPermission;

    private final String explanation;

    private RestrictionDetails(final Builder builder) {
        this.requiredPermission = builder.requiredPermission;
        this.explanation = builder.explanation;
        BundleUtils.validateNotBlank("Required Permission", requiredPermission);
        BundleUtils.validateNotBlank("Explanation", explanation);
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public String getExplanation() {
        return explanation;
    }

    public static class Builder {

        private String requiredPermission;
        private String explanation;

        public Builder requiredPermission(final String requiredPermission) {
            this.requiredPermission = requiredPermission;
            return this;
        }

        public Builder explanation(final String explanation) {
            this.explanation = explanation;
            return this;
        }

        public RestrictionDetails build() {
            return new RestrictionDetails(this);
        }
    }
}
