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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestAllGenerators {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-test.yaml").toString();
    private final String LOGS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "logs-test-combined.yaml").toString();
    private final String TRACES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "trace-definition.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private TransportStorage transportStorage;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML)
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
    public void validatePacketCounts() {
        int NETWORK_INTERFACE_COUNT = 120;
        int CONTAINER_COUNT = 150;
        int MACHINE_COUNT = 80;
        int NODE_COUNT = 25;
        int POD_COUNT = 75;
        int DISK_COUNT = 100;
        int AWS_RDS_COUNT = 50;
        int AWS_EBS_COUNT = 50;
        int METRIC_REPORTING_ENTITIES_COUNT = NETWORK_INTERFACE_COUNT + CONTAINER_COUNT + MACHINE_COUNT + NODE_COUNT +
                POD_COUNT + DISK_COUNT + AWS_EBS_COUNT + AWS_RDS_COUNT;
        int LOG_REPORTING_ENTITIES_COUNT = CONTAINER_COUNT + NODE_COUNT + 2 * POD_COUNT + MACHINE_COUNT;
        int metricPayloadCount = 10;
        int logsPayloadCount = 20;
        int expectedMetricPackets = METRIC_REPORTING_ENTITIES_COUNT * metricPayloadCount;
        //Add packets for aggregating, non-reporting entities
            expectedMetricPackets = expectedMetricPackets + 600;
        int expectedLogsPackets = LOG_REPORTING_ENTITIES_COUNT * logsPayloadCount;
        int expectedSpanPackets = 11518;
        Assert.assertEquals(testStore.getMetricsPacketCount(), expectedMetricPackets, "Mismatch in expected metric packets count");
        Assert.assertEquals(testStore.getLogsPacketCount(), expectedLogsPackets, "Mismatch in expected log packets count");
        Assert.assertEquals(testStore.getTracePacketCount(), expectedSpanPackets, "Mismatch in expected span packets count");
    }

    @Test
    public void validateStorageCounts() {
        Assert.assertEquals(transportStorage.getStoredMetricsPayloads().size(), 8,
                "Mismatch in entity type counts for metric payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().size(), 3,
                "Mismatch in entity type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().get("k8slogs").size(), 3,
                "Mismatch in entity type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().get("k8slogs").get("pod").size(), 20,
                "Mismatch in entity type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredTracesPayloads().size(), 8,
                "Mismatch in entity type counts for trace payloads");
        Assert.assertEquals(transportStorage.getMetricsResponses().size(), 8,
                "Mismatch in entity type counts for metric response statuses");
        Assert.assertEquals(transportStorage.getLogsResponses().size(), 3,
                "Mismatch in entity type counts for log response statuses");
        Assert.assertEquals(transportStorage.getTracesResponses().size(), 8,
                "Mismatch in entity type counts for trace response statuses");
        for (Map.Entry<String, List<ExportMetricsServiceRequest>> metricCounts: transportStorage.getStoredMetricsPayloads().entrySet()) {
            Assert.assertEquals(metricCounts.getValue().size(), 10, "Expected 10 metric payloads for entity type: " +
                    metricCounts.getKey());
            Assert.assertEquals(transportStorage.getMetricsResponses().get(metricCounts.getKey()).size(), 10,
                    "Expected 10 metric response statuses for entity type: " + metricCounts.getKey());
        }

        for (Map.Entry<String, Map<String, List<ExportLogsServiceRequest>>> logCounts: transportStorage.getStoredLogsPayloads().entrySet()) {
            Set<String> entityNames = logCounts.getValue().keySet();
            for(String name : entityNames){
                Assert.assertEquals(logCounts.getValue().get(name).size(), 20, "Expected 20 log payloads for entity type: " +
                        logCounts.getKey());
                Assert.assertEquals(transportStorage.getLogsResponses().get(logCounts.getKey()).get(name).size(), 20,
                        "Expected 20 log response statuses for " + "log:entity key: " + logCounts.getKey());
            }
        }

        Map<String, Integer> expectedTracePayloads = new HashMap<>();
        expectedTracePayloads.put("healthCheck::group::0", 10);
        expectedTracePayloads.put("searchAccountsRequest::group::0", 5);
        expectedTracePayloads.put("getAccountDetails::group::0", 10);
        expectedTracePayloads.put("getAccountDetails::group::1", 10);
        expectedTracePayloads.put("updateAccountDetails::group::0", 7);
        expectedTracePayloads.put("deleteAccount::group::0", 5);
        expectedTracePayloads.put("createNewAccount::group::0", 20);
        expectedTracePayloads.put("createNewAccount::group::1", 20);
        for (Map.Entry<String, Integer> eachTraceGroup: expectedTracePayloads.entrySet()) {
            Assert.assertEquals(transportStorage.getStoredTracesPayloads().get(eachTraceGroup.getKey()).size(), eachTraceGroup.getValue(),
                    "Mismatch in expected payloads count for trace group " + eachTraceGroup.getKey());
        }
    }
}