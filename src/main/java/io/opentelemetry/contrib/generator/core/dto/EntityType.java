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
