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

import io.opentelemetry.contrib.generator.core.dto.Entities;
import io.opentelemetry.contrib.generator.core.dto.EntityDefinition;
import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.core.jel.JELProvider;
import io.opentelemetry.contrib.generator.core.jel.methods.EntityModelExpressions;
import io.opentelemetry.contrib.generator.core.utils.CommonUtils;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.opentelemetry.proto.common.v1.KeyValue;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TestEntityModelGenerator {

    private Entities entities, entitiesWithRuntimeMods;
    private Map<String, List<GeneratorEntity>> entityModel, entityModelRuntimeMods;
    private final ELProcessor jelProcessor = JELProvider.getJelProcessor();

    @BeforeClass
    public void generateModel() {
        String entityDefinitionYAMLPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "entity-definition.yaml").toString();
        String entityDefinitionRuntimeModsYAMLPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "entity-definition-runtime-mods.yaml").toString();
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            entities = objectMapper.readValue(new File(entityDefinitionYAMLPath), Entities.class);
            entitiesWithRuntimeMods = objectMapper.readValue(new File(entityDefinitionRuntimeModsYAMLPath), Entities.class);
        } catch (IOException ioException) {
            log.error("Failed to read entity definition file", ioException);
        }
        setEnv();
        Map<String, EntityDefinition> entitiesMap = entities.getEntities().stream().collect(Collectors.toMap(EntityDefinition::getName, Function.identity()));
        Map<String, EntityDefinition> entitiesMapRuntimeMods = entitiesWithRuntimeMods.getEntities().stream()
                .collect(Collectors.toMap(EntityDefinition::getName, Function.identity()));
        EntityModelGenerator entityModelGenerator = new EntityModelGenerator(entitiesMap, "TestEntityModelGenerator");
        EntityModelGenerator entityModelGeneratorRuntimeMods = new EntityModelGenerator(entitiesMapRuntimeMods, "TestEntityModelGeneratorRuntimeMods");
        entityModel = entityModelGenerator.getEntityModel();
        entityModelRuntimeMods = entityModelGeneratorRuntimeMods.getEntityModel();
    }

    @Test
    public void assertEntityCounts() {
        for (EntityDefinition eachEntityType: entities.getEntities()) {
            Assert.assertEquals(entityModel.get(eachEntityType.getName()).size(), (int)eachEntityType.getCount(),
                    "Entity count does not match for type " + eachEntityType.getName());
        }
    }

    @Test
    public void assertRuntimeModificationEntityCounts() {
        int expectedNodeCount = 25 + (5 * 6);
        int expectedPodCount = 75 + (6 * 15) + (25 * 4);
        int expectedMachineCount = 80 + (10 * 6);
        int expectedContainerCount = 150;
        int expectedHTTPBackendCount = 30 + (5 * 6);
        int expectedSvcInstanceCount = 150 + (25 * 4) + (10 * 8);
        Assert.assertEquals(entityModelRuntimeMods.get("node").size(), expectedNodeCount, "Node count mismatch for runtime mods model");
        Assert.assertEquals(entityModelRuntimeMods.get("pod").size(), expectedPodCount, "Pod count mismatch for runtime mods model");
        Assert.assertEquals(entityModelRuntimeMods.get("machine").size(), expectedMachineCount,
                "Machine count mismatch for runtime mods model");
        Assert.assertEquals(entityModelRuntimeMods.get("container").size(), expectedContainerCount,
                "Container count mismatch for runtime mods model");
        Assert.assertEquals(entityModelRuntimeMods.get("http_backend").size(), expectedHTTPBackendCount,
                "HTTP backend count mismatch for runtime mods model");
        Assert.assertEquals(entityModelRuntimeMods.get("service_instance").size(), expectedSvcInstanceCount,
                "Service instance count mismatch for runtime mods model");
    }

    @Test
    public void assertActiveEntities() {
        for (String eachType: entityModel.keySet()) {
            int count = entityModel.get(eachType).size();
            Assert.assertTrue(entityModel.get(eachType).get(ThreadLocalRandom.current().nextInt(count)).isActive());
        }
        for (String eachType: entityModelRuntimeMods.keySet()) {
            System.out.println("Type is " + eachType);
            int count = eachType.equals("node") ? 25 :
                    eachType.equals("pod") ? 75 :
                    eachType.equals("service_instance") ? 150 :
                    eachType.equals("machine") ? 80 :
                    eachType.equals("http_backend") ? 30 :
                            entityModelRuntimeMods.get(eachType).size();
            Assert.assertTrue(entityModelRuntimeMods.get(eachType).get(ThreadLocalRandom.current().nextInt(count)).isActive());
        }
    }

    @Test
    public void assertInactiveEntities() {
        int nodeCount = entityModelRuntimeMods.get("node").size();
        int podCount = entityModelRuntimeMods.get("pod").size();
        int svcInstanceCount = entityModelRuntimeMods.get("service_instance").size();
        Assert.assertFalse(entityModelRuntimeMods.get("node").get(ThreadLocalRandom.current().nextInt(25, nodeCount)).isActive());
        Assert.assertFalse(entityModelRuntimeMods.get("pod").get(ThreadLocalRandom.current().nextInt(75, podCount)).isActive());
        Assert.assertFalse(entityModelRuntimeMods.get("service_instance").get(ThreadLocalRandom.current().nextInt(150, svcInstanceCount)).isActive());
    }

    @Test
    public void assertAttributeCounts() {
        for (EntityDefinition eachEntityType: entities.getEntities()) {
            int expectedSize = eachEntityType.getAttributes().size() +
                    CollectionUtils.emptyIfNull(eachEntityType.getAttributeOperations()).size();
            for (GeneratorEntity eachEntity: entityModel.get(eachEntityType.getName())) {
                Assert.assertTrue(eachEntity.getOTelResource().getAttributesCount() >= expectedSize,
                        "Less than defined number of attributes found for entity " + eachEntity);
            }
        }
    }

    @Test
    public void assertAllAttributesForRandomEntity() {
        for (EntityDefinition eachEntityType: entities.getEntities()) {
            GeneratorEntity randomEntity = entityModel.get(eachEntityType.getName())
                    .get(ThreadLocalRandom.current().nextInt(eachEntityType.getCount()));
            for (String attribute: eachEntityType.getAttributes().keySet()) {
                String attributeValue = getAttributeValue(randomEntity, attribute);
                Assert.assertTrue(attributeValue.length() > 0,
                        "Failed to validate attribute value for entity " + eachEntityType.getName() + " and attribute " + attribute);
            }
        }
    }

    private String getAttributeValue(GeneratorEntity entity, String attribute) {
        Optional<KeyValue> optionalAttr = entity.getOTelResource().getAttributesList().stream()
                .filter(attr -> attr.getKey().equals(attribute)).findAny();
        if (optionalAttr.isEmpty()) {
            return "";
        }
        return CommonUtils.anyValueToString(optionalAttr.get().getValue());
    }

    @Test
    public void assertChildrenCountsForRandomParents() {
        for (EntityDefinition eachEntityType: entities.getEntities().stream()
                .filter(type -> !MapUtils.emptyIfNull(type.getChildrenDistribution()).isEmpty())
                .collect(Collectors.toList())) {
            int randomIndex = ThreadLocalRandom.current().nextInt(eachEntityType.getCount());
            GeneratorEntity randomParent = entityModel.get(eachEntityType.getName()).get(randomIndex);
            boolean isLastEntity = eachEntityType.getCount()-1 == randomIndex;
            for (Map.Entry<String, String> childExpr: eachEntityType.getChildrenDistribution().entrySet()) {
                EntityModelExpressions.expressionsGlobalKey = "TEST:" + eachEntityType.getName() + ":" + childExpr.getKey();
                if (isLastEntity) {
                    Assert.assertTrue(randomParent.getChildrenByType().get(childExpr.getKey()).size() >=
                                    getExpectedChildCount(childExpr.getValue(), randomIndex),
                            "Children count mismatch for parent type " + eachEntityType.getName() + " and child type " + childExpr.getKey());
                } else {
                    Assert.assertEquals(randomParent.getChildrenByType().get(childExpr.getKey()).size(),
                            getExpectedChildCount(childExpr.getValue(), randomIndex),
                            "Children count mismatch for parent type " + eachEntityType.getName() + " and child type " + childExpr.getKey());
                }
            }
        }
    }

    private int getExpectedChildCount(String distributionExpression, int parentIndex) {
        IntStream.range(0, parentIndex).forEach(i -> jelProcessor.eval(distributionExpression));
        return (int)jelProcessor.eval(distributionExpression);
    }

    @Test
    public void assertAttributeOperationsForRandomEntities() {
        GeneratorEntity randomReplicaSet = entityModel.get("replicaset").get(ThreadLocalRandom.current().nextInt(20));
        assertParentChildAttribute(randomReplicaSet, "namespace", "k8s.cluster.name");
        assertParentChildAttribute(randomReplicaSet, "namespace", "k8s.namespace.name");

        int randomIndex = ThreadLocalRandom.current().nextInt(75);
        GeneratorEntity randomPod = entityModel.get("pod").get(randomIndex);
        log.debug("Random pod index: " + randomIndex);
        log.debug("Random pod: " + randomPod.toString());
        assertParentChildAttribute(randomPod, "node", "k8s.cluster.name");
        assertParentChildAttribute(randomPod, "workload", "k8s.namespace.name");
        String expectedValue = getAttributeValue(randomPod.getParentsByType().get("node").get(0), "k8s.cluster.name") +
                "-pod-" + (randomIndex+1);
        Assert.assertEquals(getAttributeValue(randomPod, "k8s.pod.name"), expectedValue);

        GeneratorEntity randomContainer = entityModel.get("container").get(ThreadLocalRandom.current().nextInt(150));
        assertParentChildAttribute(randomContainer, "pod", "k8s.namespace.name");
        assertParentChildAttribute(randomContainer, "pod", "k8s.pod.name");
        assertParentChildAttribute(randomContainer, "machine", "host.name");
    }

    private void assertParentChildAttribute(GeneratorEntity child, String parentType, String attributeName) {
        String childValue = getAttributeValue(child, attributeName);
        String parentValue = getAttributeValue(child.getParentsByType().get(parentType).get(0), attributeName);
        Assert.assertEquals(childValue, parentValue, "Attribute value mismatch between parent type " + parentType +
                " and child type " + child.getType());
    }

    @Test
    public void assertAlphanumericFromEnvironmentForEntities() {
        List<GeneratorEntity> replicasetEntities = entityModel.get("replicaset");
        List<String> outputs = Arrays.asList("d2gd9W", "d2gd9X", "d2gd9Y", "d2gd9Z", "d2gda0", "d2gda1", "d2gda2", "d2gda3", "d2gda4", "d2gda5");
        for(int i=0; i<10; i++){
            Assert.assertEquals(getAttributeValue(replicasetEntities.get(i), "k8s.replicaset.uid"), outputs.get(i));
        }
    }

    private static void setEnv() {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(Constants.ENV_ALPHANUMERIC, "d2gd9W");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

}