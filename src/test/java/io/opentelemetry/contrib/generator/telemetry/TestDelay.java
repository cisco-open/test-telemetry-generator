package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestDelay implements Runnable {

    TelemetryGenerator telemetryGenerator, telemetryGenerator2;
    GeneratorInput generatorInput;
    PayloadHandler payloadStore;

    public static void main(String[] s) throws InterruptedException {
        TestDelay testDelay = new TestDelay();
        String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "entity-definition.yaml").toString();
        String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "metrics-test.yaml").toString();
        String LOGS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "logs-test-combined.yaml").toString();
        String TRACES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "trace-definition.yaml").toString();
        testDelay.payloadStore = new TestPayloadHandler();
        testDelay.generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML)
                .withMetricDefinitionYAML(METRICS_YAML)
                //.withLogDefinitionYAML(LOGS_YAML)
                //.withTraceDefinitionYAML(TRACES_YAML)
                .build();
        Thread thread = new Thread(testDelay);
        thread.start();
        TimeUnit.SECONDS.sleep(75);
        Map<String, List<GeneratorEntity>> entityModel = testDelay.telemetryGenerator.getEntityModel();
        testDelay.telemetryGenerator.getGeneratorsMonitor().killGenerator();
        testDelay.telemetryGenerator2 = new TelemetryGenerator(testDelay.generatorInput, testDelay.payloadStore, entityModel);
        Thread thread2 = new Thread(testDelay);
        thread2.start();
        thread.join();

    }

    @Override
    public void run() {
        if (telemetryGenerator == null) {
            telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
            telemetryGenerator.runGenerator();
        } else {
            telemetryGenerator2.runGenerator();
        }
    }
}
