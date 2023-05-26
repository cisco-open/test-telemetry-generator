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

import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.MetricDefinition;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.Sum;

import java.util.concurrent.TimeUnit;

/**
 * Class to generate metric values of Sum type.
 */
public class SumGenerator {

    private final String requestID;
    private final ExpressionProcessor jelProcessor;

    public SumGenerator(String requestID, ExpressionProcessor jelProcessor) {
        this.requestID = requestID;
        this.jelProcessor = jelProcessor;
    }

    public Metric.Builder getOTelMetric(MetricDefinition metricDefinition) {
        Metric.Builder partialMetric = Metric.newBuilder().setName(metricDefinition.getName())
                .setUnit(metricDefinition.getUnit());
        return metricDefinition.getIsDouble() ?
                partialMetric.setSum(getDoubleSumDataPoint(metricDefinition)) :
                partialMetric.setSum(getIntSumDataPoint(metricDefinition));
    }

    private Sum getDoubleSumDataPoint(MetricDefinition metricDefinition) {
        long[] times = getTimes(metricDefinition);
        var value = Double.parseDouble(jelProcessor.eval(metricDefinition.getValueFunction()).toString());
        return Sum.newBuilder()
                .setAggregationTemporality(metricDefinition.getAggregationTemporality())
                .setIsMonotonic(metricDefinition.getIsMonotonic())
                .addDataPoints(NumberDataPoint.newBuilder()
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .setAsDouble(value)
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

    private Sum getIntSumDataPoint(MetricDefinition metricDefinition) {
        long[] times = getTimes(metricDefinition);
        Double value = Double.parseDouble(jelProcessor.eval(metricDefinition.getValueFunction()).toString());
        return Sum.newBuilder()
                .setAggregationTemporality(metricDefinition.getAggregationTemporality())
                .setIsMonotonic(metricDefinition.getIsMonotonic())
                .addDataPoints(NumberDataPoint.newBuilder()
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .setAsInt(value.longValue())
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

    private long[] getTimes(MetricDefinition metricDefinition) {
        long[] times = GeneratorUtils.normalizeTimestamp(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()),
                metricDefinition.getPayloadFrequencySeconds());
        if (metricDefinition.getAggregationTemporality() == AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE) {
            if (GeneratorsStateProvider.getMetricGeneratorState(requestID).getFixedStartTime() == 0) {
                GeneratorsStateProvider.getMetricGeneratorState(requestID).setFixedStartTime(times[0]);
            }
            times[0] = GeneratorsStateProvider.getMetricGeneratorState(requestID).getFixedStartTime();
        }
        return times;
    }
}
