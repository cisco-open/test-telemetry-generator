package io.opentelemetry.contrib.generator.core;

import io.opentelemetry.contrib.generator.telemetry.TelemetryGenerator;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class TestGeneratorsUsingEntityWithNoAttrs {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition-noattrs.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-noattrsentity.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML)
                .withMetricDefinitionYAML(METRICS_YAML)
                .build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
    }

    @Test
    public void validatePacketCounts() {
        Assert.assertEquals(testStore.getMetricsPacketCount(), 1800);
    }
}
