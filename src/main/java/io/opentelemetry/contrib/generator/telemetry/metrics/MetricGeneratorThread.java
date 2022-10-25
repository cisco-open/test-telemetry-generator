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

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.EntityModelProvider;
import io.opentelemetry.contrib.generator.telemetry.jel.JELProvider;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.MetricDefinition;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main thread class which generates and posts metric packets to the specified destination via PayloadHandler.
 */
@Slf4j
public class MetricGeneratorThread implements Runnable {

    private final String groupKey;
    private final String requestID;
    private final List<MetricDefinition> metrics;
    private final PayloadHandler payloadHandler;
    private final GeneratorState<MetricGeneratorThread> metricGeneratorState;
    private final GaugeGenerator gaugeGenerator;
    private final SumGenerator sumGenerator;
    private final SummaryGenerator summaryGenerator;
    private int currentCount;

    public MetricGeneratorThread(String groupKey, List<MetricDefinition> metrics, PayloadHandler payloadHandler,
                                 String requestID) {
        this.groupKey = groupKey;
        this.requestID = requestID;
        this.metrics = metrics;
        this.payloadHandler = payloadHandler;
        this.metricGeneratorState = GeneratorsStateProvider.getMetricGeneratorState(requestID);
        ELProcessor jelProcessor = JELProvider.getJelProcessor();
        gaugeGenerator = new GaugeGenerator(jelProcessor);
        sumGenerator = new SumGenerator(requestID, jelProcessor);
        summaryGenerator = new SummaryGenerator(jelProcessor);
        currentCount = 0;
    }

    @Override
    public void run() {
        log.debug(requestID + ": Metric generator thread invoked for entity type: " + groupKey + " with metrics: " +
                metrics.stream().map(MetricDefinition::getName).collect(Collectors.toList()));
        if (metricGeneratorState.isGenerateData() && currentCount < metrics.get(0).getPayloadCount()) {
            List<ResourceMetrics> resourceMetricsList = new ArrayList<>();
            ResourceMetrics resourceMetric;
            List<Metric> otelMetrics = metrics.stream()
                    .map(this::getMetric).collect(Collectors.toList());
            List<Resource> entities = EntityModelProvider.getEntityModel(requestID).get(groupKey.split("::")[0]).stream()
                    .filter(GeneratorEntity::isActive)
                    .map(GeneratorEntity::getOTelResource)
                    .collect(Collectors.toList());
            log.debug(requestID + ": Preparing " + entities.size() + " resource metric packets for " + groupKey);
            for (Resource eachResource: entities) {
                resourceMetric = ResourceMetrics.newBuilder()
                        .setResource(eachResource)
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .setScope(InstrumentationScope.newBuilder()
                                        .setName("@opentelemetry/test-telemetry-generator")
                                        .setVersion("22.10.0")
                                        .build())
                                .addAllMetrics(otelMetrics)
                                .build())
                        .build();
                resourceMetricsList.add(resourceMetric);
            }
            ExportMetricsServiceRequest resourceMetrics = ExportMetricsServiceRequest.newBuilder().addAllResourceMetrics(resourceMetricsList).build();
            log.info(requestID + ": Sending payload for: " + groupKey);
            log.debug(requestID + ": Complete payload for " + groupKey + ": " + resourceMetrics);
            boolean responseStatus = payloadHandler.postPayload(resourceMetrics);
            if (metricGeneratorState.getTransportStorage() != null) {
                metricGeneratorState.getTransportStorage().store(groupKey, resourceMetrics, responseStatus);
            }
            currentCount++;
            metricGeneratorState.getThreadPayloadCounts().put(groupKey, currentCount);
        }
    }

    private Metric getMetric(MetricDefinition metricDefinition) {
        switch (metricDefinition.getOtelType()) {
            case Constants.GAUGE:
                return gaugeGenerator.getOTelMetric(metricDefinition);
            case Constants.SUM:
                return sumGenerator.getOTelMetric(metricDefinition);
            default:
                return summaryGenerator.getOTelMetric(metricDefinition);
        }
    }
}
