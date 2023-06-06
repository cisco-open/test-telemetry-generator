package io.opentelemetry.contrib.generator.fso_demo;

import io.opentelemetry.contrib.generator.telemetry.TelemetryGenerator;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.AuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.OAuthHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.rest.RESTPayloadHandler;

import java.nio.file.Paths;

public class RunFSODemo {

    private static final String DEFINITIONS_PATH = Paths.get(System.getProperty("user.dir"),
            "example-definitions", "FSO-demo").toString();

    public static void main(String[] s) {
        TelemetryGenerator generator = new TelemetryGenerator(getGeneratorInput(), getPayloadHandler(), false);
        generator.runGenerator();
    }

    private static RESTPayloadHandler getPayloadHandler() {
        String baseURL = "https://alameda-92-iam-qe.saas.appd-test.com";
        String tokenURL = baseURL + "/auth/8a8a98ae-9472-4cee-88fa-0cec938aa14a/default/oauth2/token";
        String clientId = "";
        String clientSecret = "";
        AuthHandler authHandler = new OAuthHandler(tokenURL, clientId, clientSecret, null);
        RESTPayloadHandler restPayloadHandler = new RESTPayloadHandler(baseURL, authHandler);
        restPayloadHandler.setMetricsURL("/data/v1/metrics");
        restPayloadHandler.setLogsURL("/data/v1/logs");
        restPayloadHandler.setTracesURL("/data/v1/trace");
        return restPayloadHandler;
    }

    private static GeneratorInput getGeneratorInput() {
        String resourceDefinition = Paths.get(DEFINITIONS_PATH, "resource-definition.yaml").toString();
        String metricDefinition = Paths.get(DEFINITIONS_PATH, "metric-definition.yaml").toString();
        String logDefinition = Paths.get(DEFINITIONS_PATH, "log-definition.yaml").toString();
        String traceDefinition = Paths.get(DEFINITIONS_PATH, "trace-definition.yaml").toString();
        return new GeneratorInput.YAMLFilesBuilder(resourceDefinition)
                .withMetricDefinitionYAML(metricDefinition)
                .withLogDefinitionYAML(logDefinition)
                .withTraceDefinitionYAML(traceDefinition)
                .build();
    }
}
