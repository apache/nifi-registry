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
package org.apache.nifi.registry.event;

import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.hook.Event;
import org.apache.nifi.registry.hook.EventFieldName;
import org.apache.nifi.registry.hook.EventType;
import org.apache.nifi.registry.security.authorization.user.NiFiUserUtils;

/**
 * Factory to create Events from domain objects.
 */
public class EventFactory {

    public static Event bucketCreated(final Bucket bucket) {
        return new StandardEvent.Builder()
                .eventType(EventType.CREATE_BUCKET)
                .addField(EventFieldName.BUCKET_ID, bucket.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event bucketUpdated(final Bucket bucket) {
        return new StandardEvent.Builder()
                .eventType(EventType.UPDATE_BUCKET)
                .addField(EventFieldName.BUCKET_ID, bucket.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event bucketDeleted(final Bucket bucket) {
        return new StandardEvent.Builder()
                .eventType(EventType.DELETE_BUCKET)
                .addField(EventFieldName.BUCKET_ID, bucket.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event flowCreated(final VersionedFlow versionedFlow) {
        return new StandardEvent.Builder()
                .eventType(EventType.CREATE_FLOW)
                .addField(EventFieldName.BUCKET_ID, versionedFlow.getBucketIdentifier())
                .addField(EventFieldName.FLOW_ID, versionedFlow.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event flowUpdated(final VersionedFlow versionedFlow) {
        return new StandardEvent.Builder()
                .eventType(EventType.UPDATE_FLOW)
                .addField(EventFieldName.BUCKET_ID, versionedFlow.getBucketIdentifier())
                .addField(EventFieldName.FLOW_ID, versionedFlow.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event flowDeleted(final VersionedFlow versionedFlow) {
        return new StandardEvent.Builder()
                .eventType(EventType.DELETE_FLOW)
                .addField(EventFieldName.BUCKET_ID, versionedFlow.getBucketIdentifier())
                .addField(EventFieldName.FLOW_ID, versionedFlow.getIdentifier())
                .addField(EventFieldName.USER, NiFiUserUtils.getNiFiUserIdentity())
                .build();
    }

    public static Event flowVersionCreated(final VersionedFlowSnapshot versionedFlowSnapshot) {
        return new StandardEvent.Builder()
                .eventType(EventType.CREATE_FLOW_VERSION)
                .addField(EventFieldName.BUCKET_ID, versionedFlowSnapshot.getSnapshotMetadata().getBucketIdentifier())
                .addField(EventFieldName.FLOW_ID, versionedFlowSnapshot.getSnapshotMetadata().getFlowIdentifier())
                .addField(EventFieldName.VERSION, String.valueOf(versionedFlowSnapshot.getSnapshotMetadata().getVersion()))
                .addField(EventFieldName.USER, versionedFlowSnapshot.getSnapshotMetadata().getAuthor())
                .addField(EventFieldName.COMMENT, versionedFlowSnapshot.getSnapshotMetadata().getComments())
                .build();
    }

}
