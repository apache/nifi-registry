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
package org.apache.nifi.registry.metadata;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.metadata.generated.Buckets;
import org.apache.nifi.registry.metadata.generated.Flow;
import org.apache.nifi.registry.metadata.generated.Flows;
import org.apache.nifi.registry.metadata.generated.Metadata;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A MetadataProvider that persists metadata to the local filesystem.
 */
public class FileSystemMetadataProvider implements MetadataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemMetadataProvider.class);

    private static final String METADATA_XSD = "/metadata.xsd";
    private static final String JAXB_GENERATED_PATH = "org.apache.nifi.registry.metadata.generated";
    private static final JAXBContext JAXB_CONTEXT = initializeJaxbContext();

    /**
     * Load the JAXBContext.
     */
    private static JAXBContext initializeJaxbContext() {
        try {
            return JAXBContext.newInstance(JAXB_GENERATED_PATH, FileSystemMetadataProvider.class.getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.");
        }
    }

    static final String METADATA_FILE_PROP = "Metadata File";

    private File metadataFile;
    private Schema metadataSchema;
    private final AtomicReference<MetadataHolder> metadataHolder = new AtomicReference<>(null);

    public FileSystemMetadataProvider() throws ProviderCreationException {
        try {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            metadataSchema = schemaFactory.newSchema(FileSystemMetadataProvider.class.getResource(METADATA_XSD));
        } catch (SAXException e) {
            throw new ProviderCreationException("Unable to create MetadataProvider due to: " + e.getMessage(), e);
        }
    }

    @Override
    public void onConfigured(final ProviderConfigurationContext configurationContext) throws ProviderCreationException {
        final Map<String,String> config = configurationContext.getProperties();
        if (!config.containsKey(METADATA_FILE_PROP)) {
            throw new ProviderCreationException("The property " + METADATA_FILE_PROP + " must be provided");
        }

        final String metadataFileValue = config.get(METADATA_FILE_PROP);
        if (StringUtils.isBlank(metadataFileValue)) {
            throw new ProviderCreationException("The property " + METADATA_FILE_PROP + " cannot be null or blank");
        }

        try {
            metadataFile = new File(metadataFileValue);
            if (metadataFile.exists()) {
                LOGGER.info("Loading metadata file from {}", new Object[] {metadataFile.getAbsolutePath()});
                final Metadata metadata = unmarshallMetadata();
                metadataHolder.set(new MetadataHolder(metadata));
            } else {
                LOGGER.info("Creating new metadata file at {}", new Object[] {metadataFile.getAbsolutePath()});

                final Metadata metadata = new Metadata();
                metadata.setBuckets(new Buckets());
                metadata.setFlows(new Flows());

                saveMetadata(metadata);
                metadataHolder.set(new MetadataHolder(metadata));
            }
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to configure MetadataProvider due to: " + e.getMessage(), e);
        }
    }

    private Metadata unmarshallMetadata() throws JAXBException {
        final Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        unmarshaller.setSchema(metadataSchema);

        final JAXBElement<Metadata> element = unmarshaller.unmarshal(new StreamSource(metadataFile), Metadata.class);
        return element.getValue();
    }

    private void saveMetadata(final Metadata metadata) throws JAXBException {
        final Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        marshaller.setSchema(metadataSchema);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(metadata, metadataFile);
    }

    private synchronized void saveAndRefresh(final Metadata metadata) {
        try {
            saveMetadata(metadata);
            metadataHolder.set(new MetadataHolder(metadata));
        } catch (JAXBException e) {
            throw new MetadataProviderException("Unable to save metadata", e);
        }
    }

    @Override
    public synchronized BucketMetadata createBucket(final BucketMetadata bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        final org.apache.nifi.registry.metadata.generated.Bucket jaxbBucket = new org.apache.nifi.registry.metadata.generated.Bucket();
        jaxbBucket.setIdentifier(bucket.getIdentifier());
        jaxbBucket.setName(bucket.getName());
        jaxbBucket.setDescription(bucket.getDescription());
        jaxbBucket.setCreatedTimestamp(bucket.getCreatedTimestamp());

        final MetadataHolder holder = metadataHolder.get();

        final Metadata metadata = holder.getMetadata();
        metadata.getBuckets().getBucket().add(jaxbBucket);

        saveAndRefresh(metadata);
        return metadataHolder.get().getBucketsBydId().get(bucket.getIdentifier());
    }

    @Override
    public BucketMetadata getBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        return holder.getBucketsBydId().get(bucketIdentifier);
    }

    @Override
    public Set<BucketMetadata> getBuckets() {
        final MetadataHolder holder = metadataHolder.get();
        final Map<String,BucketMetadata> bucketsBydId = holder.getBucketsBydId();
        return new HashSet<>(bucketsBydId.values());
    }

    @Override
    public synchronized BucketMetadata updateBucket(final BucketMetadata bucket) {
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        final Buckets buckets = holder.getMetadata().getBuckets();

        final org.apache.nifi.registry.metadata.generated.Bucket jaxbBucket = buckets.getBucket().stream()
                .filter(b -> bucket.getIdentifier().equals(b.getIdentifier()))
                .findFirst()
                .orElse(null);

        if (jaxbBucket == null) {
            return null;
        }

        jaxbBucket.setName(bucket.getName());
        jaxbBucket.setDescription(bucket.getDescription());

        saveAndRefresh(holder.getMetadata());
        return metadataHolder.get().getBucketsBydId().get(bucket.getIdentifier());
    }

    @Override
    public synchronized void deleteBucket(final String bucketIdentifier) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        final Flows flows = holder.getMetadata().getFlows();
        final Buckets buckets = holder.getMetadata().getBuckets();

        // first remove any flow that reference the bucket
        boolean deletedFlow = false;
        final Iterator<Flow> flowIterator = flows.getFlow().iterator();
        while (flowIterator.hasNext()) {
            final Flow flow = flowIterator.next();
            if (flow.getBucketIdentifier().equals(bucketIdentifier)) {
                flowIterator.remove();
                deletedFlow = true;
            }
        }

        // now delete the actual bucket
        boolean deleteBucket = false;
        final Iterator<org.apache.nifi.registry.metadata.generated.Bucket> bucketIterator = buckets.getBucket().iterator();
        while (bucketIterator.hasNext()) {
            final org.apache.nifi.registry.metadata.generated.Bucket bucket = bucketIterator.next();
            if (bucket.getIdentifier().equals(bucketIdentifier)) {
               bucketIterator.remove();
               deleteBucket = true;
               break;
            }
        }

        if (deletedFlow || deleteBucket) {
            saveAndRefresh(holder.getMetadata());
        }
    }

    @Override
    public synchronized FlowMetadata createFlow(final String bucketIdentifier, final FlowMetadata versionedFlow) {
        if (bucketIdentifier == null) {
            throw new IllegalArgumentException("Bucket Identifier cannot be blank");
        }

        if (versionedFlow == null) {
            throw new IllegalArgumentException("Versioned Flow cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();

        final BucketMetadata bucket = holder.getBucketsBydId().get(bucketIdentifier);
        if (bucket == null) {
            throw new IllegalStateException("Unable to create Versioned Flow because Bucket does not exist with id " + bucketIdentifier);
        }

        final Flow jaxbFlow = new Flow();
        jaxbFlow.setIdentifier(versionedFlow.getIdentifier());
        jaxbFlow.setName(versionedFlow.getName());
        jaxbFlow.setDescription(versionedFlow.getDescription());
        jaxbFlow.setCreatedTimestamp(versionedFlow.getCreatedTimestamp());
        jaxbFlow.setModifiedTimestamp(versionedFlow.getModifiedTimestamp());
        jaxbFlow.setBucketIdentifier(bucketIdentifier);

        final Metadata metadata = holder.getMetadata();
        metadata.getFlows().getFlow().add(jaxbFlow);

        saveAndRefresh(metadata);
        return metadataHolder.get().getFlowsById().get(versionedFlow.getIdentifier());
    }

    @Override
    public FlowMetadata getFlow(final String flowIdentifier) {
        if (flowIdentifier == null) {
            throw new IllegalArgumentException("Flow Identifier cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        return holder.getFlowsById().get(flowIdentifier);
    }

    @Override
    public Set<FlowMetadata> getFlows() {
        final MetadataHolder holder = metadataHolder.get();
        final Map<String,FlowMetadata> flowsById = holder.getFlowsById();
        return new HashSet<>(flowsById.values());
    }

    @Override
    public Set<FlowMetadata> getFlows(String bucketId) {
        final MetadataHolder holder = metadataHolder.get();

        final Map<String,Set<FlowMetadata>> flowsByBucket = holder.getFlowsByBucket();
        if (flowsByBucket.containsKey(bucketId)) {
            return new HashSet<>(flowsByBucket.get(bucketId));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public synchronized FlowMetadata updateFlow(final FlowMetadata versionedFlow) {
        if (versionedFlow == null) {
            throw new IllegalArgumentException("Versioned Flow cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        final Flows flows = holder.getMetadata().getFlows();

        final Flow jaxbFlow = flows.getFlow().stream()
                .filter(f -> versionedFlow.getIdentifier().equals(f.getIdentifier()))
                .findFirst()
                .orElse(null);

        if (jaxbFlow == null) {
            return null;
        }

        // TODO should we allow changing the bucket id here, if so it needs to be passed in
        jaxbFlow.setName(versionedFlow.getName());
        jaxbFlow.setDescription(versionedFlow.getDescription());
        jaxbFlow.setModifiedTimestamp(System.currentTimeMillis());

        saveAndRefresh(holder.getMetadata());
        return metadataHolder.get().getFlowsById().get(versionedFlow.getIdentifier());
    }

    @Override
    public synchronized void deleteFlow(final String flowIdentifier) {
        if (flowIdentifier == null) {
            throw new IllegalArgumentException("Flow Identifier cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        final Flows flows = holder.getMetadata().getFlows();

        boolean deleted = false;
        final Iterator<Flow> flowIter = flows.getFlow().iterator();

        while (flowIter.hasNext()) {
            final Flow jaxbFlow = flowIter.next();
            if (jaxbFlow.getIdentifier().equals(flowIdentifier)) {
                flowIter.remove();
                deleted = true;
                break;
            }
        }

        if (deleted) {
            saveAndRefresh(holder.getMetadata());
        }
    }

    @Override
    public synchronized FlowSnapshotMetadata createFlowSnapshot(final FlowSnapshotMetadata flowSnapshot) {
        if (flowSnapshot == null) {
            throw new IllegalArgumentException("Versioned Flow Snapshot cannot be null");
        }

        final String flowIdentifier = flowSnapshot.getFlowIdentifier();
        final int snapshotVersion = flowSnapshot.getVersion();

        final MetadataHolder holder = metadataHolder.get();
        final Flows flows = holder.getMetadata().getFlows();

        final Flow jaxbFlow = flows.getFlow().stream()
                .filter(f -> flowIdentifier.equals(f.getIdentifier()))
                .findFirst()
                .orElse(null);

        if (jaxbFlow == null) {
            throw new IllegalStateException("Unable to create snapshot because Versioned Flow does not exist for id " + flowIdentifier);
        }

        final Flow.Snapshot jaxbSnapshot = new Flow.Snapshot();
        jaxbSnapshot.setVersion(flowSnapshot.getVersion());
        jaxbSnapshot.setComments(flowSnapshot.getComments());
        jaxbSnapshot.setCreatedTimestamp(flowSnapshot.getCreatedTimestamp());

        jaxbFlow.getSnapshot().add(jaxbSnapshot);
        saveAndRefresh(holder.getMetadata());

        final FlowMetadata versionedFlow = metadataHolder.get().getFlowsById().get(flowIdentifier);
        return versionedFlow.getSnapshot(snapshotVersion);
    }

    @Override
    public FlowSnapshotMetadata getFlowSnapshot(final String flowIdentifier, final Integer version) {
        if (flowIdentifier == null) {
            throw new IllegalArgumentException("Flow Identifier cannot be null");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();

        final FlowMetadata versionedFlow = holder.getFlowsById().get(flowIdentifier);
        if (versionedFlow == null) {
            return null;
        }

        return versionedFlow.getSnapshot(version);
    }

    @Override
    public synchronized void deleteFlowSnapshot(final String flowIdentifier, final Integer version) {
        if (flowIdentifier == null) {
            throw new IllegalArgumentException("Flow Identifier cannot be null");
        }

        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }

        final MetadataHolder holder = metadataHolder.get();
        final Flows flows = holder.getMetadata().getFlows();

        final Flow jaxbFlow = flows.getFlow().stream()
                .filter(f -> flowIdentifier.equals(f.getIdentifier()))
                .findFirst()
                .orElse(null);

        if (jaxbFlow == null) {
            return;
        }

        boolean deletedSnapshot = false;
        final Iterator<Flow.Snapshot> snapshotIterator = jaxbFlow.getSnapshot().iterator();

        while (snapshotIterator.hasNext()) {
            final Flow.Snapshot snapshot = snapshotIterator.next();
            if (snapshot.getVersion().equals(version)) {
                snapshotIterator.remove();
                deletedSnapshot = true;
                break;
            }
        }

        if (deletedSnapshot) {
            saveAndRefresh(holder.getMetadata());
        }
    }

}
