package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityModelProvider {

    private static final ConcurrentMap<String, Map<String, List<GeneratorEntity>>> entityModels = new ConcurrentHashMap<>();

    private EntityModelProvider() {}

    public static void putEntityModel(String requestID, Map<String, List<GeneratorEntity>> entityModel) {
        entityModels.put(requestID, entityModel);
    }

    public static Map<String, List<GeneratorEntity>> getEntityModel(String requestID) {
        return entityModels.get(requestID);
    }
}
