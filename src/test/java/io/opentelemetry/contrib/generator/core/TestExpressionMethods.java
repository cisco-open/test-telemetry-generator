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

package io.opentelemetry.contrib.generator.core;

import io.opentelemetry.contrib.generator.core.jel.JELProvider;
import io.opentelemetry.contrib.generator.core.jel.methods.EntityModelExpressions;
import jakarta.el.ELProcessor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.IntStream;

public class TestExpressionMethods {

    private final ELProcessor jelProcessor = JELProvider.getJelProcessor();

    @Test
    public void testCounter() {
        String inputExpression = "counter(\"test-\")";
        IntStream.range(1, 5).forEach(i -> jelProcessor.eval(inputExpression));
        Assert.assertEquals(jelProcessor.eval(inputExpression), "test-5");
    }

    @Test
    public void testUUIDFromStringCounter() {
        String inputExpression = "UUIDFromStringCounter(\"container-id\")";
        int size = 1000;
        Set<String> UUIDs1 = new HashSet<>();
        for (int i=0; i<size; i++) {
            UUIDs1.add(jelProcessor.eval(inputExpression).toString());
        }
        //Check all are unique
        Assert.assertEquals(UUIDs1.size(), size, "UUIDs generated were not all unique");
        EntityModelExpressions.resetCaches();
        Set<String> UUIDs2 = new HashSet<>();
        for (int i=0; i<size; i++) {
            UUIDs2.add(jelProcessor.eval(inputExpression).toString());
        }
        //Check they are deterministic
        Assert.assertEquals(UUIDs1, UUIDs2, "UUIDs are not deterministic");
    }

    @Test
    public void testRoundRobin() {
        String expression1 = "roundRobin([\"FIXED\"])";
        String expression2 = "roundRobin([\"VAL0\", \"VAL1\", \"VAL2\", \"VAL3\"])";
        for (int i=0; i<5; i++) {
            Assert.assertEquals(jelProcessor.eval(expression1), "FIXED");
            int num = i > 3 ? 0 : i;
            Assert.assertEquals(jelProcessor.eval(expression2), "VAL" + num);
        }
    }

    @Test
    public void testAlphanumericSequence() {
        String expression1 = "alphanumericSequence(\"af1z8x\")";
        List<String> outputs1 = Arrays.asList("af1z8x", "af1z8y", "af1z8z", "af1z8A", "af1z8B", "af1z8C", "af1z8D", "af1z8E", "af1z8F", "af1z8G");
        String expression2 = "alphanumericSequence(\"sdc3a7\")";
        List<String> outputs2 = Arrays.asList("sdc3a7", "sdc3a8", "sdc3a9", "sdc3aa", "sdc3ab", "sdc3ac", "sdc3ad", "sdc3ae", "sdc3af", "sdc3ag");
        String expression3 = "alphanumericSequence(\"d2gd9W\")";
        List<String> outputs3 = Arrays.asList("d2gd9W", "d2gd9X", "d2gd9Y", "d2gd9Z", "d2gda0", "d2gda1", "d2gda2", "d2gda3", "d2gda4", "d2gda5");
        String expression4 = "alphanumericSequence(\"ZZZZZX\")";
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
        String expression1 = "IPv4Sequence(\"10.10.10.1\")";
        List<String> outputs1 = new ArrayList<>();
        IntStream.range(1, 11).forEach(i -> outputs1.add("10.10.10." + i));
        String expression2 = "IPv4Sequence(\"128.10.13.253\")";
        List<String> outputs2 = new ArrayList<>();
        IntStream.range(253, 256).forEach(i -> outputs2.add("128.10.13." + i));
        IntStream.range(1, 8).forEach(i -> outputs2.add("128.10.14." + i));
        String expression3 = "IPv4Sequence(\"255.255.255.250\")";
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
        String simpleExpression = "[counter(\"abc\"), alphanumericSequence(\"abc\"), roundRobin([\"abc\"])]";
        String concatenatedExpression = "[counter(\"c.abc\").concat(roundRobin([\"-c1\", \"-c2\"])), " +
                "\"concat-\".concat(alphanumericSequence(\"c.abc\"))]";
        String nestedExpression = "[counter(\"n.abc\"), [counter(\"n1.abc\"), roundRobin([\"n1.abc\"])], " +
                "\"nested-\".concat(IPv4Sequence(\"10.10.11.1\"))]";
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
            String simpleOutput = StringUtils.join(jelProcessor.eval(simpleExpression));
            String concatenatedOutput = StringUtils.join(jelProcessor.eval(concatenatedExpression));
            String nestedOutput = StringUtils.join(jelProcessor.eval(nestedExpression));
            Assert.assertEquals(simpleOutput, StringUtils.join(simpleExpectedOutputs.get(i)));
            Assert.assertEquals(concatenatedOutput, StringUtils.join(concatenatedExpectedOutputs.get(i)));
            Assert.assertEquals(nestedOutput, StringUtils.join(nestedExpectedOutputs.get(i)));
        });
    }

    @Test
    public void testMap() {
        EntityModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "{\"name\": alphanumericSequence(\"abM\").concat(\"-service\"), " +
                "\"pod-name\": counter(\"abz-\"), \"owner\": \"dpp-squad\", \"pod-id\": UUIDFromStringCounter(\"pod-abz\"), " +
                "\"containers\": 3}";
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
        EntityModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getLong(count() * 200 + 10)";
        List<Long> expectedValues = Arrays.asList(210L, 410L, 610L, 810L, 1010L);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testDouble() {
        EntityModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getDouble(count() / 4)";
        List<Double> expectedValues = Arrays.asList(0.25D, 0.5D, 0.75D, 1.0D, 1.25D);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testBoolean() {
        EntityModelExpressions.expressionsGlobalKey = RandomStringUtils.randomAlphanumeric(16);
        String expression = "getBoolean(count() % 2)";
        List<Boolean> expectedValues = Arrays.asList(false, true, false, true, false);
        IntStream.range(0, 5).forEach(i -> Assert.assertEquals(jelProcessor.eval(expression), expectedValues.get(i)));
    }

    @Test
    public void testDistribution() {
        String expression1 = "distribution(3, 0, 0)";
        String expression2 = "distribution(3, 2, 0)";
        List<Integer> outputs1n2 = new ArrayList<>(Collections.nCopies(10, 3));
        String expression3 = "distribution(5, 3, 5)";
        List<Integer> outputs3 = new ArrayList<>(Collections.nCopies(10, 5));
        outputs3.set(2, 10);
        outputs3.set(5, 10);
        outputs3.set(8, 10);
        String expression4 = "distribution(3, 1, 1)";
        List<Integer> outputs4 = new ArrayList<>(Collections.nCopies(10, 4));
        for (int i=0; i<10; i++) {
            Assert.assertEquals(jelProcessor.eval(expression1), outputs1n2.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression2), outputs1n2.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression3), outputs3.get(i), "Mismatch at index " + i);
            Assert.assertEquals(jelProcessor.eval(expression4), outputs4.get(i), "Mismatch at index " + i);
        }
    }
}
