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
package org.apache.nifi.registry.provider;

import org.apache.nifi.registry.provider.sync.RepositorySyncStatus;

import java.io.IOException;

public interface ProviderSynchronization {
    /**
     * define that this persistence provider is capable of making a synchronization
     *
     * @return true, if the instance can synchronize the repository otherwise false
     */
    Boolean canBeSynchronized();
    /**
     * synchronizes the repository with the remote repository (pulling changes)
     */
    void getLatestChangesOfRemoteRepository() throws IOException;

    /**
     * reset repository completely and re-synchronize with the remote repository
     */
    void resetRepository() throws IOException;

    /**
     * get the current status of the repository synchronization
     *
     * @return RepositorySyncStatus assembling the information about the status
     * @throws IOException when the provider cannot execute the status command
     */
    RepositorySyncStatus getStatus() throws IOException;
}
