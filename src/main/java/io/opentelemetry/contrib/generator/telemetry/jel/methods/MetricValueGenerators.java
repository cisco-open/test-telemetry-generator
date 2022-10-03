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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * This holds the implementation for the supported metric value expressions specified in the metric definition YAML. Since we are using ELProcessor
 * in a standalone context to process the expressions, all the implementing methods have to be public static.
 * @see ELProcessor
 */
@SuppressWarnings("unused")
public class MetricValueGenerators {

    private static final ELProcessor jelProcessor = new ELProcessor();
    private static final ConcurrentMap<String, Double> controlledRandom = new ConcurrentHashMap<>();
    private static final DecimalFormat formatter = new DecimalFormat("##.##");

    private MetricValueGenerators() {}

    /**
     * Gets the Nth value in an arithmetic progression where N = current payload count. The value thus obtained can be
     * further modified with a basic arithmetic expression. <p>
     * For eg: arithmeticSequence(3, 2, "*5") produces: <p>
     * First payload → (3 + (2*0)) * 5 = 15 <p>
     * Second payload → (3 + (2*1)) * 5 = 25 <p>
     * Third payload → (3 + (2*2)) * 5 = 35 and so on.
     */
    public static Object arithmeticSequence(String requestID, String groupKey, double startWith, double changeBy, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = startWith + (changeBy * currentCount);
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the arithmeticSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> arithmeticSequenceSummary(String requestID, String groupKey, double startWith, double changeBy,
                                                         String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = startWith + (changeBy * countVal);
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the Nth value in a geometric progression where N = current payload count. The value thus obtained can be
     * further modified with a basic arithmetic expression. <p>
     * For eg: geometricSequence(3, 2, "*5") produces: <p>
     * First payload → (3 * (2^0)) * 5 = 15 <p>
     * Second payload → (3 * (2^1)) * 5 = 30 <p>
     * Third payload → (3 * (2^2)) * 5 = 60 and so on.
     */
    public static Object geometricSequence(String requestID, String groupKey, double startWith, double commonRatio, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = startWith * Math.pow(commonRatio, currentCount);
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the geometricSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> geometricSequenceSummary(String requestID, String groupKey, double startWith, double commonRatio,
                                                        String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = startWith * Math.pow(commonRatio, countVal);
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the Nth value in an exponential progression where N = current payload count. The value thus obtained can be
     * further modified with a basic arithmetic expression. <p>
     * For eg: exponentialSequence(3, 2, "*5") produces: <p>
     * First payload → (3 * e^(2*0)) * 5 = 15 <p>
     * Second payload → (3 * e^(2*1)) * 5 = 110.83 <p>
     * Third payload → (3 * e^(2*2)) * 5 = 818.97 and so on.
     */
    public static Object exponentialSequence(String requestID, String groupKey, double startWith, double growthRate, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = startWith * Math.exp(growthRate * currentCount);
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the exponentialSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> exponentialSequenceSummary(String requestID, String groupKey, double startWith, double growthRate,
                                                          String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = startWith * Math.exp(growthRate * countVal);
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the Nth value in a logarithmic progression where N = current payload count + 1 since log(0) leads to weird things.
     * The value thus obtained can be further modified with a basic arithmetic expression. <p>
     * For eg: logarithmicSequence(3, 2, "*5") produces: <p>
     * First payload → (3 + 2*log(1)) * 5 = 15 <p>
     * Second payload → (3 + 2*log(2)) * 5 = 21.93 <p>
     * Third payload → (3 + 2*log(3)) * 5 = 25.98 and so on.
     */
    public static Object logarithmicSequence(String requestID, String groupKey, double startWith, double growthRate, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = startWith + growthRate * Math.log(currentCount+1);
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the logarithmicSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> logarithmicSequenceSummary(String requestID, String groupKey, double startWith, double growthRate,
                                                          String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = startWith + growthRate * Math.log(countVal+1);
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the value of Math.abs(Math.sin(N)) where N = current payload count. The value obtained can be further modified
     * with a basic arithmetic expression. <p>
     * For eg: absoluteSineSequence("*7000") produces: <p>
     * First payload → Math.abs(Math.sin(0)) * 7000 = 0 <p>
     * Second payload → Math.abs(Math.sin(1)) * 7000 = 5890.3 <p>
     * Third payload → Math.abs(Math.sin(2)) * 7000 = 6365.08
     */
    public static Object absoluteSineSequence(String requestID, String groupKey, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = Math.abs(Math.sin(currentCount));
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the absoluteSineSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> absoluteSineSequenceSummary(String requestID, String groupKey, String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = Math.abs(Math.sin(countVal));
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the value of Math.abs(Math.cos(N)) where N = current payload count. The value obtained can be further
     * modified with a basic arithmetic expression. <p>
     * For eg: absoluteCosineSequence("*50") <p>
     * First payload → Math.abs(Math.cos(0)) * 50 = 50 <p>
     * Second payload → Math.abs(Math.cos(1)) * 50 = 27.02 <p>
     * Third payload → Math.abs(Math.cos(2)) * 50 =20.81
     */
    public static Object absoluteCosineSequence(String requestID, String groupKey, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = Math.abs(Math.cos(currentCount));
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the absoluteCosineSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> absoluteCosineSequenceSummary(String requestID, String groupKey, String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = Math.abs(Math.cos(countVal));
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets the value of Math.abs(Math.tan(N)) where N = current payload count. The value obtained can be further
     * modified with a basic arithmetic expression. <p>
     * For eg: absoluteTangentSequence("*20+3") <p>
     * First payload → Math.abs(Math.tan(0)) * 20 + 3 = 3 <p>
     * Second payload → Math.abs(Math.tan(1)) * 20 + 3 = 34.15 <p>
     * Third payload → Math.abs(Math.tan(2)) * 20 + 3 = 46.7
     */
    public static Object absoluteTangentSequence(String requestID, String groupKey, String applyExpression) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        double baseValue = Math.abs(Math.tan(currentCount));
        return formatter.format(jelProcessor.eval(baseValue + applyExpression));
    }

    /**
     * Summary variant of the absoluteTangentSequence expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> absoluteTangentSequenceSummary(String requestID, String groupKey, String applyExpression, int count) {
        int currentCount = GeneratorsStateProvider.getMetricGeneratorState(requestID).getThreadPayloadCounts().get(groupKey);
        List<Object> values = new ArrayList<>();
        IntStream.range(currentCount, currentCount+count).forEach(countVal -> {
            double baseValue = Math.abs(Math.tan(countVal));
            values.add(formatter.format(jelProcessor.eval(baseValue + applyExpression)));
        });
        return values;
    }

    /**
     * Gets a random value between the specified MIN (inclusive) & MAX (exclusive) double values. The value obtained can be
     * further modified with a basic arithmetic expression.
     */
    public static Object random(double minValue, double maxValue, String applyExpression) {
        return formatter.format(jelProcessor.eval(ThreadLocalRandom.current().nextDouble(minValue, maxValue) + applyExpression));
    }

    /**
     * Summary variant of the random expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> randomSummary(double minValue, double maxValue, String applyExpression, int count) {
        List<Object> values = new ArrayList<>();
        IntStream.range(0, count).forEach(countVal -> values.add(random(minValue, maxValue, applyExpression)));
        return values;
    }

    /**
     * A controlled variant of random function which generates a random value between the specified range for the first
     * term but all the subsequent values generated in the sequence are always in between the -20% to +20% range of
     * their previous value.
     */
    public static Object controlledRandom(String requestID, String groupKey, double minValue, double maxValue, String applyExpression) {
        String expressionId = requestID + ":" + groupKey + ":" + minValue + ":" + maxValue + ":" + applyExpression;
        controlledRandom.putIfAbsent(expressionId, ThreadLocalRandom.current().nextDouble(minValue, maxValue));
        double prevValue = controlledRandom.get(expressionId);
        var newValue = ThreadLocalRandom.current().nextDouble(prevValue * 0.8D, prevValue * 1.2D);
        controlledRandom.put(expressionId, (Double) jelProcessor.eval(newValue + applyExpression));
        return formatter.format(controlledRandom.get(expressionId));
    }

    /**
     * Summary variant of the controlledRandom expression implementation. This will return the values in the same way
     * but will return count values as specified in the parameter instead of a single value.
     */
    public static List<Object> controlledRandomSummary(String requestID, String groupKey, double minValue, double maxValue,
                                                       String applyExpression, int count) {
        List<Object> values = new ArrayList<>();
        IntStream.range(0, count).forEach(countVal -> values.add(controlledRandom(requestID, groupKey, minValue, maxValue, applyExpression)));
        return values;
    }

}
