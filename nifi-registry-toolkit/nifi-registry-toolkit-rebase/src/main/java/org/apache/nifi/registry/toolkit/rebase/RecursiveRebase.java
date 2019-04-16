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
package org.apache.nifi.registry.toolkit.rebase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.client.impl.JerseyNiFiRegistryClient;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.security.util.KeystoreType;
import org.apache.nifi.registry.serialization.VersionedProcessGroupSerializer;
import org.apache.nifi.registry.service.alias.RegistryUrlAliasService;
import org.apache.nifi.registry.toolkit.rebase.merge.MergeConflict;
import org.apache.nifi.registry.toolkit.rebase.merge.VersionedComponentDeserializer;
import org.apache.nifi.registry.url.aliaser.generated.Alias;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.nifi.registry.toolkit.rebase.RebaseRegistryFacade.INTERNAL_REPO_TOKEN;

public class RecursiveRebase {
    public static final ObjectMapper OBJECT_MAPPER = initObjectMapper();

    private static ObjectMapper initObjectMapper() {
        ObjectMapper result = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)).registerModule(new Jdk8Module());
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(VersionedComponent.class, new VersionedComponentDeserializer());
        result.registerModule(simpleModule);
        return result;
    }

    public interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    private static void printTopLevelHelpAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println(errorMessage);
            System.err.println();
        }
        System.err.println("Usage: ./rebase-toolkit.sh OPERATION OPERATION_OPTIONS...");
        System.err.println("  Supported operations:");
        System.err.println("    diff:  Generate diff of branch against upstream");
        System.err.println("    apply: Apply diff against upstream");
        System.exit(1);
    }

    private static void printHelpAndExit(String errorMessage, Options options) {
        if (errorMessage != null) {
            System.err.println(errorMessage);
            System.err.println();
        }

        HelpFormatter formatter = new HelpFormatter();
        try (PrintWriter writer = new PrintWriter(System.err)) {
            formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, "./rebase-toolkit.sh", null, options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null, true);
        }

        System.exit(1);
    }

    private static RebaseRegistryFacade createClient(String prefix, CommandLine commandLine) throws IOException {
        NiFiRegistryClientConfig.Builder configBuilder = new NiFiRegistryClientConfig.Builder();

        AtomicReference<String> externalUrl = new AtomicReference<>();

        if (commandLine.hasOption(prefix + "PropertiesFile")) {
            Properties properties = new Properties();
            try (FileReader reader = new FileReader(new File(commandLine.getOptionValue(prefix + "PropertiesFile")))) {
                properties.load(reader);
            }
            Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.WEB_HTTP_HOST)).ifPresent(configBuilder::baseUrl);
            Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.WEB_HTTPS_HOST)).ifPresent(httpsHost -> {
                configBuilder.baseUrl(httpsHost);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_KEYSTORE)).ifPresent(configBuilder::keystoreFilename);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_PASSWD)).ifPresent(configBuilder::keystorePassword);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_KEY_PASSWD)).ifPresent(configBuilder::keyPassword);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_KEYSTORE_TYPE)).map(KeystoreType::valueOf).ifPresent(configBuilder::keystoreType);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE)).ifPresent(configBuilder::truststoreFilename);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_PASSWD)).ifPresent(configBuilder::truststorePassword);
                Optional.ofNullable(properties.getProperty(NiFiRegistryProperties.SECURITY_TRUSTSTORE_TYPE)).map(KeystoreType::valueOf).ifPresent(configBuilder::truststoreType);
            });

            Optional.ofNullable(properties.getProperty("nifi.registry.client.read.timeout")).map(Integer::parseInt).ifPresent(configBuilder::readTimeout);
            Optional.ofNullable(properties.getProperty("nifi.registry.client.connect.timeout")).map(Integer::parseInt).ifPresent(configBuilder::connectTimeout);

            Optional.ofNullable(properties.getProperty("nifi.registry.external.url")).ifPresent(externalUrl::set);
        }

        Optional.ofNullable(commandLine.getOptionValue(prefix)).ifPresent(configBuilder::baseUrl);

        NiFiRegistryClientConfig clientConfig = configBuilder.build();

        Alias alias = new Alias();
        alias.setInternal(INTERNAL_REPO_TOKEN);
        alias.setExternal(Optional.ofNullable(commandLine.getOptionValue(prefix + "ExternalUrl"))
                .orElseGet(() -> Optional.ofNullable(externalUrl.get()).orElseGet(clientConfig::getBaseUrl)));

        return new RebaseRegistryFacade(new JerseyNiFiRegistryClient.Builder().config(clientConfig).build(), new RegistryUrlAliasService(Collections.singletonList(alias)));
    }

    protected static void diff(RebaseRegistryFacade branch, RebaseRegistryFacade upstream, String bucketId, String flowId, ThrowingSupplier<OutputStream, IOException> outputSupplier)
            throws IOException, NiFiRegistryException {
        RebaseCalculationContext rebaseCalculationContext = new RebaseCalculationContext(branch, upstream);
        List<VersionedFlowSnapshotReconciliation> reconciliations = new ArrayList<>(Collections.singletonList(new VersionedFlowSnapshotReconciliation(rebaseCalculationContext, bucketId, flowId)));
        reconciliations.addAll(rebaseCalculationContext.getReconciledSnapshots());
        try (OutputStream outputStream = outputSupplier.get()) {
            OBJECT_MAPPER.writeValue(outputStream, reconciliations);
        }
    }

    private static String sanitizeName(VersionedFlowSnapshot snapshot, Set<String> existingNames) {
        String orig = snapshot.getFlow().getName().replaceAll("\\W+", "");
        String result = orig;
        int count = 1;
        while (!existingNames.add(result)) {
            result = orig + (count++);
        }
        return result;
    }

    private static void writeDryRun(Optional<Path> outputDirectory, Set<String> sanitizedNames, VersionedProcessGroupSerializer serializer, VersionedFlowSnapshot snapshot) throws IOException {
        if (outputDirectory.isPresent()) {
            Path beforeDir = outputDirectory.get();
            Files.createDirectories(beforeDir);
            try (OutputStream os = Files.newOutputStream(beforeDir.resolve(sanitizeName(snapshot, sanitizedNames)))) {
                serializer.serialize(snapshot.getFlowContents(), os);
            }
        }
    }

    protected static void apply(RebaseRegistryFacade upstream, ThrowingSupplier<InputStream, IOException> inputSupplier, Optional<Path> dryRunDirectory,
                                boolean ignoreVersionMismatch) throws IOException, NiFiRegistryException {
        List<VersionedFlowSnapshotReconciliation> versionedFlowSnapshotReconciliation = parseDiff(inputSupplier);

        Map<Pair<String, String>, VersionedFlowSnapshot> snapshots = new HashMap<>();
        Map<Pair<String, String>, Integer> reconciledVersions = new HashMap<>();

        VersionedProcessGroupSerializer serializer = new VersionedProcessGroupSerializer();

        Set<String> sanitizedNames = new HashSet<>();
        for (VersionedFlowSnapshotReconciliation reconciliation : versionedFlowSnapshotReconciliation) {
            VersionedFlowSnapshot latestSnapshot = upstream.getLatestSnapshot(reconciliation.getBucketId(), reconciliation.getFlowId());

            int latestVersion = latestSnapshot.getSnapshotMetadata().getVersion();
            if (latestVersion != reconciliation.getUpstreamVersion()) {
                String message = "Expected flow " + latestSnapshot.getFlow().getName() + "(bucketId: " + latestSnapshot.getSnapshotMetadata().getBucketIdentifier()+ ", flowId: "
                        + latestSnapshot.getSnapshotMetadata().getBucketIdentifier() + ") to be version " + reconciliation.getUpstreamVersion() + " in upstream but was " + latestVersion;
                if (ignoreVersionMismatch) {
                    System.err.println("WARNING: " + message);
                } else {
                    throw new IllegalStateException(message);
                }
            }

            writeDryRun(dryRunDirectory.map(d -> d.resolve("before")),sanitizedNames,  serializer, latestSnapshot);
            Pair<String, String> bucketAndFlow = Pair.of(reconciliation.getBucketId(), reconciliation.getFlowId());

            VersionedFlowSnapshotMetadata metadata = latestSnapshot.getSnapshotMetadata();

            metadata.setAuthor("rebase-application");
            metadata.setTimestamp(System.currentTimeMillis());
            metadata.setVersion(latestVersion + 1);
            metadata.setComments(String.join("\n", reconciliation.getBranchComments()));

            reconciledVersions.put(bucketAndFlow, metadata.getVersion());
            snapshots.put(bucketAndFlow, latestSnapshot);
        }

        sanitizedNames.clear();
        Map<Pair<String, String>, Map<ComponentType, Map<String, List<MergeConflict>>>> allConflicts = new HashMap<>();
        RebaseApplicationContext rebaseApplicationContext = new RebaseApplicationContext(reconciledVersions);

        for (VersionedFlowSnapshotReconciliation reconciliation : versionedFlowSnapshotReconciliation) {
            VersionedFlowSnapshot versionedFlowSnapshot = snapshots.get(Pair.of(reconciliation.getBucketId(), reconciliation.getFlowId()));
            Map<ComponentType, Map<String, List<MergeConflict>>> conflicts = reconciliation.apply(rebaseApplicationContext, versionedFlowSnapshot);
            if (conflicts.size() > 0) {
                allConflicts.put(Pair.of(reconciliation.getBucketId(), reconciliation.getFlowId()), conflicts);
            }
            writeDryRun(dryRunDirectory.map(d -> d.resolve("after")),sanitizedNames,  serializer, versionedFlowSnapshot);
        }

        if (allConflicts.size() > 0) {
            throw new IllegalStateException("Unresolved conflicts:\n" + OBJECT_MAPPER.writeValueAsString(allConflicts));
        }

        // Skip actual push in dry run
        if (dryRunDirectory.isPresent()) {
            return;
        }

        for (VersionedFlowSnapshot updatedSnapshot : snapshots.values()) {
            upstream.updateSnapshot(updatedSnapshot);
        }
    }

    protected static List<VersionedFlowSnapshotReconciliation> parseDiff(ThrowingSupplier<InputStream, IOException> inputSupplier) throws IOException {
        try (InputStream is = inputSupplier.get()) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<List<VersionedFlowSnapshotReconciliation>>() {});
        }
    }

    private static CommandLine parse(Options options, String[] args) throws ParseException {
        // Test for help flag with no required args
        List<Option> optionClones = options.getOptions().stream().map(Option::clone).map(Option.class::cast).collect(Collectors.toList());
        optionClones.forEach(o -> o.setRequired(false));
        Options optionsClone = new Options();
        optionClones.forEach(optionsClone::addOption);

        if (new DefaultParser().parse(optionsClone, args).hasOption("help")) {
            printHelpAndExit(null, options);
        }

        return new DefaultParser().parse(options, args);
    }

    public static void main(String[] args) throws IOException, NiFiRegistryException {
        if (args.length == 0) {
            printTopLevelHelpAndExit("Expected operation as first argument.");
        }

        String[] argsMinusOperation = new String[args.length - 1];
        System.arraycopy(args, 1, argsMinusOperation, 0, args.length - 1);

        Options options = new Options();
        options.addOption("h", "help", false, "Print help and exit.");
        try {
            options.addOption("u", "upstream", true, "Upstream to rebase against/push changes to.");
            options.addOption(null, "upstreamPropertiesFile", true, "Upstream to rebase against/push changes to client config file.");
            options.addOption(null, "upstreamExternalUrl", true, "External url for upstream (default to upstream value).");

            String operation = args[0];
            if (operation.equalsIgnoreCase("diff")) {
                options.addOption("b", "branch", true, "Branch with changes to apply upstream.");
                options.addOption(null, "branchPropertiesFile", true, "Branch with changes to client config file.");
                options.addOption(null, "branchExternalUrl", true, "External url for branch (default to branch value).");
                options.addRequiredOption(null, "bucketId", true, "Bucket id of flow.");
                options.addRequiredOption(null, "flowId", true, "Flow id of flow.");
                options.addOption("o", "output", true, "Output diff to file (default to stdout)");

                CommandLine commandLine = parse(options, argsMinusOperation);

                ThrowingSupplier<OutputStream, IOException> outputSupplier;
                if (commandLine.hasOption("output")) {
                    outputSupplier = () -> new FileOutputStream(commandLine.getOptionValue("output"));
                } else {
                    // Prevent other output from polluting our yaml output
                    PrintStream origOut = System.out;
                    System.setOut(System.err);
                    outputSupplier = () -> origOut;
                }
                diff(createClient("branch", commandLine), createClient("upstream", commandLine), commandLine.getOptionValue("bucketId"), commandLine.getOptionValue("flowId"), outputSupplier);
            } else if (operation.equalsIgnoreCase("apply")) {
                options.addOption("d", "dryRun", true, "Perform dry run, writing before and after files to this directory.");
                options.addOption("i", "input", true, "Input file containing diffs with resolved conflicts (default to stdin).");
                options.addOption(null, "ignoreVersionMismatch", false, "DANGER: Continue even if upstream has possibly conflicting changes since diff was generated.");

                CommandLine commandLine = parse(options, argsMinusOperation);

                ThrowingSupplier<InputStream, IOException> inputSupplier;
                if (commandLine.hasOption("input")) {
                    inputSupplier = () -> new FileInputStream(commandLine.getOptionValue("input"));
                } else {
                    inputSupplier = () -> System.in;
                }

                Optional<Path> dryRunDirectory;
                if (commandLine.hasOption("dryRun")) {
                    dryRunDirectory = Optional.of(Paths.get(commandLine.getOptionValue("dryRun")));
                } else {
                    dryRunDirectory = Optional.empty();
                }

                apply(createClient("upstream", commandLine), inputSupplier, dryRunDirectory, commandLine.hasOption("ignoreVersionMismatch"));
            } else {
                printTopLevelHelpAndExit("Unrecognized operation " + operation);
            }
        } catch (ParseException e) {
            printHelpAndExit(e.getMessage(), options);
        }
    }
}
