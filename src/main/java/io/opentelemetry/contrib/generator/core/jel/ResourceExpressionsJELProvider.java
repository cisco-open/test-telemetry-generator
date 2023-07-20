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

package io.opentelemetry.contrib.generator.core.jel;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Provides ELProcessor initialized with all the expression methods supported by the resource model definition YAML.
 */
public class ResourceExpressionsJELProvider {

    private static ExpressionProcessor jelProcessor;

    private ResourceExpressionsJELProvider() {}

    public static ExpressionProcessor getJelProcessor() {
        if (jelProcessor == null) {
            jelProcessor = new ExpressionProcessor();
        }
        var expressionsClass = "io.opentelemetry.contrib.generator.core.jel.methods.ResourceModelExpressions";
        List<String> methods = Arrays.asList("counter", "UUIDFromStringCounter", "roundRobin", "alphanumericSequenceFromEnv",
                "alphanumericSequence", "IPv4Sequence", "distribution", "count", "getLong", "getDouble", "getBoolean");
        addMethods(expressionsClass, methods);
        var operationsClass = "io.opentelemetry.contrib.generator.core.ResourceModelGenerator";
        methods = Arrays.asList("copyFromParent", "modifyFromParent");
        addMethods(operationsClass, methods);
        return jelProcessor;
    }

    private static void addMethods(String expressionsClass, List<String> methods) {
        methods.forEach(method -> addExpression("", "", expressionsClass, method));
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
}
