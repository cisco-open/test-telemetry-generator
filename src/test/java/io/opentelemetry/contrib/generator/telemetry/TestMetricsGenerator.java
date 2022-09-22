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

public class TestMetricsGenerator {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-test.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private final int NETWORK_INTERFACE_COUNT = 120;
    private final int CONTAINER_COUNT = 150;
    private final int MACHINE_COUNT = 80;
    private final int NODE_COUNT = 25;
    private final int POD_COUNT = 75;
    private final int DISK_COUNT = 100;
    private final int AWS_RDS_COUNT = 50;
    private final int AWS_EBS_COUNT = 50;
    private final int PAYLOAD_COUNT = 10;
    private final int REPORTING_ENTITIES_COUNT = NETWORK_INTERFACE_COUNT + CONTAINER_COUNT + MACHINE_COUNT + NODE_COUNT +
            POD_COUNT + DISK_COUNT + AWS_EBS_COUNT + AWS_RDS_COUNT;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML).withMetricDefinitionYAML(METRICS_YAML).build();
        TelemetryGenerator generator = new TelemetryGenerator(generatorInput, payloadStore);
        generator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
        EntityModelExpressions.resetCaches();
    }

    @Test
    public void testPayloadAndPacketCounts() {
        //Check payload count = payload count * each of 8 types of entities
        int payloadCount = 8 * PAYLOAD_COUNT;
        //Add payloads for aggregating, non-reporting entities
        payloadCount = payloadCount + 19;
        Assert.assertEquals(testStore.getMetricPayloads().size(), payloadCount, "Mismatch in payload count");
        //Check packet count = payload count * number of entities
        int expectedPacketCount = REPORTING_ENTITIES_COUNT * PAYLOAD_COUNT;
        expectedPacketCount = expectedPacketCount + 600;
        Assert.assertEquals(testStore.getMetricsPacketCount(), expectedPacketCount, "Mismatch in resource metrics packet count");
        //Check metric count for each metric = number of reporting entities * number of payloads
    }

    @Test
    public void testMetricCounts() {
        int systemNetworkInKbSec_Count = (NETWORK_INTERFACE_COUNT + CONTAINER_COUNT + MACHINE_COUNT) * PAYLOAD_COUNT;
        int systemNetworkOutKbSec_Count = (NETWORK_INTERFACE_COUNT + CONTAINER_COUNT + MACHINE_COUNT) * PAYLOAD_COUNT;
        int podRestarts_Count = POD_COUNT * PAYLOAD_COUNT;
        int cpuUsed_Count = (NODE_COUNT + CONTAINER_COUNT + POD_COUNT + MACHINE_COUNT) * PAYLOAD_COUNT;
        int filesystemUsed_Count = (REPORTING_ENTITIES_COUNT - NETWORK_INTERFACE_COUNT) * PAYLOAD_COUNT;
        int memoryUsed_Count = (AWS_RDS_COUNT + NODE_COUNT + CONTAINER_COUNT + POD_COUNT + MACHINE_COUNT) * PAYLOAD_COUNT;
        Assert.assertEquals(testStore.getMetricsCount().get("system.network.in.kb.sec").get(), systemNetworkInKbSec_Count,
                "Mismatch in metrics count for metric system.network.in.kb.sec");
        Assert.assertEquals(testStore.getMetricsCount().get("system.network.out.kb.sec").get(), systemNetworkOutKbSec_Count,
                "Mismatch in metrics count for metric system.network.out.kb.sec");
        Assert.assertEquals(testStore.getMetricsCount().get("pod.restarts").get(), podRestarts_Count,
                "Mismatch in metrics count for metric pod.restarts");
        Assert.assertEquals(testStore.getMetricsCount().get("cpu.used").get(), cpuUsed_Count,
                "Mismatch in metrics count for metric cpu.used");
        Assert.assertEquals(testStore.getMetricsCount().get("filesystem.used").get(), filesystemUsed_Count,
                "Mismatch in metrics count for metric filesystem.used");
        Assert.assertEquals(testStore.getMetricsCount().get("memory.used").get(), memoryUsed_Count,
                "Mismatch in metrics count for metric memory.used");
    }
}