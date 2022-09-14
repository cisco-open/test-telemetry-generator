package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class ValidateExampleDefinitionsTest {

    private final String DEFINITION_PATH = Paths.get(System.getProperty("user.dir"), "example-definitions").toString();

    @Test
    public void validateEntityAndMetricDefinitions() {
        String entityDefinitions = Paths.get(DEFINITION_PATH, "entity-definition.yaml").toString();
        String metricDefinitions = Paths.get(DEFINITION_PATH, "metric-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(entityDefinitions)
                .withMetricDefinitionYAML(metricDefinitions).build();
        generatorInput.validate("ValidateExampleDefinitionsTest_EntityMetricDefinitions");
    }

    @Test
    public void validateLogDefinitions() {
        String entityDefinitions = Paths.get(DEFINITION_PATH, "entity-definition.yaml").toString();
        String logDefinitions = Paths.get(DEFINITION_PATH, "log-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(entityDefinitions)
                .withLogDefinitionYAML(logDefinitions).build();
        generatorInput.validate("ValidateExampleDefinitionsTest_LogDefinitions");
    }

    @Test
    public void validateTraceDefinitions() {
        String entityDefinitions = Paths.get(DEFINITION_PATH, "entity-definition.yaml").toString();
        String traceDefinitions = Paths.get(DEFINITION_PATH, "trace-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(entityDefinitions)
                .withTraceDefinitionYAML(traceDefinitions).build();
        generatorInput.validate("ValidateExampleDefinitionsTest_TraceDefinitions");
    }
}
