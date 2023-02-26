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

package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TestAllGeneratorsWithCopyResourceAttributes {

    private final String RESOURCES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "resource-definition-small.yaml").toString();
    private final String METRICS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "metrics-test.yaml").toString();
    private final String LOGS_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "logs-test.yaml").toString();
    private final String TRACES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "trace-definition.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private final String NODEIPAttr = "k8s.node.ip.internal", PODIPAttr = "k8s.pod.ip";
    private TestPayloadHandler testStore;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(RESOURCES_YAML)
                .withMetricDefinitionYAML(METRICS_YAML)
                .withLogDefinitionYAML(LOGS_YAML)
                .withTraceDefinitionYAML(TRACES_YAML)
                .build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
    }

    @Test
    public void testCopiedResourceAttributesInMetrics() {
        String cpuUsed = "cpu.used", podRestarts = "pod.restarts", nwOut = "system.network.out.kb.sec";
        String fsUsed = "filesystem.used";
        List<ResourceMetrics> allMetricPackets = testStore.getMetricPayloads().stream()
                .map(ExportMetricsServiceRequest::getResourceMetricsList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (ResourceMetrics eachPacket: allMetricPackets) {
            String nodeIpValue = getAttributeValue(eachPacket.getResource(), NODEIPAttr);
            String podIpValue = getAttributeValue(eachPacket.getResource(), PODIPAttr);
            List<NumberDataPoint> cpuUsedDPs = getSumGaugeMetricDPs(eachPacket, cpuUsed, true);
            List<NumberDataPoint> podRestartDPs = getSumGaugeMetricDPs(eachPacket, podRestarts, false);
            List<SummaryDataPoint> nwOutDPs = getSummaryMetricDPs(eachPacket, nwOut);
            if (!cpuUsedDPs.isEmpty()) {
                validateAttributeInNumDPs(cpuUsedDPs, NODEIPAttr, nodeIpValue);
                validateAttributeInNumDPs(cpuUsedDPs, PODIPAttr, podIpValue);
            }
            if (!podRestartDPs.isEmpty()) {
                validateAttributeInNumDPs(podRestartDPs, NODEIPAttr, nodeIpValue);
                validateAttributeInNumDPs(podRestartDPs, PODIPAttr, podIpValue);
            }
            if (!nwOutDPs.isEmpty()) {
                validateAttributeInSummaryDPs(nwOutDPs, NODEIPAttr, nodeIpValue);
                validateAttributeInSummaryDPs(nwOutDPs, PODIPAttr, podIpValue);
            }
            List<NumberDataPoint> fsUsedDPs = getSumGaugeMetricDPs(eachPacket, fsUsed, false);
            if (!fsUsedDPs.isEmpty()) {
                validateAttrNotExistsInDP(fsUsedDPs, NODEIPAttr);
                validateAttrNotExistsInDP(fsUsedDPs, PODIPAttr);
            }
        }
    }

    @Test
    public void testCopiedResourceAttributesInLogs() {
        List<ResourceLogs> allLogsPackets = testStore.getLogsPayloads().stream()
                .map(ExportLogsServiceRequest::getResourceLogsList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (ResourceLogs eachPacket: allLogsPackets) {
            String nodeIpValue = getAttributeValue(eachPacket.getResource(), NODEIPAttr);
            String podIpValue = getAttributeValue(eachPacket.getResource(), PODIPAttr);
            List<LogRecord> allRecords = eachPacket.getScopeLogsList().stream()
                    .map(ScopeLogs::getLogRecordsList)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            validateAttributeInLogRecords(allRecords, NODEIPAttr, nodeIpValue);
            validateAttributeInLogRecords(allRecords, PODIPAttr, podIpValue);
        }
    }

    @Test
    public void testCopiedResourceAttributesInSpans() {
        String healthCheck = "healthCheck", createAcct = "createNewAccount", delAcctHttp = "deleteAccountHTTPRequest",
                delAcctDb = "deleteAccountQuery";
        String createAcctHttp = "createAccountHTTPRequest";
        String httpPort = "http_port", type = "type", dbType = "database.type", name = "name";
        List<ResourceSpans> allSpanPackets = testStore.getTracePayloads().stream()
                .map(ExportTraceServiceRequest::getResourceSpansList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (ResourceSpans eachPacket: allSpanPackets) {
            String httpPortVal = getAttributeValue(eachPacket.getResource(), httpPort);
            String typeVal = getAttributeValue(eachPacket.getResource(), type);
            String dbTypeVal = getAttributeValue(eachPacket.getResource(), dbType);
            String nameVal = getAttributeValue(eachPacket.getResource(), name);
            List<Span> healthCheckSpans = getFilteredSpans(eachPacket, healthCheck);
            List<Span> createAcctSpans = getFilteredSpans(eachPacket, createAcct);
            List<Span> delAcctHttpSpans = getFilteredSpans(eachPacket, delAcctHttp);
            List<Span> delAcctDbSpans = getFilteredSpans(eachPacket, delAcctDb);
            if (!healthCheckSpans.isEmpty()) {
                validateAttributeInSpans(healthCheckSpans, httpPort, httpPortVal);
            }
            if (!createAcctSpans.isEmpty()) {
                validateAttributeInSpans(createAcctSpans, type, typeVal);
            }
            if (!delAcctHttpSpans.isEmpty()) {
                validateAttributeInSpans(delAcctHttpSpans, httpPort, httpPortVal);
            }
            if (!delAcctDbSpans.isEmpty()) {
                validateAttributeInSpans(delAcctDbSpans, dbType, dbTypeVal);
                validateAttributeInSpans(delAcctDbSpans, name, nameVal);
            }
            List<Span> createAcctHttpSpans = getFilteredSpans(eachPacket, createAcctHttp);
            if (!createAcctHttpSpans.isEmpty()) {
                validateAttributeNotExistsInSpans(createAcctHttpSpans, httpPort);
                validateAttributeNotExistsInSpans(createAcctHttpSpans, name);
            }
        }
    }

    private void validateAttributeInLogRecords(List<LogRecord> logRecords, String attrName, String attrValue) {
        for (LogRecord eachRecord: logRecords) {
            Optional<KeyValue> attr = eachRecord.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isPresent());
            Assert.assertEquals(attr.get().getValue().getStringValue(), attrValue);
        }
    }

    private void validateAttrNotExistsInDP(List<NumberDataPoint> numDPs, String attrName) {
        for (NumberDataPoint eachDP: numDPs) {
            Optional<KeyValue> attr = eachDP.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isEmpty());
        }
    }

    private void validateAttributeInNumDPs(List<NumberDataPoint> numDPs, String attrName, String attrValue) {
        for (NumberDataPoint eachDP: numDPs) {
            Optional<KeyValue> attr = eachDP.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isPresent());
            Assert.assertEquals(attr.get().getValue().getStringValue(), attrValue);
        }
    }

    private void validateAttributeInSummaryDPs(List<SummaryDataPoint> summaryDPs, String attrName, String attrValue) {
        for (SummaryDataPoint eachDP: summaryDPs) {
            Optional<KeyValue> attr = eachDP.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isPresent());
            Assert.assertEquals(attr.get().getValue().getStringValue(), attrValue);
        }
    }

    private String getAttributeValue(Resource resource, String attrName) {
        Optional<KeyValue> attr = resource.getAttributesList().stream()
                .filter(kv -> kv.getKey().equals(attrName))
                .findAny();
        return attr.map(keyValue -> keyValue.getValue().getStringValue()).orElse("");
    }

    private List<NumberDataPoint> getSumGaugeMetricDPs(ResourceMetrics metricPacket, String metricName, boolean isGauge) {
        List<Metric> metrics = metricPacket.getScopeMetricsList().stream()
                .map(ScopeMetrics::getMetricsList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        List<NumberDataPoint> dataPoints = new ArrayList<>();
        for (Metric eachMetric: metrics) {
            if (eachMetric.getName().equals(metricName)) {
                dataPoints.addAll(isGauge ? eachMetric.getGauge().getDataPointsList() : eachMetric.getSum().getDataPointsList());
            }
        }
        return dataPoints;
    }

    private List<SummaryDataPoint> getSummaryMetricDPs(ResourceMetrics metricPacket, String metricName) {
        List<Metric> metrics = metricPacket.getScopeMetricsList().stream()
                .map(ScopeMetrics::getMetricsList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        List<SummaryDataPoint> dataPoints = new ArrayList<>();
        for (Metric eachMetric: metrics) {
            if (eachMetric.getName().equals(metricName)) {
                dataPoints.addAll(eachMetric.getSummary().getDataPointsList());
            }
        }
        return dataPoints;
    }

    private List<Span> getFilteredSpans(ResourceSpans resourceSpan, String spanName) {
        List<Span> filteredSpans = new ArrayList<>();
        List<Span> allSpans = resourceSpan.getScopeSpansList().stream()
                .map(ScopeSpans::getSpansList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (Span eachSpan: allSpans) {
            if (eachSpan.getName().equals(spanName)) {
                filteredSpans.add(eachSpan);
            }
        }
        return filteredSpans;
    }

    private void validateAttributeInSpans(List<Span> spans, String attrName, String attrValue) {
        for (Span eachSpan: spans) {
            Optional<KeyValue> attr = eachSpan.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isPresent());
            Assert.assertEquals(attr.get().getValue().getStringValue(), attrValue);
        }
    }

    private void validateAttributeNotExistsInSpans(List<Span> spans, String attrName) {
        for (Span eachSpan: spans) {
            Optional<KeyValue> attr = eachSpan.getAttributesList().stream()
                    .filter(kv -> kv.getKey().equals(attrName))
                    .findAny();
            Assert.assertTrue(attr.isEmpty());
        }
    }
}