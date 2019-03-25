package org.apache.nifi.registry.provider.flow.git;

import java.util.Collection;

public class SyncStatus {
    private boolean isClean;
    private boolean hasUncommittedChanges;
    private Collection<String> conflictingChanges;

    public SyncStatus(boolean isClean, boolean hasUncommittedChanges, Collection<String> conflictingChanges) {
        this.isClean = isClean;
        this.hasUncommittedChanges = hasUncommittedChanges;
        this.conflictingChanges = conflictingChanges;
    }

    public boolean isClean() {
        return isClean;
    }

    public boolean hasUncommittedChanges() {
        return hasUncommittedChanges;
    }

    public Collection<String> getConflictingChanges() {
        return conflictingChanges;
    }
}
