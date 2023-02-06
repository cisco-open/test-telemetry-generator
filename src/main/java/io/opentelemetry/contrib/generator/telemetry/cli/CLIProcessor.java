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
import io.opentelemetry.contrib.generator.telemetry.cli.dto.TargetEnvironmentDetails;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.TelemetryGenerator;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.AuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.BasicAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.NoAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.OAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.grpc.GRPCPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.rest.RESTPayloadHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class CLIProcessor {

    public static void main(String[] args) throws ParseException {

        CommandLineParser cliParser = new DefaultParser();
        CommandLine line = cliParser.parse(getOptions(), args);
        if (!(line.hasOption("m") || line.hasOption("l") || line.hasOption("s"))) {
            throw new GeneratorException("One of metricDefinition, logDefinition or traceDefinition must be provided");
        }
        PayloadHandler payloadHandler = getPayloadHandler(line.getOptionValue("t"));
        GeneratorInput input;
        if (line.hasOption("j")) {
            GeneratorInput.JSONFilesBuilder inputBuilder = new GeneratorInput.JSONFilesBuilder(line.getOptionValue("r"));
            if (line.hasOption("m")) {
                inputBuilder.withMetricDefinitionJSON(line.getOptionValue("m"));
            }
            if (line.hasOption("l")) {
                inputBuilder.withLogDefinitionJSON(line.getOptionValue("l"));
            }
            if (line.hasOption("s")) {
                inputBuilder.withTraceDefinitionJSON(line.getOptionValue("s"));
            }
            input = inputBuilder.build();
        } else {
            GeneratorInput.YAMLFilesBuilder inputBuilder = new GeneratorInput.YAMLFilesBuilder(line.getOptionValue("r"));
            if (line.hasOption("m")) {
                inputBuilder.withMetricDefinitionYAML(line.getOptionValue("m"));
            }
            if (line.hasOption("l")) {
                inputBuilder.withLogDefinitionYAML(line.getOptionValue("l"));
            }
            if (line.hasOption("s")) {
                inputBuilder.withTraceDefinitionYAML(line.getOptionValue("s"));
            }
            input = inputBuilder.build();
        }
        TelemetryGenerator generator = new TelemetryGenerator(input, payloadHandler);
        generator.runGenerator();
    }

    private static Options getOptions() {
        Option resourceDefinition = Option.builder("r")
                .argName("resourceDefinition")
                .longOpt("resourceDefinition")
                .desc("Path to the resource definition YAML")
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
        Option jsonFormatFlag = Option.builder("j")
                .argName("jsonFormat")
                .longOpt("jsonFormat")
                .desc("Flag to use JSON format for input definitions")
                .build();
        Options options = new Options();
        options.addOption(resourceDefinition);
        options.addOption(metricDefinition);
        options.addOption(logsDefinition);
        options.addOption(traceDefinition);
        options.addOption(targetEnvYAML);
        options.addOption(jsonFormatFlag);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("test-telemetry-generator-all.jar", options, true);
        return options;
    }

    private static PayloadHandler getPayloadHandler(String targetEnvYAML) {
        TargetEnvironmentDetails targetEnvironmentDetails = getTargetEnvDetails(targetEnvYAML);
        String nonNullGRPCHost = StringUtils.defaultString(targetEnvironmentDetails.getGRPCHost());
        String nonNullGRPCPort = StringUtils.defaultString(targetEnvironmentDetails.getGRPCPort());
        boolean restURLProvided = targetEnvironmentDetails.getRestURL() != null &&
                !StringUtils.defaultString(targetEnvironmentDetails.getRestURL().getBaseURL()).isBlank();
        if (!restURLProvided && (nonNullGRPCHost.isBlank() || nonNullGRPCPort.isBlank())) {
            throw new GeneratorException("Either restURL (for REST endpoint) or gRPCHost & gRPCPort (for gRPC endpoint) " +
                    "must be provided in environment target YAML");
        }
        String authMode = StringUtils.defaultString(targetEnvironmentDetails.getAuthMode()).toUpperCase();
        if (authMode.isBlank() || (!authMode.equals("NONE") && !authMode.equals("BASIC") && !authMode.equals("OAUTH"))) {
            log.warn("authMode not provided or invalid value provided in environment target YAML. Valid values are - none/basic/oauth." +
                    "Will use none authentication for data posting.");
            authMode = "NONE";
        }
        AuthHandler authHandler;
        if (authMode.equals("NONE")) {
            authHandler = new NoAuthHandler();
        } else if (authMode.equals("BASIC")) {
            if (StringUtils.defaultString(targetEnvironmentDetails.getUsername()).isBlank()) {
                throw new GeneratorException("Select auth mode is Basic but username not provided");
            }
            if (StringUtils.defaultString(targetEnvironmentDetails.getPassword()).isBlank()) {
                throw new GeneratorException("Select auth mode is Basic but password not provided");
            }
            authHandler = new BasicAuthHandler(targetEnvironmentDetails.getUsername(),
                    targetEnvironmentDetails.getPassword());
        } else {
            if (StringUtils.defaultString(targetEnvironmentDetails.getTokenURL()).isBlank()) {
                throw new GeneratorException("Select auth mode is OAuth but tokenURL not provided");
            }
            if (StringUtils.defaultString(targetEnvironmentDetails.getClientId()).isBlank()) {
                throw new GeneratorException("Select auth mode is Basic but clientId not provided");
            }
            if (StringUtils.defaultString(targetEnvironmentDetails.getClientSecret()).isBlank()) {
                throw new GeneratorException("Select auth mode is Basic but clientSecret not provided");
            }
            authHandler = new OAuthHandler(targetEnvironmentDetails.getTokenURL(), targetEnvironmentDetails.getClientId(),
                    targetEnvironmentDetails.getClientSecret(), targetEnvironmentDetails.getScope());
        }
        if (restURLProvided) {
            String restBaseURL = targetEnvironmentDetails.getRestURL().getBaseURL();
            try {
                new URI(restBaseURL);
            } catch (URISyntaxException e) {
                log.warn("Invalid rest URL provided in environment target YAML", e);
            }
            RESTPayloadHandler restPayloadHandler = new RESTPayloadHandler(restBaseURL, authHandler);
            if (!StringUtils.defaultString(targetEnvironmentDetails.getRestURL().getMetricsPath()).isBlank()) {
                restPayloadHandler.setMetricsURL(targetEnvironmentDetails.getRestURL().getMetricsPath());
            }
            if (!StringUtils.defaultString(targetEnvironmentDetails.getRestURL().getLogsPath()).isBlank()) {
                restPayloadHandler.setMetricsURL(targetEnvironmentDetails.getRestURL().getLogsPath());
            }
            if (!StringUtils.defaultString(targetEnvironmentDetails.getRestURL().getTracesPath()).isBlank()) {
                restPayloadHandler.setMetricsURL(targetEnvironmentDetails.getRestURL().getTracesPath());
            }
            return restPayloadHandler;
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
