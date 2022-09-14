package io.opentelemetry.contrib.generator.telemetry.dto;

import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Holds data shared by all metrics/logs/traces thread for a single instance of the generator. Since the generators return
 * the control back to the caller after initializing the threads, a public static instance of this in the generator helps with control & monitoring.
 */
@Data
public class GeneratorState<T extends Runnable> {

    private final ScheduledExecutorService executorService;
    private long fixedStartTime;
    private Map<String, T> generatorThreadMap;
    private ConcurrentMap<String, Integer> threadPayloadCounts;
    private int totalPayloadCount;
    private boolean generateData;
    private TransportStorage transportStorage;

    public int getCurrentPayloadCount() {
        return threadPayloadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isDataGenerationComplete() {
        return getCurrentPayloadCount() >= totalPayloadCount;
    }
}
