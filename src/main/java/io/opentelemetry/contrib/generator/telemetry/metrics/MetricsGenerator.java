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

package io.opentelemetry.contrib.generator.telemetry.metrics;

import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.MetricDefinition;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.Metrics;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for initializing and scheduling all the metric generator threads.
 */
@Slf4j
public class MetricsGenerator {

    private final Metrics metrics;
    private final PayloadHandler payloadHandler;
    private final String requestID;
    private final TransportStorage transportStorage;
    private final Map<String, List<MetricDefinition>> metricThreadGroups;

    private GeneratorState<MetricGeneratorThread> generatorState;

    public MetricsGenerator(Metrics metrics, PayloadHandler payloadHandler, String requestID, TransportStorage transportStorage) {
        this.metrics = metrics;
        this.payloadHandler = payloadHandler;
        this.requestID = requestID;
        this.transportStorage = transportStorage;
        metricThreadGroups = getMetricThreadGroups();
        initGeneratorState();
    }

    public void runGenerator() {
        int totalPayloadCount = metricThreadGroups.values().stream()
                .map(metricsList -> metricsList.get(0).getPayloadCount())
                .mapToInt(Integer::intValue).sum();
        generatorState.setTotalPayloadCount(totalPayloadCount);
        GeneratorsStateProvider.putMetricGeneratorState(requestID, generatorState);
        Map<String, MetricGeneratorThread> generatorThreadMap = new HashMap<>();
        generatorState.setGeneratorThreadMap(generatorThreadMap);
        log.info(requestID + ": Initializing " + metricThreadGroups.size() + " metric generator threads");
        initThreads();
        generatorState.setGenerateData(true);
        log.debug(requestID + ": Flipped generate data flag to true for metric threads");
    }

    private void initGeneratorState() {
        generatorState = new GeneratorState<>(Executors.newScheduledThreadPool(metricThreadGroups.size()));
        generatorState.setThreadPayloadCounts(new ConcurrentHashMap<>());
        if (transportStorage != null) {
            transportStorage.initMetricResponseMaps();
            generatorState.setTransportStorage(transportStorage);
        }
    }

    /**
     * Since the metric generator threads are grouped by entity type rather than each metric, we need to also consider
     * metrics having different payload count and/or payload frequency. To handle this, we introduce the concept of a group key
     * which is obtained as - reportingEntityType::payloadFrequency::payloadCount. <p>
     * After the grouping, we need to add the group key to expression method parameters to identify each group uniquely
     * when processing the value expressions.
     * @return list of metrics grouped by group key
     */
    private Map<String, List<MetricDefinition>> getMetricThreadGroups() {
        Set<String> expressionsFilter = new HashSet<>(Arrays.asList("arithmeticSequence", "geometricSequence",
                "exponentialSequence", "logarithmicSequence", "arithmeticSequenceSummary", "geometricSequenceSummary",
                "exponentialSequenceSummary", "logarithmicSequenceSummary", "controlledRandom", "controlledRandomSummary",
                "absoluteSineSequence", "absoluteSineSequenceSummary", "absoluteCosineSequence",
                "absoluteCosineSequenceSummary", "absoluteTangentSequence", "absoluteTangentSequenceSummary"));
        Map<String, List<MetricDefinition>> metricThreadGroups = new HashMap<>();
        for (MetricDefinition eachMetric: metrics.getMetrics()) {
            Set<String> groupKeys = eachMetric.getReportingEntities();
            groupKeys.forEach(eachKey -> {
                eachKey = eachKey + "::" + eachMetric.getPayloadFrequencySeconds() + "::" + eachMetric.getPayloadCount();
                MetricDefinition metricDefinition = getMetricWithModifiedExpression(eachKey, eachMetric, expressionsFilter);
                if (metricDefinition != null) {
                    metricThreadGroups.putIfAbsent(eachKey, new ArrayList<>());
                    metricThreadGroups.get(eachKey).add(metricDefinition);
                }
            });
        }
        return metricThreadGroups;
    }

    private MetricDefinition getMetricWithModifiedExpression(String groupKey, MetricDefinition sourceMetric, Set<String> expressionsFilter) {
        MetricDefinition metricDefinition = null;
        try {
            metricDefinition = sourceMetric.clone();
            String expression = sourceMetric.getValueFunction();
            String expressionMethodName = expression.split("\\(")[0];
            if (expressionsFilter.contains(expressionMethodName)) {
                String modifiedExpression = expression.replace("(", "(\"" + requestID + "\", \"" + groupKey + "\", ");
                metricDefinition.setValueFunction(modifiedExpression);
            }
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            log.error(requestID + ": Failed to clone metric " + sourceMetric.getName() + ". Error: " +
                    cloneNotSupportedException.getMessage());
        }
        return metricDefinition;
    }

    private void initThreads() {
        MetricGeneratorThread metricGeneratorThread;
        for (Map.Entry<String, List<MetricDefinition>> eachGroup: metricThreadGroups.entrySet()) {
            metricGeneratorThread = new MetricGeneratorThread(eachGroup.getKey(), eachGroup.getValue(), payloadHandler, requestID);
            generatorState.getExecutorService().scheduleAtFixedRate(metricGeneratorThread, 0,
                    eachGroup.getValue().get(0).getPayloadFrequencySeconds(), TimeUnit.SECONDS);
            generatorState.getGeneratorThreadMap().put(eachGroup.getKey(), metricGeneratorThread);
            generatorState.getThreadPayloadCounts().put(eachGroup.getKey(), 0);
            log.debug(requestID + ": Scheduled metric data generator thread for group " + eachGroup.getKey());
        }
    }

}
