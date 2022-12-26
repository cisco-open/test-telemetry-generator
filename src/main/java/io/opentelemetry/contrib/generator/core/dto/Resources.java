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

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Resources {

    private List<ResourceDefinition> resources;
    private boolean hasRuntimeModifications;

    public Set<String> validate() {
        Set<String> allResourceNames = resources.stream().map(ResourceDefinition::getName).collect(Collectors.toSet());
        for (ResourceDefinition eachType: resources) {
            eachType.validate(allResourceNames);
            hasRuntimeModifications = hasRuntimeModifications || CollectionUtils.emptyIfNull(eachType.getRuntimeModifications()).size() > 0;
        }
        return allResourceNames;
    }
}
