package io.opentelemetry.contrib.generator.core.dto;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Entities {

    private List<EntityDefinition> entities;
    private boolean hasRuntimeModifications;

    public Set<String> validate() {
        Set<String> allEntityNames = entities.stream().map(EntityDefinition::getName).collect(Collectors.toSet());
        for (EntityDefinition eachType: entities) {
            eachType.validate(allEntityNames);
            hasRuntimeModifications = hasRuntimeModifications || CollectionUtils.emptyIfNull(eachType.getRuntimeModifications()).size() > 0;
        }
        return allEntityNames;
    }
}
