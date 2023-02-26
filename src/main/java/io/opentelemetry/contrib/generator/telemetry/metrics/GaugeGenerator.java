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

import io.opentelemetry.contrib.generator.telemetry.metrics.dto.MetricDefinition;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import jakarta.el.ELProcessor;

import java.util.concurrent.TimeUnit;

/**
 * Class to generate metric values of Gauge type.
 */
public class GaugeGenerator {

    private final ELProcessor jelProcessor;

    public GaugeGenerator(ELProcessor jelProcessor) {
        this.jelProcessor = jelProcessor;
    }

    public Metric.Builder getOTelMetric(MetricDefinition metricDefinition) {
        Metric.Builder partialMetric = Metric.newBuilder().setName(metricDefinition.getName())
                .setUnit(metricDefinition.getUnit());
        return metricDefinition.getIsDouble() ?
                partialMetric.setGauge(getDoubleGaugeDataPoint(metricDefinition)) :
                partialMetric.setGauge(getIntGaugeDataPoint(metricDefinition));
    }

    private Gauge getDoubleGaugeDataPoint(MetricDefinition metricDefinition) {
        long[] times = GeneratorUtils.normalizeTimestamp(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()),
                metricDefinition.getPayloadFrequencySeconds());
        var value = Double.parseDouble(jelProcessor.eval(metricDefinition.getValueFunction()).toString());
        return Gauge.newBuilder()
                .addDataPoints(NumberDataPoint.newBuilder()
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .setAsDouble(value)
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

    private Gauge getIntGaugeDataPoint(MetricDefinition metricDefinition) {
        long[] times = GeneratorUtils.normalizeTimestamp(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()),
                metricDefinition.getPayloadFrequencySeconds());
        Double value = Double.parseDouble(jelProcessor.eval(metricDefinition.getValueFunction()).toString());
        return Gauge.newBuilder()
                .addDataPoints(NumberDataPoint.newBuilder()
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .setAsInt(value.longValue())
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

}
