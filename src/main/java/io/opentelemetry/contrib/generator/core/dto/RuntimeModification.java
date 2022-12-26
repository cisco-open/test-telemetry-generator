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
    private String resourceType;
    @JsonIgnore
    private String modificationId;
    private ResourceModificationType resourceModificationType;
    private int modificationFrequencyMinutes;
    private int modificationQuantity;
    private int startAfterMinutes;
    private int endAfterMinutes;

    public void validate(String resourceType, int originalCount) {
        this.resourceType = resourceType;
        modificationId = RandomStringUtils.randomAlphanumeric(16);
        if (resourceModificationType == null || modificationFrequencyMinutes < 1 || modificationQuantity < 1) {
            String message = "Invalid runtime modification added for " + resourceType +
                    ". All 3 params: (resourceModificationType, modificationFrequencyMinutes, modificationQuantity) must be provided.";
            throw new GeneratorException(message);
        }
        if (resourceModificationType != ResourceModificationType.ADD && modificationQuantity > originalCount) {
            throw new GeneratorException(resourceModificationType + " modification type has a modification quantity more than the resource count for " + resourceType);
        }
        if (endAfterMinutes < modificationFrequencyMinutes) {
            throw new GeneratorException(resourceModificationType + " has an end time of " + endAfterMinutes + " minutes which exceeds the " +
                    modificationFrequencyMinutes + " minutes specified for frequency");
        }
    }
}
