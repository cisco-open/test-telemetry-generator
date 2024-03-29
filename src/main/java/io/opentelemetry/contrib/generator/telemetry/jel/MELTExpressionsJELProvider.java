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

package io.opentelemetry.contrib.generator.telemetry.jel;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.telemetry.jel.methods.LogSeverityGenerator;
import io.opentelemetry.contrib.generator.telemetry.jel.methods.MetricValueGenerators;
import io.opentelemetry.contrib.generator.telemetry.jel.methods.MELTAttributeGenerators;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Provides ELProcessor initialized with all the expression methods supported by the metrics, logs & traces definition YAML files.
 */
public class MELTExpressionsJELProvider {

    private static ExpressionProcessor jelProcessor;

    private MELTExpressionsJELProvider() {}

    public static ExpressionProcessor getJelProcessor() {
        if (jelProcessor == null) {
            jelProcessor = new ExpressionProcessor();
        }
        defineMetricFunction("arithmeticSequence", String.class, String.class, double.class, double.class, String.class);
        defineMetricFunction("arithmeticSequenceSummary", String.class, String.class, double.class, double.class, String.class, int.class);
        defineMetricFunction("geometricSequence", String.class, String.class, double.class, double.class, String.class);
        defineMetricFunction("geometricSequenceSummary", String.class, String.class, double.class, double.class, String.class, int.class);
        defineMetricFunction("exponentialSequence", String.class, String.class, double.class, double.class, String.class);
        defineMetricFunction("exponentialSequenceSummary", String.class, String.class, double.class, double.class, String.class, int.class);
        defineMetricFunction("logarithmicSequence", String.class, String.class, double.class, double.class, String.class);
        defineMetricFunction("logarithmicSequenceSummary", String.class, String.class, double.class, double.class, String.class, int.class);
        defineMetricFunction("absoluteSineSequence", String.class, String.class, String.class);
        defineMetricFunction("absoluteSineSequenceSummary", String.class, String.class, String.class, int.class);
        defineMetricFunction("absoluteCosineSequence", String.class, String.class, String.class);
        defineMetricFunction("absoluteCosineSequenceSummary", String.class, String.class, String.class, int.class);
        defineMetricFunction("absoluteTangentSequence", String.class, String.class, String.class);
        defineMetricFunction("absoluteTangentSequenceSummary", String.class, String.class, String.class, int.class);
        defineMetricFunction("random", double.class, double.class, String.class);
        defineMetricFunction("randomSummary", double.class, double.class, String.class, int.class);
        defineMetricFunction("controlledRandom", String.class, String.class, double.class, double.class, String.class);
        defineMetricFunction("controlledRandomSummary", String.class, String.class, double.class, double.class, String.class, int.class);
        defineLogSeverityFunction("severityDistributionCount", String.class, String.class, List.class, List.class);
        defineLogSeverityFunction("severityDistributionPercentage", String.class, String.class, List.class, List.class);
        defineMELTAttributesFunction("counter", String.class, String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("UUIDFromStringCounter", String.class, String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("roundRobin", String.class, String.class, String.class, String.class, List.class);
        defineMELTAttributesFunction("alphanumericSequenceFromEnv", String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("alphanumericSequence", String.class, String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("IPv4Sequence", String.class, String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("count", String.class, String.class, String.class, String.class);
        defineMELTAttributesFunction("getDouble", String.class);
        defineMELTAttributesFunction("getLong", String.class);
        defineMELTAttributesFunction("getBoolean", String.class);
        return jelProcessor;
    }

    /**
     * See <a href="https://jakarta.ee/specifications/platform/9/apidocs/jakarta/el/elprocessor#defineFunction-java.lang.String-java.lang.String-java.lang.String-java.lang.String-">...</a>
     * Must be called in your code before telemetry generation is started.
     */
    public static void addExpression(String prefix, String function, String className, String methodName) {
        if (jelProcessor == null) {
            jelProcessor = new ExpressionProcessor();
        }
        try {
            jelProcessor.defineFunction(prefix, function, className, methodName);
        } catch (ClassNotFoundException e) {
            throw new GeneratorException("Unable to find " + className + " having expression methods");
        } catch (NoSuchMethodException e) {
            throw new GeneratorException("Unknown expression method " + className + "." + methodName +
                    " provided for class", e);
        }
    }

    /**
     * See <a href="https://jakarta.ee/specifications/platform/9/apidocs/jakarta/el/elprocessor#defineFunction-java.lang.String-java.lang.String-java.lang.reflect.Method-">...</a>
     * Must be called in your code before telemetry generation is started.
     */
    public static void addExpression(String prefix, String function, Method method) {
        if (jelProcessor == null) {
            jelProcessor = new ExpressionProcessor();
        }
        try {
            jelProcessor.defineFunction(prefix, function, method);
        } catch (NoSuchMethodException e) {
            throw new GeneratorException("Unknown expression method " + method + " provided", e);
        }
    }

    private static void defineMetricFunction(String methodName, Class<?>... parameterTypes) {
        defineFunction(methodName, MetricValueGenerators.class, parameterTypes);
    }

    private static void defineMELTAttributesFunction(String methodName, Class<?>... parameterTypes) {
        defineFunction(methodName, MELTAttributeGenerators.class, parameterTypes);
    }

    private static void defineLogSeverityFunction(String methodName, Class<?>... parameterTypes) {
        defineFunction(methodName, LogSeverityGenerator.class, parameterTypes);
    }

    private static void defineFunction(String methodName, Class<?> generatorClass, Class<?>... parameterTypes) {
        try {
            jelProcessor.defineFunction("", "", generatorClass.getMethod(methodName, parameterTypes));
        } catch (NoSuchMethodException e) {
            throw new GeneratorException("Unknown expression method " + methodName + " provided for class " +
                    MetricValueGenerators.class.getName());
        }
    }
}
