package org.apache.nifi.registry.sync;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
@ApiModel(value = "RepositorySyncStatus")
public class RepositorySyncStatus {
    private boolean isClean;
    private boolean hasChanges;
    private Collection<String> changes;

    public RepositorySyncStatus(boolean isClean, boolean hasChanges, Collection<String> changes) {
        this.isClean = isClean;
        this.hasChanges = hasChanges;
        this.changes = changes;
    }

    public RepositorySyncStatus(){}

    @ApiModelProperty(value = "Repository is in sync with registry.", required = true)
    public boolean getIsClean() {
        return isClean;
    }
    public void setIsClean(boolean isClean) { this.isClean = isClean; }

    @ApiModelProperty(value = "The repository contains changes not reflected in registry.", required = true)
    public boolean getHasChanges() {
        return this.hasChanges;
    }
    public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }

    @ApiModelProperty(value = "List of changes in the repository which should be synchronized with registry.", required = true)
    public Collection<String> getChanges() {
        return this.changes;
    }
    public void setChanges(Collection<String> changes) {
        this.changes = changes;
    }
}
