package io.opentelemetry.contrib.generator.core.dto;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;

@Data
public class RuntimeModification {

    @JsonIgnore
    private String entityType;
    @JsonIgnore
    private String modificationId;
    private EntityModificationType entityModificationType;
    private int modificationFrequencyMinutes;
    private int modificationQuantity;
    private int startAfterMinutes;
    private int endAfterMinutes;

    public void validate(String entityType, int originalCount) {
        this.entityType = entityType;
        modificationId = RandomStringUtils.randomAlphanumeric(16);
        if (entityModificationType == null || modificationFrequencyMinutes < 1 || modificationQuantity < 1) {
            String message = "Invalid runtime modification added for " + entityType +
                    ". All 3 params: (entityModificationType, modificationFrequencyMinutes, modificationQuantity) must be provided.";
            throw new GeneratorException(message);
        }
        if (entityModificationType != EntityModificationType.ADD && modificationQuantity > originalCount) {
            throw new GeneratorException(entityModificationType + " modification type has a modification quantity more than the entity count for " + entityType);
        }
        if (endAfterMinutes < modificationFrequencyMinutes) {
            throw new GeneratorException(entityModificationType + " has an end time of " + endAfterMinutes + " minutes which exceeds the " +
                    modificationFrequencyMinutes + " minutes specified for frequency");
        }
    }
}
