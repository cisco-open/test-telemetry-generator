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

package io.opentelemetry.contrib.generator.telemetry.jel.methods;

import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This holds the implementation for the supported severity expressions specified in the log definition YAML. Since we are using ELProcessor
 * in a standalone context to process the expressions, all the implementing methods have to be public static.
 * @see ELProcessor
 */
@Slf4j
public class LogSeverityGenerator {

    private LogSeverityGenerator() {}

    /**
     * Provides a severity value from the specified severity list based on the frequency specified for each severity.
     * Fo eg: severityDistributionCount(["Normal", "Severe", "Warning"], [25, 5, 10])
     * In this case, the first 25 events generated will be of Normal severity, followed by 5 Severe events and
     * finally 10 Warning events. The cycle repeats itself in the same order.
     * @param requestID internal ID for each running generator instance
     * @param logName name of the log in log definition
     * @param severity list severities
     * @param frequency corresponding list of frequencies
     * @return severity
     */
    public static String severityDistributionCount(String requestID, String logName, List<String> severity, List<Long> frequency) {
        int currentPayloadCount = GeneratorsStateProvider.getLogGeneratorState(requestID).getThreadPayloadCounts().get(logName) + 1;
        int totalFreqs = frequency.stream().mapToInt(Long::intValue).sum();
        int iterationPayloadCount = currentPayloadCount % totalFreqs;
        if (iterationPayloadCount == 0) {
            return severity.get(0);
        }
        int index = 0;
        while (iterationPayloadCount > frequency.get(index)) {
            iterationPayloadCount = iterationPayloadCount - frequency.get(index).intValue();
            index++;
        }
        return severity.get(index);
    }

    /**
     * Provides a severity value from the specified severity list based on the percentages specified for each severity.
     * Fo eg: severityDistributionPercentage(["Normal", "Severe", "Warning"], [20, 70, 10])
     * In this case, if the payload count equals 10 then, 2 events will be of type Normal, 7 will be of type Severe and 1 will be of type Warning.
     * Note: The sum of frequency percentages in percentageValue should always be equal to 100.
     * @param requestID internal ID for each running generator instance
     * @param logName name of the log in log definition
     * @param severity list severities
     * @param percentages corresponding list of percentages
     * @return severity
     */
    public static String severityDistributionPercentage(String requestID, String logName, List<String> severity, List<Long> percentages) {
        int totalPayloadCount = GeneratorsStateProvider.getLogGeneratorState(requestID).getGeneratorThreadMap()
                .get(logName).getLogDefinition().getPayloadCount();
        List<Long> frequencies = percentages.stream()
                .map(pctg -> (totalPayloadCount * pctg) / 100)
                .collect(Collectors.toList());
        return severityDistributionCount(requestID, logName, severity, frequencies);
    }
}
