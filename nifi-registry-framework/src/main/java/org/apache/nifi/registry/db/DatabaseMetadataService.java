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
package org.apache.nifi.registry.db;

import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.db.entity.BucketEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntity;
import org.apache.nifi.registry.db.entity.BucketItemEntityType;
import org.apache.nifi.registry.db.entity.FlowEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntity;
import org.apache.nifi.registry.db.entity.FlowSnapshotEntityKey;
import org.apache.nifi.registry.db.repository.BucketItemRepository;
import org.apache.nifi.registry.db.repository.BucketRepository;
import org.apache.nifi.registry.db.repository.FlowRepository;
import org.apache.nifi.registry.db.repository.FlowSnapshotRepository;
import org.apache.nifi.registry.service.MetadataService;
import org.apache.nifi.registry.service.QueryParameters;
import org.apache.nifi.registry.params.SortOrder;
import org.apache.nifi.registry.params.SortParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A MetadataProvider backed by the embedded relational database. All database access should occur through this class,
 * other services should not directly use the repositories.
 */
@Service
public class DatabaseMetadataService implements MetadataService {

    private final BucketRepository bucketRepository;
    private final FlowRepository flowRepository;
    private final FlowSnapshotRepository flowSnapshotRepository;
    private final BucketItemRepository itemRepository;
    private final EntityManager entityManager;

    private final Set<String> bucketFields;
    private final Set<String> bucketItemFields;
    private final Set<String> flowFields;

    @Autowired
    public DatabaseMetadataService(final BucketRepository bucketRepository,
                                   final FlowRepository flowRepository,
                                   final FlowSnapshotRepository flowSnapshotRepository,
                                   final BucketItemRepository itemRepository,
                                   final EntityManager entityManager) {
        this.bucketRepository = bucketRepository;
        this.flowRepository = flowRepository;
        this.flowSnapshotRepository = flowSnapshotRepository;
        this.itemRepository = itemRepository;
        this.entityManager = entityManager;

        Validate.notNull(this.bucketRepository);
        Validate.notNull(this.flowRepository);
        Validate.notNull(this.flowSnapshotRepository);
        Validate.notNull(this.itemRepository);
        Validate.notNull(this.entityManager);

        this.bucketFields = Collections.unmodifiableSet(getEntityFields(BucketEntity.class));
        this.bucketItemFields = Collections.unmodifiableSet(getEntityFields(BucketItemEntity.class));
        this.flowFields = Collections.unmodifiableSet(getEntityFields(FlowEntity.class));
    }

    private Set<String> getEntityFields(Class<?> clazz) {
        final Metamodel metamodel = entityManager.getMetamodel();
        final ManagedType<?> bucketEntityManagedType = metamodel.managedType(clazz);
        return bucketEntityManagedType.getSingularAttributes().stream().map(s -> s.getName()).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------------------------

    @Override
    public BucketEntity createBucket(final BucketEntity bucket) {
        bucket.setCreated(new Date());
        return bucketRepository.save(bucket);
    }

    @Override
    public BucketEntity getBucketById(final String bucketIdentifier) {
        return bucketRepository.findById(bucketIdentifier).orElse(null);
    }

    @Override
    public List<BucketEntity> getBucketsByName(final String name) {
        List<BucketEntity> buckets = new ArrayList<>();

        final Iterable<BucketEntity> retrievedBuckets = bucketRepository.findByNameIgnoreCase(name);
        if (retrievedBuckets != null) {
            for (BucketEntity bucket : retrievedBuckets) {
                buckets.add(bucket);
            }
        }

        return buckets;
    }

    @Override
    public BucketEntity updateBucket(final BucketEntity bucket) {
        return bucketRepository.save(bucket);
    }

    @Override
    public void deleteBucket(final BucketEntity bucketEntity) {
        bucketRepository.delete(bucketEntity);
    }

    @Override
    public List<BucketEntity> getAllBuckets() {
        final List<BucketEntity> buckets = new ArrayList<>();
        for (BucketEntity bucket : bucketRepository.findAll()) {
            buckets.add(bucket);
        }
        return buckets;
    }

    @Override
    public List<BucketEntity> getBuckets(final QueryParameters params, final Set<String> bucketIds) {
        if (params != null && params.getNumRows() != null && params.getPageNum() != null) {
            return getPagedBuckets(params, bucketIds);
        } else if (params != null && params.getSortParameters() != null && params.getSortParameters().size() > 0) {
            return getSortedBuckets(params, bucketIds);
        } else {
            return getAllBuckets(bucketIds);
        }
    }

    private List<BucketEntity> getAllBuckets(final Set<String> bucketIds) {
        final List<BucketEntity> buckets = new ArrayList<>();
        for (BucketEntity bucket : bucketRepository.findByIdIn(bucketIds)) {
            buckets.add(bucket);
        }
        return buckets;
    }

    private List<BucketEntity> getPagedBuckets(final QueryParameters params, final Set<String> bucketIds) {
        final Pageable pageable = getPageRequest(params);
        final List<BucketEntity> buckets = new ArrayList<>();
        for (BucketEntity bucket : bucketRepository.findByIdIn(bucketIds, pageable)) {
            buckets.add(bucket);
        }
        return buckets;
    }

    private List<BucketEntity> getSortedBuckets(final QueryParameters params, final Set<String> bucketIds) {
        final Sort sort = getSort(params);
        final List<BucketEntity> buckets = new ArrayList<>();
        for (BucketEntity bucket : bucketRepository.findByIdIn(bucketIds, sort)) {
            buckets.add(bucket);
        }
        return buckets;
    }

    private Set<BucketEntity> getBuckets(Set<String> bucketIds) {
        return new HashSet<>(bucketRepository.findByIdIn(bucketIds));
    }

    // ------------------------------------------------------------------------------------

    @Override
    public List<BucketItemEntity> getBucketItems(final QueryParameters params, final BucketEntity bucket) {
        if (params != null && params.getNumRows() != null && params.getPageNum() != null) {
            return getPagedBucketItems(params, bucket);
        } else if (params != null && params.getSortParameters() != null && params.getSortParameters().size() > 0) {
            return getSortedBucketItems(params, bucket);
        } else {
            return getBucketItems(bucket);
        }
    }

    private List<BucketItemEntity> getBucketItems(final BucketEntity bucket) {
        final Iterable<BucketItemEntity> items = itemRepository.findByBucket(bucket);
        return getItemsWithCounts(items);
    }

    private List<BucketItemEntity> getPagedBucketItems(final QueryParameters params, final BucketEntity bucket) {
        final Pageable pageable = getPageRequest(params);
        final Iterable<BucketItemEntity> items = itemRepository.findByBucket(bucket, pageable);
        return getItemsWithCounts(items);
    }

    private List<BucketItemEntity> getSortedBucketItems(final QueryParameters params, final BucketEntity bucket) {
        final Sort sort = getSort(params);
        final Iterable<BucketItemEntity> items = itemRepository.findByBucket(bucket, sort);
        return getItemsWithCounts(items);
    }

    // ------------------------------------------------------------------------------------

    @Override
    public List<BucketItemEntity> getBucketItems(final QueryParameters params, final Set<String> bucketIds) {
        Set<BucketEntity> filterBuckets = getBuckets(bucketIds);
        if (params != null && params.getNumRows() != null && params.getPageNum() != null) {
            return getPagedBucketItems(params, filterBuckets);
        } else if (params != null && params.getSortParameters() != null && params.getSortParameters().size() > 0) {
            return getSortedBucketItems(params, filterBuckets);
        } else {
            return getBucketItems(filterBuckets);
        }
    }

    private List<BucketItemEntity> getBucketItems(final Set<BucketEntity> buckets) {
        final List<BucketItemEntity> items = itemRepository.findByBucketIn(buckets);
        return getItemsWithCounts(items);
    }

    private List<BucketItemEntity> getPagedBucketItems(final QueryParameters params, final Set<BucketEntity> buckets) {
        final Pageable pageable = getPageRequest(params);
        final List<BucketItemEntity> items = itemRepository.findByBucketIn(buckets, pageable);
        return getItemsWithCounts(items);
    }

    private List<BucketItemEntity> getSortedBucketItems(final QueryParameters params, final Set<BucketEntity> buckets) {
        final Sort sort = getSort(params);
        final List<BucketItemEntity> items = itemRepository.findByBucketIn(buckets, sort);
        return getItemsWithCounts(items);
    }

    // ------------------------------------------------------------------------------------

    private List<BucketItemEntity> getItemsWithCounts(final Iterable<BucketItemEntity> items) {
        final Map<String,Long> snapshotCounts = getFlowSnapshotCounts();

        final List<BucketItemEntity> itemWithCounts = new ArrayList<>();
        for (final BucketItemEntity item : items) {
            if (item.getType() == BucketItemEntityType.FLOW) {
                final Long snapshotCount = snapshotCounts.get(item.getId());
                if (snapshotCount != null) {
                    final FlowEntity flowEntity = (FlowEntity) item;
                    flowEntity.setSnapshotCount(snapshotCount);
                }
            }

            itemWithCounts.add(item);
        }

        return itemWithCounts;
    }

    private Map<String,Long> getFlowSnapshotCounts() {
        final Map<String,Long> flowSnapshotCounts = new HashMap<>();
        flowSnapshotRepository.countByFlow().stream().forEach(c -> flowSnapshotCounts.put(c.getFlowIdentifier(), c.getSnapshotCount()));
        return flowSnapshotCounts;
    }

    // ------------------------------------------------------------------------------------

    @Override
    public FlowEntity createFlow(final FlowEntity flow) {
        flow.setCreated(new Date());
        flow.setModified(new Date());
        return flowRepository.save(flow);
    }

    @Override
    public FlowEntity getFlowById(final String bucketIdentifier, final String flowIdentifier) {
        FlowEntity flow = flowRepository.findById(flowIdentifier).orElse(null);

        if (flow == null || flow.getBucket() == null || !bucketIdentifier.equals(flow.getBucket().getId())) {
            return null;
        }

        return flow;
    }

    @Override
    public FlowEntity getFlowByIdWithSnapshotCounts(final String bucketIdentifier, final String flowIdentifier) {
        FlowEntity flow = flowRepository.findById(flowIdentifier).orElse(null);

        if (flow == null || flow.getBucket() == null || !bucketIdentifier.equals(flow.getBucket().getId())) {
            return null;
        }

        final Map<String,Long> snapshotCounts = getFlowSnapshotCounts();

        final Long snapshotCount = snapshotCounts.get(flow.getId());
        if (snapshotCount != null) {
            flow.setSnapshotCount(snapshotCount);
        }

        return flow;
    }

    @Override
    public List<FlowEntity> getFlowsByName(final String name) {
        List<FlowEntity> flows = new ArrayList<>();

        final Iterable<FlowEntity> retrievedFlows = flowRepository.findByNameIgnoreCase(name);
        if (retrievedFlows != null) {
            for (FlowEntity flow : retrievedFlows) {
                flows.add(flow);
            }
        }

        return flows;
    }

    @Override
    public List<FlowEntity> getFlowsByBucket(final BucketEntity bucketEntity) {
        final Map<String,Long> snapshotCounts = getFlowSnapshotCounts();

        final List<FlowEntity> flows = flowRepository.findByBucket(bucketEntity);
        for (final FlowEntity flowEntity : flows) {
            final Long snapshotCount = snapshotCounts.get(flowEntity.getId());
            if (snapshotCount != null) {
                flowEntity.setSnapshotCount(snapshotCount);
            }
        }

        return flows;
    }

    @Override
    public FlowEntity updateFlow(final FlowEntity flow) {
        flow.setModified(new Date());
        return flowRepository.save(flow);
    }

    @Override
    public void deleteFlow(final FlowEntity flow) {
        flowRepository.delete(flow);
    }

    // ------------------------------------------------------------------------------------

    @Override
    public FlowSnapshotEntity createFlowSnapshot(final FlowSnapshotEntity flowSnapshot) {
        if (flowSnapshot.getCreated() == null) {
            flowSnapshot.setCreated(new Date());
        }
        return flowSnapshotRepository.save(flowSnapshot);
    }

    @Override
    public FlowSnapshotEntity getFlowSnapshot(final String bucketIdentifier, final String flowIdentifier, final Integer version) {
        final FlowSnapshotEntityKey key = new FlowSnapshotEntityKey(flowIdentifier, version);
        FlowSnapshotEntity flowSnapshot = flowSnapshotRepository.findById(key).orElse(null);

        if (flowSnapshot == null
                || flowSnapshot.getFlow() == null
                || flowSnapshot.getFlow().getBucket() == null
                || !bucketIdentifier.equals(flowSnapshot.getFlow().getBucket().getId())) {
            return null;
        }

        return flowSnapshot;
    }

    @Override
    public FlowSnapshotEntity getLatestSnapshot(final FlowEntity flowEntity) {
        if (flowEntity == null) {
            return null;
        }

        final FlowSnapshotEntity flowSnapshot = flowSnapshotRepository.findFirstByFlowOrderByIdVersionDesc(flowEntity);
        if (flowSnapshot == null) {
            return null;
        }
        return flowSnapshot;
    }

    @Override
    public void deleteFlowSnapshot(final FlowSnapshotEntity flowSnapshot) {
        flowSnapshotRepository.delete(flowSnapshot);
    }

    @Override
    public Set<String> getBucketFields() {
        return bucketFields;
    }

    @Override
    public Set<String> getBucketItemFields() {
        return bucketItemFields;
    }

    @Override
    public Set<String> getFlowFields() {
        return flowFields;
    }

    /**
     * Converts the registry query parameters to Spring Data's PageRequest.
     *
     * @param parameters the registry query parameters
     * @return the equivalent Pageable
     */
    private Pageable getPageRequest(final QueryParameters parameters) {
        final Sort sort = getSort(parameters);
        if (sort == null) {
            return PageRequest.of(parameters.getPageNum(), parameters.getNumRows());
        } else {
            return PageRequest.of(parameters.getPageNum(), parameters.getNumRows(), sort);
        }
    }

    /**
     * Converts the registry sort parameters to Spring Data's Sort.
     *
     * @param parameters the registry query parameters
     * @return the equivalent Sort
     */
    private Sort getSort(final QueryParameters parameters) {
        final List<Sort.Order> orders = new ArrayList<>();

        for (SortParameter sortParameter : parameters.getSortParameters()) {
            final Sort.Direction direction = sortParameter.getOrder() == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            final Sort.Order order = new Sort.Order(direction, sortParameter.getFieldName());
            orders.add(order);
        }

        if (orders.isEmpty()) {
            return null;
        } else {
            return Sort.by(orders);
        }
    }

}
