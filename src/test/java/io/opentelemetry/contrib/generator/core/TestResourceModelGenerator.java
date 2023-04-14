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

import io.opentelemetry.contrib.generator.core.dto.Resources;
import io.opentelemetry.contrib.generator.core.dto.ResourceDefinition;
import io.opentelemetry.contrib.generator.core.dto.GeneratorResource;
import io.opentelemetry.contrib.generator.core.jel.JELProvider;
import io.opentelemetry.contrib.generator.core.jel.methods.ResourceModelExpressions;
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
public class TestResourceModelGenerator {

    private Resources resources, resourcesWithRuntimeMods;
    private Map<String, List<GeneratorResource>> resourceModel, resourceModelRuntimeMods;
    private final ELProcessor jelProcessor = JELProvider.getJelProcessor();

    @BeforeClass
    public void generateModel() {
        String resourceDefinitionYAMLPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "resource-definition.yaml").toString();
        String resourceDefinitionRuntimeModsYAMLPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
                "test-definitions", "resource-definition-runtime-mods.yaml").toString();
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            resources = objectMapper.readValue(new File(resourceDefinitionYAMLPath), Resources.class);
            resourcesWithRuntimeMods = objectMapper.readValue(new File(resourceDefinitionRuntimeModsYAMLPath), Resources.class);
        } catch (IOException ioException) {
            log.error("Failed to read resource definition file", ioException);
        }
        System.setProperty(Constants.ENV_ALPHANUMERIC, "d2gd9W");
        Map<String, ResourceDefinition> resourcesMap = resources.getResources().stream()
                .collect(Collectors.toMap(ResourceDefinition::getName, Function.identity()));
        Map<String, ResourceDefinition> resourcesMapRuntimeMods = resourcesWithRuntimeMods.getResources().stream()
                .collect(Collectors.toMap(ResourceDefinition::getName, Function.identity()));
        ResourceModelGenerator resourceModelGenerator = new ResourceModelGenerator(resourcesMap, "TestResourceModelGenerator");
        ResourceModelGenerator resourceModelGeneratorRuntimeMods = new ResourceModelGenerator(resourcesMapRuntimeMods, "TestResourceModelGeneratorRuntimeMods");
        resourceModel = resourceModelGenerator.getResourceModel();
        resourceModelRuntimeMods = resourceModelGeneratorRuntimeMods.getResourceModel();
    }

    @Test
    public void assertResourceCounts() {
        for (ResourceDefinition eachResourceType: resources.getResources()) {
            Assert.assertEquals(resourceModel.get(eachResourceType.getName()).size(), (int)eachResourceType.getCount(),
                    "Resource count does not match for type " + eachResourceType.getName());
        }
    }

    @Test
    public void assertRuntimeModificationResourceCounts() {
        int expectedNodeCount = 25 + (5 * 6);
        int expectedPodCount = 75 + (6 * 15) + (25 * 4);
        int expectedMachineCount = 80 + (10 * 6);
        int expectedContainerCount = 150;
        int expectedHTTPBackendCount = 30 + (5 * 6);
        int expectedSvcInstanceCount = 150 + (25 * 4) + (10 * 8);
        Assert.assertEquals(resourceModelRuntimeMods.get("node").size(), expectedNodeCount, "Node count mismatch for runtime mods model");
        Assert.assertEquals(resourceModelRuntimeMods.get("pod").size(), expectedPodCount, "Pod count mismatch for runtime mods model");
        Assert.assertEquals(resourceModelRuntimeMods.get("machine").size(), expectedMachineCount,
                "Machine count mismatch for runtime mods model");
        Assert.assertEquals(resourceModelRuntimeMods.get("container").size(), expectedContainerCount,
                "Container count mismatch for runtime mods model");
        Assert.assertEquals(resourceModelRuntimeMods.get("http_backend").size(), expectedHTTPBackendCount,
                "HTTP backend count mismatch for runtime mods model");
        Assert.assertEquals(resourceModelRuntimeMods.get("service_instance").size(), expectedSvcInstanceCount,
                "Service instance count mismatch for runtime mods model");
    }

    @Test
    public void assertActiveResources() {
        for (String eachType: resourceModel.keySet()) {
            int count = resourceModel.get(eachType).size();
            Assert.assertTrue(resourceModel.get(eachType).get(ThreadLocalRandom.current().nextInt(count)).isActive());
        }
        for (String eachType: resourceModelRuntimeMods.keySet()) {
            System.out.println("Type is " + eachType);
            int count = eachType.equals("node") ? 25 :
                    eachType.equals("pod") ? 75 :
                    eachType.equals("service_instance") ? 150 :
                    eachType.equals("machine") ? 80 :
                    eachType.equals("http_backend") ? 30 :
                            resourceModelRuntimeMods.get(eachType).size();
            Assert.assertTrue(resourceModelRuntimeMods.get(eachType).get(ThreadLocalRandom.current().nextInt(count)).isActive());
        }
    }

    @Test
    public void assertInactiveResources() {
        int nodeCount = resourceModelRuntimeMods.get("node").size();
        int podCount = resourceModelRuntimeMods.get("pod").size();
        int svcInstanceCount = resourceModelRuntimeMods.get("service_instance").size();
        Assert.assertFalse(resourceModelRuntimeMods.get("node").get(ThreadLocalRandom.current().nextInt(25, nodeCount)).isActive());
        Assert.assertFalse(resourceModelRuntimeMods.get("pod").get(ThreadLocalRandom.current().nextInt(75, podCount)).isActive());
        Assert.assertFalse(resourceModelRuntimeMods.get("service_instance").get(ThreadLocalRandom.current().nextInt(150, svcInstanceCount)).isActive());
    }

    @Test
    public void assertAttributeCounts() {
        for (ResourceDefinition eachResourceType: resources.getResources()) {
            int expectedSize = eachResourceType.getAttributes().size() +
                    CollectionUtils.emptyIfNull(eachResourceType.getAttributeOperations()).size();
            for (GeneratorResource eachResource: resourceModel.get(eachResourceType.getName())) {
                Assert.assertTrue(eachResource.getOTelResource().getAttributesCount() >= expectedSize,
                        "Less than defined number of attributes found for resource " + eachResource);
            }
        }
    }

    @Test
    public void assertAllAttributesForRandomResource() {
        for (ResourceDefinition eachResourceType: resources.getResources()) {
            GeneratorResource randomResource = resourceModel.get(eachResourceType.getName())
                    .get(ThreadLocalRandom.current().nextInt(eachResourceType.getCount()));
            for (String attribute: eachResourceType.getAttributes().keySet()) {
                String attributeValue = getAttributeValue(randomResource, attribute);
                Assert.assertTrue(attributeValue.length() > 0,
                        "Failed to validate attribute value for resource " + eachResourceType.getName() + " and attribute " + attribute);
            }
        }
    }

    private String getAttributeValue(GeneratorResource resource, String attribute) {
        Optional<KeyValue> optionalAttr = resource.getOTelResource().getAttributesList().stream()
                .filter(attr -> attr.getKey().equals(attribute)).findAny();
        if (optionalAttr.isEmpty()) {
            return "";
        }
        return CommonUtils.anyValueToString(optionalAttr.get().getValue());
    }

    @Test
    public void assertChildrenCountsForRandomParents() {
        for (ResourceDefinition eachResourceType: resources.getResources().stream()
                .filter(type -> !MapUtils.emptyIfNull(type.getChildrenDistribution()).isEmpty())
                .toList()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(eachResourceType.getCount());
            GeneratorResource randomParent = resourceModel.get(eachResourceType.getName()).get(randomIndex);
            boolean isLastResource = eachResourceType.getCount()-1 == randomIndex;
            for (Map.Entry<String, String> childExpr: eachResourceType.getChildrenDistribution().entrySet()) {
                ResourceModelExpressions.expressionsGlobalKey = "TEST:" + eachResourceType.getName() + ":" + childExpr.getKey();
                if (isLastResource) {
                    Assert.assertTrue(randomParent.getChildrenByType().get(childExpr.getKey()).size() >=
                                    getExpectedChildCount(childExpr.getValue(), randomIndex),
                            "Children count mismatch for parent type " + eachResourceType.getName() + " and child type " + childExpr.getKey());
                } else {
                    Assert.assertEquals(randomParent.getChildrenByType().get(childExpr.getKey()).size(),
                            getExpectedChildCount(childExpr.getValue(), randomIndex),
                            "Children count mismatch for parent type " + eachResourceType.getName() + " and child type " + childExpr.getKey());
                }
            }
        }
    }

    private int getExpectedChildCount(String distributionExpression, int parentIndex) {
        IntStream.range(0, parentIndex).forEach(i -> jelProcessor.eval(distributionExpression));
        return (int)jelProcessor.eval(distributionExpression);
    }

    @Test
    public void assertAttributeOperationsForRandomResources() {
        GeneratorResource randomReplicaSet = resourceModel.get("replicaset").get(ThreadLocalRandom.current().nextInt(20));
        assertParentChildAttribute(randomReplicaSet, "namespace", "k8s.cluster.name");
        assertParentChildAttribute(randomReplicaSet, "namespace", "k8s.namespace.name");

        int randomIndex = ThreadLocalRandom.current().nextInt(75);
        GeneratorResource randomPod = resourceModel.get("pod").get(randomIndex);
        log.debug("Random pod index: " + randomIndex);
        log.debug("Random pod: " + randomPod.toString());
        assertParentChildAttribute(randomPod, "node", "k8s.cluster.name");
        assertParentChildAttribute(randomPod, "workload", "k8s.namespace.name");
        String expectedValue = getAttributeValue(randomPod.getParentsByType().get("node").get(0), "k8s.cluster.name") +
                "-pod-" + (randomIndex+1);
        Assert.assertEquals(getAttributeValue(randomPod, "k8s.pod.name"), expectedValue);

        GeneratorResource randomContainer = resourceModel.get("container").get(ThreadLocalRandom.current().nextInt(150));
        assertParentChildAttribute(randomContainer, "pod", "k8s.namespace.name");
        assertParentChildAttribute(randomContainer, "pod", "k8s.pod.name");
        assertParentChildAttribute(randomContainer, "machine", "host.name");
    }

    private void assertParentChildAttribute(GeneratorResource child, String parentType, String attributeName) {
        String childValue = getAttributeValue(child, attributeName);
        String parentValue = getAttributeValue(child.getParentsByType().get(parentType).get(0), attributeName);
        Assert.assertEquals(childValue, parentValue, "Attribute value mismatch between parent type " + parentType +
                " and child type " + child.getType());
    }

    @Test
    public void assertAlphanumericFromEnvironmentForResources() {
        List<GeneratorResource> replicasetResources = resourceModel.get("replicaset");
        List<String> outputs = Arrays.asList("d2gd9W", "d2gd9X", "d2gd9Y", "d2gd9Z", "d2gda0", "d2gda1", "d2gda2", "d2gda3", "d2gda4", "d2gda5");
        for(int i=0; i<10; i++){
            Assert.assertEquals(getAttributeValue(replicasetResources.get(i), "k8s.replicaset.uid"), outputs.get(i));
        }
    }

}