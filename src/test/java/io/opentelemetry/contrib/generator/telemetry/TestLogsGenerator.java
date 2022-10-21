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
        int k8sEvents_Count = (POD_COUNT + CONTAINER_COUNT_K8S) * POST_COUNT_K8S_LOG;
        int logEvents1_Count = NODE_COUNT * POST_COUNT_LOG_LOG_1;
        int logEvents2_Count = (MACHINE_COUNT + CONTAINER_COUNT_LOG) * POST_COUNT_LOG_LOG_2;
        Assert.assertEquals(testStore.getLogsPacketCount(), k8sEvents_Count + logEvents1_Count + logEvents2_Count,
                "Mismatch in events count");
    }
}