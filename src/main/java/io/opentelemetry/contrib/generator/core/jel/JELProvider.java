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

import java.util.Arrays;    
import java.util.List;

/**
 * Provides ELProcessor initialized with all the expression methods supported by the resource model definition YAML.
 */
public class JELProvider {

    private static ExpressionProcessor jelProcessor;

    private JELProvider() {}

    public static ExpressionProcessor getJelProcessor() {
        if (jelProcessor == null) {
            jelProcessor = new ExpressionProcessor();
            var expressionsClass = "io.opentelemetry.contrib.generator.core.jel.methods.ResourceModelExpressions";
            List<String> methods = Arrays.asList("counter", "UUIDFromStringCounter", "roundRobin", "alphanumericSequenceFromEnv",
                    "alphanumericSequence", "IPv4Sequence", "distribution", "count", "getLong", "getDouble", "getBoolean");
            addMethods(expressionsClass, methods);
            var operationsClass = "io.opentelemetry.contrib.generator.core.ResourceModelGenerator";
            methods = Arrays.asList("copyFromParent", "modifyFromParent");
            addMethods(operationsClass, methods);
        }
        return jelProcessor;
    }

    private static void addMethods(String expressionsClass, List<String> methods) {
        for (String methodName: methods) {
            try {
                jelProcessor.defineFunction("", "", expressionsClass, methodName);
            } catch (ClassNotFoundException e) {
                throw new GeneratorException("Unable to find " + expressionsClass + " having expression methods");
            } catch (NoSuchMethodException e) {
                throw new GeneratorException("Unknown expression method " + expressionsClass + "." + methodName +
                        " provided for class");
            }
        }
    }
}
