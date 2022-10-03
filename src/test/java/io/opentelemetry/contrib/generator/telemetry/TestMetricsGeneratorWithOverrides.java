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

public class TestMetricsGeneratorWithOverrides {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-overrides-test.yaml").toString();
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

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML).withMetricDefinitionYAML(METRICS_YAML).build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
        EntityModelExpressions.resetCaches();
    }

    @Test
    public void testPayloadAndPacketCounts() {
        int NW_IFC_PAYLOADS = 10 + 5;
        int CONTAINER_PAYLOADS = 10 + 5 + 15 + 10;
        int MACHINE_PAYLOADS = 10 + 5 + 15 + 10;
        int NODE_PAYLOADS = 15 + 10;
        int POD_PAYLOADS = 15 + 10;
        int DISK_PAYLOADS = 15;
        int AWS_RDS_PAYLOADS = 15 + 10;
        int AWS_EBS_PAYLOADS = 15;
        int payloadCount = NW_IFC_PAYLOADS + CONTAINER_PAYLOADS + MACHINE_PAYLOADS + NODE_PAYLOADS + POD_PAYLOADS +
                DISK_PAYLOADS + AWS_RDS_PAYLOADS + AWS_EBS_PAYLOADS;
        Assert.assertEquals(testStore.getMetricPayloads().size(), payloadCount, "Mismatch in payload count");
        //Check packet count = payload count * number of entities
        int expectedPacketCount = (NW_IFC_PAYLOADS * NETWORK_INTERFACE_COUNT) + (CONTAINER_PAYLOADS * CONTAINER_COUNT) +
                (MACHINE_PAYLOADS * MACHINE_COUNT) + (NODE_PAYLOADS * NODE_COUNT) + (POD_PAYLOADS * POD_COUNT) +
                (DISK_PAYLOADS * DISK_COUNT) + (AWS_RDS_PAYLOADS * AWS_RDS_COUNT) + (AWS_EBS_PAYLOADS * AWS_EBS_COUNT);
        Assert.assertEquals(testStore.getMetricsPacketCount(), expectedPacketCount, "Mismatch in resource metrics packet count");
        //Check metric count for each metric = number of reporting entities * number of payloads
    }

    @Test
    public void testMetricCounts() {
        int SYS_NW_IN_PAYLOADS = 10;
        int SYS_NW_OUT_PAYLOADS = 5;
        int FS_USED_PAYLOADS = 15;
        int MEM_USED_PAYLOADS = 10;
        int systemNetworkIn_Count = (NETWORK_INTERFACE_COUNT + CONTAINER_COUNT +MACHINE_COUNT) * SYS_NW_IN_PAYLOADS;
        int systemNetworkOut_Count = (NETWORK_INTERFACE_COUNT + CONTAINER_COUNT +MACHINE_COUNT) * SYS_NW_OUT_PAYLOADS;
        int filesystemUsed_Count = (CONTAINER_COUNT + MACHINE_COUNT + NODE_COUNT + POD_COUNT + DISK_COUNT + AWS_EBS_COUNT +
                AWS_RDS_COUNT) * FS_USED_PAYLOADS;
        int memoryUsed_Count = (CONTAINER_COUNT + MACHINE_COUNT + NODE_COUNT + POD_COUNT + AWS_RDS_COUNT) * MEM_USED_PAYLOADS;
        Assert.assertEquals(testStore.getMetricsCount().get("system.network.in.kb.sec").get(), systemNetworkIn_Count,
                "Mismatch in metrics count for metric system.network.in.kb.sec");
        Assert.assertEquals(testStore.getMetricsCount().get("system.network.out.kb.sec").get(), systemNetworkOut_Count,
                "Mismatch in metrics count for metric system.network.out.kb.sec");
        Assert.assertEquals(testStore.getMetricsCount().get("filesystem.used").get(), filesystemUsed_Count,
                "Mismatch in metrics count for metric filesystem.used");
        Assert.assertEquals(testStore.getMetricsCount().get("memory.used").get(), memoryUsed_Count,
                "Mismatch in metrics count for metric memory.used");
    }
}