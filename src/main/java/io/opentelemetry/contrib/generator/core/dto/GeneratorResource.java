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
 * Internal representation of a single resource.
 */
@Slf4j
@Data
public class GeneratorResource {

    private String type; //type of this resource. There can be multiple resources of this type.
    private boolean isActive; //if this resource is currently reporting any MELT data
    private Resource.Builder otelResource; //OTel representation of the resource
    private Map<String, String> evaluatedAttributes;
    private Map<String, List<GeneratorResource>> childrenByType; //list of all the child resources of this resource, grouped by type
    private Map<String, List<GeneratorResource>> parentsByType; //list of all the parent resources of this resource, grouped by type

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

    public void setEvaluatedAttributes() {
        evaluatedAttributes = new HashMap<>();
        for (KeyValue eachKv: CollectionUtils.emptyIfNull(otelResource.getAttributesList())) {
            evaluatedAttributes.put(eachKv.getKey(), CommonUtils.anyValueToString(eachKv.getValue()));
        }
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
    public boolean equals(Object generatorResource) {
        if (!(generatorResource instanceof GeneratorResource compareResource)) {
            return false;
        }
        if (!Objects.equals(this.type, compareResource.type)) {
            return false;
        }
        return this.getAttributes().equals(compareResource.getAttributes());
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
