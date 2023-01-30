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
import io.opentelemetry.contrib.generator.core.dto.Resources;
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

    private String resourceDefinitionYAML;
    @Getter
    private Resources resourceDefinitions;
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
        this.resourceDefinitionYAML = yamlFilesBuilder.resourceDefinitionYAML;
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
        this.resourceDefinitionYAML = jsonFilesBuilder.resourceDefinitionJSON;
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
        this.resourceDefinitions = dtoBuilder.resourceDefinitions;
        this.metricDefinitions = dtoBuilder.metricDefinitions;
        this.logDefinitions = dtoBuilder.logDefinitions;
        this.traceDefinitions = dtoBuilder.traceDefinitions;
        hasMetrics = metricDefinitions != null;
        hasLogs = logDefinitions != null;
        hasTraces = traceDefinitions != null;
        loadYAMLs = false;
        loadJSONs = false;
    }

    public static YAMLFilesBuilder builder(String resourceDefinitionYAML) {
        return new YAMLFilesBuilder(resourceDefinitionYAML);
    }

    public static DTOBuilder builder(Resources resourceDefinitions) {
        return new DTOBuilder(resourceDefinitions);
    }

    public static final class YAMLFilesBuilder {
        private final String resourceDefinitionYAML;
        private String metricDefinitionYAML;
        private String logDefinitionYAML;
        private String traceDefinitionYAML;

        public YAMLFilesBuilder(String resourceDefinitionYAML) {
            this.resourceDefinitionYAML = resourceDefinitionYAML;
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
        private final String resourceDefinitionJSON;
        private String metricDefinitionJSON;
        private String logDefinitionJSON;
        private String traceDefinitionJSON;

        public JSONFilesBuilder(String resourceDefinitionJSON) {
            this.resourceDefinitionJSON = resourceDefinitionJSON;
        }

        public JSONFilesBuilder withMetricDefinitionJSON(String metricDefinitionYAML) {
            this.metricDefinitionJSON = metricDefinitionYAML;
            return this;
        }

        public JSONFilesBuilder withLogDefinitionJSON(String logDefinitionYAML) {
            this.logDefinitionJSON = logDefinitionYAML;
            return this;
        }

        public JSONFilesBuilder withTraceDefinitionJSON(String traceDefinitionYAML) {
            this.traceDefinitionJSON = traceDefinitionYAML;
            return this;
        }

        public GeneratorInput build() {
            return new GeneratorInput(this);
        }
    }


    public static final class DTOBuilder {
        private final Resources resourceDefinitions;
        private Metrics metricDefinitions;
        private Logs logDefinitions;
        private Traces traceDefinitions;

        public DTOBuilder(Resources resourceDefinitions) {
            this.resourceDefinitions = resourceDefinitions;
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
        Set<String> allResourceTypes = resourceDefinitions.validate();
        if (hasMetrics) {
            metricDefinitions.validate(requestID, allResourceTypes);
        }
        if (hasLogs) {
            logDefinitions.validate(requestID, allResourceTypes);
        }
        if (hasTraces) {
            traceDefinitions.validate(requestID, allResourceTypes);
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
        File resourcesYAML = validateFile(resourceDefinitionYAML);
        resourceDefinitions = fileMapper.readValue(resourcesYAML, Resources.class);
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
        return new YAMLFilesBuilder(System.getenv(Constants.RESOURCE_DEFINITION_ENV))
                .withMetricDefinitionYAML(System.getenv(Constants.METRICS_DEFINITION_ENV))
                .withLogDefinitionYAML(System.getenv(Constants.LOGS_DEFINITION_ENV))
                .withTraceDefinitionYAML(System.getenv(Constants.TRACES_DEFINITION_YAML))
                .build();
    }
}
