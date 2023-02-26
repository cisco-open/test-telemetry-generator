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

package io.opentelemetry.contrib.generator.telemetry.traces.dto;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SpanDefinition {

    private String name;
    private Span.SpanKind spanKind;
    private Integer errorFrequency;
    private List<String> childSpans;
    private String reportingResource;
    private Integer spanStartTimePct;
    private Integer spanEndTimePct;
    private Set<String> copyResourceAttributes;
    private Map<String, Object> attributes;
    @JsonIgnore
    private List<SpanDefinition> childSpanNodes;
    @JsonIgnore
    private Map<String, SpanDefinition> parentNodes;
    @JsonIgnore
    private int totalChildNodeCount;
    @JsonIgnore
    private long startTimeMillisOffset;
    @JsonIgnore
    private long endTimeMillisOffset;

    public void validate(String requestID, Set<String> allResourceTypes, Set<String> allSpanNames) {
        validateMandatoryFields();
        validateResourceTypes(allResourceTypes);
        validateChildSpans(allSpanNames);
        validateTimeOffsets();
        attributes = GeneratorUtils.validateAttributes(attributes);
        attributes = GeneratorUtils.addArgsToAttributeExpressions(requestID, "span", ":HOLDER:", attributes);
        if (spanKind == null) {
            spanKind = Span.SpanKind.SPAN_KIND_UNSPECIFIED;
        }
        if (errorFrequency == null) {
            errorFrequency = 0;
        }
        if (copyResourceAttributes == null) {
            copyResourceAttributes = new HashSet<>();
        }
        parentNodes = new HashMap<>();
    }

    private void validateMandatoryFields() {
        if (StringUtils.defaultString(name).isBlank()) {
            throw new GeneratorException("Mandatory field 'name' not provided in trace definition YAML");
        }
        if (StringUtils.defaultString(reportingResource).isBlank()) {
            throw new GeneratorException("Mandatory field 'reportingResource' not provided in trace definition YAML");
        }
    }

    public void validateResourceTypes(Set<String> resourceTypes) {
        if (!resourceTypes.contains(reportingResource)) {
            throw new GeneratorException("Invalid resource type (" + reportingResource + ") found in trace definition YAML " +
                    "for span " + name);
        }
    }

    public void validateTimeOffsets() {
        if (spanStartTimePct != null && spanEndTimePct == null) {
            throw new GeneratorException("Start time percent provided but end time percent not provided for span " + name);
        }
        if (spanEndTimePct != null && spanStartTimePct == null) {
            throw new GeneratorException("End time percent provided but start time percent not provided for span " + name);
        }
        if (spanStartTimePct != null) {
            if (spanStartTimePct < 0 || spanStartTimePct > 99) {
                throw new GeneratorException("Start time percent for span " + name + " is invalid. Valid range is 0-99");
            }
            if (spanEndTimePct < 1) {
                throw new GeneratorException("End time percent for span " + name + " is invalid. Valid range is 1-100");
            }
            if (spanStartTimePct >= spanEndTimePct) {
                throw new GeneratorException("Specified span start time percent cannot be equal to or more than span end time percent");
            }
        }
    }

    private void validateChildSpans(Set<String> allSpanNames) {
        Set<String> unknownChildSpans = CollectionUtils.emptyIfNull(childSpans).stream()
                .filter(childSpan -> !allSpanNames.contains(childSpan))
                .collect(Collectors.toSet());
        if (unknownChildSpans.size() > 0) {
            throw new GeneratorException("Span with name " + name + " has unknown child spans: " + unknownChildSpans);
        }
    }

    @Override
    public String toString() {
        StringBuilder spanString = new StringBuilder("name: " + name + ", \n");
        spanString.append("spanKind: ").append(spanKind.toString()).append(", \n");
        spanString.append("reportingResource: ").append(reportingResource).append(", \n");
        spanString.append("errorFrequency: ").append(errorFrequency).append(", \n");
        spanString.append("attributes: ").append(StringUtils.join(MapUtils.emptyIfNull(attributes))).append(", \n");
        spanString.append("childSpans: ").append(StringUtils.join(CollectionUtils.emptyIfNull(childSpans))).append(", \n");
        Map<String, String> parentNodesInTree = new HashMap<>();
        parentNodes.forEach((treeName, parentNode) -> parentNodesInTree.put(treeName, parentNode.getName()));
        spanString.append("parentSpans: ").append(StringUtils.join(parentNodesInTree)).append(", \n");
        spanString.append("totalChildNodeCount: ").append(totalChildNodeCount);
        return spanString.toString();
    }
}
