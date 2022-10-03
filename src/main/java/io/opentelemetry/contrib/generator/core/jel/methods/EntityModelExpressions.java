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

package io.opentelemetry.contrib.generator.core.jel.methods;

import io.opentelemetry.contrib.generator.core.jel.JELProvider;
import io.opentelemetry.contrib.generator.core.jel.helpers.AlphanumericHelper;
import io.opentelemetry.contrib.generator.core.jel.helpers.IPHelper;
import jakarta.el.ELProcessor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.opentelemetry.contrib.generator.telemetry.misc.Constants.ENV_ALPHANUMERIC;

/**
 * This holds the implementation for all the supported expressions in entity/resource definition YAML. Since we are using ELProcessor
 * in a standalone context to process the expressions, all the implementing methods have to be public static.
 * @see ELProcessor
 */
@SuppressWarnings("unused")
public class EntityModelExpressions {

    private static final ConcurrentHashMap<String, Integer> counters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> doubleCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> stringCounters = new ConcurrentHashMap<>();
    private static final ELProcessor jelProcessor = JELProvider.getJelProcessor();
    public static String expressionsGlobalKey = ""; //Modified by the entity model generator every time a new entity/attribute is being processed

    private EntityModelExpressions() {}

    /**
     * Returns a string prefix to a counter value. Counter is stateful & is linked to the input string.
     * Eg: counter("abc") -> "abc1"
     * Calling the same method again would result in output of "abc2"
     * @param string Input string
     * @return Output string
     */
    public static String counter(String string) {
        String expressionId = expressionsGlobalKey + ":" + string;
        counters.putIfAbsent(expressionId, 0);
        counters.put(expressionId, counters.get(expressionId) + 1);
        return string + counters.get(expressionId);
    }

    /**
     * Returns a type 3 UUID based on a specific string. The string itself consists of the provided input string prefixed to
     * a counter. So a call to this method with "abc" means UUID is generated with the value "abc1" and a subsequent call
     * would result in the UUID being generated using "abc2". This keeps the UUIDs being generated in sequence & deterministic.
     * @param stringSequenceSeed Input string for sequence start
     * @return UUID string based on string sequence
     */
    public static String UUIDFromStringCounter(String stringSequenceSeed) {
        String expressionId = expressionsGlobalKey + ":" + stringSequenceSeed;
        counters.putIfAbsent(expressionId, 0);
        counters.put(expressionId, counters.get(expressionId) + 1);
        return UUID.nameUUIDFromBytes((stringSequenceSeed + counters.get(expressionId)).getBytes()).toString();
    }

    /**
     * Returns one of the strings provided in the input in a sequential & stateful manner.
     * Eg: roundRobin(["abc", "def", "ghi"]) -> "abc"
     * Next call will return "def", "ghi" and then back to "abc".
     * @param values List of values to loop over
     * @return Current output value
     */
    public static String roundRobin(List<String> values) {
        String expressionId = expressionsGlobalKey + ":" + StringUtils.join(values);
        if (!counters.containsKey(expressionId)) {
            counters.put(expressionId, 0);
            return values.get(0);
        }
        int nextIndex = counters.get(expressionId) + 1 == values.size() ? 0 : counters.get(expressionId) + 1;
        counters.put(expressionId, nextIndex);
        return values.get(counters.get(expressionId));
    }

    /**
     * Returns the next alphanumeric string in a stateful manner. The next alphanumeric string means we increment the
     * rightmost possible character to the next character in sequence.
     * Eg: alphanumericSequence("abc8") -> "abc8" and next calls will return "abc9", "abca", "abcb" and so on.
     * @see AlphanumericHelper
     * @param sequenceSeed Input string
     * @return Output string in sequence
     */
    public static String alphanumericSequence(String sequenceSeed) {
        String expressionId = expressionsGlobalKey + ":" + sequenceSeed;
        if (!stringCounters.containsKey(expressionId)) {
            stringCounters.put(expressionId, sequenceSeed);
            return sequenceSeed;
        }
        stringCounters.put(expressionId, AlphanumericHelper.getNext(stringCounters.get(expressionId)));
        return stringCounters.get(expressionId);
    }

    /**
     * Takes the value set for ENV_ALPHANUMERIC in the environment or the system property and calculates the next
     * alphanumeric sequence.
     * @see EntityModelExpressions#alphanumericSequence(String)
     * @return Next alphanumeric string based on input provided via environment variable or system property.
     */
    public static String alphanumericSequenceFromEnv() {
        String sequenceSeed = Optional.ofNullable(System.getenv(ENV_ALPHANUMERIC))
                .orElse(System.getProperty(ENV_ALPHANUMERIC));
        if (StringUtils.defaultString(sequenceSeed).isBlank()) sequenceSeed = "default";
        return alphanumericSequence(sequenceSeed);
    }

    /**
     * Return the next IPv4 address in a stateful manner. The next IPv4 address means we increment the rightmost possible octet.
     * Eg: IPv4Sequence("128.10.114.254") -> "128.10.114.254" and next calls will return "128.10.114.255", "10.10.10.10", "10.10.10.11"
     * @see IPHelper
     * @param sequenceSeed Input IP
     * @return Output IP in sequence
     */
    public static String IPv4Sequence(String sequenceSeed) {
        String expressionId = expressionsGlobalKey + ":" + sequenceSeed;
        if (!stringCounters.containsKey(expressionId)) {
            stringCounters.put(expressionId, sequenceSeed);
            return sequenceSeed;
        }
        stringCounters.put(expressionId, IPHelper.nextIPv4Address(stringCounters.get(expressionId)));
        return stringCounters.get(expressionId);
    }

    /**
     * Used to return a numerical sequence in double format starting with 0 and incrmented by 1 with every subsequent call.
     * @return Next value in sequence
     */
    public static double count() {
        String expressionId = expressionsGlobalKey + ":count";
        doubleCounters.putIfAbsent(expressionId, 0D);
        doubleCounters.put(expressionId, doubleCounters.get(expressionId) + 1);
        return doubleCounters.get(expressionId);
    }

    /**
     * Used to return a double value based on an expression. Can be used along with count() to generate a sequence.
     * For eg: getDouble("count() / 4") → 0.25, and next calls will return 0.5, 0.75, 1.0 and so on.
     * @param expression Input arithmetic expression
     * @return Next double value
     */
    public static double getDouble(String expression) {
        return Double.parseDouble(jelProcessor.eval(expression).toString());
    }

    /**
     * Used to return a long value based on an expression. Can be used along with count() to generate a sequence.
     * For eg: getLong("count() * 3 + 1") → 4 and next calls will return 7, 10, 13 and so on.
     * @param expression Input arithmetic expression
     * @return Next long value
     */
    public static long getLong(String expression) {
        return (long) getDouble(expression);
    }

    /**
     * Used to return a boolean value based on an expression. If the arithmetic expression evaluates to 0, false is returned
     * while true will be returned in all other cases.
     * For eg: getBoolean("count() % 2") → false and next calls will return true, false, true and so on.
     * @param expression Input arithmetic expression
     * @return Next boolean value
     */
    public static boolean getBoolean(String expression) {
        return getDouble(expression) == 0;
    }

    /**
     * Returns a number based on a distribution specification consisting of 2 quantities:
     * base - a value to be distributed to every index
     * more - a value to be distributed to only certain indices specified by the everyOther param
     * <p>
     * Each call to this method with a unique combinations of the arguments starts a state for that combination with index 0.
     * Every subsequent call for this combination, including the first one returns a value which is equal to either:
     * base OR (base + more)
     * This is decided by everyOther which indicates the factor of the indices to which more should be added. This implementation uses
     * 1-based indexing for better readability. This method is ideally read as:
     * add a base quantity and for everyOther index, also add a more quantity
     * <p>
     * @param base value to be distributed to every index
     * @param everyOther indicates the factor of the indices to which more should be added
     * @param more value to be distributed to only certain indices
     * @return Current distribution count
     */
    public static int distribution(int base, int everyOther, int more) {
        String expressionId = expressionsGlobalKey + ":" + base + everyOther + more;
        if (!counters.containsKey(expressionId)) {
            counters.put(expressionId, 0);
            return getDistributionValue(1, base, everyOther, more);
        }
        counters.put(expressionId, counters.get(expressionId) + 1);
        return getDistributionValue(counters.get(expressionId) + 1, base, everyOther, more);
    }

    private static int getDistributionValue(int currentIndex, int base, int everyOther, int more) {
        int value = base;
        if (everyOther > 0 && currentIndex % everyOther == 0) {
            value = value + more;
        }
        return value;
    }

    /**
     * Reset the state of all counters maintained by this class. Used only for the tests as of now.
     */
    public static void resetCaches() {
        stringCounters.clear();
        doubleCounters.clear();
        counters.clear();
    }

}
