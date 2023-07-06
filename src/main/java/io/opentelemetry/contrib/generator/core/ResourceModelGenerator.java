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

import io.opentelemetry.contrib.generator.core.dto.*;
import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.core.jel.ResourceExpressionsJELProvider;
import io.opentelemetry.contrib.generator.core.jel.methods.ResourceModelExpressions;
import io.opentelemetry.contrib.generator.core.utils.CommonUtils;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class is responsible for processing all the resource types mentioned in the resource definition YAML, generating the
 * respective resources and storing them in a map format as:<p>
 * (resource_type, list of resources of that type)
 * <p>
 * This is done in 3 phases: <p>
 *     - Generate the resources of each type with the specified attributes stored in the OTel representation <p>
 *     - Perform parent-child mapping between the resources <p>
 *     - Execute the attribute operations last since they are dependent on the parent-child mappings
 */
@Slf4j
public class ResourceModelGenerator {

    private final Map<String, ResourceDefinition> allResources; //input resource definitions
    private final String requestID;
    private static final ExpressionProcessor jelProcessor = ResourceExpressionsJELProvider.getJelProcessor();
    private static Map<String, List<GeneratorResource>> resourceModel; //output resource model
    private Map<String, ResourceType> typeMappings; //stores parent & child types for each resource type

    public ResourceModelGenerator(Map<String, ResourceDefinition> allResources, String requestID) {
        this.allResources = allResources;
        this.requestID = requestID;
    }

    public Map<String, List<GeneratorResource>> getResourceModel() {
        generateMappedResources();
        executeAttributeOperations();
        return resourceModel;
    }

    /**
     * Processes first 2 phases of the resource model generation.
     */
    private void generateMappedResources() {
        resourceModel = new HashMap<>();
        typeMappings = new HashMap<>();
        for (ResourceDefinition eachResourceType: allResources.values()) {
            typeMappings.put(eachResourceType.getName(), new ResourceType(eachResourceType.getName()));
            resourceModel.put(eachResourceType.getName(), getInstances(eachResourceType));
            log.info(requestID + ": Generated " + resourceModel.get(eachResourceType.getName()).size() + " " +
                    eachResourceType.getName() + " resources");
        }
        mapChildResources();
    }

    private List<GeneratorResource> getInstances(ResourceDefinition resourceDefinition) {
        List<GeneratorResource> resources = new ArrayList<>();
        resourceDefinition.setCountWithRuntimeModifications(resourceDefinition.getCount() + getRuntimeResourcesCount(resourceDefinition.getRuntimeModifications()));
        IntStream.range(0, resourceDefinition.getCount()).forEach(eachCount -> resources.add(getInstance(resourceDefinition, true)));
        IntStream.range(resourceDefinition.getCount(), resourceDefinition.getCountWithRuntimeModifications()).forEach(eachCount -> resources.add(getInstance(resourceDefinition, false)));
        return resources;
    }

    /**
     * Get instance of a resource
     * @param resourceDefinition - Resource metadata about the type of resource to be generated
     * @return GeneratorResource
     */
    private GeneratorResource getInstance(ResourceDefinition resourceDefinition, boolean isActive) {
        var newResource = new GeneratorResource();
        newResource.setType(resourceDefinition.getName());
        newResource.setOtelResource(Resource.newBuilder());
        newResource.setActive(isActive);
        for (Map.Entry<String, String> eachAttribute: resourceDefinition.getAttributes().entrySet()) {
            ResourceModelExpressions.expressionsGlobalKey = requestID + ":" + newResource.getType() + ":" + eachAttribute.getKey() + ":" +
                    eachAttribute.getValue().hashCode();
            newResource.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(eachAttribute.getKey())
                    .setValue(CommonUtils.buildAnyValue(jelProcessor.eval(eachAttribute.getValue())))
                    .build());
        }
        return newResource;
    }

    /**
     * Apart from the number of resources mentioned in the count field for each type, we also need to consider any extra
     * resources that will be needed during the data generation based on the specified runtime modifications. All such extra resources
     * will be marked as inactive to begin with.
     * @param runtimeModifications List of all the runtime modifications for a particular resource type
     * @return Number of resources that will be needed
     */
    private int getRuntimeResourcesCount(List<RuntimeModification> runtimeModifications) {
        if (CollectionUtils.emptyIfNull(runtimeModifications).isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RuntimeModification eachModification: CollectionUtils.emptyIfNull(runtimeModifications).stream()
                .filter(modification -> !modification.getResourceModificationType().equals(ResourceModificationType.REMOVE))
                .toList()) {
            int newCount = ((eachModification.getEndAfterMinutes() - eachModification.getStartAfterMinutes())
                    / eachModification.getModificationFrequencyMinutes()) * eachModification.getModificationQuantity();
            count = count + newCount;
        }
        return count;
    }

    /**
     * Performs second phase of the resource model generation, i.e. parent-child mappings.
     */
    private void mapChildResources() {
        //We will do this only for resources that have some children defined
        for (ResourceDefinition parentType: allResources.values().stream()
                .filter(resource -> !MapUtils.emptyIfNull(resource.getChildrenDistribution()).isEmpty())
                .toList()) {
            setParentToChildrenTypes(parentType.getName(), parentType.getChildrenDistribution().keySet());
            //For each childType-distribution expression pair
            for (Map.Entry<String, String> eachChildTypeExpr: parentType.getChildrenDistribution().entrySet()) {
                String childType = eachChildTypeExpr.getKey();
                int childrenSize = resourceModel.get(childType).size();
                ResourceModelExpressions.expressionsGlobalKey = requestID + ":" + parentType.getName() + ":" + childType;
                var nextChildIndex = 0;
                //For each resource of the parent type
                for (var parentCounter = 0; parentCounter < parentType.getCountWithRuntimeModifications(); parentCounter++) {
                    int count = jelProcessor.eval(eachChildTypeExpr.getValue());
                    int childEndIndex = nextChildIndex + count;
                    if (childEndIndex > childrenSize) {
                        childEndIndex = childrenSize;
                    }
                    log.debug("Mapping parent resource '" + parentType.getName() + "' at index " + parentCounter +
                            " to children of type '" + childType + "' at indices range " + nextChildIndex + "-" + childEndIndex);
                    setParentToChildren(resourceModel.get(parentType.getName()).get(parentCounter), childType,
                            nextChildIndex, childEndIndex);
                    nextChildIndex = nextChildIndex + count;
                    if (nextChildIndex >= childrenSize) {
                        nextChildIndex = 0;
                    }
                }
                //Map any remaining child resource to the last parent resource
                if (nextChildIndex < childrenSize) {
                    log.debug("Remaining children of type '" + childType + "' mapped to the last parent of type '" + parentType +
                            "' at index " + (parentType.getCountWithRuntimeModifications()-1));
                    setParentToChildren(resourceModel.get(parentType.getName()).get(parentType.getCountWithRuntimeModifications()-1), childType,
                            nextChildIndex, childrenSize);
                }
            }
        }
    }

    /**
     * Performs third and last phase of the model generation, i.e. execution of attribute operations.
     */
    private void executeAttributeOperations() {
        Set<String> resourceWithOperations = allResources.values().stream()
                .filter(resource -> !CollectionUtils.emptyIfNull(resource.getAttributeOperations()).isEmpty())
                .map(ResourceDefinition::getName)
                .collect(Collectors.toSet());
        //If there are some resource types that need attribute operations, we need to know the top-down levels of the types
        //since the attribute operations like copyFromParent need to be processed top -> down
        List<Set<String>> resourceLevels = resourceWithOperations.isEmpty() ? new ArrayList<>() : getResourceLevels();
        resourceLevels.forEach(resources -> resources.stream()
                .filter(resourceWithOperations::contains)
                .map(allResources::get)
                .forEach(resourceDefinition -> {
                    log.info(requestID + ": Executing " + resourceDefinition.getAttributeOperations().size() +
                            " attribute operations for " + resourceDefinition.getName() + " types");
                    ResourceModelExpressions.expressionsGlobalKey = requestID + ":" + resourceDefinition.getName();
                    for (String expression: resourceDefinition.getAttributeOperations()) {
                        jelProcessor.eval(expression);
                    }
                }));
        //We also want to set the evaluated attributes at this stage
        resourceModel.values().forEach(resourceList -> resourceList.forEach(GeneratorResource::setEvaluatedAttributes));
    }

    private void setParentToChildrenTypes(String parentType, Set<String> childTypes) {
        log.info(requestID + ": Mapping parent -> children types: " + parentType + " -> " + StringUtils.join(childTypes));
        typeMappings.get(parentType).setChildTypes(new HashSet<>(childTypes));
        for (String eachChildType: childTypes) {
            if (typeMappings.get(eachChildType).getParentTypes() == null) {
                typeMappings.get(eachChildType).setParentTypes(new HashSet<>());
            }
            typeMappings.get(eachChildType).getParentTypes().add(parentType);
        }
        //Also initialize the children types map for all the resources of parent type
        resourceModel.get(parentType).forEach(parentResource -> parentResource.setChildrenByType(new HashMap<>()));
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private void setParentToChildren(GeneratorResource parent, String childrenType, int childStartIndex, int childEndIndex) {
        List<GeneratorResource> children = resourceModel.get(childrenType).subList(childStartIndex, childEndIndex);
        if (!parent.getChildrenByType().containsKey(childrenType) || parent.getChildrenByType().get(childrenType) == null) {
            parent.getChildrenByType().put(childrenType, new ArrayList<>());
        }
        parent.getChildrenByType().get(childrenType).addAll(children);
        children.forEach(eachChild -> {
            if (eachChild.getParentsByType() == null) {
                eachChild.setParentsByType(new HashMap<>());
                eachChild.getParentsByType().put(parent.getType(), new ArrayList<>(Arrays.asList(parent)));
                return;
            }
            if (!eachChild.getParentsByType().containsKey(parent.getType())) {
                eachChild.getParentsByType().put(parent.getType(), new ArrayList<>(Arrays.asList(parent)));
                return;
            }
            eachChild.getParentsByType().get(parent.getType()).add(parent);
        });
    }

    /**
     * The attribute operations need to be performed from top->bottom (parent->child types) and this method provides the list of
     * all the resource types in this order with all the resources at a similar level in the hierarchy put in the same set of that level.
     * @return List of Set of all resource types at their particular level
     */
    private List<Set<String>> getResourceLevels() {
        List<Set<String>> resourceLevels = new ArrayList<>();
        Map<String, Integer> parentCounts = new HashMap<>();
        typeMappings.values().forEach(eachType -> parentCounts.put(eachType.getName(), getResourceLevel(eachType.getName())));
        IntStream.range(0, Collections.max(parentCounts.values())+1).forEach(i -> resourceLevels.add(new HashSet<>()));
        for(Map.Entry<String, Integer> typeLevel: parentCounts.entrySet()) {
            resourceLevels.get(typeLevel.getValue()).add(typeLevel.getKey());
        }
        CollectionUtils.filter(resourceLevels, level -> !level.isEmpty());
        log.debug(requestID + ": Resource levels are: " + StringUtils.join(resourceLevels));
        return resourceLevels;
    }

    private int getResourceLevel(String resurceType) {
        if (CollectionUtils.emptyIfNull(typeMappings.get(resurceType).getParentTypes()).isEmpty()) {
            return 0;
        }
        int level = typeMappings.get(resurceType).getParentTypes().size();
        for (String eachParent: typeMappings.get(resurceType).getParentTypes()) {
            level = Math.max(level, getResourceLevel(eachParent)+1);
        }
        return level;
    }

    /**
     * Copies an attribute key-value from the first parent of the specified type to all the resources of the current/child type.
     * This expression implementation has to be defined in this class since it needs access to the resource model.
     * @param parentType - Resource type of the parent from which to copy the attribute
     * @param attribute - Name/Key of the attribute to copy
     */
    @SuppressWarnings("unused")
    public static void copyFromParent(String parentType, String attribute) {
        for (GeneratorResource eachResource: resourceModel
                .get(ResourceModelExpressions.expressionsGlobalKey.split(":")[1]).stream()
                .filter(resource -> resource.getParentsByType()!=null)
                .filter(resource -> resource.getParentsByType().containsKey(parentType))
                .toList()) {
            Optional<KeyValue> parentAttribute = eachResource.getParentsByType().get(parentType).get(0)
                    .getOTelResource().getAttributesList().stream()
                    .filter(attrKV -> attrKV.getKey().equals(attribute)).findAny();
            String attributeValue = parentAttribute.map(keyValue -> keyValue.getValue().getStringValue()).orElse("");
            eachResource.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(attribute)
                    .setValue(CommonUtils.buildAnyValue(attributeValue))
                    .build());
        }
    }

    /**
     * Copies an attribute key-value from the first parent of the specified type, appends a string obtained from another expression
     * and adds it as a new attribute to all the resources of the current type. The suffix expression specified is evaluated for
     * every resource of the current type.
     * This expression implementation has to be defined in this class since it needs access to the resource model.
     * @param parentType - Parent resource type from which to copy the attribute
     * @param sourceAttribute - Name of the attribute in the parent resource
     * @param targetAttribute - Name of the attribute to be set in the current resource
     * @param suffixExpression - Expression to be evaluated for the suffix to the parent attribute value
     */
    @SuppressWarnings("unused")
    public static void modifyFromParent(String parentType, String sourceAttribute, String targetAttribute, String suffixExpression) {
        for (GeneratorResource eachResource: resourceModel.get(ResourceModelExpressions
                .expressionsGlobalKey.split(":")[1])) {
            Optional<KeyValue> parentAttribute = eachResource.getParentsByType().get(parentType).get(0)
                    .getOTelResource().getAttributesList().stream()
                    .filter(attrKV -> attrKV.getKey().equals(sourceAttribute)).findAny();
            String attributeValue = parentAttribute.map(keyValue -> keyValue.getValue().getStringValue()).orElse("");
            if (suffixExpression.length() > 0) {
                attributeValue = attributeValue + jelProcessor.eval(suffixExpression);
            }
            eachResource.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(targetAttribute)
                    .setValue(CommonUtils.buildAnyValue(attributeValue))
                    .build());
        }
    }
}
