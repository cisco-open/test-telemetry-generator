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

package io.opentelemetry.contrib.generator.telemetry.metrics.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Data
@Slf4j
public class MetricDefinition implements Cloneable {

    private String name;
    private String unit;
    private String otelType;
    private String aggregationTemporality;
    private Boolean isMonotonic;
    private Boolean isDouble;
    private List<Double> quantiles;
    private String valueFunction;
    private Integer payloadFrequencySeconds;
    private Integer payloadCount;
    private Set<String> reportingResources;
    private Map<String, Set<String>> filteredReportingResources;
    private Set<String> copyResourceAttributes;
    private Map<String, Object> attributes;
    @JsonIgnore
    private Map<String, Map<String, String>> parsedFilteredReportingResources;

    public void validate(String requestID, Set<String> allResourceTypes, Integer globalPayloadFrequency,
                         Integer globalPayloadCount) {
        validateMandatoryFields();
        validateResourceTypes(allResourceTypes);
        parseFilteredReportingResources();
        setPayloadFrequencyAndPayloadCount(globalPayloadFrequency, globalPayloadCount);
        attributes = GeneratorUtils.addArgsToAttributeExpressions(requestID, "metric", name, attributes);
        if (isDouble == null) {
            isDouble = false;
        }
        if (isMonotonic == null) {
            isMonotonic = false;
        }
        if (copyResourceAttributes == null) {
            copyResourceAttributes = new HashSet<>();
        }
    }

    private void validateMandatoryFields() {
        validateMandatoryField("UNKNOWN", "name", name);
        validateMandatoryField(name, "unit", unit);
        validateMandatoryField(name, "otelType", otelType);
        validateMandatoryField(name, "valueFunction", valueFunction);
        if (CollectionUtils.emptyIfNull(reportingResources).isEmpty() &&
                MapUtils.emptyIfNull(filteredReportingResources).isEmpty()) {
            throw new GeneratorException("At least one resource type must be specified in either reportingResources" +
                    " or filteredReportingResources for metric " + name);
        }
        validateOTelType();
    }

    private void validateMandatoryField(String metricName, String fieldName, String fieldValue) {
        if (StringUtils.defaultString(fieldValue).isBlank()) {
            throw new GeneratorException("Mandatory field '" + fieldName + "' not provided in the metrics definition " +
                    "YAML for metric: " + metricName);
        }
    }

    private void validateOTelType() {
        if (!Constants.validMetricTypes.contains(otelType)) {
            throw new GeneratorException("Invalid OTeltype '" + otelType + "' found for metric " + name  + " ." +
                    "Valid types are " + StringUtils.join(Constants.validMetricTypes));
        }
        validateAggregationTemporalityForSum();
    }

    private void validateAggregationTemporalityForSum() {
        if (otelType.equalsIgnoreCase(Constants.SUM)) {
            if (aggregationTemporality==null || aggregationTemporality.isBlank()) {
                throw new GeneratorException("OTel type for metric " + name + " is of 'sum' type but Aggregation " +
                        "temporality not provided");
            }
            //Check aggregation temporality is valid
            if (!(aggregationTemporality.equalsIgnoreCase(Constants.CUMULATIVE) ||
                    aggregationTemporality.equalsIgnoreCase(Constants.DELTA))) {
                throw new GeneratorException("Invalid aggregation temporality " + aggregationTemporality +
                        " specified for metric " + name + ". Valid types are (Cumulative, Delta)");
            }
        }
        validateAttributes();
    }

    private void validateAttributes() {
        attributes = GeneratorUtils.validateAttributes(attributes);
    }

    private void validateResourceTypes(Set<String> allResourceTypes) {
        for (String eachResource: CollectionUtils.emptyIfNull(reportingResources)) {
            if (!allResourceTypes.contains(eachResource)) {
                throw new GeneratorException("Invalid resource type (" + eachResource + ") found in " +
                        "reportingResources for metric " + name + " in the metric definition YAML ");
            }
        }
        for (String resourceType: MapUtils.emptyIfNull(filteredReportingResources).keySet()) {
            if (!allResourceTypes.contains(resourceType)) {
                throw new GeneratorException("Invalid resource type (" + resourceType + ") found in " +
                        "filteredReportingResources for metric " + name + " in the metric definition YAML ");
            }
        }
    }

    private void setPayloadFrequencyAndPayloadCount(Integer globalPayloadFrequencySeconds, Integer globalPayloadCount) {
        if (payloadFrequencySeconds == null) {
            payloadFrequencySeconds = globalPayloadFrequencySeconds;
        } else {
            if (payloadFrequencySeconds < 10) {
                throw new GeneratorException("Override payload frequency defined for metric " + name +
                        " is less than 10 seconds");
            }
        }
        if (payloadCount == null) {
            payloadCount = globalPayloadCount;
        } else {
            if (payloadCount < 1) {
                throw new GeneratorException("Override payload count defined for metric " + name + " is less than 1");
            }
        }
    }

    private void parseFilteredReportingResources() {
        if (!MapUtils.emptyIfNull(filteredReportingResources).isEmpty()) {
            parsedFilteredReportingResources = new HashMap<>();
            for (Map.Entry<String, Set<String>> eachFilteredReportingResource: filteredReportingResources.entrySet()) {
                String resourceType = eachFilteredReportingResource.getKey();
                parsedFilteredReportingResources.put(resourceType, new HashMap<>());
                for (String attributeFilter : eachFilteredReportingResource.getValue()) {
                    String[] filterTokens = attributeFilter.split("=");
                    if (filterTokens.length != 2) {
                        log.warn("Attribute filter " + attributeFilter + " provided for resource type " +
                                resourceType + " in the metric definition YAML for metric " + name + " is not valid. " +
                                "Must contain a single '='");
                        continue;
                    }
                    parsedFilteredReportingResources.get(resourceType).put(filterTokens[0], filterTokens[1]);
                }
            }
        }
    }

    public AggregationTemporality getAggregationTemporality() {
        if (aggregationTemporality == null) {
            return AggregationTemporality.AGGREGATION_TEMPORALITY_UNSPECIFIED;
        }
        if (aggregationTemporality.equalsIgnoreCase(Constants.CUMULATIVE)) {
            return AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE;
        } else if (aggregationTemporality.equalsIgnoreCase(Constants.DELTA)) {
            return AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA;
        } else {
            return AggregationTemporality.AGGREGATION_TEMPORALITY_UNSPECIFIED;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object metricDefinition) {
        if (this == metricDefinition) {
            return true;
        }
        if (metricDefinition instanceof MetricDefinition) {
            return this.name.equals(((MetricDefinition) metricDefinition).getName());
        }
        return false;
    }

    @Override
    public MetricDefinition clone() throws CloneNotSupportedException {
        return (MetricDefinition) super.clone();
    }
}
