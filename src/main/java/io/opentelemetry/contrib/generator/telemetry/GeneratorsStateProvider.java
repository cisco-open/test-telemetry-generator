package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.logs.LogGeneratorThread;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.metrics.MetricGeneratorThread;
import io.opentelemetry.contrib.generator.telemetry.traces.TraceGeneratorThread;

import java.util.HashMap;
import java.util.Map;

public class GeneratorsStateProvider {

    private static final Map<String, GeneratorState<TraceGeneratorThread>> traceGeneratorsState = new HashMap<>();
    private static final Map<String, GeneratorState<MetricGeneratorThread>> metricGeneratorsStates = new HashMap<>();
    private static final Map<String, GeneratorState<LogGeneratorThread>> logGeneratorStates = new HashMap<>();

    private GeneratorsStateProvider() {}

    public static void putMetricGeneratorState(String requestID, GeneratorState<MetricGeneratorThread> metricGeneratorState) {
        metricGeneratorsStates.put(requestID, metricGeneratorState);
    }

    public static void putTraceGeneratorState(String requestID, GeneratorState<TraceGeneratorThread> traceGeneratorState) {
        traceGeneratorsState.put(requestID, traceGeneratorState);
    }

    public static GeneratorState<MetricGeneratorThread> getMetricGeneratorState(String requestID) {
        return metricGeneratorsStates.get(requestID);
    }

    public static void putLogGeneratorState(String requestID, GeneratorState<LogGeneratorThread> logGeneratorState) {
        logGeneratorStates.put(requestID, logGeneratorState);
    }

    public static GeneratorState<LogGeneratorThread> getLogGeneratorState(String requestID) {
        return logGeneratorStates.get(requestID);
    }

    public static GeneratorState<TraceGeneratorThread> getTraceGeneratorState(String requestID) {
        return traceGeneratorsState.get(requestID);
    }
}
