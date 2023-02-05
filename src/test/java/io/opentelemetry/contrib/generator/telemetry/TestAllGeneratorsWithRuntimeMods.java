/*
 * Copyright 2022 AppDynamics Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestAllGeneratorsWithRuntimeMods {

    private final String RESOURCES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "resource-definition-runtime-mods.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-test-runtime-mods.yaml").toString();
    private final String LOGS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "logs-test-runtime-mods.yaml").toString();
    private final String TRACES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "traces-runtime-mods.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private TransportStorage transportStorage;
    //Every minute the number of pods increase by 15 and 2 payloads are sent every minute for 5 mins
    private final int reportingPodCount = (75 * 2) + (90 * 2) + (105 * 2) + (120 * 2) + (135 * 2);
    //Every 2 mins the number of nodes increase by 5 and 2 payloads are sent every minute for 5 mins
    private final int reportingNodeCount = (25 * 4) + (30 * 4) + (35 * 2); //1340
    //Every minute the number of containers decrease by 5 and 2 payloads are sent every minute for 5 mins
    private final int reportingContainerCount = (150 * 2) + (145 * 2) + (140 * 2) + (135 * 2) + (130 * 2); //2740
    //Every minute the number of machines increase by 10 and every 2 mins they decrease by 15 and 2 payloads are sent every minute for 5 mins
    private final int reportingMachineCount = (80 * 2) + (90 * 2) + (85 * 2) + (95 * 2) + (90 * 2);

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(RESOURCES_YAML)
                .withMetricDefinitionYAML(METRICS_YAML)
                .withLogDefinitionYAML(LOGS_YAML)
                .withTraceDefinitionYAML(TRACES_YAML)
                .build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
        transportStorage = telemetryGenerator.getTransportStorage();
    }

    @Test
    public void validateTotalMetricPacketCounts() {
        Assert.assertEquals(testStore.getMetricsCount().get("pod.restarts").get(), reportingPodCount);
        Assert.assertEquals(testStore.getMetricsCount().get("cpu.used").get(), reportingNodeCount);
        Assert.assertEquals(testStore.getMetricsCount().get("filesystem.used").get(), reportingContainerCount);
        Assert.assertEquals(testStore.getMetricsCount().get("system.network.in.kb.sec").get(), reportingMachineCount);
        Assert.assertEquals(testStore.getMetricsCount().get("memory.used").get(),
                reportingPodCount + reportingNodeCount + reportingContainerCount);
    }

    @Test
    public void validateTotalLogPacketCounts() {
        int totalCount = reportingContainerCount + reportingNodeCount + reportingMachineCount + reportingPodCount;
        //Add counts for log events also
        totalCount = totalCount + reportingPodCount + reportingNodeCount + reportingMachineCount;
        Assert.assertEquals(testStore.getLogsPacketCount(), totalCount);
    }

    @Test
    public void validateTotalSpansCounts() {
        Assert.assertEquals(testStore.getSpanCount().get("deleteAccountProcessing").get(), 10);
        Assert.assertEquals(testStore.getSpanCount().get("deactivateAccountCentral").get(), 10);
        Assert.assertEquals(testStore.getSpanCount().get("deleteAccountHTTPRequest").get(), 10);
        Assert.assertEquals(testStore.getSpanCount().get("healthCheck").get(), 10 * 20);
    }

    @Test
    public void validateFirstAddOperation() {
        String expectedPodIP = "133.29.54.90";
        ExportMetricsServiceRequest podMetricPayload = transportStorage.getStoredMetricsPayloads().get("pod::30::10").get(2);
        Assert.assertNotNull(podMetricPayload);
        ExportLogsServiceRequest podLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_1").get("pod").get(2);
        Assert.assertNotNull(podLogPayload);
        Set<String> podIPsFromMetricPayloads = podMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromMetricPayloads.contains(expectedPodIP));
        Set<String> podIPsFromPodPayloads = podLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromPodPayloads.contains(expectedPodIP));

        String expectedNodeName = "quaG.node";
        ExportMetricsServiceRequest nodeMetricPayload = transportStorage.getStoredMetricsPayloads().get("node::30::10").get(4);
        Assert.assertNotNull(nodeMetricPayload);
        ExportLogsServiceRequest nodePodPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get("node").get(4);
        Assert.assertNotNull(nodePodPayload);
        Set<String> nodeNamesFromMetricPayloads = nodeMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.node.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(nodeNamesFromMetricPayloads.contains(expectedNodeName));
        Set<String> nodeNamesFromLogPayloads = nodePodPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.node.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(nodeNamesFromLogPayloads.contains(expectedNodeName));

        String expectedMachineName = "idC.host";
        ExportMetricsServiceRequest machineMetricPayload = transportStorage.getStoredMetricsPayloads().get("machine::30::10").get(2);
        Assert.assertNotNull(machineMetricPayload);
        ExportLogsServiceRequest machineLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_3").get("machine").get(2);
        Assert.assertNotNull(machineLogPayload);
        Set<String> machineNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(machineNamesFromMetricPayloads.contains(expectedMachineName));
        Set<String> machineNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(machineNamesFromLogPayloads.contains(expectedMachineName));
    }

    @Test
    public void validateFirstRemoveOperation() {
        String unexpectedMachineName = "icq.host";
        ExportMetricsServiceRequest machineMetricPayload = transportStorage.getStoredMetricsPayloads().get("machine::30::10").get(4);
        Assert.assertNotNull(machineMetricPayload);
        ExportLogsServiceRequest machineLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_3").get("machine").get(4);
        Assert.assertNotNull(machineLogPayload);
        Set<String> machineNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(machineNamesFromMetricPayloads.contains(unexpectedMachineName));
        Set<String> machineNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(machineNamesFromLogPayloads.contains(unexpectedMachineName));

        String unexpectedContainerName = "tamt.container";
        ExportMetricsServiceRequest containerMetricPayload = transportStorage.getStoredMetricsPayloads().get("container::30::10").get(2);
        Assert.assertNotNull(containerMetricPayload);
        ExportLogsServiceRequest containerLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_2").get("container").get(2);
        Assert.assertNotNull(containerLogPayload);
        Set<String> containerNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("container.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(containerNamesFromMetricPayloads.contains(unexpectedContainerName));
        Set<String> containerNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("container.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(containerNamesFromLogPayloads.contains(unexpectedMachineName));
    }

    @Test
    public void validateFirstChurnOperation() {
        String expectedPodIP = "133.29.54.130";
        String unexpectedPodIP = "133.29.54.25";
        ExportMetricsServiceRequest podMetricPayload = transportStorage.getStoredMetricsPayloads().get("pod::30::10").get(4);
        Assert.assertNotNull(podMetricPayload);
        ExportLogsServiceRequest podLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_1").get("pod").get(4);
        Assert.assertNotNull(podLogPayload);
        Set<String> podIPsFromMetricPayloads = podMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromMetricPayloads.contains(expectedPodIP));
        Assert.assertFalse(podIPsFromMetricPayloads.contains(unexpectedPodIP));
        Set<String> podIPsFromLogPayloads = podLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromLogPayloads.contains(expectedPodIP));
        Assert.assertFalse(podIPsFromLogPayloads.contains(unexpectedPodIP));
    }

    @Test
    public void validateLastAddOperation() {
        String expectedPodIP = "133.29.54.160";
        ExportMetricsServiceRequest podMetricPayload = transportStorage.getStoredMetricsPayloads().get("pod::30::10").get(9);
        Assert.assertNotNull(podMetricPayload);
        ExportLogsServiceRequest podLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_1").get("pod").get(9);
        Assert.assertNotNull(podLogPayload);
        Set<String> podIPsFromMetricPayloads = podMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromMetricPayloads.contains(expectedPodIP));
        Set<String> podIPsFromLogPayloads = podLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromLogPayloads.contains(expectedPodIP));

        String expectedNodeName = "quaL.node";
        ExportMetricsServiceRequest nodeMetricPayload = transportStorage.getStoredMetricsPayloads().get("node::30::10").get(9);
        Assert.assertNotNull(nodeMetricPayload);
        ExportLogsServiceRequest nodeLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get("node").get(9);
        Assert.assertNotNull(nodeLogPayload);
        Set<String> nodeNamesFromMetricPayloads = nodeMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.node.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(nodeNamesFromMetricPayloads.contains(expectedNodeName));
        Set<String> nodeNamesFromLogPayloads = nodeLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.node.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(nodeNamesFromLogPayloads.contains(expectedNodeName));

        String expectedMachineName = "ie6.host";
        ExportMetricsServiceRequest machineMetricPayload = transportStorage.getStoredMetricsPayloads().get("machine::30::10").get(9);
        Assert.assertNotNull(machineMetricPayload);
        ExportLogsServiceRequest machineLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_3").get("machine").get(9);
        Assert.assertNotNull(machineLogPayload);
        Set<String> machineNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(machineNamesFromMetricPayloads.contains(expectedMachineName));
        Set<String> machineNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(machineNamesFromLogPayloads.contains(expectedMachineName));
    }

    @Test
    public void validateLastRemoveOperation() {
        String unexpectedMachineName = "icF.host";
        ExportMetricsServiceRequest machineMetricPayload = transportStorage.getStoredMetricsPayloads().get("machine::30::10").get(9);
        Assert.assertNotNull(machineMetricPayload);
        ExportLogsServiceRequest machineLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_3").get("machine").get(9);
        Assert.assertNotNull(machineLogPayload);
        Set<String> machineNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(machineNamesFromMetricPayloads.contains(unexpectedMachineName));
        Set<String> machineNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("host.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(machineNamesFromLogPayloads.contains(unexpectedMachineName));

        String unexpectedContainerName = "tamA.container";
        ExportMetricsServiceRequest containerMetricPayload = transportStorage.getStoredMetricsPayloads().get("container::30::10").get(9);
        Assert.assertNotNull(containerMetricPayload);
        ExportLogsServiceRequest containerLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_2").get("container").get(9);
        Assert.assertNotNull(containerLogPayload);
        Set<String> containerNamesFromMetricPayloads = machineMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("container.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(containerNamesFromMetricPayloads.contains(unexpectedContainerName));
        Set<String> containerNamesFromLogPayloads = machineLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("container.name"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertFalse(containerNamesFromLogPayloads.contains(unexpectedMachineName));
    }

    @Test
    public void validateLastChurnOperation() {
        String expectedPodIP = "133.29.54.185";
        String unexpectedPodIP = "133.29.54.50";
        ExportMetricsServiceRequest podMetricPayload = transportStorage.getStoredMetricsPayloads().get("pod::30::10").get(9);
        Assert.assertNotNull(podMetricPayload);
        ExportLogsServiceRequest podLogPayload = transportStorage.getStoredLogsPayloads().get("log_by_ttg_1").get("pod").get(9);
        Assert.assertNotNull(podLogPayload);
        Set<String> podIPsFromMetricPayloads = podMetricPayload.getResourceMetricsList().stream()
                .map(rm -> rm.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromMetricPayloads.contains(expectedPodIP));
        Assert.assertFalse(podIPsFromMetricPayloads.contains(unexpectedPodIP));
        Set<String> podIPsFromLogPayloads = podLogPayload.getResourceLogsList().stream()
                .map(rl -> rl.getResource().getAttributesList())
                .flatMap(List::stream)
                .filter(kv -> kv.getKey().equals("k8s.pod.ip"))
                .map(kv -> kv.getValue().getStringValue())
                .collect(Collectors.toSet());
        Assert.assertTrue(podIPsFromLogPayloads.contains(expectedPodIP));
        Assert.assertFalse(podIPsFromLogPayloads.contains(unexpectedPodIP));
    }
}