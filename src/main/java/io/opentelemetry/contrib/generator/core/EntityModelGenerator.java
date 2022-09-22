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
import io.opentelemetry.contrib.generator.core.jel.JELProvider;
import io.opentelemetry.contrib.generator.core.jel.methods.EntityModelExpressions;
import io.opentelemetry.contrib.generator.core.utils.CommonUtils;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import jakarta.el.ELProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class is responsible for processing all the entity types mentioned in the entity/resource definition YAML, generating the
 * respective entities and storing them in a map format as:<p>
 * (entity_type, list of entities of that type)
 * <p>
 * This is done in 3 phases: <p>
 *     - Generate the entities of each type with the specified attributes stored in the OTel representation <p>
 *     - Perform parent-child mapping between the entities <p>
 *     - Execute the attribute operations last since they are dependent on the parent-child mappings
 */
@Slf4j
public class EntityModelGenerator {

    private final Map<String, EntityDefinition> allEntities; //input entity definitions
    private final String requestID;
    private static final ELProcessor jelProcessor = JELProvider.getJelProcessor();
    private static Map<String, List<GeneratorEntity>> entityModel; //output entity model
    private Map<String, EntityType> typeMappings; //stores parent & child types for each entity type

    public EntityModelGenerator(Map<String, EntityDefinition> allEntities, String requestID) {
        this.allEntities = allEntities;
        this.requestID = requestID;
    }

    public Map<String, List<GeneratorEntity>> getEntityModel() {
        generateMappedEntities();
        executeAttributeOperations();
        return entityModel;
    }

    /**
     * Processes first 2 phases of the entity model generation.
     */
    private void generateMappedEntities() {
        entityModel = new HashMap<>();
        typeMappings = new HashMap<>();
        for (EntityDefinition eachEntityType: allEntities.values()) {
            typeMappings.put(eachEntityType.getName(), new EntityType(eachEntityType.getName()));
            entityModel.put(eachEntityType.getName(), getInstances(eachEntityType));
            log.info(requestID + ": Generated " + entityModel.get(eachEntityType.getName()).size() + " " +
                    eachEntityType.getName() + " entities");
        }
        mapChildEntities();
    }

    private List<GeneratorEntity> getInstances(EntityDefinition entityDefinition) {
        List<GeneratorEntity> entities = new ArrayList<>();
        entityDefinition.setCountWithRuntimeModifications(entityDefinition.getCount() + getRuntimeEntitiesCount(entityDefinition.getRuntimeModifications()));
        IntStream.range(0, entityDefinition.getCount()).forEach(eachCount -> entities.add(getInstance(entityDefinition, true)));
        IntStream.range(entityDefinition.getCount(), entityDefinition.getCountWithRuntimeModifications()).forEach(eachCount -> entities.add(getInstance(entityDefinition, false)));
        return entities;
    }

    /**
     * Get instance of an entity
     * @param entityDefinition - Entity metadata about the type of entity to be generated
     * @return GeneratorEntity
     */
    private GeneratorEntity getInstance(EntityDefinition entityDefinition, boolean isActive) {
        var newEntity = new GeneratorEntity();
        newEntity.setType(entityDefinition.getName());
        newEntity.setOtelResource(Resource.newBuilder());
        newEntity.setActive(isActive);
        for (Map.Entry<String, String> eachAttribute: entityDefinition.getAttributes().entrySet()) {
            EntityModelExpressions.expressionsGlobalKey = requestID + ":" + newEntity.getType() + ":" + eachAttribute.getKey() + ":" +
                    eachAttribute.getValue().hashCode();
            newEntity.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(eachAttribute.getKey())
                    .setValue(CommonUtils.buildAnyValue(jelProcessor.eval(eachAttribute.getValue())))
                    .build());
        }
        return newEntity;
    }

    /**
     * Apart from the number of entities mentioned in the count field for each type, we also need to consider any extra
     * entities that will be needed during the data generation based on the specified runtime modifications. All such extra entities
     * will be marked as inactive to begin with.
     * @param runtimeModifications List of all the runtime modifications for a particular entity type
     * @return Number of entities that will be needed
     */
    private int getRuntimeEntitiesCount(List<RuntimeModification> runtimeModifications) {
        if (CollectionUtils.emptyIfNull(runtimeModifications).isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RuntimeModification eachModification: CollectionUtils.emptyIfNull(runtimeModifications).stream()
                .filter(modification -> !modification.getEntityModificationType().equals(EntityModificationType.REMOVE))
                .collect(Collectors.toList())) {
            int newCount = ((eachModification.getEndAfterMinutes() - eachModification.getStartAfterMinutes())
                    / eachModification.getModificationFrequencyMinutes()) * eachModification.getModificationQuantity();
            count = count + newCount;
        }
        return count;
    }

    /**
     * Performs second phase of the entity model generation, i.e. parent-child mappings.
     */
    private void mapChildEntities() {
        //We will do this only for entities that have some children defined
        for (EntityDefinition parentType: allEntities.values().stream()
                .filter(entity -> !MapUtils.emptyIfNull(entity.getChildrenDistribution()).isEmpty())
                .collect(Collectors.toList())) {
            setParentToChildrenTypes(parentType.getName(), parentType.getChildrenDistribution().keySet());
            //For each childType-distribution expression pair
            for (Map.Entry<String, String> eachChildTypeExpr: parentType.getChildrenDistribution().entrySet()) {
                String childType = eachChildTypeExpr.getKey();
                int childrenSize = entityModel.get(childType).size();
                EntityModelExpressions.expressionsGlobalKey = requestID + ":" + parentType.getName() + ":" + childType;
                var nextChildIndex = 0;
                //For each entity of the parent type
                for (var parentCounter = 0; parentCounter < parentType.getCountWithRuntimeModifications(); parentCounter++) {
                    int count = (int) jelProcessor.eval(eachChildTypeExpr.getValue());
                    int childEndIndex = nextChildIndex + count;
                    if (childEndIndex > childrenSize) {
                        childEndIndex = childrenSize;
                    }
                    log.debug("Mapping parent entity '" + parentType.getName() + "' at index " + parentCounter +
                            " to children of type '" + childType + "' at indices range " + nextChildIndex + "-" + childEndIndex);
                    setParentToChildren(entityModel.get(parentType.getName()).get(parentCounter), childType,
                            nextChildIndex, childEndIndex);
                    nextChildIndex = nextChildIndex + count;
                    if (nextChildIndex >= childrenSize) {
                        nextChildIndex = 0;
                    }
                }
                //Map any remaining child entities to the last parent entity
                if (nextChildIndex < childrenSize) {
                    log.debug("Remaining children of type '" + childType + "' mapped to the last parent of type '" + parentType +
                            "' at index " + (parentType.getCountWithRuntimeModifications()-1));
                    setParentToChildren(entityModel.get(parentType.getName()).get(parentType.getCountWithRuntimeModifications()-1), childType,
                            nextChildIndex, childrenSize);
                }
            }
        }
    }

    /**
     * Performs third and last phase of the model generation, i.e. execution of attribute operations.
     */
    private void executeAttributeOperations() {
        Set<String> entitiesWithOperations = allEntities.values().stream()
                .filter(entity -> !CollectionUtils.emptyIfNull(entity.getAttributeOperations()).isEmpty())
                .map(EntityDefinition::getName)
                .collect(Collectors.toSet());
        //If there are some entity types that need attribute operations, we need to know the top-down levels of the types
        //since the attribute operations like copyFromParent need to be processed top -> down
        List<Set<String>> entityLevels = entitiesWithOperations.isEmpty() ? new ArrayList<>() : getEntityLevels();
        entityLevels.forEach(entities -> entities.stream()
                .filter(entitiesWithOperations::contains)
                .map(allEntities::get)
                .forEach(entityDefinition -> {
                    log.info(requestID + ": Executing " + entityDefinition.getAttributeOperations().size() +
                            " attribute operations for " + entityDefinition.getName() + " types");
                    EntityModelExpressions.expressionsGlobalKey = requestID + ":" + entityDefinition.getName();
                    for (String expression: entityDefinition.getAttributeOperations()) {
                        jelProcessor.eval(expression);
                    }
                }));
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
        //Also initialize the children types map for all the entities of parent type
        entityModel.get(parentType).forEach(parentEntity -> parentEntity.setChildrenByType(new HashMap<>()));
    }

    private void setParentToChildren(GeneratorEntity parent, String childrenType, int childStartIndex, int childEndIndex) {
        List<GeneratorEntity> children = entityModel.get(childrenType).subList(childStartIndex, childEndIndex);
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
     * all the entity types in this order with all the entities at a similar level in the hierarchy put in the same set of that level.
     * @return List of Set of all entity types at their particular level
     */
    private List<Set<String>> getEntityLevels() {
        List<Set<String>> entityLevels = new ArrayList<>();
        Map<String, Integer> parentCounts = new HashMap<>();
        typeMappings.values().forEach(eachType -> parentCounts.put(eachType.getName(), getEntityLevel(eachType.getName())));
        IntStream.range(0, Collections.max(parentCounts.values())+1).forEach(i -> entityLevels.add(new HashSet<>()));
        for(Map.Entry<String, Integer> typeLevel: parentCounts.entrySet()) {
            entityLevels.get(typeLevel.getValue()).add(typeLevel.getKey());
        }
        CollectionUtils.filter(entityLevels, level -> !level.isEmpty());
        log.debug(requestID + ": Entity levels are: " + StringUtils.join(entityLevels));
        return entityLevels;
    }

    private int getEntityLevel(String entityType) {
        if (CollectionUtils.emptyIfNull(typeMappings.get(entityType).getParentTypes()).isEmpty()) {
            return 0;
        }
        int level = typeMappings.get(entityType).getParentTypes().size();
        for (String eachParent: typeMappings.get(entityType).getParentTypes()) {
            level = Math.max(level, getEntityLevel(eachParent)+1);
        }
        return level;
    }

    /**
     * Copies an attribute key-value from the first parent of the specified type to all the entities of the current/child type.
     * This expression implementation has to be defined in this class since it needs access to the entity model.
     * @param parentType - Entity type of the parent from which to copy the attribute
     * @param attribute - Name/Key of the attribute to copy
     */
    public static void copyFromParent(String parentType, String attribute) {
        for (GeneratorEntity eachEntity: entityModel.get(EntityModelExpressions.expressionsGlobalKey.split(":")[1]).stream()
                .filter(entity -> entity.getParentsByType()!=null)
                .filter(entity -> entity.getParentsByType().containsKey(parentType))
                .collect(Collectors.toList())) {
            Optional<KeyValue> parentAttribute = eachEntity.getParentsByType().get(parentType).get(0)
                    .getOTelResource().getAttributesList().stream()
                    .filter(attrKV -> attrKV.getKey().equals(attribute)).findAny();
            String attributeValue = parentAttribute.isEmpty() ? "" : parentAttribute.get().getValue().getStringValue();
            eachEntity.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(attribute)
                    .setValue(CommonUtils.buildAnyValue(attributeValue))
                    .build());
        }
    }

    /**
     * Copies an attribute key-value from the first parent of the specified type, appends a string obtained from another expression
     * and adds it as a new attribute to all the entities of the current type. The suffix expression specified is evaluated for
     * every entity of the current type.
     * This expression implementation has to be defined in this class since it needs access to the entity model.
     * @param parentType - Parent entity type from which to copy the attribute
     * @param sourceAttribute - Name of the attribute in the parent entity
     * @param targetAttribute - Name of the attribute to be set in the current entity
     * @param suffixExpression - Expression to be evaluated for the suffix to the parent attribute value
     */
    public static void modifyFromParent(String parentType, String sourceAttribute, String targetAttribute, String suffixExpression) {
        for (GeneratorEntity eachEntity: entityModel.get(EntityModelExpressions.expressionsGlobalKey.split(":")[1])) {
            Optional<KeyValue> parentAttribute = eachEntity.getParentsByType().get(parentType).get(0)
                    .getOTelResource().getAttributesList().stream()
                    .filter(attrKV -> attrKV.getKey().equals(sourceAttribute)).findAny();
            String attributeValue = parentAttribute.isEmpty() ? "" : parentAttribute.get().getValue().getStringValue();
            if (suffixExpression.length() > 0) {
                attributeValue = attributeValue + jelProcessor.eval(suffixExpression);
            }
            eachEntity.getOTelResourceBuilder().addAttributes(KeyValue.newBuilder()
                    .setKey(targetAttribute)
                    .setValue(CommonUtils.buildAnyValue(attributeValue))
                    .build());
        }
    }
}
