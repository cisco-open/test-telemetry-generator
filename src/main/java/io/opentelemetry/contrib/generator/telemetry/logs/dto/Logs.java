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

    public void validate(String requestID, Set<String> allResourceTypes) {
        if (globalPayloadFrequencySeconds != null && globalPayloadFrequencySeconds < 10) {
            throw new GeneratorException("Global Payload generation frequency cannot be less than 10 second");
        }
        maxPostSeconds = 0;
        totalPayloadCount = 0;
        int logIndex = 0;

        for (LogDefinition eachLog: logs) {
            maxPostSeconds = Math.max(maxPostSeconds, eachLog.validate(requestID, allResourceTypes, globalPayloadFrequencySeconds));
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
