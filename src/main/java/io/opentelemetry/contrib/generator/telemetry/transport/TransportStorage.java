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

package io.opentelemetry.contrib.generator.telemetry.transport;

import com.google.protobuf.GeneratedMessageV3;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class TransportStorage {

    private ConcurrentMap<String, List<ExportMetricsServiceRequest>> storedMetricsPayloads;
    private ConcurrentMap<String, Map<String, List<ExportLogsServiceRequest>>> storedLogsPayloads;
    private ConcurrentMap<String, List<ExportTraceServiceRequest>> storedTracesPayloads;
    private ConcurrentMap<String, List<Boolean>> metricsResponses;
    private ConcurrentMap<String, Map<String, List<Boolean>>> logsResponses;
    private ConcurrentMap<String, List<Boolean>> tracesResponses;

    public void initMetricResponseMaps() {
        metricsResponses = new ConcurrentHashMap<>();
        storedMetricsPayloads = new ConcurrentHashMap<>();
    }

    public void initLogResponseMaps() {
        logsResponses = new ConcurrentHashMap<>();
        storedLogsPayloads = new ConcurrentHashMap<>();
    }

    public void initTraceResponseMaps() {
        tracesResponses = new ConcurrentHashMap<>();
        storedTracesPayloads = new ConcurrentHashMap<>();
    }

    public void store(String entityType, GeneratedMessageV3 payload, boolean isSuccess) {
        if (payload instanceof ExportMetricsServiceRequest) {
            metricsResponses.putIfAbsent(entityType, new ArrayList<>());
            storedMetricsPayloads.putIfAbsent(entityType, new ArrayList<>());
            metricsResponses.get(entityType).add(isSuccess);
            storedMetricsPayloads.get(entityType).add((ExportMetricsServiceRequest) payload);
        } else {
            tracesResponses.putIfAbsent(entityType, new ArrayList<>());
            storedTracesPayloads.putIfAbsent(entityType, new ArrayList<>());
            tracesResponses.get(entityType).add(isSuccess);
            storedTracesPayloads.get(entityType).add((ExportTraceServiceRequest) payload);
        }
    }

    public void store(String logName, String entityType, GeneratedMessageV3 payload, boolean isSuccess){
        if (payload instanceof ExportLogsServiceRequest) {
            logsResponses.putIfAbsent(logName, new HashMap<>());
            storedLogsPayloads.putIfAbsent(logName, new HashMap<>());
            logsResponses.get(logName).putIfAbsent(entityType, new ArrayList<>());
            storedLogsPayloads.get(logName).putIfAbsent(entityType, new ArrayList<>());
            logsResponses.get(logName).get(entityType).add(isSuccess);
            storedLogsPayloads.get(logName).get(entityType).add((ExportLogsServiceRequest) payload);
        }
    }
}
