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
package org.apache.nifi.registry.provider.hook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.FlowSnapshotContext;
import org.apache.nifi.registry.hook.FlowHookEvent;
import org.apache.nifi.registry.hook.FlowHookException;
import org.apache.nifi.registry.hook.FlowHookProvider;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FlowHookProvider that is used to execute a script before a flow snapshot version is committed.
 */
public class ScriptFlowHookProvider implements FlowHookProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(ScriptFlowHookProvider.class);
    static final String SCRIPT_PATH_PROP = "Script Path";
    static final String SCRIPT_WORKDIR_PROP = "Working Directory";
    private File scriptFile;
    private File workDirFile;

    @Override
    public void postCreateBucket(String bucketId) throws FlowHookException {
        this.executeScript(FlowHookEvent.CREATE_BUCKET, bucketId, null, null, null, null);
    }

    @Override
    public void postCreateFlow(String bucketId, String flowId) throws FlowHookException {
        this.executeScript(FlowHookEvent.CREATE_FLOW, bucketId, flowId, null, null, null);
    }

    @Override
    public void postDeleteBucket(String bucketId) throws FlowHookException {
        this.executeScript(FlowHookEvent.DELETE_BUCKET, bucketId, null, null, null, null);
    }

    @Override
    public void postDeleteFlow(String bucketId, String flowId) throws FlowHookException {
        this.executeScript(FlowHookEvent.DELETE_FLOW, bucketId, flowId, null, null, null);
    }

    @Override
    public void postDeleteFlowVersion(String bucketId, String flowId, int version) throws FlowHookException {
        this.executeScript(FlowHookEvent.DELETE_FLOW, bucketId, flowId, Integer.toString(version), null, null);
    }

    @Override
    public void postUpdateBucket(String bucketId) throws FlowHookException {
        this.executeScript(FlowHookEvent.UPDATE_BUCKET, bucketId, null, null, null, null);
    }

    @Override
    public void postUpdateFlow(String bucketId, String flowId) throws FlowHookException {
        this.executeScript(FlowHookEvent.UPDATE_FLOW, bucketId, flowId, null, null, null);
    }

    @Override
    public void postCreateFlowVersion(final FlowSnapshotContext flowSnapshotContext) throws FlowHookException {
        this.executeScript(FlowHookEvent.CREATE_VERSION, flowSnapshotContext.getBucketId(), flowSnapshotContext.getFlowId(),
                Integer.toString(flowSnapshotContext.getVersion()), flowSnapshotContext.getComments(), flowSnapshotContext.getAuthor());
    }

    private void executeScript(final FlowHookEvent eventType, String bucketId, String flowId, String version, String comment, String author) {
        List<String> command = new ArrayList<String>();
        command.add(scriptFile.getAbsolutePath());
        command.add(eventType.name());
        command.add(bucketId);

        if(flowId != null) {
            command.add(flowId);
        }

        if(version != null) {
            command.add(version);
        }

        if(comment != null) {
            command.add(comment);
        }

        if(author != null) {
            command.add(author);
        }

        final String commandString = StringUtils.join(command, " ");
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDirFile);
        LOGGER.debug("Execution of " + commandString);

        try {
            builder.start();
        } catch (IOException e) {
            LOGGER.error("Execution of {0} failed with: {1}", new Object[] { commandString, e.getLocalizedMessage() }, e);
        }
    }

    @Override
    public void onConfigured(ProviderConfigurationContext configurationContext) throws ProviderCreationException {
        final Map<String,String> props = configurationContext.getProperties();
        if (!props.containsKey(SCRIPT_PATH_PROP)) {
            throw new ProviderCreationException("The property " + SCRIPT_PATH_PROP + " must be provided");
        }

        final String scripPath = props.get(SCRIPT_PATH_PROP);
        if (StringUtils.isBlank(scripPath)) {
            throw new ProviderCreationException("The property " + SCRIPT_PATH_PROP + " cannot be null or blank");
        }

        if(props.containsKey(SCRIPT_WORKDIR_PROP) && !StringUtils.isBlank(props.get(SCRIPT_WORKDIR_PROP))) {
            final String workdir = props.get(SCRIPT_WORKDIR_PROP);
            try {
                workDirFile = new File(workdir);
                FileUtils.ensureDirectoryExistAndCanRead(workDirFile);
            } catch (IOException e) {
                throw new ProviderCreationException("The working directory " + workdir + " cannot be read.");
            }
        }

        scriptFile = new File(scripPath);
        if(scriptFile.isFile() && scriptFile.canExecute()) {
            LOGGER.info("Configured ScriptFlowHookProvider with script {}", new Object[] {scriptFile.getAbsolutePath()});
        } else {
            throw new ProviderCreationException("The script file " + scriptFile.getAbsolutePath() + " cannot be executed.");
        }
    }

}
