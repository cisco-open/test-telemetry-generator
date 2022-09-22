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

package io.opentelemetry.contrib.generator.core.utils;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommonUtils {

    private CommonUtils() {}

    /**
     * Convert a Java object to its corresponding OpenTelemetry representation.
     * @param value Java object
     * @return OpenTelemetry AnyValue
     */
    @SuppressWarnings("unchecked")
    public static AnyValue buildAnyValue(Object value) {
        if (value instanceof String) {
            return AnyValue.newBuilder().setStringValue((String) value).build();
        } else if (value instanceof Boolean) {
            return AnyValue.newBuilder().setBoolValue((Boolean) value).build();
        } else if (value instanceof Long || value instanceof Integer) {
            return AnyValue.newBuilder().setIntValue(Long.parseLong(value.toString())).build();
        } else if (value instanceof Double) {
            return AnyValue.newBuilder().setDoubleValue((Double) value).build();
        } else if (value instanceof List) {
            return AnyValue.newBuilder().setArrayValue(getArrayValueFromList((List<Object>) value)).build();
        } else if (value instanceof Map) {
            return AnyValue.newBuilder().setKvlistValue(getKvListFromMap((Map<String, Object>) value)).build();
        } else if (value != null){
            return AnyValue.newBuilder().setBytesValue(ByteString.copyFromUtf8(value.toString())).build();
        } else {
            return null;
        }
    }

    /**
     * Convert OpenTelemetry AnyValue to its corresponding Java object representation.
     * @param anyValue OpenTelemetry AnyValue
     * @return Java object
     */
    public static String anyValueToString(AnyValue anyValue) {
        if (anyValue.hasStringValue()) {
            return anyValue.getStringValue();
        } else if (anyValue.hasBoolValue()) {
            return String.valueOf(anyValue.getBoolValue());
        } else if (anyValue.hasIntValue()) {
            return String.valueOf(anyValue.getIntValue());
        } else if (anyValue.hasDoubleValue()) {
            return String.valueOf(anyValue.getDoubleValue());
        } else if (anyValue.hasArrayValue()) {
            List<AnyValue> values = anyValue.getArrayValue().getValuesList();
            return StringUtils.join(values.stream()
                    .map(CommonUtils::anyValueToString).collect(Collectors.toList()));
        } else if (anyValue.hasKvlistValue()) {
            List<KeyValue> kvList = anyValue.getKvlistValue().getValuesList();
            return StringUtils.join(kvList.stream()
                    .map(kv -> kv.getKey() + ":" + anyValueToString(kv.getValue())).collect(Collectors.toList()));
        } else if (anyValue.hasBytesValue()) {
            return anyValue.getBytesValue().toStringUtf8();
        } else {
            return null;
        }
    }

    private static ArrayValue getArrayValueFromList(List<Object> values) {
        List<AnyValue> arrayValues = new ArrayList<>();
        AnyValue nextValue;
        for (Object eachValue: CollectionUtils.emptyIfNull(values)) {
            nextValue = buildAnyValue(eachValue);
            arrayValues.add(nextValue);
        }
        return ArrayValue.newBuilder().addAllValues(arrayValues).build();
    }

    private static KeyValueList getKvListFromMap(Map<String, Object> value) {
        List<KeyValue> kvList = new ArrayList<>();
        KeyValue nextKv;
        for (Map.Entry<String, Object> eachEntry: MapUtils.emptyIfNull(value).entrySet()) {
            nextKv = KeyValue.newBuilder().setKey(eachEntry.getKey()).setValue(buildAnyValue(eachEntry.getValue())).build();
            kvList.add(nextKv);
        }
        return KeyValueList.newBuilder().addAllValues(kvList).build();
    }
}
