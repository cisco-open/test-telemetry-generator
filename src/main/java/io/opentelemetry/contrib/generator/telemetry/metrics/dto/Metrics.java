package io.opentelemetry.contrib.generator.telemetry.metrics.dto;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class Metrics {

    private Integer payloadFrequencySeconds;
    private Integer payloadCount;
    private List<MetricDefinition> metrics;
    @JsonIgnore
    private long maxPostSeconds;

    public void validate(String requestID, Set<String> allEntityTypes) {
        if (payloadFrequencySeconds == null || payloadFrequencySeconds < 10) {
            throw new GeneratorException("Global payload generation frequency is not defined or is less than 10");
        }
        if (payloadCount == null || payloadCount < 1) {
            throw new GeneratorException("Global payload count is not defined or is less than 1");
        }
        maxPostSeconds = 0;
        for (MetricDefinition eachMetric: metrics) {
            eachMetric.validate(requestID, allEntityTypes, payloadFrequencySeconds, payloadCount);
            maxPostSeconds = Math.max(maxPostSeconds, (long) eachMetric.getPayloadFrequencySeconds() * eachMetric.getPayloadCount());
        }
    }
}
