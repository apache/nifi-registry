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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Details about a specific extension, such as a processor, controller service, or reporting task.
 */
public class ExtensionDetails {

    private final String name;

    private final String description;

    private final ExtensionType type;

    private final Set<String> tags;

    private final String generalRestrictionExplanation;

    private final Set<RestrictionDetails> restrictions;

    private final Set<ProvidedServiceApiDetails> providedServiceApis;

    private ExtensionDetails(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.tags = Collections.unmodifiableSet(new HashSet<>(builder.tags));
        this.generalRestrictionExplanation = builder.generalRestrictionExplanation;
        this.restrictions = Collections.unmodifiableSet(new HashSet<>(builder.restrictions));
        this.providedServiceApis = Collections.unmodifiableSet(new HashSet<>(builder.providedServiceApis));

        BundleUtils.validateNotBlank("Name", this.name);
        BundleUtils.validateNotNull("Type", this.type);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getGeneralRestrictionExplanation() {
        return generalRestrictionExplanation;
    }

    public Set<RestrictionDetails> getRestrictions() {
        return restrictions;
    }

    public ExtensionType getType() {
        return type;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<ProvidedServiceApiDetails> getProvidedServiceApis() {
        return providedServiceApis;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ExtensionDetails)) {
            return false;
        }

        final ExtensionDetails other = (ExtensionDetails) obj;
        return Objects.equals(this.name, other.name);
    }

    /**
     * Builder for ExtensionDetails.
     */
    public static class Builder {

        private String name;
        private String description;
        private ExtensionType type;
        private Set<String> tags = new HashSet<>();
        private String generalRestrictionExplanation;
        private Set<RestrictionDetails> restrictions = new HashSet<>();
        private Set<ProvidedServiceApiDetails> providedServiceApis = new HashSet<>();

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder generalRestriction(final String generalRestriction) {
            this.generalRestrictionExplanation = generalRestriction;
            return this;
        }

        public Builder addRestriction(final RestrictionDetails restrictionDetails) {
            if (restrictionDetails != null) {
                this.restrictions.add(restrictionDetails);
            }
            return this;
        }

        public Builder type(final ExtensionType extensionType) {
            this.type = extensionType;
            return this;
        }

        public Builder addTag(final String tag) {
            if (tag != null) {
                this.tags.add(tag);
            }
            return this;
        }

        public Builder addProvidedServiceApi(final ProvidedServiceApiDetails providedServiceApi) {
            if (providedServiceApi != null) {
                this.providedServiceApis.add(providedServiceApi);
            }
            return this;
        }

        public ExtensionDetails build() {
            return new ExtensionDetails(this);
        }

    }


}
