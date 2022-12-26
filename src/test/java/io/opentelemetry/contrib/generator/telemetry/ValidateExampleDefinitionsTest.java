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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.opentelemetry.contrib.generator.telemetry.cli.dto.TargetEnvironmentDetails;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ValidateExampleDefinitionsTest {
    
    private final String DEFINITION_BASE_PATH = Paths.get(System.getProperty("user.dir"), "example-definitions").toString();
    private final String DEFINITION_PATH = Paths.get(DEFINITION_BASE_PATH, "qa").toString();

    @Test
    public void validateQAResourceAndMetricDefinitions() {
        String resourceDefinitions = Paths.get(DEFINITION_PATH, "resource-definition.yaml").toString();
        String metricDefinitions = Paths.get(DEFINITION_PATH, "metric-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(resourceDefinitions)
                .withMetricDefinitionYAML(metricDefinitions).build();
        generatorInput.validate("ValidateQAExampleDefinitionsTest_ResourceMetricDefinitions");
    }

    @Test
    public void validateQALogDefinitions() {
        String resourceDefinitions = Paths.get(DEFINITION_PATH, "resource-definition.yaml").toString();
        String logDefinitions = Paths.get(DEFINITION_PATH, "log-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(resourceDefinitions)
                .withLogDefinitionYAML(logDefinitions).build();
        generatorInput.validate("ValidateQAExampleDefinitionsTest_LogDefinitions");
    }

    @Test
    public void validateQATraceDefinitions() {
        String resourceDefinitions = Paths.get(DEFINITION_PATH, "resource-definition.yaml").toString();
        String traceDefinitions = Paths.get(DEFINITION_PATH, "trace-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(resourceDefinitions)
                .withTraceDefinitionYAML(traceDefinitions).build();
        generatorInput.validate("ValidateQAExampleDefinitionsTest_TraceDefinitions");
    }

    @Test
    public void validateDemoResourceAndTraceDefinitions() {
        String resourceDefinitions = Paths.get(DEFINITION_BASE_PATH, "demo", "resource-definition.yaml").toString();
        String tracesDefinitions = Paths.get(DEFINITION_BASE_PATH, "demo", "trace-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(resourceDefinitions)
                .withTraceDefinitionYAML(tracesDefinitions).build();
        generatorInput.validate("ValidateDemoExampleDefinitionsTest_ResourceTraceDefinitions");
    }

    @Test
    public void validateSimpleResourceAndTraceDefinitions() {
        String resourceDefinitions = Paths.get(DEFINITION_BASE_PATH, "simple", "resource-definition.yaml").toString();
        String tracesDefinitions = Paths.get(DEFINITION_BASE_PATH, "simple", "trace-definition.yaml").toString();
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(resourceDefinitions)
                .withTraceDefinitionYAML(tracesDefinitions).build();
        generatorInput.validate("ValidateSimpleExampleDefinitionsTest_ResourceTraceDefinitions");
    }

    @Test
    public void validateCLITargetYAMLs() throws IOException {
        File cliRestTargetYAML = Paths.get(DEFINITION_BASE_PATH, "cli-target-rest.yaml").toFile();
        File cligRPCTargetYAML = Paths.get(DEFINITION_BASE_PATH, "cli-target-grpc.yaml").toFile();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.readValue(cliRestTargetYAML, TargetEnvironmentDetails.class);
        yamlMapper.readValue(cligRPCTargetYAML, TargetEnvironmentDetails.class);
    }
}
