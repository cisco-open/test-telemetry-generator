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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class EntityDefinition {

    private String name;
    private Integer count;
    private Map<String, String> childrenDistribution;
    private Map<String, String> attributes;
    private List<String> attributeOperations;
    private List<RuntimeModification> runtimeModifications;
    @JsonIgnore
    private Integer countWithRuntimeModifications;

    public void validate(Set<String> allEntityNames) {
        validateMandatoryFields();
        validateAttributes();
        validateChildren(allEntityNames);
        validateRuntimeModifications();
    }
    
    private void validateMandatoryFields() {
        name = StringUtils.defaultString(name).trim();
        if (name.isBlank()) {
            throw new GeneratorException("Mandatory field 'name' not provided in entity definition YAML");
        }
        if (count == null || count < 1) {
            throw new GeneratorException("Mandatory field 'count' not provided or less than 1 in entity definition YAML for entity");
        }
    }

    private void validateAttributes() {
        if (MapUtils.emptyIfNull(attributes).isEmpty()) {
            throw new GeneratorException("At least one item must be available in 'attributes' for entity " + name);
        }
        Map<String, String> attrs = new HashMap<>();
        for (Map.Entry<String, String> eachAttribute: attributes.entrySet()) {
            if (eachAttribute.getKey().trim().length() == 0 || eachAttribute.getValue().trim().length() == 0) {
                throw new GeneratorException("Blank key or value found in 'attributes' for entity " + name);
            }
            attrs.put(eachAttribute.getKey().trim(), eachAttribute.getValue().trim());
        }
        attributes = attrs;
    }

    private void validateChildren(Set<String> allEntityNames) {
        if (childrenDistribution == null) {
            return;
        }
        Map<String, String> children = new HashMap<>();
        for (Map.Entry<String, String> eachChild: childrenDistribution.entrySet()) {
            if (!allEntityNames.contains(eachChild.getKey().trim())) {
                throw new GeneratorException("Unknown child entity name '" + eachChild.getKey() + "' provided for entity " + name);
            }
            children.put(eachChild.getKey().trim(), eachChild.getValue().trim());
        }
        childrenDistribution = children;
    }

    private void validateRuntimeModifications() {
        for (RuntimeModification eachModification: CollectionUtils.emptyIfNull(runtimeModifications)) {
            eachModification.validate(name, count);
        }
    }
}
