package io.opentelemetry.contrib.generator.core.jel;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import jakarta.el.ELProcessor;

import java.util.Arrays;    
import java.util.List;

/**
 * Provides ELProcessor initialized with all the expression methods supported by the entity/resource model definition YAML.
 */
public class JELProvider {

    private static ELProcessor jelProcessor;

    private JELProvider() {}

    public static ELProcessor getJelProcessor() {
        if (jelProcessor == null) {
            jelProcessor = new ELProcessor();
            var expressionsClass = "io.opentelemetry.contrib.generator.core.jel.methods.EntityModelExpressions";
            List<String> methods = Arrays.asList("counter", "UUIDFromStringCounter", "roundRobin", "alphanumericSequenceFromEnv",
                    "alphanumericSequence", "IPv4Sequence", "distribution", "count", "getLong", "getDouble", "getBoolean");
            addMethods(expressionsClass, methods);
            var operationsClass = "io.opentelemetry.contrib.generator.core.EntityModelGenerator";
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
