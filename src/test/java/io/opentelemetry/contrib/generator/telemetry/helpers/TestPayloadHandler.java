package io.opentelemetry.contrib.generator.telemetry.helpers;

import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import com.google.protobuf.GeneratedMessageV3;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
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
    private final ConcurrentMap<String, AtomicInteger> logsCount;
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
        logsCount = new ConcurrentHashMap<>();
        metricPayloads = Collections.synchronizedList(new ArrayList<>());
        tracePayloads = Collections.synchronizedList(new ArrayList<>());
        logsPayloads = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public boolean postPayload(GeneratedMessageV3 message) {
        if (message instanceof ExportMetricsServiceRequest) {
            ExportMetricsServiceRequest payload = (ExportMetricsServiceRequest) message;
            metricPayloads.add(payload);
            int resourceMetricsCount = payload.getResourceMetricsCount();
            metricsPacketCount.addAndGet(resourceMetricsCount);
            payload.getResourceMetrics(0)
                    .getInstrumentationLibraryMetrics(0)
                    .getMetricsList()
                    .forEach(metric -> {
                        if (!metricsCount.containsKey(metric.getName())) {
                            synchronized (metricsCount) {
                                metricsCount.putIfAbsent(metric.getName(), new AtomicInteger(0));
                            }
                        }
                        metricsCount.get(metric.getName()).addAndGet(resourceMetricsCount);
                    });
        } else if (message instanceof ExportLogsServiceRequest) {
            ExportLogsServiceRequest payload = (ExportLogsServiceRequest) message;
            logsPayloads.add(payload);
            int resourceLogsCount = payload.getResourceLogsCount();
            logsPacketCount.addAndGet(resourceLogsCount);
            payload.getResourceLogs(0)
                    .getInstrumentationLibraryLogs(0)
                    .getLogsList()
                    .forEach(log -> {
                        if (!logsCount.containsKey(log.getName())) {
                            synchronized (logsCount) {
                                logsCount.putIfAbsent(log.getName(), new AtomicInteger(0));
                            }
                        }
                        logsCount.get(log.getName()).addAndGet(resourceLogsCount);
                    });
        } else if (message instanceof ExportTraceServiceRequest) {
            ExportTraceServiceRequest payload = (ExportTraceServiceRequest) message;
            tracePayloads.add(payload);
            int spanPacketCount = payload.getResourceSpansCount();
            spansPacketCount.addAndGet(spanPacketCount);
            List<ResourceSpans> resourceSpans = payload.getResourceSpansList();
            for (ResourceSpans resourceSpan : resourceSpans) {
                List<InstrumentationLibrarySpans> instrumentationLibrarySpans = resourceSpan.getInstrumentationLibrarySpansList();
                for (InstrumentationLibrarySpans instrumentationLibrarySpan : instrumentationLibrarySpans) {
                    List<Span> spans = instrumentationLibrarySpan.getSpansList();
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

    public Map<String, AtomicInteger> getLogsCount() {
        return logsCount;
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
