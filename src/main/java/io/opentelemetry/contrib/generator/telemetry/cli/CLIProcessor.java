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

package io.opentelemetry.contrib.generator.telemetry.cli;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.TelemetryGenerator;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.AuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.BasicAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.NoAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.grpc.GRPCPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.rest.RESTPayloadHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

@Slf4j
public class CLIProcessor {

    public static void main(String[] args) throws ParseException {

        CommandLineParser cliParser = new DefaultParser();
        CommandLine line = cliParser.parse(getOptions(), args);
        if (!(line.hasOption("m") || line.hasOption("l") || line.hasOption("s"))) {
            throw new GeneratorException("One of metricDefinition, logDefinition or traceDefinition must be provided");
        }
        PayloadHandler payloadHandler = getPayloadHandler(line.getOptionValue("t"));
        GeneratorInput.YAMLFilesBuilder inputBuilder = new GeneratorInput.YAMLFilesBuilder(line.getOptionValue("e"));
        if (line.hasOption("m")) {
            inputBuilder.withMetricDefinitionYAML(line.getOptionValue("m"));
        }
        if (line.hasOption("l")) {
            inputBuilder.withLogDefinitionYAML(line.getOptionValue("l"));
        }
        if (line.hasOption("s")) {
            inputBuilder.withTraceDefinitionYAML(line.getOptionValue("s"));
        }
        TelemetryGenerator generator = new TelemetryGenerator(inputBuilder.build(), payloadHandler);
        generator.runGenerator();
    }

    private static Options getOptions() {
        Option entityDefinition = Option.builder("e")
                .argName("entityDefinition")
                .longOpt("entityDefinition")
                .desc("Path to the entity definition YAML")
                .hasArg()
                .required()
                .build();
        Option metricDefinition = Option.builder("m")
                .argName("metricDefinition")
                .longOpt("metricDefinition")
                .desc("Path to the metric definition YAML")
                .hasArg()
                .build();
        Option logsDefinition = Option.builder("l")
                .argName("logDefinition")
                .longOpt("logDefinition")
                .desc("Path to the log definition YAML")
                .hasArg()
                .build();
        Option traceDefinition = Option.builder("s")
                .argName("spanDefinition")
                .longOpt("spanDefinition")
                .desc("Path to the trace definition YAML")
                .hasArg()
                .build();
        Option targetEnvYAML = Option.builder("t")
                .argName("target")
                .longOpt("target")
                .desc("Path to the YAML containing details of the environment to target")
                .hasArg()
                .required()
                .build();
        Options options = new Options();
        options.addOption(entityDefinition);
        options.addOption(metricDefinition);
        options.addOption(logsDefinition);
        options.addOption(traceDefinition);
        options.addOption(targetEnvYAML);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("test-telemetry-generator-all.jar", options, true);
        return options;
    }

    private static PayloadHandler getPayloadHandler(String targetEnvYAML) {
        TargetEnvironmentDetails targetEnvironmentDetails = getTargetEnvDetails(targetEnvYAML);
        String nonNullRestURL = StringUtils.defaultString(targetEnvironmentDetails.getRestURL());
        String nonNullGRPCHost = StringUtils.defaultString(targetEnvironmentDetails.getGRPCHost());
        String nonNullGRPCPort = StringUtils.defaultString(targetEnvironmentDetails.getGRPCPort());
        if (nonNullRestURL.isBlank() && (nonNullGRPCHost.isBlank() || nonNullGRPCPort.isBlank())) {
            throw new GeneratorException("Either restURL (for REST endpoint) or gRPCHost & gRPCPort (for gRPC endpoint) " +
                    "must be provided in environment target YAML");
        }
        AuthHandler authHandler;
        if (targetEnvironmentDetails.getAuthMode().equalsIgnoreCase("NONE")) {
            authHandler = new NoAuthHandler();
        } else {
            if (StringUtils.defaultString(targetEnvironmentDetails.getUsername()).isBlank()) {
                throw new GeneratorException("Missing username in environment target YAML");
            }
            if (StringUtils.defaultString(targetEnvironmentDetails.getPassword()).isBlank()) {
                throw new GeneratorException("Missing password in environment target YAML");
            }
            authHandler = new BasicAuthHandler(targetEnvironmentDetails.getUsername(),
                    targetEnvironmentDetails.getPassword());
        }
        if (!nonNullRestURL.isBlank()) {
            return new RESTPayloadHandler(nonNullRestURL, authHandler);
        }
        int gRPCPort;
        try {
            gRPCPort = Integer.parseInt(nonNullGRPCPort);
        } catch (NumberFormatException numberFormatException) {
            throw new GeneratorException("Invalid gRPC port " + nonNullGRPCPort + " provided in environment target YAML");
        }
        return new GRPCPayloadHandler(nonNullGRPCHost, gRPCPort, authHandler);
    }

    private static TargetEnvironmentDetails getTargetEnvDetails(String targetEnvYAML) {
        File yamlFile = new File(targetEnvYAML);
        if (!(yamlFile.exists() && yamlFile.canRead())) {
            throw new GeneratorException("Unable to read provided target environment YAML file " + targetEnvYAML);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(yamlFile, TargetEnvironmentDetails.class);
        } catch (IOException e) {
            throw new GeneratorException("Failed to parse target environment details YAML " + targetEnvYAML + " due to " + e.getMessage());
        }
    }

}
