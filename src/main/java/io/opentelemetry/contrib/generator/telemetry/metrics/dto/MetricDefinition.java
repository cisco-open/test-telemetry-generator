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

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Data
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
    private Map<String, Object> attributes;

    public void validate(String requestID, Set<String> allResourceTypes, Integer globalPayloadFrequency, Integer globalPayloadCount) {
        validateMandatoryFields();
        validateResourceTypes(allResourceTypes);
        setPayloadFrequencyAndPayloadCount(globalPayloadFrequency, globalPayloadCount);
        attributes = GeneratorUtils.addArgsToAttributeExpressions(requestID, "metric", name, attributes);
        if (isDouble == null) {
            isDouble = false;
        }
        if (isMonotonic == null) {
            isMonotonic = false;
        }
    }

    private void validateMandatoryFields() {
        validateMandatoryField("UNKNOWN", "name", name);
        validateMandatoryField(name, "unit", unit);
        validateMandatoryField(name, "otelType", otelType);
        validateMandatoryField(name, "valueFunction", valueFunction);
        if (CollectionUtils.emptyIfNull(reportingResources).isEmpty()) {
            throw new GeneratorException("Mandatory field 'reportingResources' not provided in metrics definition YAML for metric " + name);
        }
        validateOTelType();
    }

    private void validateMandatoryField(String metricName, String fieldName, String fieldValue) {
        if (StringUtils.defaultString(fieldValue).isBlank()) {
            throw new GeneratorException("Mandatory field '" + fieldName + "' not provided in metrics definition YAML for metric: " + metricName);
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
                throw new GeneratorException("OTel type for metric " + name + " is of 'sum' type but Aggregation temporality not provided");
            }
            //Check aggregation temporality is valid
            if (!(aggregationTemporality.equalsIgnoreCase(Constants.CUMULATIVE) || aggregationTemporality.equalsIgnoreCase(Constants.DELTA))) {
                throw new GeneratorException("Invalid aggregation temporality " + aggregationTemporality + " specified for metric " +
                        name + ". Valid types are (Cumulative, Delta)");
            }
        }
        validateAttributes();
    }

    private void validateAttributes() {
        attributes = GeneratorUtils.validateAttributes(attributes);
    }

    private void validateResourceTypes(Set<String> allResourceTypes) {
        for (String eachResource: reportingResources) {
            if (!allResourceTypes.contains(eachResource)) {
                throw new GeneratorException("Invalid resource type (" + eachResource + ") found in metric definition YAML " +
                        "for metric " + name);
            }
        }
    }

    private void setPayloadFrequencyAndPayloadCount(Integer globalPayloadFrequencySeconds, Integer globalPayloadCount) {
        if (payloadFrequencySeconds == null) {
            payloadFrequencySeconds = globalPayloadFrequencySeconds;
        } else {
            if (payloadFrequencySeconds < 10) {
                throw new GeneratorException("Override payload frequency defined for metric " + name + " is less than 10 seconds");
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
