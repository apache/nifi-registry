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
package org.apache.nifi.registry.service.mapper;

import org.apache.nifi.registry.bundle.model.BuildDetails;
import org.apache.nifi.registry.bundle.model.BundleCoordinate;
import org.apache.nifi.registry.bundle.model.ExtensionDetails;
import org.apache.nifi.registry.bundle.model.ExtensionType;
import org.apache.nifi.registry.bundle.model.ProvidedServiceApiDetails;
import org.apache.nifi.registry.bundle.model.RestrictionDetails;
import org.apache.nifi.registry.extension.BuildInfo;
import org.apache.nifi.registry.extension.ExtensionBundleVersionDependency;
import org.apache.nifi.registry.extension.ExtensionCategory;
import org.apache.nifi.registry.extension.ExtensionMetadata;
import org.apache.nifi.registry.extension.ExtensionProvidedServiceApi;
import org.apache.nifi.registry.extension.ExtensionRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * Mappings between the bundle details model and the standard data model.
 */
public class BundleDetailMappings {

    public static ExtensionMetadata map(final ExtensionDetails extensionDetails) {
        final ExtensionMetadata extensionMetadata = new ExtensionMetadata();
        extensionMetadata.setName(extensionDetails.getName());
        extensionMetadata.setDescription(extensionDetails.getDescription());
        extensionMetadata.setCategory(map(extensionDetails.getType()));
        extensionMetadata.setGeneralRestriction(extensionDetails.getGeneralRestrictionExplanation());

        final Set<String> tags = extensionDetails.getTags();
        if (tags != null) {
            extensionMetadata.setTags(new HashSet<>(tags));
        }

        final Set<ProvidedServiceApiDetails> providedServiceApiDetails = extensionDetails.getProvidedServiceApis();
        if (providedServiceApiDetails != null) {
            final Set<ExtensionProvidedServiceApi> providedServiceApis = new HashSet<>(providedServiceApiDetails.size());
            providedServiceApiDetails.forEach(p -> providedServiceApis.add(map(p)));
            extensionMetadata.setProvidedServiceApis(providedServiceApis);
        }

        final Set<RestrictionDetails> restrictionDetails = extensionDetails.getRestrictions();
        if (restrictionDetails != null) {
            final Set<ExtensionRestriction> restrictions = new HashSet<>(restrictionDetails.size());
            restrictionDetails.forEach(r -> restrictions.add(map(r)));
            extensionMetadata.setRestrictions(restrictions);
        }

        return extensionMetadata;
    }

    public static ExtensionCategory map(final ExtensionType extensionType) {
        switch (extensionType) {
            case PROCESSOR:
                return ExtensionCategory.PROCESSOR;
            case CONTROLLER_SERVICE:
                return ExtensionCategory.CONTROLLER_SERVICE;
            case REPORTING_TASK:
                return ExtensionCategory.REPORTING_TASK;
            default:
                throw new IllegalArgumentException("Unexpected extension type: " + extensionType);
        }
    }

    public static ExtensionProvidedServiceApi map(final ProvidedServiceApiDetails providedServiceApiDetails) {
        final ExtensionProvidedServiceApi providedServiceApi = new ExtensionProvidedServiceApi();
        providedServiceApi.setClassName(providedServiceApiDetails.getClassName());
        providedServiceApi.setGroupId(providedServiceApiDetails.getBundleCoordinate().getGroupId());
        providedServiceApi.setArtifactId(providedServiceApiDetails.getBundleCoordinate().getArtifactId());
        providedServiceApi.setVersion(providedServiceApiDetails.getBundleCoordinate().getVersion());
        return providedServiceApi;
    }

    public static ExtensionRestriction map(final RestrictionDetails restrictionDetails) {
        final ExtensionRestriction restriction = new ExtensionRestriction();
        restriction.setExplanation(restrictionDetails.getExplanation());
        restriction.setRequiredPermission(restrictionDetails.getRequiredPermission());
        return restriction;
    }

    public static ExtensionBundleVersionDependency map(final BundleCoordinate bundleCoordinate) {
        final ExtensionBundleVersionDependency versionDependency = new ExtensionBundleVersionDependency();
        versionDependency.setGroupId(bundleCoordinate.getGroupId());
        versionDependency.setArtifactId(bundleCoordinate.getArtifactId());
        versionDependency.setVersion(bundleCoordinate.getVersion());
        return versionDependency;
    }

    public static BuildInfo map(final BuildDetails buildDetails){
        final BuildInfo buildInfo = new BuildInfo();
        buildInfo.setBuildTool(buildDetails.getTool());
        buildInfo.setBuildFlags(buildDetails.getFlags());
        buildInfo.setBuildBranch(buildDetails.getBranch());
        buildInfo.setBuildTag(buildDetails.getTag());
        buildInfo.setBuildRevision(buildDetails.getRevision());
        buildInfo.setBuiltBy(buildDetails.getBuiltBy());
        buildInfo.setBuilt(buildDetails.getBuilt().getTime());
        return buildInfo;
    }

}
