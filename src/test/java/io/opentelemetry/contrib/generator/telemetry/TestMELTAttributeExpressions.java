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

import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.core.jel.methods.ResourceModelExpressions;
import io.opentelemetry.contrib.generator.telemetry.jel.MELTExpressionsJELProvider;
import io.opentelemetry.contrib.generator.telemetry.jel.methods.MELTAttributeGenerators;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.IntStream;

public class TestMELTAttributeExpressions {

    private final ExpressionProcessor jelProcessor = MELTExpressionsJELProvider.getJelProcessor();
    private final String requestID = UUID.randomUUID().toString();

    @Test
    public void testCounter() {
        String inputExpression = "counter(\"" + requestID + "\", \"log\", \"k8slog\", \"source.name\", \"test-\")";
        IntStream.range(1, 5).forEach(i -> jelProcessor.eval(inputExpression));
        Assert.assertEquals(jelProcessor.eval(inputExpression), "test-5");
        String inputExpression2 = "counter(\"" + requestID + "\", \"log\", \"k8slog2\", \"source.name\", \"test-\")";
        Assert.assertEquals(jelProcessor.eval(inputExpression2), "test-1");
    }

    @Test
    public void testAlphanumericSequenceFromEnv() {
        String randomString = RandomStringUtils.randomAlphanumeric(16);
        System.setProperty(Constants.ENV_ALPHANUMERIC, randomString);
        String inputExpression = "alphanumericSequenceFromEnv(\"" + requestID + "\", \"log\", \"k8slog\", \"source.name\")";
        Assert.assertEquals(jelProcessor.eval(inputExpression), System.getProperty(Constants.ENV_ALPHANUMERIC));
        System.setProperty(Constants.ENV_ALPHANUMERIC, "d2gd9W");
        String inputExpression2 = "alphanumericSequenceFromEnv(\"" + requestID + "\", \"log\", \"k8slog2\", \"source.name\")";
        List<String> outputs2 = Arrays.asList("d2gd9W", "d2gd9X", "d2gd9Y", "d2gd9Z", "d2gda0", "d2gda1", "d2gda2", "d2gda3", "d2gda4", "d2gda5");
        for (int i=0; i<10; i++) {
            Assert.assertEquals(jelProcessor.eval(inputExpression2), outputs2.get(i), "Mismatch at index " + i);
        }
    }

    @Test
    public void testUUIDFromStringCounter() {
        String inputExpression = "UUIDFromStringCounter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"service.id\", \"service-\")";
        String inputExpression2 = "UUIDFromStringCounter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"service.name\", \"servixe-\")";
        int size = 1000;
        Set<String> UUIDs1 = new HashSet<>();
        for (int i=0; i<size; i++) {
            UUIDs1.add(jelProcessor.eval(inputExpression).toString());
            UUIDs1.add(jelProcessor.eval(inputExpression2).toString());
        }
        //Check all are unique
        Assert.assertEquals(UUIDs1.size(), size*2, "UUIDs generated were not all unique");
        MELTAttributeGenerators.resetCaches();
        Set<String> UUIDs2 = new HashSet<>();
        for (int i=0; i<size; i++) {
            UUIDs2.add(jelProcessor.eval(inputExpression).toString());
            UUIDs2.add(jelProcessor.eval(inputExpression2).toString());
        }
        //Check they are deterministic
        Assert.assertEquals(UUIDs1, UUIDs2, "UUIDs are not deterministic");
    }

    @Test
    public void testRoundRobin() {
        String expression1 = "roundRobin(\"" + requestID + "\", \"log\", \"log1\", \"log-attr\", [\"FIXED\"])";
        String expression2 = "roundRobin(\"" + requestID + "\", \"log\", \"log2\", \"log-attr2\", [\"VAL0\", \"VAL1\", \"VAL2\", \"VAL3\"])";
        for (int i=0; i<5; i++) {
            Assert.assertEquals(jelProcessor.eval(expression1), "FIXED");
            int num = i > 3 ? 0 : i;
            Assert.assertEquals(jelProcessor.eval(expression2), "VAL" + num);
        }
    }

    @Test
    public void testAlphanumericSequence() {
        String expression1 = "alphanumericSequence(\"" + requestID + "\", \"span\", \"serviceProcessing\", \"service.id\", \"af1z8x\")";
        List<String> outputs1 = Arrays.asList("af1z8x", "af1z8y", "af1z8z", "af1z8A", "af1z8B", "af1z8C", "af1z8D", "af1z8E", "af1z8F", "af1z8G");
        String expression2 = "alphanumericSequence(\"" + requestID + "\", \"span\", \"serviceProcessing\", \"service.instance.id\", \"sdc3a7\")";
        List<String> outputs2 = Arrays.asList("sdc3a7", "sdc3a8", "sdc3a9", "sdc3aa", "sdc3ab", "sdc3ac", "sdc3ad", "sdc3ae", "sdc3af", "sdc3ag");
        String expression3 = "alphanumericSequence(\"" + requestID + "\", \"span\", \"serviceGetDetail\", \"service.id\", \"d2gd9W\")";
        List<String> outputs3 = Arrays.asList("d2gd9W", "d2gd9X", "d2gd9Y", "d2gd9Z", "d2gda0", "d2gda1", "d2gda2", "d2gda3", "d2gda4", "d2gda5");
        String expression4 = "alphanumericSequence(\"" + requestID + "-1\", \"span\", \"serviceProcessing\", \"service.id\", \"ZZZZZX\")";
        List<String> outputs4 = Arrays.asList("ZZZZZX", "ZZZZZY", "ZZZZZZ", "0000000", "0000001", "0000002", "0000003", "0000004", "0000005", "0000006");
        for (int i=0; i<10; i++) {
            Assert.assertEquals(jelProcessor.eval(expression1), outputs1.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression2), outputs2.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression3), outputs3.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression4), outputs4.get(i), "Mismatch at index " + i);
        }
    }

    @Test
    public void testIPv4Sequence() {
        String expression1 = "IPv4Sequence(\"" + requestID + "\", \"span\", \"incomingHTTPRequest\", \"incoming.ip\", \"10.10.10.1\")";
        List<String> outputs1 = new ArrayList<>();
        IntStream.range(1, 11).forEach(i -> outputs1.add("10.10.10." + i));
        String expression2 = "IPv4Sequence(\"" + requestID + "\", \"span\", \"incomingHTTPRequest\", \"incoming.host.ip\", \"128.10.13.253\")";
        List<String> outputs2 = new ArrayList<>();
        IntStream.range(253, 256).forEach(i -> outputs2.add("128.10.13." + i));
        IntStream.range(1, 8).forEach(i -> outputs2.add("128.10.14." + i));
        String expression3 = "IPv4Sequence(\"" + requestID + "\", \"span\", \"incomingRequest\", \"incoming.host.ip\", \"255.255.255.250\")";
        List<String> outputs3 = new ArrayList<>();
        IntStream.range(250, 256).forEach(i -> outputs3.add("255.255.255." + i));
        IntStream.range(10, 16).forEach(i -> outputs3.add("10.10.10." + i));
        for (int i=0; i<10; i++) {
            Assert.assertEquals(jelProcessor.eval(expression1), outputs1.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression2), outputs2.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression3), outputs3.get(i), "Mismatch at index " + i);
        }
    }

    @Test
    public void testList() {
        String simpleExpression = "[counter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"splexpr\", \"abc\"), " +
                "alphanumericSequence(\"" + requestID + "\", \"metric\", \"cpu.used\", \"spleXpr\", \"abc\"), " +
                "roundRobin(\"" + requestID + "\", \"metric\", \"cpu.used\", \"spLexpr\", [\"abc\"])]";
        String concatenatedExpression = "[counter(\"" + requestID + "\", \"metric\", \"memory.used\", \"cnctexpr\", \"c.abc\")" +
                ".concat(roundRobin(\"" + requestID + "\", \"metric\", \"memory.used\", \"cncteXpr\", [\"-c1\", \"-c2\"])), " +
                "\"concat-\".concat(alphanumericSequence(\"" + requestID + "\", \"metric\", \"memory.used\", \"cncTexpr\", \"c.abc\"))]";
        String nestedExpression = "[counter(\"" + requestID + "\", \"log\", \"k8slog\", \"nstdexpr\", \"n.abc\"), " +
                "[counter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"nstdeXpr\", \"n1.abc\"), " +
                "roundRobin(\"" + requestID + "\", \"metric\", \"cpu.used\", \"nstDexpr\", [\"n1.abc\"])], " +
                "\"nested-\".concat(IPv4Sequence(\"" + requestID + "\", \"metric\", \"cpu.used\", \"nStdexpr\", \"10.10.11.1\"))]";
        List<List<Object>> simpleExpectedOutputs = Arrays.asList(Arrays.asList("abc1", "abc", "abc"),
                Arrays.asList("abc2", "abd", "abc"), Arrays.asList("abc3", "abe", "abc"),
                Arrays.asList("abc4", "abf", "abc"), Arrays.asList("abc5", "abg", "abc"));
        List<List<Object>> concatenatedExpectedOutputs = Arrays.asList(Arrays.asList("c.abc1-c1", "concat-c.abc"),
                Arrays.asList("c.abc2-c2", "concat-c.abd"), Arrays.asList("c.abc3-c1", "concat-c.abe"),
                Arrays.asList("c.abc4-c2", "concat-c.abf"), Arrays.asList("c.abc5-c1", "concat-c.abg"));
        List<List<Object>> nestedExpectedOutputs = Arrays.asList(Arrays.asList("n.abc1", Arrays.asList("n1.abc1", "n1.abc"), "nested-10.10.11.1"),
                Arrays.asList("n.abc2", Arrays.asList("n1.abc2", "n1.abc"), "nested-10.10.11.2"),
                Arrays.asList("n.abc3", Arrays.asList("n1.abc3", "n1.abc"), "nested-10.10.11.3"),
                Arrays.asList("n.abc4", Arrays.asList("n1.abc4", "n1.abc"), "nested-10.10.11.4"),
                Arrays.asList("n.abc5", Arrays.asList("n1.abc5", "n1.abc"), "nested-10.10.11.5"));
        IntStream.range(0, 5).forEach(i -> {
            String simpleOutput = StringUtils.join((List<String>) jelProcessor.eval(simpleExpression));
            String concatenatedOutput = StringUtils.join((List<String>) jelProcessor.eval(concatenatedExpression));
            String nestedOutput = StringUtils.join((List<String>) jelProcessor.eval(nestedExpression));
            Assert.assertEquals(simpleOutput, StringUtils.join(simpleExpectedOutputs.get(i)));
            Assert.assertEquals(concatenatedOutput, StringUtils.join(concatenatedExpectedOutputs.get(i)));
            Assert.assertEquals(nestedOutput, StringUtils.join(nestedExpectedOutputs.get(i)));
        });
    }

    @Test
    public void testMap() {
        ResourceModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "{\"name\": alphanumericSequence(\"" + requestID + "\", \"metric\", \"cpu.used\", \"mapexpr\", \"abM\")" +
                ".concat(\"-service\"), \"pod-name\": counter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"mapeXpr\", \"abz-\"), " +
                "\"owner\": \"dpp-squad\", \"pod-id\": " +
                "UUIDFromStringCounter(\"" + requestID + "\", \"metric\", \"cpu.used\", \"maPexpr\", \"pod-abz\"), \"containers\": 3}";
        List<Map<String, Object>> expectedOutputs = Arrays.asList(
                Map.of("name", "abM-service", "pod-name", "abz-1", "owner", "dpp-squad",
                        "pod-id", UUID.nameUUIDFromBytes("pod-abz1".getBytes()).toString(), "containers", 3L),
                Map.of("name", "abN-service", "pod-name", "abz-2", "owner", "dpp-squad",
                        "pod-id", UUID.nameUUIDFromBytes("pod-abz2".getBytes()).toString(), "containers", 3L),
                Map.of("name", "abO-service", "pod-name", "abz-3", "owner", "dpp-squad",
                        "pod-id", UUID.nameUUIDFromBytes("pod-abz3".getBytes()).toString(), "containers", 3L),
                Map.of("name", "abP-service", "pod-name", "abz-4", "owner", "dpp-squad",
                        "pod-id", UUID.nameUUIDFromBytes("pod-abz4".getBytes()).toString(), "containers", 3L),
                Map.of("name", "abQ-service", "pod-name", "abz-5", "owner", "dpp-squad",
                        "pod-id", UUID.nameUUIDFromBytes("pod-abz5".getBytes()).toString(), "containers", 3L));
        IntStream.range(0, 5).forEach(i -> {
            Map<String, Object> actualOutput = (Map<String, Object>) jelProcessor.eval(expression);
            Assert.assertEquals(actualOutput, expectedOutputs.get(i));
        });
    }

    @Test
    public void testLong() {
        ResourceModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getLong(count(\"" + requestID + "\", \"metric\", \"cpu.used\", \"longexpr\") * 200 + 10)";
        List<Long> expectedValues = Arrays.asList(210L, 410L, 610L, 810L, 1010L);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testDouble() {
        ResourceModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getDouble(count(\"" + requestID + "\", \"metric\", \"cpu.used\", \"dblexpr\") / 4)";
        List<Double> expectedValues = Arrays.asList(0.25D, 0.5D, 0.75D, 1.0D, 1.25D);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testBoolean() {
        ResourceModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getBoolean(count(\"" + requestID + "\", \"metric\", \"cpu.used\", \"boolexpr\") % 2)";
        List<Boolean> expectedValues = Arrays.asList(false, true, false, true, false);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testString() {
        String stringVal = "testString";
        Assert.assertEquals(jelProcessor.eval(stringVal), stringVal);
    }
}
