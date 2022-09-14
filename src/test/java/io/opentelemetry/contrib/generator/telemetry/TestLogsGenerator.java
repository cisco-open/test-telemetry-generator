package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.jel.methods.EntityModelExpressions;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class TestLogsGenerator {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition.yaml").toString();
    private final String LOGS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "logs-test.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private final int CONTAINER_COUNT_K8S = 10;
    private final int CONTAINER_COUNT_LOG = 150;
    private final int MACHINE_COUNT = 4;
    private final int NODE_COUNT = 25;
    private final int POD_COUNT = 10;
    private final int POST_COUNT_K8S_LOG = 10;
    private final int POST_COUNT_LOG_LOG_1 = 2;
    private final int POST_COUNT_LOG_LOG_2 = 5;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML).withLogDefinitionYAML(LOGS_YAML).build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
        EntityModelExpressions.resetCaches();
    }

    @Test
    public void testPayloadAndPacketCounts() {
        //Check payload count = Summation of all post counts per log definition
        int expectedPayloadCount = 2 * POST_COUNT_K8S_LOG + POST_COUNT_LOG_LOG_1 + 2 * POST_COUNT_LOG_LOG_2;
        Assert.assertEquals(testStore.getLogsPayloads().size(), expectedPayloadCount, "Mismatch in payload count");
        //Check packet count = Summation (payload count * number of entities) for every log
        int expectedPacketCount = (CONTAINER_COUNT_K8S + POD_COUNT) * POST_COUNT_K8S_LOG + NODE_COUNT * POST_COUNT_LOG_LOG_1
                + (MACHINE_COUNT + CONTAINER_COUNT_LOG) * POST_COUNT_LOG_LOG_2;
        Assert.assertEquals(testStore.getLogsPacketCount(), expectedPacketCount, "Mismatch in resource logs packet count");
        //Check log count for each log = number of reporting entities * number of payloads defined per log definition
    }

    @Test
    public void testLogsCounts() {
        //Check total log count for each log = number of reporting entities * number of payloads defined per log definition
        int COPY_COUNT_K8S_LOG = 1000;
        int k8sLogs_Count = (POD_COUNT + CONTAINER_COUNT_K8S) * POST_COUNT_K8S_LOG * COPY_COUNT_K8S_LOG;
        Assert.assertEquals(testStore.getLogsCount().get("k8slogs").get(), k8sLogs_Count,
                "Mismatch in logs count for log k8slogs");
        int COPY_COUNT_LOG_LOG_1 = 200;
        int logEvents1_Count = NODE_COUNT * POST_COUNT_LOG_LOG_1 * COPY_COUNT_LOG_LOG_1;
        Assert.assertEquals(testStore.getLogsCount().get("logevents1").get(), logEvents1_Count,
                "Mismatch in logs count for log logevents1");
        int logEvents2_Count = (MACHINE_COUNT + CONTAINER_COUNT_LOG) * POST_COUNT_LOG_LOG_2;
        Assert.assertEquals(testStore.getLogsCount().get("logevents2").get(), logEvents2_Count,
                "Mismatch in logs count for log logevents2");
    }
}