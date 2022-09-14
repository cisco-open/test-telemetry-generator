package io.opentelemetry.contrib.generator.telemetry.logs;

import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.logs.dto.LogDefinition;
import io.opentelemetry.contrib.generator.telemetry.logs.dto.Logs;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for initializing and scheduling all the log generator threads.
 */
@Slf4j
public class LogsGenerator {
        private final Map<String, LogGeneratorThread> generatorThreadMap;
        private final PayloadHandler payloadHandler;
        private final String requestID;
        private final TransportStorage transportStorage;
        private final Logs logs;
        private GeneratorState<LogGeneratorThread> generatorState;

        public LogsGenerator(Logs logs, PayloadHandler payloadHandler, String requestID, TransportStorage transportStorage) {
            this.payloadHandler = payloadHandler;
            this.requestID = requestID;
            this.transportStorage = transportStorage;
            this.logs = logs;
            generatorThreadMap = new HashMap<>();
            initGeneratorState(logs.getTotalPayloadCount());
        }

        public void runGenerator() {
            log.info(requestID + ": Initializing " + logs.getLogs().size() + " logs generator threads with total payload count ("
                    + generatorState.getTotalPayloadCount() + ")");
            generatorState.setGeneratorThreadMap(generatorThreadMap);
            GeneratorsStateProvider.putLogGeneratorState(requestID, generatorState);
            logs.getLogs().forEach(this::initThread);
            generatorState.setGenerateData(true);
            log.debug(requestID + ": Flipped generate data flag to true for log threads");
        }

        private void initGeneratorState(int totalPayloadCount) {
            generatorState = new GeneratorState<>(Executors.newScheduledThreadPool(logs.getLogs().size()));
            generatorState.setTotalPayloadCount(totalPayloadCount);
            generatorState.setThreadPayloadCounts(new ConcurrentHashMap<>());
            if (transportStorage != null) {
                transportStorage.initLogResponseMaps();
                generatorState.setTransportStorage(transportStorage);
            }
        }

        private void initThread(LogDefinition logDefinition) {
            var logGeneratorThread = new LogGeneratorThread(logDefinition, payloadHandler, requestID);
            generatorState.getExecutorService().scheduleAtFixedRate(logGeneratorThread, 10,
                    logDefinition.getPayloadFrequencySeconds(), TimeUnit.SECONDS);
            generatorThreadMap.put(logDefinition.getName(), logGeneratorThread);
            generatorState.getThreadPayloadCounts().put(logDefinition.getName(), 0);
            log.debug(requestID + ": Scheduled log generator thread for Log Definition " + logDefinition.getName());
        }

}
