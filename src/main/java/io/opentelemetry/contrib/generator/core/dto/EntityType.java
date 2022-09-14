package io.opentelemetry.contrib.generator.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class EntityType {

    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private Set<String> parentTypes;
    @Setter
    private Set<String> childTypes;

    public EntityType(String name) {
        this.name = name;
    }
}
