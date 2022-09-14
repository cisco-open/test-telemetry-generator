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

    public Metric getOTelMetric(MetricDefinition metricDefinition) {
        Metric.Builder partialMetric = Metric.newBuilder().setName(metricDefinition.getName())
                .setUnit(metricDefinition.getUnit());
        return metricDefinition.getIsDouble() ?
                partialMetric.setGauge(getDoubleGaugeDataPoint(metricDefinition)).build() :
                partialMetric.setGauge(getIntGaugeDataPoint(metricDefinition)).build();
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
