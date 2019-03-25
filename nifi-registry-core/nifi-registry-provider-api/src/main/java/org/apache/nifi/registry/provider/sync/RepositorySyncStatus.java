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
