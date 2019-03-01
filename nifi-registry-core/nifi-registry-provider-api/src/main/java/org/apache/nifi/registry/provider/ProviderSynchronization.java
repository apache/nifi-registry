package org.apache.nifi.registry.provider;

import java.io.IOException;
import java.net.URI;

public interface ProviderSynchronization {
    Boolean canBeSynchronized();
    /**
     * synchronizes the repository with the remote repository (pulling changes)
     */
    void getLatestChangesOfRemoteRepository() throws IOException;

    /**
     * reset repository completely and re-synchronize with the remote repository
     */
    void resetRepository(URI repositoryURI) throws IOException;
}
