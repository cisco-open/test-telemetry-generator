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

package io.opentelemetry.contrib.generator.telemetry.helpers;

import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import com.google.protobuf.GeneratedMessageV3;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TestPayloadHandler implements PayloadHandler {

    private final AtomicInteger metricsPacketCount;
    private final AtomicInteger logsPacketCount;
    private final AtomicInteger spansPacketCount;
    private final ConcurrentMap<String, AtomicInteger> metricsCount;
    private final ConcurrentMap<String, AtomicInteger> spanCount;
    private final List<ExportMetricsServiceRequest> metricPayloads;
    private final List<ExportLogsServiceRequest> logsPayloads;
    private final List<ExportTraceServiceRequest> tracePayloads;

    public TestPayloadHandler() {
        metricsPacketCount = new AtomicInteger(0);
        logsPacketCount = new AtomicInteger(0);
        spansPacketCount = new AtomicInteger(0);
        metricsCount = new ConcurrentHashMap<>();
        spanCount = new ConcurrentHashMap<>();
        metricPayloads = Collections.synchronizedList(new ArrayList<>());
        tracePayloads = Collections.synchronizedList(new ArrayList<>());
        logsPayloads = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public boolean postPayload(GeneratedMessageV3 message) {
        if (message instanceof ExportMetricsServiceRequest payload) {
            metricPayloads.add(payload);
            int resourceMetricsCount = payload.getResourceMetricsCount();
            metricsPacketCount.addAndGet(resourceMetricsCount);
            for (ResourceMetrics eachRM: payload.getResourceMetricsList()) {
                eachRM.getScopeMetrics(0).getMetricsList().forEach(metric -> {
                    if (!metricsCount.containsKey(metric.getName())) {
                        synchronized (metricsCount) {
                            metricsCount.putIfAbsent(metric.getName(), new AtomicInteger(0));
                        }
                    }
                    metricsCount.get(metric.getName()).addAndGet(1);
                });
            }
        } else if (message instanceof ExportLogsServiceRequest payload) {
            logsPayloads.add(payload);
            int resourceLogsCount = payload.getResourceLogsCount();
            logsPacketCount.addAndGet(resourceLogsCount);
        } else if (message instanceof ExportTraceServiceRequest payload) {
            tracePayloads.add(payload);
            int spanPacketCount = payload.getResourceSpansCount();
            spansPacketCount.addAndGet(spanPacketCount);
            List<ResourceSpans> resourceSpans = payload.getResourceSpansList();
            for (ResourceSpans resourceSpan : resourceSpans) {
                List<ScopeSpans> scopeSpans = resourceSpan.getScopeSpansList();
                for (ScopeSpans scopeSpan : scopeSpans) {
                    List<Span> spans = scopeSpan.getSpansList();
                    for (Span span : spans) {
                        if (!spanCount.containsKey(span.getName())) {
                            synchronized (spanCount) {
                                if (!spanCount.containsKey(span.getName())) {
                                    spanCount.put(span.getName(), new AtomicInteger(0));
                                }
                            }
                        }
                        spanCount.get(span.getName()).incrementAndGet();
                    }
                }
            }
        }
        return true;
    }

    public int getMetricsPacketCount() {
        return metricsPacketCount.get();
    }

    public Map<String, AtomicInteger> getMetricsCount() {
        return metricsCount;
    }

    public List<ExportMetricsServiceRequest> getMetricPayloads() {
        return metricPayloads;
    }

    public int getLogsPacketCount() {
        return logsPacketCount.get();
    }

    public List<ExportLogsServiceRequest> getLogsPayloads() {
        return logsPayloads;
    }

    public int getTracePacketCount() {
        return spansPacketCount.get();
    }

    public Map<String, AtomicInteger> getSpanCount() {
        return spanCount;
    }

    public List<ExportTraceServiceRequest> getTracePayloads () {
        return tracePayloads;
    }
}
