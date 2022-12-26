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

    public void validate(String requestID, Set<String> allResourceTypes) {
        if (payloadFrequencySeconds == null || payloadFrequencySeconds < 10) {
            throw new GeneratorException("Global payload generation frequency is not defined or is less than 10");
        }
        if (payloadCount == null || payloadCount < 1) {
            throw new GeneratorException("Global payload count is not defined or is less than 1");
        }
        maxPostSeconds = 0;
        for (MetricDefinition eachMetric: metrics) {
            eachMetric.validate(requestID, allResourceTypes, payloadFrequencySeconds, payloadCount);
            maxPostSeconds = Math.max(maxPostSeconds, (long) eachMetric.getPayloadFrequencySeconds() * eachMetric.getPayloadCount());
        }
    }
}
