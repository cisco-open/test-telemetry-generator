package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class TestInvalidScenarios {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entity-definition.yaml").toString();
    private final TestPayloadHandler payloadStore = new TestPayloadHandler();

    @Test(expectedExceptions = GeneratorException.class)
    public void testOnlyEntitiesYAML() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML).build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore);
        telemetryGenerator.runGenerator();
    }
}