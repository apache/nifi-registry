package org.apache.nifi.registry.provider;

import org.apache.nifi.registry.provider.sync.RepositorySyncStatus;

import java.io.IOException;

public interface ProviderSynchronization {
    /**
     * define that the instance is capable of making a synchronization
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
     * @throws IOException
     */
    RepositorySyncStatus getStatus() throws IOException;
}
