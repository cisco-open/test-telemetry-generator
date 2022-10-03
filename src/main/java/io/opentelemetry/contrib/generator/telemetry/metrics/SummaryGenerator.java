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
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.Summary;
import io.opentelemetry.proto.metrics.v1.SummaryDataPoint;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class to generate metric values of Summary type.
 */
@Slf4j
public class SummaryGenerator {

    private final ELProcessor jelProcessor;

    public SummaryGenerator(ELProcessor jelProcessor) {
        this.jelProcessor = jelProcessor;
    }

    public Metric getOTelMetric(MetricDefinition metricDefinition) {
        return Metric.newBuilder()
                .setName(metricDefinition.getName())
                .setUnit(metricDefinition.getUnit())
                .setSummary(getDoubleSummaryDataPoint(metricDefinition))
                .build();
    }

    private Summary getDoubleSummaryDataPoint(MetricDefinition metricDefinition) {
        long[] times = GeneratorUtils.normalizeTimestamp(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()),
                metricDefinition.getPayloadFrequencySeconds());
        List<Object> values = getCountValues(metricDefinition);
        double sum = getValuesSum(values);
        int count = (values.size());
        return Summary.newBuilder()
                .addDataPoints(SummaryDataPoint.newBuilder()
                        .setCount(count)
                        .setSum(sum)
                        .addAllQuantileValues(getQuantiles(values, metricDefinition))
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

    private List<Object> getCountValues(MetricDefinition metricDefinition) {
        Object value = jelProcessor.eval(metricDefinition.getValueFunction());
        if (value instanceof List) {
            //noinspection unchecked
            return (List<Object>) value;
        }
        log.warn("Non summary variant used in valueFunction for summary metric '" + metricDefinition.getName() + "' " +
                "copying the single value generated 5 times");
        return Collections.nCopies(5, value);
    }

    private double getValuesSum(List<Object> values) {
        return values.stream().map(value -> Double.parseDouble(value.toString())).mapToDouble(Double::doubleValue).sum();
    }

    private List<SummaryDataPoint.ValueAtQuantile> getQuantiles(List<Object> values, MetricDefinition metricDefinition) {
        List<Double> vals = values.stream().map(value -> Double.parseDouble(value.toString())).sorted().collect(Collectors.toList());
        double normalizedQuantile;
        List<SummaryDataPoint.ValueAtQuantile> quantiles = new ArrayList<>();
        for (Double eachQuantile: metricDefinition.getQuantiles()) {
            normalizedQuantile = eachQuantile > 1 ? eachQuantile/100 : eachQuantile;
            double valueAtQuantile = getValueAtPercentile(normalizedQuantile, vals);
            quantiles.add(SummaryDataPoint.ValueAtQuantile.newBuilder()
                    .setQuantile(metricDefinition.getIsDouble() ? normalizedQuantile : eachQuantile)
                    .setValue(valueAtQuantile)
                    .build());
        }
        return quantiles;
    }

    private double getValueAtPercentile(double percentile, List<Double> values) {
        return values.get((int) Math.round(percentile * (values.size() - 1)));
    }

}
