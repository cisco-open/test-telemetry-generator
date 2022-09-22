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

package io.opentelemetry.contrib.generator.telemetry.misc;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.core.utils.CommonUtils;

import io.opentelemetry.proto.common.v1.KeyValue;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

@Slf4j
public class GeneratorUtils {

    private GeneratorUtils() {}

    /**
     * Changes millis unit to nano unit timestamp and normalizes it using the input frequency.
     * @param timestamp        Input timestamp in milliseconds
     * @param frequencySeconds Frequency in seconds
     * @return Normalized timestamp in nanoseconds
     */
    public static long[] normalizeTimestamp(long timestamp, int frequencySeconds) {
        long freqNano = frequencySeconds * (long) Math.pow(10, 9);
        long normalized = timestamp - (timestamp % freqNano);
        return new long[]{normalized - freqNano, normalized};
    }

    public static Map<String, Object> validateAttributes(Map<String, Object> inputAttributes) {
        Map<String, Object> attrs = new HashMap<>();
        for (Map.Entry<String, Object> eachAttribute : MapUtils.emptyIfNull(inputAttributes).entrySet()) {
            if (eachAttribute.getKey().trim().length() == 0) {
                throw new GeneratorException("Blank key found in 'attributes'");
            }
            attrs.put(eachAttribute.getKey().trim(), eachAttribute.getValue());
        }
        return attrs;
    }

    public static Map<String, Object> addArgsToAttributeExpressions(String requestID, String objectType, String objectID,
                                                                    Map<String, Object> inputAttributes) {
        String counterMethod = "counter(";
        String UUIDMethod = "UUIDFromStringCounter(";
        String roundRobinMethod = "roundRobin(";
        String alphanumericMethod = "alphanumericSequence(";
        String alphanumericFromEnvMethod = "alphanumericSequenceFromEnv(";
        String ipv4Method = "IPv4Sequence(";
        //For MELT attribute generators to work, we need to add 4 arguments to each user supplied expression:
        // - requestID
        // - objectType - span/metric/log
        // - objectId - span name/metric name/log name
        // - attribute name
        //Using the above 4, a key is created to uniquely identify each expression's previous value.
        Map<String, Object> attrs = new HashMap<>();
        String replacementPrefix = "\"" + requestID + "\", \"" + objectType + "\", \"" + objectID + "\", ";
        for (Map.Entry<String, Object> attrNameExpression: MapUtils.emptyIfNull(inputAttributes).entrySet()) {
            String expression = attrNameExpression.getValue().toString();
            String replacement = replacementPrefix + "\"" + attrNameExpression.getKey() + "\", ";
            String modifiedExpression = expression.replace(counterMethod, counterMethod + replacement)
                    .replace(UUIDMethod, UUIDMethod + replacement)
                    .replace(roundRobinMethod, roundRobinMethod + replacement)
                    .replace(alphanumericMethod, alphanumericMethod + replacement)
                    .replace(alphanumericFromEnvMethod, alphanumericFromEnvMethod + replacement)
                    .replace(ipv4Method, ipv4Method + replacement);
            attrs.put(attrNameExpression.getKey(), modifiedExpression);
        }
        return attrs;
    }

    public static List<KeyValue> getEvaluatedAttributes(ELProcessor jelProcessor, Map<String, Object> attributesDefinitions) {
        List<KeyValue> attributes = new ArrayList<>();
        KeyValue eachAttribute;
        for (Map.Entry<String, Object> definedAttributes: MapUtils.emptyIfNull(attributesDefinitions).entrySet()) {
            eachAttribute = KeyValue.newBuilder()
                    .setKey(definedAttributes.getKey())
                    .setValue(CommonUtils.buildAnyValue(jelProcessor.eval(definedAttributes.getValue().toString())))
                    .build();
            attributes.add(eachAttribute);
        }
        return attributes;
    }

}
