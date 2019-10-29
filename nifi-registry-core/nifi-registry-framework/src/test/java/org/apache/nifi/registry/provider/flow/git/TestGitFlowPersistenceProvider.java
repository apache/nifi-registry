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
package org.apache.nifi.registry.provider.flow.git;

import org.apache.nifi.registry.flow.FlowPersistenceException;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.apache.nifi.registry.provider.StandardProviderConfigurationContext;
import org.apache.nifi.registry.provider.flow.StandardFlowSnapshotContext;
import org.apache.nifi.registry.provider.sync.RepositorySyncStatus;
import org.apache.nifi.registry.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.apache.nifi.registry.provider.flow.git.GitFlowPersistenceProvider.REMOTE_TO_PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestGitFlowPersistenceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TestGitFlowPersistenceProvider.class);
    private String REMOTE_REPO_DIR_PROP = "REMOTE_REPO_DIR_PROP";

    private void assertCreationFailure(final Map<String, String> properties, final Consumer<ProviderCreationException> assertion) {
        final GitFlowPersistenceProvider persistenceProvider = new GitFlowPersistenceProvider();

        try {
            final ProviderConfigurationContext configurationContext = new StandardProviderConfigurationContext(properties);
            persistenceProvider.onConfigured(configurationContext);
            fail("Should fail");
        } catch (ProviderCreationException e) {
            assertion.accept(e);
        }
    }

    @Test
    public void testNoFlowStorageDirSpecified() {
        final Map<String, String> properties = new HashMap<>();
        assertCreationFailure(properties,
                e -> assertEquals("The property Flow Storage Directory must be provided", e.getMessage()));
    }

    @Test
    public void testLoadNonExistingDir() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/non-existing");
        assertCreationFailure(properties,
                e -> assertEquals("'target" + File.separator + "non-existing' is not a directory or does not exist.", e.getCause().getMessage()));
    }

    @Test
    public void testLoadNonGitDir() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target");
        assertCreationFailure(properties,
                e -> assertEquals("Directory 'target' does not contain a .git directory." +
                                " Please init and configure the directory with 'git init' command before using it from NiFi Registry.",
                        e.getCause().getMessage()));
    }

    @FunctionalInterface
    private interface GitConsumer {
        void accept(Git git) throws GitAPIException;
    }

    private void assertProvider(final Map<String, String> properties, final GitConsumer gitConsumer, final Consumer<GitFlowPersistenceProvider> assertion, boolean deleteDir)
            throws IOException, GitAPIException {

        final File gitDir = new File(properties.get(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP));
        try {
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(gitDir);

            initializeLocalRepository(gitConsumer, gitDir);

            final GitFlowPersistenceProvider persistenceProvider = configureGitFlowPersistenceProvider(properties);
            assertion.accept(persistenceProvider);

        } finally {
            if (deleteDir) {
                FileUtils.deleteFile(gitDir, true);
            }
        }
    }

    private void initializeLocalRepository(GitConsumer gitConsumer, File gitDir) throws IOException, GitAPIException {
        try (final Git git = Git.init().setDirectory(gitDir).call()) {
            logger.debug("Initiated a git repository {}", git);
            final StoredConfig config = git.getRepository().getConfig();
            config.setString("user", null, "name", "git-user");
            config.setString("user", null, "email", "git-user@example.com");
            config.save();
            gitConsumer.accept(git);
        }
    }

    private void cleanupGitRepository(File gitDir) throws IOException {
        deleteGitRepository(gitDir);
        FileUtils.ensureDirectoryExistAndCanReadAndWrite(gitDir);
    }

    private GitFlowPersistenceProvider configureGitFlowPersistenceProvider(Map<String, String> properties) {
        final GitFlowPersistenceProvider persistenceProvider = new GitFlowPersistenceProvider();
        final ProviderConfigurationContext configurationContext = new StandardProviderConfigurationContext(properties);
        persistenceProvider.onConfigured(configurationContext);
        return persistenceProvider;
    }

    private void cloneIntoLocalRepository(GitConsumer gitConsumer, File gitDir, File remoteGitDir) throws IOException, GitAPIException {
        try (final Git git = Git.cloneRepository()
                .setURI(remoteGitDir.getAbsolutePath())
                .setDirectory(gitDir).call()) {
            logger.debug("Initiated a git repository {}", git);
            final StoredConfig config = git.getRepository().getConfig();
            config.setString("user", null, "name", "git-user");
            config.setString("user", null, "email", "git-user@example.com");
            config.save();
            gitConsumer.accept(git);
        }
    }

    private void createGitRemoteRepository(File remoteGitDir) throws GitAPIException, IOException {
        boolean gitRepoExists = remoteGitDir.exists()
                                    && org.apache.commons.io.FileUtils.directoryContains(remoteGitDir,
                                                                        new File(remoteGitDir, "HEAD"));
        if (gitRepoExists) {
            return;
        }

        Git.init().setBare(true).setDirectory(remoteGitDir).setGitDir(remoteGitDir).call().close();
        logger.info("initialized remote git repository at " + remoteGitDir.getAbsolutePath());
    }

    private void deleteGitRepository(File gitDir) throws IOException {
        if (gitDir.exists()) {
            org.apache.commons.io.FileUtils.deleteDirectory(gitDir);
        }
    }

    @Test
    public void testLoadEmptyGitDir() throws GitAPIException, IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/empty-git");

        assertProvider(properties, g -> {
        }, p -> {
            try {
                p.getFlowContent("bucket-id-A", "flow-id-1", 1);
            } catch (FlowPersistenceException e) {
                assertEquals("Bucket ID bucket-id-A was not found.", e.getMessage());
            }
        }, true);
    }

    @Test
    public void testLoadCommitHistories() throws GitAPIException, IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/repo-with-histories");

        assertProvider(properties, g -> {}, p -> {
            // Create some Flows and keep the directory.
            final StandardFlowSnapshotContext.Builder contextBuilder = new StandardFlowSnapshotContext.Builder()
                    .bucketId("bucket-id-A")
                    .bucketName("C'est/Bucket A/です。")
                    .flowId("flow-id-1")
                    .flowName("テスト_用/フロー#1\\[contains invalid chars]")
                    .author("unit-test-user")
                    .comments("Initial commit.")
                    .snapshotTimestamp(new Date().getTime())
                    .version(1);

            final byte[] flow1Ver1 = "Flow1 ver.1".getBytes(StandardCharsets.UTF_8);
            p.saveFlowContent(contextBuilder.build(), flow1Ver1);

            contextBuilder.comments("2nd commit.").version(2);
            final byte[] flow1Ver2 = "Flow1 ver.2".getBytes(StandardCharsets.UTF_8);
            p.saveFlowContent(contextBuilder.build(), flow1Ver2);

            // Rename flow.
            contextBuilder.flowName("FlowOne").comments("3rd commit.").version(3);
            final byte[] flow1Ver3 = "FlowOne ver.3".getBytes(StandardCharsets.UTF_8);
            p.saveFlowContent(contextBuilder.build(), flow1Ver3);

            // Adding another flow.
            contextBuilder.flowId("flow-id-2").flowName("FlowTwo").comments("4th commit.").version(1);
            final byte[] flow2Ver1 = "FlowTwo ver.1".getBytes(StandardCharsets.UTF_8);
            p.saveFlowContent(contextBuilder.build(), flow2Ver1);

            // Rename bucket.
            contextBuilder.bucketName("New name for Bucket A").comments("5th commit.").version(2);
            final byte[] flow2Ver2 = "FlowTwo ver.2".getBytes(StandardCharsets.UTF_8);
            p.saveFlowContent(contextBuilder.build(), flow2Ver2);


        }, false);

        assertProvider(properties, g -> {
            // Assert commit.
            final AtomicInteger commitCount = new AtomicInteger(0);
            final String[] commitMessages = {
                    "5th commit.\n\nBy NiFi Registry user: unit-test-user",
                    "4th commit.\n\nBy NiFi Registry user: unit-test-user",
                    "3rd commit.\n\nBy NiFi Registry user: unit-test-user",
                    "2nd commit.\n\nBy NiFi Registry user: unit-test-user",
                    "Initial commit.\n\nBy NiFi Registry user: unit-test-user"
            };
            for (RevCommit commit : g.log().call()) {
                assertEquals("git-user", commit.getAuthorIdent().getName());
                final int commitIndex = commitCount.getAndIncrement();
                assertEquals(commitMessages[commitIndex], commit.getFullMessage());
            }
            assertEquals(commitMessages.length, commitCount.get());
        }, p -> {
            // Should be able to load flow from commit histories.
            final byte[] flow1Ver1 = p.getFlowContent("bucket-id-A", "flow-id-1", 1);
            assertEquals("Flow1 ver.1", new String(flow1Ver1, StandardCharsets.UTF_8));

            final byte[] flow1Ver2 = p.getFlowContent("bucket-id-A", "flow-id-1", 2);
            assertEquals("Flow1 ver.2", new String(flow1Ver2, StandardCharsets.UTF_8));

            // Even if the name of flow has been changed, it can be retrieved by the same flow id.
            final byte[] flow1Ver3 = p.getFlowContent("bucket-id-A", "flow-id-1", 3);
            assertEquals("FlowOne ver.3", new String(flow1Ver3, StandardCharsets.UTF_8));

            final byte[] flow2Ver1 = p.getFlowContent("bucket-id-A", "flow-id-2", 1);
            assertEquals("FlowTwo ver.1", new String(flow2Ver1, StandardCharsets.UTF_8));

            // Even if the name of bucket has been changed, it can be retrieved by the same flow id.
            final byte[] flow2Ver2 = p.getFlowContent("bucket-id-A", "flow-id-2", 2);
            assertEquals("FlowTwo ver.2", new String(flow2Ver2, StandardCharsets.UTF_8));

            // Delete the 2nd flow.
            p.deleteAllFlowContent("bucket-id-A", "flow-id-2");

        }, false);

        assertProvider(properties, g -> {
            // Assert commit.
            final AtomicInteger commitCount = new AtomicInteger(0);
            final String[] commitMessages = {
                    "Deleted flow FlowTwo.snapshot:flow-id-2 in bucket New_name_for_Bucket_A:bucket-id-A.",
                    "5th commit.",
                    "4th commit.",
                    "3rd commit.",
                    "2nd commit.",
                    "Initial commit."
            };
            for (RevCommit commit : g.log().call()) {
                assertEquals("git-user", commit.getAuthorIdent().getName());
                final int commitIndex = commitCount.getAndIncrement();
                assertEquals(commitMessages[commitIndex], commit.getShortMessage());
            }
            assertEquals(commitMessages.length, commitCount.get());
        }, p -> {
            // Should be able to load flow from commit histories.
            final byte[] flow1Ver1 = p.getFlowContent("bucket-id-A", "flow-id-1", 1);
            assertEquals("Flow1 ver.1", new String(flow1Ver1, StandardCharsets.UTF_8));

            final byte[] flow1Ver2 = p.getFlowContent("bucket-id-A", "flow-id-1", 2);
            assertEquals("Flow1 ver.2", new String(flow1Ver2, StandardCharsets.UTF_8));

            // Even if the name of flow has been changed, it can be retrieved by the same flow id.
            final byte[] flow1Ver3 = p.getFlowContent("bucket-id-A", "flow-id-1", 3);
            assertEquals("FlowOne ver.3", new String(flow1Ver3, StandardCharsets.UTF_8));

            // The 2nd flow has been deleted, and should not exist.
            try {
                p.getFlowContent("bucket-id-A", "flow-id-2", 1);
            } catch (FlowPersistenceException e) {
                assertEquals("Flow ID flow-id-2 was not found in bucket New_name_for_Bucket_A:bucket-id-A.", e.getMessage());
            }

            try {
                p.getFlowContent("bucket-id-A", "flow-id-2", 2);
            } catch (FlowPersistenceException e) {
                assertEquals("Flow ID flow-id-2 was not found in bucket New_name_for_Bucket_A:bucket-id-A.", e.getMessage());
            }

            // Delete the 1st flow, too.
            p.deleteAllFlowContent("bucket-id-A", "flow-id-1");

        }, false);

        assertProvider(properties, g -> {
            // Assert commit.
            final AtomicInteger commitCount = new AtomicInteger(0);
            final String[] commitMessages = {
                    "Deleted flow FlowOne.snapshot:flow-id-1 in bucket New_name_for_Bucket_A:bucket-id-A.",
                    "Deleted flow FlowTwo.snapshot:flow-id-2 in bucket New_name_for_Bucket_A:bucket-id-A.",
                    "5th commit.",
                    "4th commit.",
                    "3rd commit.",
                    "2nd commit.",
                    "Initial commit."
            };
            for (RevCommit commit : g.log().call()) {
                assertEquals("git-user", commit.getAuthorIdent().getName());
                final int commitIndex = commitCount.getAndIncrement();
                assertEquals(commitMessages[commitIndex], commit.getShortMessage());
            }
            assertEquals(commitMessages.length, commitCount.get());
        }, p -> {
            // The 1st flow has been deleted, and should not exist. Moreover, the bucket A has been deleted since there's no flow.
            try {
                p.getFlowContent("bucket-id-A", "flow-id-1", 1);
            } catch (FlowPersistenceException e) {
                assertEquals("Bucket ID bucket-id-A was not found.", e.getMessage());
            }
        }, true);
    }

    @Test
    public void testResetGitRepository() throws IOException, GitAPIException, InterruptedException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/local-repo");
        properties.put(REMOTE_TO_PUSH, "origin");
        //inject variable which exists in the test env only
        properties.put(REMOTE_REPO_DIR_PROP, "target/remote-repo");
        final File gitDir = new File(properties.get(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP));
        final File remoteGitDir = new File(properties.get(REMOTE_REPO_DIR_PROP));

        try {
            cleanupGitRepository(gitDir);
            cleanupGitRepository(remoteGitDir);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(gitDir);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(remoteGitDir);

            createGitRemoteRepository(remoteGitDir);

            cloneIntoLocalRepository(g -> {
            }, gitDir, remoteGitDir);
            final GitFlowPersistenceProvider sut = configureGitFlowPersistenceProvider(properties);
            commitInitialSampleChanges(sut, builder -> {
            });
            waitUntilPushHasBeenFinished();

            sut.resetRepository();

            final byte[] flowVersion = sut.getFlowContent("bucket-id-A", "flow-id-1", 2);
            assertEquals("Flow1 ver.2", new String(flowVersion, StandardCharsets.UTF_8));
            // free all handles
            sut.flowMetaData.closeRepository();
        } finally {
            deleteGitRepository(gitDir);
            deleteGitRepository(remoteGitDir);
        }
    }

    private void commitInitialSampleChanges(GitFlowPersistenceProvider p,
                                            Consumer<StandardFlowSnapshotContext.Builder> postInitialChangesLambda) {
        final StandardFlowSnapshotContext.Builder contextBuilder = new StandardFlowSnapshotContext.Builder()
                .bucketId("bucket-id-A")
                .bucketName("C'est/Bucket A/です。")
                .flowId("flow-id-1")
                .flowName("テスト_用/フロー#1\\[contains invalid chars]")
                .author("unit-test-user")
                .comments("Initial commit.")
                .snapshotTimestamp(new Date().getTime())
                .version(1);

        final byte[] flow1Ver1 = "Flow1 ver.1".getBytes(StandardCharsets.UTF_8);
        p.saveFlowContent(contextBuilder.build(), flow1Ver1);

        contextBuilder.comments("2nd commit.").version(2);
        final byte[] flow1Ver2 = "Flow1 ver.2".getBytes(StandardCharsets.UTF_8);
        p.saveFlowContent(contextBuilder.build(), flow1Ver2);

        postInitialChangesLambda.accept(contextBuilder);
    }

    @Test
    public void testPullChanges() throws InterruptedException, GitAPIException, IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/local-repo");
        properties.put(REMOTE_TO_PUSH, "origin");
        //inject variable which exists in the test env only
        properties.put(REMOTE_REPO_DIR_PROP, "target/remote-repo");
        final File gitDir = new File(properties.get(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP));
        final File secondGitDir = new File("target/second-local-repo");
        final File remoteGitDir = new File(properties.get(REMOTE_REPO_DIR_PROP));


        try {
            cleanupGitRepository(gitDir);
            cleanupGitRepository(secondGitDir);
            cleanupGitRepository(remoteGitDir);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(gitDir);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(remoteGitDir);

            createGitRemoteRepository(remoteGitDir);
            cloneIntoLocalRepository(g -> {
            }, gitDir, remoteGitDir);
            final GitFlowPersistenceProvider sut = configureGitFlowPersistenceProvider(properties);
            commitInitialSampleChanges(sut, builder -> {
            });
            waitUntilPushHasBeenFinished();

            cloneIntoLocalRepository(g -> {
            }, secondGitDir, remoteGitDir);
            properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/second-local-repo");
            final GitFlowPersistenceProvider secondRepo = configureGitFlowPersistenceProvider(properties);
            commitInitialSampleChanges(secondRepo, builder -> {
                builder.comments("3rd commit made in the remote repository only.").version(3);
                final byte[] flow1Ver3 = "Flow1 ver.3".getBytes(StandardCharsets.UTF_8);
                secondRepo.saveFlowContent(builder.build(), flow1Ver3);
            });
            waitUntilPushHasBeenFinished();

            sut.getLatestChangesOfRemoteRepository();
            final byte[] flowVersion = sut.getFlowContent("bucket-id-A", "flow-id-1", 3);
            assertEquals("Flow1 ver.3", new String(flowVersion, StandardCharsets.UTF_8));

            // free all handles
            sut.flowMetaData.closeRepository();
            secondRepo.flowMetaData.closeRepository();
        } finally {
            deleteGitRepository(gitDir);
            deleteGitRepository(secondGitDir);
            deleteGitRepository(remoteGitDir);
        }
    }

    /*
    just testing the happy path
     */
    @Test
    public void testGetSyncStatus() throws IOException, GitAPIException, InterruptedException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP, "target/local-repo");
        final File gitDir = new File(properties.get(GitFlowPersistenceProvider.FLOW_STORAGE_DIR_PROP));

        try {
            cleanupGitRepository(gitDir);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(gitDir);

            initializeLocalRepository(g -> {}, gitDir);
            final GitFlowPersistenceProvider sut = configureGitFlowPersistenceProvider(properties);
            final RepositorySyncStatus actualSyncStatus = sut.getStatus();
            final RepositorySyncStatus expectedSyncStatus = RepositorySyncStatus.SuccessfulSynchronizedRepository();

            assertEquals(expectedSyncStatus.isClean(), actualSyncStatus.isClean());
            assertEquals(expectedSyncStatus.hasChanges(), actualSyncStatus.hasChanges());
            assertEquals(expectedSyncStatus.changes().isEmpty(), actualSyncStatus.changes().isEmpty());

            // free all handles
            sut.flowMetaData.closeRepository();
        } finally {
            deleteGitRepository(gitDir);
        }
    }

    private void waitUntilPushHasBeenFinished() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
