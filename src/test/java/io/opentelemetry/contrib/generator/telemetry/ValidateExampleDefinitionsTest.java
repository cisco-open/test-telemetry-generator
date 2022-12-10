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
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class ValidateExampleDefinitionsTest {

    private final String DEFINITION_PATH = Paths.get(System.getProperty("user.dir"), "example-definitions", "qa").toString();

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
