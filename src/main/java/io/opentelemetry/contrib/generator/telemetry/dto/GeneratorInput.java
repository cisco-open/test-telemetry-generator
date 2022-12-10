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

package io.opentelemetry.contrib.generator.telemetry.dto;


import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.core.dto.Entities;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.opentelemetry.contrib.generator.telemetry.logs.dto.Logs;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.Metrics;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.Traces;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class GeneratorInput {

    private String entityDefinitionYAML;
    @Getter
    private Entities entityDefinitions;
    private String metricDefinitionYAML;
    @Getter
    private Metrics metricDefinitions;
    private String logDefinitionYAML;
    @Getter
    private Logs logDefinitions;
    private String traceDefinitionYAML;
    @Getter
    private Traces traceDefinitions;
    @Getter
    private final boolean hasMetrics;
    @Getter
    private final boolean hasLogs;
    @Getter
    private final boolean hasTraces;
    private final boolean loadYAMLs;
    private final boolean loadJSONs;

    private GeneratorInput(YAMLFilesBuilder yamlFilesBuilder) {
        this.entityDefinitionYAML = yamlFilesBuilder.entityDefinitionYAML;
        this.metricDefinitionYAML = yamlFilesBuilder.metricDefinitionYAML;
        this.logDefinitionYAML = yamlFilesBuilder.logDefinitionYAML;
        this.traceDefinitionYAML = yamlFilesBuilder.traceDefinitionYAML;
        hasMetrics = !StringUtils.defaultString(metricDefinitionYAML).trim().isBlank();
        hasLogs = !StringUtils.defaultString(logDefinitionYAML).trim().isBlank();
        hasTraces = !StringUtils.defaultString(traceDefinitionYAML).trim().isBlank();
        loadYAMLs = true;
        loadJSONs = false;
    }

    private GeneratorInput(JSONFilesBuilder jsonFilesBuilder) {
        this.entityDefinitionYAML = jsonFilesBuilder.entityDefinitionJSON;
        this.metricDefinitionYAML = jsonFilesBuilder.metricDefinitionJSON;
        this.logDefinitionYAML = jsonFilesBuilder.logDefinitionJSON;
        this.traceDefinitionYAML = jsonFilesBuilder.traceDefinitionJSON;
        hasMetrics = !StringUtils.defaultString(metricDefinitionYAML).trim().isBlank();
        hasLogs = !StringUtils.defaultString(logDefinitionYAML).trim().isBlank();
        hasTraces = !StringUtils.defaultString(traceDefinitionYAML).trim().isBlank();
        loadYAMLs = false;
        loadJSONs = true;
    }

    private GeneratorInput(DTOBuilder dtoBuilder) {
        this.entityDefinitions = dtoBuilder.entityDefinitions;
        this.metricDefinitions = dtoBuilder.metricDefinitions;
        this.logDefinitions = dtoBuilder.logDefinitions;
        this.traceDefinitions = dtoBuilder.traceDefinitions;
        hasMetrics = metricDefinitions != null;
        hasLogs = logDefinitions != null;
        hasTraces = traceDefinitions != null;
        loadYAMLs = false;
        loadJSONs = false;
    }

    public static YAMLFilesBuilder builder(String entityDefinitionYAML) {
        return new YAMLFilesBuilder(entityDefinitionYAML);
    }

    public static DTOBuilder builder(Entities entityDefinitions) {
        return new DTOBuilder(entityDefinitions);
    }

    public static final class YAMLFilesBuilder {
        private final String entityDefinitionYAML;
        private String metricDefinitionYAML;
        private String logDefinitionYAML;
        private String traceDefinitionYAML;

        public YAMLFilesBuilder(String entityDefinitionYAML) {
            this.entityDefinitionYAML = entityDefinitionYAML;
        }

        public YAMLFilesBuilder withMetricDefinitionYAML(String metricDefinitionYAML) {
            this.metricDefinitionYAML = metricDefinitionYAML;
            return this;
        }

        public YAMLFilesBuilder withLogDefinitionYAML(String logDefinitionYAML) {
            this.logDefinitionYAML = logDefinitionYAML;
            return this;
        }

        public GeneratorInput.YAMLFilesBuilder withTraceDefinitionYAML(String traceDefinitionYAML) {
            this.traceDefinitionYAML = traceDefinitionYAML;
            return this;
        }

        public GeneratorInput build() {
            return new GeneratorInput(this);
        }
    }

    public static final class JSONFilesBuilder {
        private final String entityDefinitionJSON;
        private String metricDefinitionJSON;
        private String logDefinitionJSON;
        private String traceDefinitionJSON;

        public JSONFilesBuilder(String entityDefinitionJSON ) {
            this.entityDefinitionJSON = entityDefinitionJSON;
        }

        public JSONFilesBuilder withMetricDefinitionYAML(String metricDefinitionYAML) {
            this.metricDefinitionJSON = metricDefinitionYAML;
            return this;
        }

        public JSONFilesBuilder withLogDefinitionYAML(String logDefinitionYAML) {
            this.logDefinitionJSON = logDefinitionYAML;
            return this;
        }

        public JSONFilesBuilder withTraceDefinitionYAML(String traceDefinitionYAML) {
            this.traceDefinitionJSON = traceDefinitionYAML;
            return this;
        }

        public GeneratorInput build() {
            return new GeneratorInput(this);
        }
    }

    public static final class DTOBuilder {
        private final Entities entityDefinitions;
        private Metrics metricDefinitions;
        private Logs logDefinitions;
        private Traces traceDefinitions;

        public DTOBuilder(Entities entityDefinitions) {
            this.entityDefinitions = entityDefinitions;
        }

        public DTOBuilder withMetricDefinitions(Metrics metricDefinitions) {
            this.metricDefinitions = metricDefinitions;
            return this;
        }

        public DTOBuilder withLogDefinitions(Logs logDefinitions) {
            this.logDefinitions = logDefinitions;
            return this;
        }

        public GeneratorInput.DTOBuilder withTraceDefinitions(Traces traceDefinitions) {
            this.traceDefinitions = traceDefinitions;
            return this;
        }

        public GeneratorInput build() {
            return new GeneratorInput(this);
        }
    }

    public void validate(String requestID) {
        checkPreconditions();
        Set<String> allEntityTypes = entityDefinitions.validate();
        if (hasMetrics) {
            metricDefinitions.validate(requestID, allEntityTypes);
        }
        if (hasLogs) {
            logDefinitions.validate(requestID, allEntityTypes);
        }
        if (hasTraces) {
            traceDefinitions.validate(requestID, allEntityTypes);
        }
    }

    public void checkPreconditions() {
        if (!(hasMetrics || hasLogs || hasTraces)) {
            throw new GeneratorException("One of metrics/logs/traces must be provided for data generation");
        }
        if (loadYAMLs) {
            loadYAMLFiles();
        }
        if(loadJSONs) {
            loadJSONFiles();
        }
    }

    private void loadYAMLFiles() {
        var yamlMapper = new ObjectMapper(new YAMLFactory());
        try {
            setDTOs(yamlMapper);
        } catch (IOException ioException) {
            throw new GeneratorException("Exception occurred while loading YAML files due to " + ioException.getMessage());
        }
    }

    private void loadJSONFiles() {
        var jsonMapper = new ObjectMapper();
        try {
            setDTOs(jsonMapper);
        } catch (IOException ioException) {
            throw new GeneratorException("Exception occurred while loading JSON files due to " + ioException.getMessage());
        }
    }

    private void setDTOs(ObjectMapper fileMapper) throws IOException {
        File entitiesYAML = validateFile(entityDefinitionYAML);
        entityDefinitions = fileMapper.readValue(entitiesYAML, Entities.class);
        if (hasMetrics) {
            File metricsYAML = validateFile(metricDefinitionYAML);
            metricDefinitions = fileMapper.readValue(metricsYAML, Metrics.class);
        }
        if (hasLogs) {
            File logsYAML = validateFile(logDefinitionYAML);
            logDefinitions = fileMapper.readValue(logsYAML, Logs.class);
        }
        if (hasTraces) {
            File tracesYAML = validateFile(traceDefinitionYAML);
            traceDefinitions = fileMapper.readValue(tracesYAML, Traces.class);
        }
    }

    private File validateFile(String filePath) {
        var file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            throw new GeneratorException("Unable to read provided YAML: " + filePath);
        }
        return file;
    }

    public static GeneratorInput loadFromEnvironment() {
        return new YAMLFilesBuilder(System.getenv(Constants.ENTITY_DEFINITION_ENV))
                .withMetricDefinitionYAML(System.getenv(Constants.METRICS_DEFINITION_ENV))
                .withLogDefinitionYAML(System.getenv(Constants.LOGS_DEFINITION_ENV))
                .withTraceDefinitionYAML(System.getenv(Constants.TRACES_DEFINITION_YAML))
                .build();
    }
}
