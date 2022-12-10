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

package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.logs.LogGeneratorThread;
import io.opentelemetry.contrib.generator.telemetry.jel.JELProvider;
import io.opentelemetry.contrib.generator.telemetry.logs.dto.LogDefinition;
import io.opentelemetry.contrib.generator.telemetry.metrics.MetricGeneratorThread;
import jakarta.el.ELProcessor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestValueExpressions {

    private final ELProcessor jelProcessor = JELProvider.getJelProcessor();
    private final int[] VALUES_TO_CHECK = new int[]{0, 3, 6, 10, 13};
    private static final DecimalFormat doubleFormatter = new DecimalFormat("0.00");
    private final String GENERATOR_KEY = "ExpressionsTest";
    private final String LOG_NAME = "LogTest";
    private final String GENERATOR_THREAD_KEY = "TestValueExpressions";

    @BeforeClass
    public void setupCounter() {
        GeneratorState<MetricGeneratorThread> metricGeneratorState = new GeneratorState<>(null);
        metricGeneratorState.setThreadPayloadCounts(new ConcurrentHashMap<>());
        GeneratorsStateProvider.putMetricGeneratorState(GENERATOR_KEY, metricGeneratorState);

        GeneratorState<LogGeneratorThread> logGeneratorState = new GeneratorState<>(null);
        logGeneratorState.setThreadPayloadCounts(new ConcurrentHashMap<>());
        GeneratorsStateProvider.putLogGeneratorState(GENERATOR_KEY, logGeneratorState);
    }

    @Test
    public void testArithmeticSequence() {
        String expression = "arithmeticSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 3, 5, \"*2\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = (3 + (5 * eachIndex)) * 2;
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testGeometricSequence() {
        String expression = "geometricSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 1.5, 2.5, \"/3\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format(1.5D * Math.pow(2.5D, eachIndex) / 3D));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testExponentialSequence() {
        String expression = "exponentialSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 10, 3, \"*5/7\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format(10D * Math.exp(3D * eachIndex) * 5D / 7D));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testLogarithmicSequence() {
        String expression = "logarithmicSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 10, 10, \"*3\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format((10D + 10D * Math.log(eachIndex+1)) * 3D));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testAbsoluteSineSequence() {
        String expression = "absoluteSineSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*10\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.sin(eachIndex)) * 10));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testAbsoluteCosineSequence() {
        String expression = "absoluteCosineSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*25\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.cos(eachIndex)) * 25));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testAbsoluteTangentSequence() {
        String expression = "absoluteTangentSequence(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*100+15\")";
        for (int eachIndex: VALUES_TO_CHECK) {
            double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.tan(eachIndex)) * 100 + 15));
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            double actualValue = Double.parseDouble(jelProcessor.eval(expression).toString());
            Assert.assertEquals(actualValue, expectedValue, "Mismatch in values");
        }
    }

    @Test
    public void testArithmeticSequenceSummary() {
        String expression = "arithmeticSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 3, 5, \"*2\", 10)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 10).forEach(countVal -> {
                double expectedValue = (3 + (5 * countVal)) * 2;
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(obj.toString())).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testGeometricSequenceSummary() {
        String expression = "geometricSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 1.5, 2.5, \"/3\", 5)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 5).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format(1.5D * Math.pow(2.5D, countVal) / 3D));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testExponentialSequenceSummary() {
        String expression = "exponentialSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 10, 3, \"*5/7\", 3)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 3).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format(10D * Math.exp(3D * countVal) * 5D / 7D));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testLogarithmicSequenceSummary() {
        String expression = "logarithmicSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", 10, 10, \"*3\", 7)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 7).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format((10D + 10D * Math.log(countVal+1)) * 3D));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testAbsoluteSineSequenceSummary() {
        String expression = "absoluteSineSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*7\", 7)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 7).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.sin(countVal)) * 7));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testAbsoluteCosineSequenceSummary() {
        String expression = "absoluteCosineSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*30\", 10)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 10).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.cos(countVal)) * 30));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testAbsoluteTangentSequenceSummary() {
        String expression = "absoluteTangentSequenceSummary(\"" + GENERATOR_KEY + "\", \"" + GENERATOR_THREAD_KEY + "\", \"*100+15\", 5)";
        for (int eachIndex: VALUES_TO_CHECK) {
            List<Double> expectedValues = new ArrayList<>();
            IntStream.range(eachIndex, eachIndex + 5).forEach(countVal -> {
                double expectedValue = Double.parseDouble(doubleFormatter.format(Math.abs(Math.tan(countVal)) * 100 + 15));
                expectedValues.add(expectedValue);
            });
            GeneratorsStateProvider.getMetricGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(GENERATOR_THREAD_KEY, eachIndex);
            List<Double> actualValues = ((List<Object>) jelProcessor.eval(expression)).stream()
                    .map(obj -> Double.parseDouble(doubleFormatter.format(Double.parseDouble(obj.toString())))).collect(Collectors.toList());
            Assert.assertTrue(CollectionUtils.isEqualCollection(actualValues, expectedValues), "Mismatch in summary values. Actual: "
                    + StringUtils.join(actualValues) + "\nExpected: " + StringUtils.join(expectedValues));
        }
    }

    @Test
    public void testSeverityDistributionCount() {
        String expression = "severityDistributionCount(\"" + GENERATOR_KEY + "\", \"" + LOG_NAME + "\", [\"Val0\", \"Val1\", \"Val2\"], [2, 4, 3])";
        List<String> expected = new ArrayList<>(Arrays.asList("Val0", "Val1", "Val2", "Val0", "Val1"));
        for (var i=0; i<VALUES_TO_CHECK.length; i++) {
            GeneratorsStateProvider.getLogGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(LOG_NAME, VALUES_TO_CHECK[i]);
            String actual = jelProcessor.eval(expression).toString();
            Assert.assertEquals(actual, expected.get(i));
        }
    }

    @Test
    public void testSeverityDistributionPercentage() {
        String expression = "severityDistributionPercentage(\"" + GENERATOR_KEY + "\", \"" + LOG_NAME + "\", [\"Val0\", \"Val1\", \"Val2\"], [10, 70, 20])";
        List<String> expected = new ArrayList<>(Arrays.asList("Val0", "Val1", "Val1", "Val2", "Val1"));
        LogDefinition dummyDef = new LogDefinition();
        dummyDef.setName("DummyDef");
        dummyDef.setPayloadCount(14);
        LogGeneratorThread dummyThread = new LogGeneratorThread(dummyDef, new TestPayloadHandler(), GENERATOR_KEY);
        GeneratorsStateProvider.getLogGeneratorState(GENERATOR_KEY).setGeneratorThreadMap(new HashMap<>());
        GeneratorsStateProvider.getLogGeneratorState(GENERATOR_KEY).getGeneratorThreadMap().put(LOG_NAME, dummyThread);
        for (var i=0; i< VALUES_TO_CHECK.length; i++) {
            GeneratorsStateProvider.getLogGeneratorState(GENERATOR_KEY).getThreadPayloadCounts()
                    .put(LOG_NAME, VALUES_TO_CHECK[i]);
            String actual = jelProcessor.eval(expression).toString();
            Assert.assertEquals(actual, expected.get(i));
        }
    }
}
