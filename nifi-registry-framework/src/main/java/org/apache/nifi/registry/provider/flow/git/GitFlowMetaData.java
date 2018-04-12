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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

class GitFlowMetaData {

    static final int CURRENT_LAYOUT_VERSION = 1;

    static final String LAYOUT_VERSION = "layoutVer";
    static final String BUCKET_ID = "bucketId";
    static final String FLOWS = "flows";
    static final String VER = "ver";
    static final String FILE = "file";
    static final String BUCKET_FILENAME = "bucket.yml";

    private static final Logger logger = LoggerFactory.getLogger(GitFlowMetaData.class);

    private Repository gitRepo;
    private String remoteToPush;
    private CredentialsProvider credentialsProvider;

    /**
     * Bucket ID to Bucket.
     */
    private Map<String, Bucket> buckets = new HashMap<>();

    public void setRemoteToPush(String remoteToPush) {
        this.remoteToPush = remoteToPush;
    }

    public void setRemoteCredential(String userName, String password) {
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(userName, password);
    }

    /**
     * Open a Git repository using the specified directory.
     * @param gitProjectRootDir a root directory of a Git project
     * @return created Repository
     * @throws IOException thrown when the specified directory does not exist,
     * does not have read/write privilege or not containing .git directory
     */
    private Repository openRepository(final File gitProjectRootDir) throws IOException {

        // Instead of using FileUtils.ensureDirectoryExistAndCanReadAndWrite, check availability manually here.
        // Because the util will try to create a dir if not exist.
        // The git dir should be initialized and configured by users.
        if (!gitProjectRootDir.isDirectory()) {
            throw new IOException(format("'%s' is not a directory or does not exist.", gitProjectRootDir));
        }

        if (!(gitProjectRootDir.canRead() && gitProjectRootDir.canWrite())) {
            throw new IOException(format("Directory '%s' does not have read/write privilege.", gitProjectRootDir));
        }

        // Search .git dir but avoid searching parent directories.
        final FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .readEnvironment()
                .setMustExist(true)
                .addCeilingDirectory(gitProjectRootDir)
                .findGitDir(gitProjectRootDir);

        if (builder.getGitDir() == null) {
            throw new IOException(format("Directory '%s' does not contain a .git directory." +
                    " Please init and configure the directory with 'git init' command before using it from NiFi Registry.",
                    gitProjectRootDir));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    public void loadGitRepository(File gitProjectRootDir) throws IOException, GitAPIException {
        gitRepo = openRepository(gitProjectRootDir);

        try (final Git git = new Git(gitRepo)) {

            // Check if remote exists.
            if (!isEmpty(remoteToPush)) {
                final List<RemoteConfig> remotes = git.remoteList().call();
                final boolean isRemoteExist = remotes.stream().anyMatch(remote -> remote.getName().equals(remoteToPush));
                if (!isRemoteExist) {
                    final List<String> remoteNames = remotes.stream().map(RemoteConfig::getName).collect(Collectors.toList());
                    throw new IllegalArgumentException(
                            format("The configured remote '%s' to push does not exist. Available remotes are %s", remoteToPush, remoteNames));
                }
            }

            boolean isLatestCommit = true;
            try {
                for (RevCommit commit : git.log().call()) {
                    final String shortCommitId = commit.getId().abbreviate(7).name();
                    logger.debug("Processing a commit: {}", shortCommitId);
                    final RevTree tree = commit.getTree();

                    try (final TreeWalk treeWalk = new TreeWalk(gitRepo)) {
                        treeWalk.addTree(tree);

                        // Path -> ObjectId
                        final Map<String, ObjectId> bucketObjectIds = new HashMap<>();
                        final Map<String, ObjectId> flowSnapshotObjectIds = new HashMap<>();
                        while (treeWalk.next()) {
                            if (treeWalk.isSubtree()) {
                                treeWalk.enterSubtree();
                            } else {
                                final String pathString = treeWalk.getPathString();
                                // TODO: what is this nth?? When does it get grater than 0? Tree count seems to be always 1..
                                if (pathString.endsWith("/" + BUCKET_FILENAME)) {
                                    bucketObjectIds.put(pathString, treeWalk.getObjectId(0));
                                } else if (pathString.endsWith(GitFlowPersistenceProvider.SNAPSHOT_EXTENSION)) {
                                    flowSnapshotObjectIds.put(pathString, treeWalk.getObjectId(0));
                                }
                            }
                        }

                        if (bucketObjectIds.isEmpty()) {
                            // No bucket.yml means at this point, all flows are deleted. No need to scan older commits because those are already deleted.
                            logger.debug("Tree at commit {} does not contain any " + BUCKET_FILENAME + ". Stop loading commits here.", shortCommitId);
                            return;
                        }

                        loadBuckets(gitRepo, commit, isLatestCommit, bucketObjectIds, flowSnapshotObjectIds);
                        isLatestCommit = false;
                    }
                }
            } catch (NoHeadException e) {
                logger.debug("'{}' does not have any commit yet. Starting with empty buckets.", gitProjectRootDir);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadBuckets(Repository gitRepo, RevCommit commit, boolean isLatestCommit, Map<String, ObjectId> bucketObjectIds, Map<String, ObjectId> flowSnapshotObjectIds) throws IOException {
        final Yaml yaml = new Yaml();
        for (String bucketFilePath : bucketObjectIds.keySet()) {
            final ObjectId bucketObjectId = bucketObjectIds.get(bucketFilePath);
            final Map<String, Object> bucketMeta;
            try (InputStream bucketIn = gitRepo.newObjectReader().open(bucketObjectId).openStream()) {
                bucketMeta = yaml.load(bucketIn);
            }

            if (!validateRequiredValue(bucketMeta, bucketFilePath, LAYOUT_VERSION, BUCKET_ID, FLOWS)) {
                continue;
            }

            int layoutVersion = (int) bucketMeta.get(LAYOUT_VERSION);
            if (layoutVersion > CURRENT_LAYOUT_VERSION) {
                logger.warn("{} has unsupported {} {}. This Registry can only support {} or lower. Skipping it.",
                        bucketFilePath, LAYOUT_VERSION, layoutVersion, CURRENT_LAYOUT_VERSION);
                continue;
            }

            final String bucketId = (String) bucketMeta.get(BUCKET_ID);

            final Bucket bucket;
            if (isLatestCommit) {
                // If this is the latest commit, then create one.
                bucket = getBucketOrCreate(bucketId);
            } else {
                // Otherwise non-existing bucket means it's already deleted.
                final Optional<Bucket> bucketOpt = getBucket(bucketId);
                if (bucketOpt.isPresent()) {
                    bucket = bucketOpt.get();
                } else {
                    logger.debug("Bucket {} does not exist any longer. It may have been deleted.", bucketId);
                    continue;
                }
            }

            // E.g. DirA/DirB/DirC/bucket.yml -> DirC will be the bucket name.
            final String[] pathNames = bucketFilePath.split("/");
            final String bucketName = pathNames[pathNames.length - 2];

            // Since commits are read in LIFO order, avoid old commits overriding the latest bucket name.
            if (isEmpty(bucket.getBucketName())) {
                bucket.setBucketName(bucketName);
            }

            final Map<String, Object> flows = (Map<String, Object>) bucketMeta.get(FLOWS);
            loadFlows(commit, isLatestCommit, bucket, bucketFilePath, flows, flowSnapshotObjectIds);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFlows(RevCommit commit, boolean isLatestCommit, Bucket bucket, String backetFilePath, Map<String, Object> flows, Map<String, ObjectId> flowSnapshotObjectIds) {
        for (String flowId : flows.keySet()) {
            final Map<String, Object> flowMeta = (Map<String, Object>) flows.get(flowId);

            if (!validateRequiredValue(flowMeta, backetFilePath + ":" + flowId, VER, FILE)) {
                continue;
            }

            final Flow flow;
            if (isLatestCommit) {
                // If this is the latest commit, then create one.
                flow = bucket.getFlowOrCreate(flowId);
            } else {
                // Otherwise non-existing flow means it's already deleted.
                final Optional<Flow> flowOpt = bucket.getFlow(flowId);
                if (flowOpt.isPresent()) {
                    flow = flowOpt.get();
                } else {
                    logger.debug("Flow {} does not exist in bucket {}:{} any longer. It may have been deleted.", flowId, bucket.getBucketName(), bucket.getBucketId());
                    continue;
                }
            }

            final int version = (int) flowMeta.get(VER);
            final String flowSnapshotFilename = (String) flowMeta.get(FILE);

            // Since commits are read in LIFO order, avoid old commits overriding the latest pointer.
            if (!flow.hasVersion(version)) {
                final Flow.FlowPointer pointer = new Flow.FlowPointer(flowSnapshotFilename);
                final File flowSnapshotFile = new File(new File(backetFilePath).getParent(), flowSnapshotFilename);
                final ObjectId objectId = flowSnapshotObjectIds.get(flowSnapshotFile.getPath());
                if (objectId == null) {
                    logger.warn("Git object id for Flow {} version {} with path {} in bucket {}:{} was not found. Ignoring this entry.",
                            flowId, version, flowSnapshotFile.getPath(), bucket.getBucketName(), bucket.getBucketId());
                    continue;
                }
                pointer.setGitRev(commit.getName());
                pointer.setObjectId(objectId.getName());
                flow.putVersion(version, pointer);
            }
        }
    }

    private boolean validateRequiredValue(final Map map, String nameOfMap, Object ... keys) {
        for (Object key : keys) {
            if (!map.containsKey(key)) {
                logger.warn("{} does not have {}. Skipping it.", nameOfMap, key);
                return false;
            }
        }
        return true;
    }

    public Bucket getBucketOrCreate(String bucketId) {
        return buckets.computeIfAbsent(bucketId, k -> new Bucket(bucketId));
    }

    public Optional<Bucket> getBucket(String bucketId) {
        return Optional.ofNullable(buckets.get(bucketId));
    }


    void saveBucket(final Bucket bucket, final File bucketDir) throws IOException {
        final Yaml yaml = new Yaml();
        final Map<String, Object> serializedBucket = bucket.serialize();
        final File bucketFile = new File(bucketDir, GitFlowMetaData.BUCKET_FILENAME);

        try (final Writer writer = new OutputStreamWriter(
                new FileOutputStream(bucketFile), StandardCharsets.UTF_8)) {
            yaml.dump(serializedBucket, writer);
        }
    }

    boolean isGitDirectoryClean() throws GitAPIException {
        final Status status = new Git(gitRepo).status().call();
        return status.isClean() && !status.hasUncommittedChanges();
    }

    /**
     * Create a Git commit.
     * @param author The name of a user who created the snapshot, it will be used as the author name.
     *               If not specified, the one in Git config is used.
     * @param message Commit message.
     * @param bucket A bucket to commit.
     * @param flowPointer A flow pointer for the flow snapshot which is updated.
     *                    After a commit is created, new commit rev id and flow snapshot file object id are set to this pointer.
     *                    It can be null if none of flow content is modified.
     */
    void commit(String author, String message, Bucket bucket, Flow.FlowPointer flowPointer) throws GitAPIException, IOException {
        try (final Git git = new Git(gitRepo)) {
            // Execute add command for newly added files (if any).
            git.add().addFilepattern(".").call();

            // Execute add command again for deleted files (if any).
            git.add().addFilepattern(".").setUpdate(true).call();

            final UserConfig userConfig = gitRepo.getConfig().get(UserConfig.KEY);
            final String authorName = isEmpty(author) ? userConfig.getAuthorName() : author;
            final String authorEmail = userConfig.getAuthorEmail();
            final RevCommit commit = git.commit()
                    .setAuthor(authorName, authorEmail)
                    .setMessage(message)
                    .call();

            if (flowPointer != null) {
                final RevTree tree = commit.getTree();
                final String flowSnapshotPath = new File(bucket.getBucketName(), flowPointer.getFileName()).getPath();
                try (final TreeWalk treeWalk = new TreeWalk(gitRepo)) {
                    treeWalk.addTree(tree);

                    while (treeWalk.next()) {
                        if (treeWalk.isSubtree()) {
                            treeWalk.enterSubtree();
                        } else {
                            final String pathString = treeWalk.getPathString();
                            if (pathString.equals(flowSnapshotPath)) {
                                // Capture updated object id.
                                final String flowSnapshotObjectId = treeWalk.getObjectId(0).getName();
                                flowPointer.setObjectId(flowSnapshotObjectId);
                                break;
                            }
                        }
                    }
                }

                flowPointer.setGitRev(commit.getName());
            }

            // Push if necessary.
            if (!isEmpty(remoteToPush)) {
                logger.debug("Pushing to {}...", remoteToPush);
                final PushCommand pushCommand = new Git(gitRepo).push().setRemote(remoteToPush);
                if (credentialsProvider != null) {
                    pushCommand.setCredentialsProvider(credentialsProvider);
                }

                final Iterable<PushResult> pushResults = pushCommand.call();
                for (PushResult pushResult : pushResults) {
                    logger.debug(pushResult.getMessages());
                }
            }

        }
    }

    byte[] getContent(String objectId) throws IOException {
        final ObjectId flowSnapshotObjectId = gitRepo.resolve(objectId);
        return gitRepo.newObjectReader().open(flowSnapshotObjectId).getBytes();
    }

}
