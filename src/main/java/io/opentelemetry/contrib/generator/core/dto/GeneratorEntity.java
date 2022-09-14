package io.opentelemetry.contrib.generator.core.dto;

import io.opentelemetry.contrib.generator.core.utils.CommonUtils;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Internal representation of a single entity/resource.
 */
@Slf4j
@Data
public class GeneratorEntity {

    private String type; //type of this entity. There can be multiple entities of this type.
    private boolean isActive; //if this entity is currently reporting any MELT data
    private Resource.Builder otelResource; //OTel representation of the entity
    private Map<String, List<GeneratorEntity>> childrenByType; //list of all the child entities of this entity, grouped by type
    private Map<String, List<GeneratorEntity>> parentsByType; //list of all the parent entities of this entity, grouped by type

    public Resource getOTelResource() {
        return otelResource.build();
    }

    public Resource.Builder getOTelResourceBuilder() {
        return otelResource;
    }

    public Map<String, Integer> getChildrenTypeCounts() {
        Map<String, Integer> childrenTypeCounts = new HashMap<>();
        MapUtils.emptyIfNull(childrenByType)
                .forEach((key, value) -> childrenTypeCounts.put(key, CollectionUtils.emptyIfNull(value).size()));
        return childrenTypeCounts;
    }

    public Map<String, Integer> getParentTypeCounts() {
        Map<String, Integer> parentTypeCounts = new HashMap<>();
        MapUtils.emptyIfNull(parentsByType)
                .forEach((key, value) -> parentTypeCounts.put(key, CollectionUtils.emptyIfNull(value).size()));
        return parentTypeCounts;
    }

    @Override
    public String toString() {
        var separator = ",\n";
        var builder = new StringBuilder("type: ");
        builder.append(type);
        if (!CollectionUtils.emptyIfNull(otelResource.getAttributesList()).isEmpty()) {
            builder.append(separator);
            builder.append("attributes: ");
            builder.append(StringUtils.join(otelResource.getAttributesList().stream()
                    .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue))));
        }
        if (!MapUtils.emptyIfNull(childrenByType).isEmpty()) {
            builder.append(separator);
            builder.append("children: ");
            builder.append(StringUtils.join(getChildrenTypeCounts()));
        }
        if (!MapUtils.emptyIfNull(parentsByType).isEmpty()) {
            builder.append(separator);
            builder.append("parents: ");
            builder.append(StringUtils.join(getParentTypeCounts()));
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = type.hashCode();
        Map<String, String> attributes = getAttributes();
        String attributeKeys = StringUtils.join(attributes.keySet());
        String attributeVals = StringUtils.join(attributes.values());
        hashCode = hashCode + attributeKeys.hashCode() + attributeVals.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object generatorEntity) {
        if (!(generatorEntity instanceof GeneratorEntity)) {
            return false;
        }
        GeneratorEntity compareEntity = (GeneratorEntity) generatorEntity;
        if (!Objects.equals(this.type, compareEntity.type)) {
            return false;
        }
        return this.getAttributes().equals(compareEntity.getAttributes());
    }

    public Map<String, String> getAttributes() {
        Map<String, String> attrs = new HashMap<>();
        if (otelResource == null || CollectionUtils.emptyIfNull(otelResource.getAttributesList()).isEmpty()) {
            return attrs;
        }
        for (KeyValue eachAttrPair: otelResource.getAttributesList()) {
            attrs.put(eachAttrPair.getKey(), CommonUtils.anyValueToString(eachAttrPair.getValue()));
        }
        return attrs;
    }
}
