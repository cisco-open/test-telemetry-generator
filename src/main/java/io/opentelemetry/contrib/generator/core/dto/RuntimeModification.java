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
