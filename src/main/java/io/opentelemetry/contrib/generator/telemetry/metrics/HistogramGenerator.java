package io.opentelemetry.contrib.generator.telemetry.metrics;

import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.telemetry.metrics.dto.MetricDefinition;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.proto.metrics.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class to generate metric values of Histogram type.
 */
@Slf4j
public class HistogramGenerator {

    private final ExpressionProcessor jelProcessor;

    public HistogramGenerator(ExpressionProcessor jelProcessor) {
        this.jelProcessor = jelProcessor;
    }

    public Metric.Builder getOTelMetric(MetricDefinition metricDefinition) {
        return Metric.newBuilder()
                .setName(metricDefinition.getName())
                .setUnit(metricDefinition.getUnit())
                .setHistogram(getHistogramDataPoint(metricDefinition));

    }

    private Histogram getHistogramDataPoint(MetricDefinition metricDefinition) {
        long[] times = GeneratorUtils.normalizeTimestamp(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()),
                metricDefinition.getPayloadFrequencySeconds());
        List<Double> values = getCountValues(metricDefinition);
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        int count = (values.size());
        return Histogram.newBuilder()
                .setAggregationTemporality(metricDefinition.getAggregationTemporality())
                .addDataPoints(HistogramDataPoint.newBuilder()
                        .setCount(count)
                        .setSum(sum)
                        .setStartTimeUnixNano(times[0])
                        .setTimeUnixNano(times[1])
                        .addAllExplicitBounds(metricDefinition.getBounds())
                        .addAllBucketCounts(getBucketCounts(metricDefinition, values))
                        .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, metricDefinition.getAttributes()))
                        .build())
                .build();
    }

    private List<Double> getCountValues(MetricDefinition metricDefinition) {
        Object value = jelProcessor.eval(metricDefinition.getValueFunction());
        List<Object> rawValues = value instanceof List<?> ? (List<Object>) value : Collections.nCopies(5, value);
        return rawValues.stream().map(val -> Double.parseDouble(val.toString())).toList();
    }

    private List<Long> getBucketCounts(MetricDefinition metricDefinition, List<Double> values) {
        List<Long> bucketCounts = new ArrayList<>();
        for (int i = 0; i < metricDefinition.getBounds().size(); i++) {
            double lowerBound = i == 0 ? Double.MIN_VALUE : metricDefinition.getBounds().get(i - 1) + 1;
            double upperBound = i == metricDefinition.getBounds().size() - 1 ? Double.MAX_VALUE :
                    metricDefinition.getBounds().get(i);
            long count = countValuesInRange(values, lowerBound, upperBound);
            bucketCounts.add(count);
        }

        return bucketCounts;
    }

    private long countValuesInRange(List<Double> values, double lowerBound, double upperBound) {
        long count = 0;
        for (Double eachValue : values) {
            if (eachValue >= lowerBound && eachValue <= upperBound) {
                count++;
            }
        }
        return count;
    }


}
