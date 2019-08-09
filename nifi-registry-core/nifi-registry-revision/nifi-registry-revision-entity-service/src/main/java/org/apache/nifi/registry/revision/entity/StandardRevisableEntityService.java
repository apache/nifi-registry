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
package org.apache.nifi.registry.revision.entity;

import org.apache.nifi.registry.revision.api.EntityModification;
import org.apache.nifi.registry.revision.api.Revision;
import org.apache.nifi.registry.revision.api.RevisionClaim;
import org.apache.nifi.registry.revision.api.RevisionManager;
import org.apache.nifi.registry.revision.api.RevisionUpdate;
import org.apache.nifi.registry.revision.standard.StandardRevisionClaim;
import org.apache.nifi.registry.revision.standard.StandardRevisionUpdate;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Standard implementation of RevisableEntityService.
 */
public class StandardRevisableEntityService implements RevisableEntityService {

    private final RevisionManager revisionManager;

    public StandardRevisableEntityService(final RevisionManager revisionManager) {
        this.revisionManager = revisionManager;
    }

    @Override
    public <T extends RevisableEntity> T create(final T requestEntity, final String creatorIdentity, final Supplier<T> createEntity) {
        if (requestEntity == null) {
            throw new IllegalArgumentException("Request entity is required");
        }

        if (requestEntity.getRevision() == null || requestEntity.getRevision().getVersion() == null) {
            throw new IllegalArgumentException("Revision info is required");
        }

        if (requestEntity.getRevision().getVersion() != 0) {
            throw new IllegalArgumentException("A revision version of 0 must be specified when creating a new entity");
        }

        if (creatorIdentity == null || creatorIdentity.trim().isEmpty()) {
            throw new IllegalArgumentException("Creator identity is required");
        }

        return createOrUpdate(requestEntity, creatorIdentity, createEntity);
    }

    @Override
    public <T extends RevisableEntity> T get(final Supplier<T> getEntity) {
        final T entity = getEntity.get();
        if (entity != null) {
            populateRevision(entity);
        }
        return entity;
    }

    @Override
    public <T extends RevisableEntity> List<T> getEntities(final Supplier<List<T>> getEntities) {
        final List<T> entities = getEntities.get();
        populateRevisions(entities);
        return entities;
    }

    @Override
    public <T extends RevisableEntity> T update(final T requestEntity, final String updaterIdentity, final Supplier<T> updateEntity) {
        if (requestEntity == null) {
            throw new IllegalArgumentException("Request entity is required");
        }

        if (requestEntity.getRevision() == null || requestEntity.getRevision().getVersion() == null) {
            throw new IllegalArgumentException("Revision info is required");
        }

        if (updaterIdentity == null || updaterIdentity.trim().isEmpty()) {
            throw new IllegalArgumentException("Updater identity is required");
        }

        return createOrUpdate(requestEntity, updaterIdentity, updateEntity);
    }

    @Override
    public <T extends RevisableEntity> T delete(final String entityIdentifier, final RevisionInfo revisionInfo, final Supplier<T> deleteEntity) {
        if (entityIdentifier == null || entityIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity identifier is required");
        }

        if (revisionInfo == null || revisionInfo.getVersion() == null) {
            throw new IllegalArgumentException("Revision info is required");
        }

        final Revision revision = createRevision(entityIdentifier, revisionInfo);
        final RevisionClaim claim = new StandardRevisionClaim(revision);
        return revisionManager.deleteRevision(claim, () -> deleteEntity.get());
    }

    private <T extends RevisableEntity> T createOrUpdate(final T requestEntity, final String userIdentity, final Supplier<T> updateOrCreateEntity) {
        final Revision revision = createRevision(requestEntity.getIdentifier(), requestEntity.getRevision());
        final RevisionClaim claim = new StandardRevisionClaim(revision);

        final RevisionUpdate<T> revisionUpdate = revisionManager.updateRevision(claim, () -> {
            final T updatedEntity = updateOrCreateEntity.get();

            final Revision updatedRevision = revision.incrementRevision(revision.getClientId());
            final EntityModification entityModification = new EntityModification(updatedRevision, userIdentity);

            final RevisionInfo updatedRevisionInfo = createRevisionInfo(updatedRevision, entityModification);
            updatedEntity.setRevision(updatedRevisionInfo);

            return new StandardRevisionUpdate<>(updatedEntity, entityModification);
        });

        return revisionUpdate.getEntity();
    }

    private <T extends RevisableEntity> void populateRevisions(final Collection<T> revisableEntities) {
        if (revisableEntities == null) {
            return;
        }

        revisableEntities.forEach(e -> {
            populateRevision(e);
        });
    }

    private void populateRevision(final RevisableEntity e) {
        if (e == null) {
            return;
        }

        final Revision entityRevision = revisionManager.getRevision(e.getIdentifier());
        final RevisionInfo revisionInfo = createRevisionInfo(entityRevision);
        e.setRevision(revisionInfo);
    }

    private Revision createRevision(final String entityId, final RevisionInfo revisionInfo) {
        return new Revision(revisionInfo.getVersion(), revisionInfo.getClientId(), entityId);
    }

    private RevisionInfo createRevisionInfo(final Revision revision) {
        return createRevisionInfo(revision, null);
    }

    private RevisionInfo createRevisionInfo(final Revision revision, final EntityModification entityModification) {
        final RevisionInfo revisionInfo = new RevisionInfo();
        revisionInfo.setVersion(revision.getVersion());
        revisionInfo.setClientId(revision.getClientId());
        if (entityModification != null) {
            revisionInfo.setLastModifier(entityModification.getLastModifier());
        }
        return revisionInfo;
    }

}
