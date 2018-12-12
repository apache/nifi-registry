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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleEntityType;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionDependencyEntity;
import org.apache.nifi.registry.db.entity.ExtensionBundleVersionEntity;
import org.apache.nifi.registry.db.entity.ExtensionEntity;
import org.apache.nifi.registry.db.entity.ExtensionEntityCategory;
import org.apache.nifi.registry.db.entity.ExtensionProvidedServiceApiEntity;
import org.apache.nifi.registry.db.entity.ExtensionRestrictionEntity;
import org.apache.nifi.registry.extension.BuildInfo;
import org.apache.nifi.registry.extension.ExtensionBundle;
import org.apache.nifi.registry.extension.ExtensionBundleType;
import org.apache.nifi.registry.extension.ExtensionBundleVersionDependency;
import org.apache.nifi.registry.extension.ExtensionBundleVersionMetadata;
import org.apache.nifi.registry.extension.ExtensionCategory;
import org.apache.nifi.registry.extension.ExtensionMetadata;
import org.apache.nifi.registry.extension.ExtensionProvidedServiceApi;
import org.apache.nifi.registry.extension.ExtensionRestriction;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Mappings between Extension related DB entities and data model.
 */
public class ExtensionMappings {

    // -- Map ExtensionBundle

    public static ExtensionBundleEntity map(final ExtensionBundle bundle) {
        final ExtensionBundleEntity entity = new ExtensionBundleEntity();
        entity.setId(bundle.getIdentifier());
        entity.setName(bundle.getName());
        entity.setDescription(bundle.getDescription());
        entity.setCreated(new Date(bundle.getCreatedTimestamp()));
        entity.setModified(new Date(bundle.getModifiedTimestamp()));
        entity.setType(BucketItemEntityType.EXTENSION_BUNDLE);
        entity.setBucketId(bundle.getBucketIdentifier());

        entity.setGroupId(bundle.getGroupId());
        entity.setArtifactId(bundle.getArtifactId());
        entity.setBundleType(map(bundle.getBundleType()));
        return entity;
    }

    public static ExtensionBundle map(final BucketEntity bucketEntity, final ExtensionBundleEntity bundleEntity) {
        final ExtensionBundle bundle = new ExtensionBundle();
        bundle.setIdentifier(bundleEntity.getId());
        bundle.setName(bundleEntity.getName());
        bundle.setDescription(bundleEntity.getDescription());
        bundle.setCreatedTimestamp(bundleEntity.getCreated().getTime());
        bundle.setModifiedTimestamp(bundleEntity.getModified().getTime());
        bundle.setBucketIdentifier(bundleEntity.getBucketId());

        if (bucketEntity != null) {
            bundle.setBucketName(bucketEntity.getName());
        } else {
            bundle.setBucketName(bundleEntity.getBucketName());
        }

        bundle.setGroupId(bundleEntity.getGroupId());
        bundle.setArtifactId(bundleEntity.getArtifactId());
        bundle.setBundleType(map(bundleEntity.getBundleType()));
        bundle.setVersionCount(bundleEntity.getVersionCount());
        return bundle;
    }

    // -- Map ExtensionBundleType

    public static ExtensionBundleEntityType map(final ExtensionBundleType bundleType) {
        switch (bundleType) {
            case NIFI_NAR:
                return ExtensionBundleEntityType.NIFI_NAR;
            case MINIFI_CPP:
                return ExtensionBundleEntityType.MINIFI_CPP;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
        }
    }

    public static ExtensionBundleType map(final ExtensionBundleEntityType bundleEntityType) {
        switch (bundleEntityType) {
            case NIFI_NAR:
                return ExtensionBundleType.NIFI_NAR;
            case MINIFI_CPP:
                return ExtensionBundleType.MINIFI_CPP;
            default:
                throw new IllegalArgumentException("Unknown bundle type: " + bundleEntityType);
        }
    }

    // -- Map ExtensionBundleVersion

    public static ExtensionBundleVersionEntity map(final ExtensionBundleVersionMetadata bundleVersionMetadata) {
        final ExtensionBundleVersionEntity entity = new ExtensionBundleVersionEntity();
        entity.setId(bundleVersionMetadata.getId());
        entity.setExtensionBundleId(bundleVersionMetadata.getExtensionBundleId());
        entity.setBucketId(bundleVersionMetadata.getBucketId());
        entity.setVersion(bundleVersionMetadata.getVersion());
        entity.setCreated(new Date(bundleVersionMetadata.getTimestamp()));
        entity.setCreatedBy(bundleVersionMetadata.getAuthor());
        entity.setDescription(bundleVersionMetadata.getDescription());
        entity.setSha256Hex(bundleVersionMetadata.getSha256());
        entity.setSha256Supplied(bundleVersionMetadata.getSha256Supplied());
        entity.setContentSize(bundleVersionMetadata.getContentSize());
        entity.setSystemApiVersion(bundleVersionMetadata.getSystemApiVersion());

        final BuildInfo buildInfo = bundleVersionMetadata.getBuildInfo();
        entity.setBuildTool(buildInfo.getBuildTool());
        entity.setBuildFlags(buildInfo.getBuildFlags());
        entity.setBuildBranch(buildInfo.getBuildBranch());
        entity.setBuildTag(buildInfo.getBuildTag());
        entity.setBuildRevision(buildInfo.getBuildRevision());
        entity.setBuiltBy(buildInfo.getBuiltBy());
        entity.setBuilt(new Date(buildInfo.getBuilt()));

        return entity;
    }

    public static ExtensionBundleVersionMetadata map(final ExtensionBundleVersionEntity bundleVersionEntity) {
        final ExtensionBundleVersionMetadata bundleVersionMetadata = new ExtensionBundleVersionMetadata();
        bundleVersionMetadata.setId(bundleVersionEntity.getId());
        bundleVersionMetadata.setExtensionBundleId(bundleVersionEntity.getExtensionBundleId());
        bundleVersionMetadata.setBucketId(bundleVersionEntity.getBucketId());
        bundleVersionMetadata.setVersion(bundleVersionEntity.getVersion());
        bundleVersionMetadata.setTimestamp(bundleVersionEntity.getCreated().getTime());
        bundleVersionMetadata.setAuthor(bundleVersionEntity.getCreatedBy());
        bundleVersionMetadata.setDescription(bundleVersionEntity.getDescription());
        bundleVersionMetadata.setSha256(bundleVersionEntity.getSha256Hex());
        bundleVersionMetadata.setSha256Supplied(bundleVersionEntity.getSha256Supplied());
        bundleVersionMetadata.setContentSize(bundleVersionEntity.getContentSize());
        bundleVersionMetadata.setSystemApiVersion(bundleVersionEntity.getSystemApiVersion());

        final BuildInfo buildInfo = new BuildInfo();
        buildInfo.setBuildTool(bundleVersionEntity.getBuildTool());
        buildInfo.setBuildFlags(bundleVersionEntity.getBuildFlags());
        buildInfo.setBuildBranch(bundleVersionEntity.getBuildBranch());
        buildInfo.setBuildTag(bundleVersionEntity.getBuildTag());
        buildInfo.setBuildRevision(bundleVersionEntity.getBuildRevision());
        buildInfo.setBuiltBy(bundleVersionEntity.getBuiltBy());
        buildInfo.setBuilt(bundleVersionEntity.getBuilt().getTime());
        bundleVersionMetadata.setBuildInfo(buildInfo);

        return bundleVersionMetadata;
    }

    // -- Map ExtensionBundleVersionDependency

    public static ExtensionBundleVersionDependencyEntity map(final ExtensionBundleVersionDependency bundleVersionDependency) {
        final ExtensionBundleVersionDependencyEntity entity = new ExtensionBundleVersionDependencyEntity();
        entity.setGroupId(bundleVersionDependency.getGroupId());
        entity.setArtifactId(bundleVersionDependency.getArtifactId());
        entity.setVersion(bundleVersionDependency.getVersion());
        return entity;
    }

    public static ExtensionBundleVersionDependency map(final ExtensionBundleVersionDependencyEntity dependencyEntity) {
        final ExtensionBundleVersionDependency dependency = new ExtensionBundleVersionDependency();
        dependency.setGroupId(dependencyEntity.getGroupId());
        dependency.setArtifactId(dependencyEntity.getArtifactId());
        dependency.setVersion(dependencyEntity.getVersion());
        return dependency;
    }

    // -- Map Extension

    public static ExtensionEntity map(final ExtensionMetadata extension) {
        final ExtensionEntity entity = new ExtensionEntity();
        entity.setId(extension.getId());
        entity.setName(extension.getName());
        entity.setDescription(extension.getDescription());
        entity.setCategory(map(extension.getCategory()));
        entity.setGeneralRestriction(extension.getGeneralRestriction());

        if (extension.getTags() != null) {
            entity.setTags(StringUtils.join(extension.getTags(), ","));
        }

        if (extension.getProvidedServiceApis() != null) {
            entity.setProvidedServiceApis(extension.getProvidedServiceApis().stream()
                    .map(p -> map(p))
                    .collect(Collectors.toSet()));
        } else {
            entity.setProvidedServiceApis(Collections.emptySet());
        }

        if (extension.getRestrictions() != null) {
            entity.setRestrictions(extension.getRestrictions().stream()
                    .map(r -> map(r))
                    .collect(Collectors.toSet()));
        } else {
            entity.setRestrictions(Collections.emptySet());
        }

        return entity;
    }

    public static ExtensionMetadata map(final ExtensionEntity entity) {
        final ExtensionMetadata extension = new ExtensionMetadata();
        extension.setId(entity.getId());
        extension.setName(entity.getName());
        extension.setDescription(entity.getDescription());
        extension.setCategory(map(entity.getCategory()));
        extension.setGeneralRestriction(entity.getGeneralRestriction());

        if (entity.getTags() != null) {
            final String[] tags = entity.getTags().split("[,]");
            extension.setTags(new HashSet<>(Arrays.asList(tags)));
        } else {
            extension.setTags(Collections.emptySet());
        }

        if (entity.getProvidedServiceApis() != null) {
            extension.setProvidedServiceApis(entity.getProvidedServiceApis().stream()
                    .map(p -> map(p))
                    .collect(Collectors.toSet()));
        } else {
            extension.setProvidedServiceApis(Collections.emptySet());
        }

        if (entity.getRestrictions() != null) {
            extension.setRestrictions(entity.getRestrictions().stream()
                    .map(r -> map(r))
                    .collect(Collectors.toSet()));
        } else {
            extension.setRestrictions(Collections.emptySet());
        }

        return extension;
    }

    // -- Map Extension Category

    public static ExtensionEntityCategory map(final ExtensionCategory extensionCategory) {
        switch (extensionCategory) {
            case PROCESSOR:
                return ExtensionEntityCategory.PROCESSOR;
            case CONTROLLER_SERVICE:
                return ExtensionEntityCategory.CONTROLLER_SERVICE;
            case REPORTING_TASK:
                return ExtensionEntityCategory.REPORTING_TASK;
            default:
                throw new IllegalArgumentException("Unknown extension category: " + extensionCategory.name());
        }
    }

    public static ExtensionCategory map(final ExtensionEntityCategory extensionCategory) {
        switch (extensionCategory) {
            case PROCESSOR:
                return ExtensionCategory.PROCESSOR;
            case CONTROLLER_SERVICE:
                return ExtensionCategory.CONTROLLER_SERVICE;
            case REPORTING_TASK:
                return ExtensionCategory.REPORTING_TASK;
            default:
                throw new IllegalArgumentException("Unknown extension category: " + extensionCategory.name());
        }
    }

    // -- Map ExtensionProvidedServiceApi

    public static ExtensionProvidedServiceApiEntity map(final ExtensionProvidedServiceApi providedServiceApi) {
        final ExtensionProvidedServiceApiEntity entity = new ExtensionProvidedServiceApiEntity();
        entity.setClassName(providedServiceApi.getClassName());
        entity.setGroupId(providedServiceApi.getGroupId());
        entity.setArtifactId(providedServiceApi.getArtifactId());
        entity.setVersion(providedServiceApi.getVersion());
        return entity;
    }

    public static ExtensionProvidedServiceApi map(final ExtensionProvidedServiceApiEntity entity) {
        final ExtensionProvidedServiceApi providedServiceApi = new ExtensionProvidedServiceApi();
        providedServiceApi.setClassName(entity.getClassName());
        providedServiceApi.setGroupId(entity.getGroupId());
        providedServiceApi.setArtifactId(entity.getArtifactId());
        providedServiceApi.setVersion(entity.getVersion());
        return providedServiceApi;
    }

    // -- Map ExtensionRestriction

    public static ExtensionRestrictionEntity map(final ExtensionRestriction restriction) {
        final ExtensionRestrictionEntity restrictionEntity = new ExtensionRestrictionEntity();
        restrictionEntity.setRequiredPermission(restriction.getRequiredPermission());
        restrictionEntity.setExplanation(restriction.getExplanation());
        return restrictionEntity;
    }

    public static ExtensionRestriction map(final ExtensionRestrictionEntity restrictionEntity) {
        final ExtensionRestriction restriction = new ExtensionRestriction();
        restriction.setRequiredPermission(restrictionEntity.getRequiredPermission());
        restriction.setExplanation(restrictionEntity.getExplanation());
        return restriction;
    }

}
