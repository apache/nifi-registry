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
package org.apache.nifi.registry.extension;

/**
 * The context that will be passed to the {@link BundlePersistenceProvider} when saving a new version of an extension bundle.
 */
public interface BundleContext {

    enum BundleType {
        NIFI_NAR,
        MINIFI_CPP;
    }

    /**
     * @return the id of the bucket the bundle belongs to
     */
    String getBucketId();

    /**
     * @return the name of the bucket the bundle belongs to
     */
    String getBucketName();

    /**
     * @return the type of the bundle
     */
    BundleType getBundleType();

    /**
     * @return the NiFi Registry id of the bundle
     */
    String getBundleId();

    /**
     * @return the group id of the bundle
     */
    String getBundleGroupId();

    /**
     * @return the artifact id of the bundle
     */
    String getBundleArtifactId();

    /**
     * @return the version of the bundle
     */
    String getBundleVersion();

    /**
     * @return the size of the bundle content in bytes
     */
    long getBundleSize();

    /**
     * @return the comments for the version of the bundle
     */
    String getDescription();

    /**
     * @return the timestamp the bundle was created
     */
    long getTimestamp();

    /**
     * @return the user that created the bundle
     */
    String getAuthor();

}
