package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.dto.EntityDefinition;
import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.logs.LogsGenerator;
import io.opentelemetry.contrib.generator.telemetry.metrics.MetricsGenerator;
import io.opentelemetry.contrib.generator.telemetry.traces.TracesGenerator;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import io.opentelemetry.contrib.generator.core.EntityModelGenerator;
import io.opentelemetry.contrib.generator.core.RuntimeModificationsThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main API class if using the Telemetry Generator as a Java library in your code.
 */
@Slf4j
public class TelemetryGenerator {

    private final GeneratorInput input;
    private final PayloadHandler payloadHandler;
    private final String requestID;
    @Getter
    private final TransportStorage transportStorage;
    private Map<String, List<GeneratorEntity>> entityModel;
    @Getter
    private GeneratorsMonitor generatorsMonitor;

    public TelemetryGenerator(GeneratorInput input, PayloadHandler payloadHandler) {
        this(input, payloadHandler, RandomStringUtils.randomAlphanumeric(32));
    }

    public TelemetryGenerator(GeneratorInput input, PayloadHandler payloadHandler, String requestID) {
        this(input, payloadHandler, requestID, false);
    }

    public TelemetryGenerator(GeneratorInput input, PayloadHandler payloadHandler, boolean storePayloadStatuses) {
        this(input, payloadHandler, RandomStringUtils.randomAlphanumeric(32), storePayloadStatuses);
    }

    public TelemetryGenerator(GeneratorInput input, PayloadHandler payloadHandler, String requestID, boolean storePayloadStatuses) {
        this.input = input;
        this.payloadHandler = payloadHandler;
        this.requestID = requestID;
        this.transportStorage = storePayloadStatuses ? new TransportStorage() : null;
    }

    public void runGenerator() {
        input.validate(requestID);
        log.info("Received data generation request with metrics = (" + input.isHasMetrics() + "), logs = (" +
                input.isHasLogs() + "), traces = (" +  input.isHasTraces() + ")");
        EntityModelProvider.putEntityModel(requestID, getEntityModel());
        if (input.isHasMetrics()) {
            var metricsGenerator = new MetricsGenerator(input.getMetricDefinitions(), payloadHandler, requestID, transportStorage);
            metricsGenerator.runGenerator();
        }
        if (input.isHasTraces()) {
            input.getTraceDefinitions().initTrees(requestID);
            var tracesGenerator = new TracesGenerator(input.getTraceDefinitions(), payloadHandler, requestID, transportStorage);
            tracesGenerator.runGenerator();
        }
        if (input.isHasLogs()) {
            var logsGenerator = new LogsGenerator(input.getLogDefinitions(), payloadHandler, requestID, transportStorage);
            logsGenerator.runGenerator();
        }
        ScheduledExecutorService runtimeModsExecutor = null;
        if (input.getEntityDefinitions().isHasRuntimeModifications()) {
            var runtimeModifications = new RuntimeModificationsThread(requestID, input.getEntityDefinitions().getEntities().stream()
                    .map(eachType -> CollectionUtils.emptyIfNull(eachType.getRuntimeModifications()))
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            runtimeModsExecutor = Executors.newScheduledThreadPool(1);
            runtimeModsExecutor.scheduleAtFixedRate(runtimeModifications, 0, 2000, TimeUnit.MILLISECONDS);
        }
        generatorsMonitor = new GeneratorsMonitor(requestID, input);
        generatorsMonitor.monitorThreads();
        if (input.getEntityDefinitions().isHasRuntimeModifications() && runtimeModsExecutor != null) {
            runtimeModsExecutor.shutdown();
        }
    }

    public Map<String, List<GeneratorEntity>> getEntityModel() {
        if (entityModel == null) {
            Map<String, EntityDefinition> entitiesMap = input.getEntityDefinitions().getEntities().stream()
                    .collect(Collectors.toMap(EntityDefinition::getName, Function.identity()));
            var entityModelGenerator = new EntityModelGenerator(entitiesMap, requestID);
            entityModel = entityModelGenerator.getEntityModel();
        }
        return entityModel;
    }
}
