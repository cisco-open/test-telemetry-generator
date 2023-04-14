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

public class TestAllGeneratorsWithJSONInput {
    private final String RESOURCES_JSON = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "resource-definition.json").toString();
    private final String METRICS_JSON = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-test.json").toString();
    private final String LOGS_JSON = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "logs-test-combined.json").toString();
    private final String TRACES_JSON = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "trace-definition.json").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private TransportStorage transportStorage;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.JSONFilesBuilder(RESOURCES_JSON)
                .withMetricDefinitionJSON(METRICS_JSON)
                .withLogDefinitionJSON(LOGS_JSON)
                .withTraceDefinitionJSON(TRACES_JSON)
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
        int METRIC_REPORTING_RESOURCES_COUNT = NETWORK_INTERFACE_COUNT + CONTAINER_COUNT + MACHINE_COUNT + NODE_COUNT +
                POD_COUNT + DISK_COUNT + AWS_EBS_COUNT + AWS_RDS_COUNT;
        int LOG_REPORTING_RESOURCES_COUNT = CONTAINER_COUNT + NODE_COUNT + 2 * POD_COUNT + MACHINE_COUNT;
        int metricPayloadCount = 10;
        int logsPayloadCount = 20;
        int POD_EVENTS_COUNT = 30 * 5;
        int expectedMetricPackets = METRIC_REPORTING_RESOURCES_COUNT * metricPayloadCount;
        int expectedLogsPackets = LOG_REPORTING_RESOURCES_COUNT * logsPayloadCount + POD_EVENTS_COUNT;
        int expectedSpanPackets = 11518;
        Assert.assertEquals(testStore.getMetricsPacketCount(), expectedMetricPackets, "Mismatch in expected metric packets count");
        Assert.assertEquals(testStore.getLogsPacketCount(), expectedLogsPackets, "Mismatch in expected log packets count");
        Assert.assertEquals(testStore.getTracePacketCount(), expectedSpanPackets, "Mismatch in expected span packets count");
    }

    @Test
    public void validateStorageCounts() {
        Assert.assertEquals(transportStorage.getStoredMetricsPayloads().size(), 8,
                "Mismatch in resource type counts for metric payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().size(), 4,
                "Mismatch in resource type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").size(), 3,
                "Mismatch in resource type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get("pod").size(), 20,
                "Mismatch in resource type counts for log payloads");
        Assert.assertEquals(transportStorage.getStoredTracesPayloads().size(), 8,
                "Mismatch in resource type counts for trace payloads");
        Assert.assertEquals(transportStorage.getMetricsResponses().size(), 8,
                "Mismatch in resource type counts for metric response statuses");
        Assert.assertEquals(transportStorage.getLogsResponses().size(), 4,
                "Mismatch in resource type counts for log response statuses");
        Assert.assertEquals(transportStorage.getTracesResponses().size(), 8,
                "Mismatch in resource type counts for trace response statuses");
        for (Map.Entry<String, List<ExportMetricsServiceRequest>> metricCounts: transportStorage.getStoredMetricsPayloads().entrySet()) {
            Assert.assertEquals(metricCounts.getValue().size(), 10, "Expected 10 metric payloads for resource type: " +
                    metricCounts.getKey());
            Assert.assertEquals(transportStorage.getMetricsResponses().get(metricCounts.getKey()).size(), 10,
                    "Expected 10 metric response statuses for resource type: " + metricCounts.getKey());
        }

        for (Map.Entry<String, Map<String, List<ExportLogsServiceRequest>>> logCounts: transportStorage.getStoredLogsPayloads().entrySet()) {
            int expected = logCounts.getKey().equals("log_by_ttg_3") ? 5 : 20;
            Set<String> resourceNames = logCounts.getValue().keySet();
            for(String name : resourceNames){
                Assert.assertEquals(logCounts.getValue().get(name).size(), expected);
                Assert.assertEquals(transportStorage.getLogsResponses().get(logCounts.getKey()).get(name).size(), expected);
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
