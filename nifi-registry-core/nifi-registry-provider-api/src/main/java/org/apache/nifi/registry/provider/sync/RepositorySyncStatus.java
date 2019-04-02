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
package org.apache.nifi.registry.provider.sync;

import java.util.ArrayList;
import java.util.Collection;

public class RepositorySyncStatus {
    private boolean isClean;
    private boolean hasChanges;
    private Collection<String> changes;

    public RepositorySyncStatus(boolean isClean, boolean hasChanges, Collection<String> changes) {
        this.isClean = isClean;
        this.hasChanges = hasChanges;
        this.changes = changes;
    }

    public static RepositorySyncStatus SuccessfulSynchronizedRepository(){
        return new RepositorySyncStatus(true, false, new ArrayList<>());
    }

    public boolean isClean() {
        return isClean;
    }

    public boolean hasChanges() {
        return this.hasChanges;
    }

    public Collection<String> changes() {
        return this.changes;
    }
}
