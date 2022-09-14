package io.opentelemetry.contrib.generator.telemetry.logs.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Logs {

    private List<LogDefinition> logs;
    private Integer globalPayloadFrequencySeconds;
    @JsonIgnore
    private long maxPostSeconds;
    @JsonIgnore
    private Integer totalPayloadCount;

    public void validate(String requestID, Set<String> allEntityTypes) {
        if (globalPayloadFrequencySeconds != null && globalPayloadFrequencySeconds < 10) {
            throw new GeneratorException("Global Payload generation frequency cannot be less than 10 second");
        }
        maxPostSeconds = 0;
        totalPayloadCount = 0;
        for (LogDefinition eachLog: logs) {
            maxPostSeconds = Math.max(maxPostSeconds, eachLog.validate(requestID, allEntityTypes, globalPayloadFrequencySeconds));
            totalPayloadCount += eachLog.getPayloadCount();
        }
        validateUniqueLogName();
    }

    private void validateUniqueLogName() {
        Set<String> logDefinitionNames = logs.stream().map(LogDefinition::getName).collect(Collectors.toSet());
        if (logDefinitionNames.size() < logs.size()) {
            throw new GeneratorException("Duplicate Log Definition Name found.");
        }
    }
}
