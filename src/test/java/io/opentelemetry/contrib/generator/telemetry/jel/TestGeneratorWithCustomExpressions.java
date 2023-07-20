package io.opentelemetry.contrib.generator.telemetry.jel;

import io.opentelemetry.contrib.generator.core.dto.GeneratorResource;
import io.opentelemetry.contrib.generator.core.jel.ResourceExpressionsJELProvider;
import io.opentelemetry.contrib.generator.telemetry.TelemetryGenerator;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.helpers.TestPayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TestGeneratorWithCustomExpressions {

    private final String defsRoot = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "jel-custom-expressions").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;
    private TransportStorage transportStorage;
    private Map<String, List<GeneratorResource>> resourceModel;
    private final String parent_resource = "parent_resource";
    private final String child1 = "child_resource_1";
    private final String child2 = "child_resource_2";
    private final int parentResourceCount = 5;
    private final int childRsrc1Count = 32;
    private final int childRsrc2Count = 10;

    @BeforeClass
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(Paths.get(defsRoot, "resources.yaml").toString())
                .withMetricDefinitionYAML(Paths.get(defsRoot, "metrics.yaml").toString())
                .withLogDefinitionYAML(Paths.get(defsRoot, "logs.yaml").toString())
                .withTraceDefinitionYAML(Paths.get(defsRoot, "traces.yaml").toString())
                .build();
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "geometricWith2");
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "increaseByXDecreaseByY");
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "increasingErrorSeverity");
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomIPv6");
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomUUID");
        ResourceExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomHexadecimal");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "geometricWith2");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "increaseByXDecreaseByY");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "increasingErrorSeverity");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomIPv6");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomUUID");
        MELTExpressionsJELProvider.addExpression("", "", "io.opentelemetry.contrib.generator.telemetry.jel.CustomExpressions", "randomHexadecimal");
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
        transportStorage = telemetryGenerator.getTransportStorage();
        resourceModel = telemetryGenerator.getResourceModel();
    }

    @Test
    public void validateResourceCounts() {
        Assert.assertTrue(resourceModel.containsKey(parent_resource));
        Assert.assertTrue(resourceModel.containsKey(child1));
        Assert.assertTrue(resourceModel.containsKey(child2));
        Assert.assertEquals(resourceModel.get(parent_resource).size(), parentResourceCount);
        Assert.assertEquals(resourceModel.get(child1).size(), childRsrc1Count);
        Assert.assertEquals(resourceModel.get(child2).size(), childRsrc2Count);
    }

    @Test(dependsOnMethods = {"validateResourceCounts"})
    public void validateParentChildMapping() {
        List<GeneratorResource> parentResources = resourceModel.get(parent_resource);
        int expectedChild1Count, expectedChild2Count = 2;
        for (int i=0; i<parentResourceCount; i++) {
            Map<String, Integer> childrenTypeCounts = parentResources.get(i).getChildrenTypeCounts();
            expectedChild1Count = i==0 ? 1 : (i==4 ? (int) Math.pow(2, i) + 1 : (int) Math.pow(2, i));
            Assert.assertTrue(childrenTypeCounts.containsKey(child1));
            Assert.assertTrue(childrenTypeCounts.containsKey(child2));
            Assert.assertEquals(childrenTypeCounts.get(child1), expectedChild1Count);
            Assert.assertEquals(childrenTypeCounts.get(child2), expectedChild2Count);
        }
        resourceModel.get(child1).forEach(child ->
                Assert.assertEquals(child.getParentsByType().get(parent_resource).size(), 1));
        resourceModel.get(child2).forEach(child ->
                Assert.assertEquals(child.getParentsByType().get(parent_resource).size(), 1));
    }

    @Test(dependsOnMethods = {"validateParentChildMapping"})
    public void validateResourceAttrs() {
        List<GeneratorResource> parentResources = resourceModel.get(parent_resource);
        for (int i=0; i<parentResourceCount; i++) {
            Assert.assertTrue(parentResources.get(i).getAttributes().containsKey("resource.name"));
            Assert.assertEquals(parentResources.get(i).getAttributes().get("resource.name"),
                    "custom-expr-resource-" + (i+1));
            Assert.assertTrue(parentResources.get(i).getAttributes().containsKey("resource.id"));
            Assert.assertEquals(parentResources.get(i).getAttributes().get("resource.id").length(), 32);
        }
        resourceModel.get(child1).forEach(child -> {
            Assert.assertTrue(child.getAttributes().containsKey("resource.ip"));
            Assert.assertEquals(child.getAttributes().get("resource.ip").length(), 39);
            Assert.assertTrue(child.getAttributes().containsKey("parent.resource.name"));
        });
    }

    @Test(dependsOnMethods = {"validateResourceAttrs"})
    public void validatePacketCounts() {
        int expectedMetricCounts = (parentResourceCount + childRsrc1Count + childRsrc2Count) * 5;
        int expectedLogCounts = (childRsrc1Count + childRsrc2Count) * 5;
        int expectedSpanCounts = 3 * 2;
        Assert.assertEquals(testStore.getMetricsPacketCount(), expectedMetricCounts);
        Assert.assertEquals(testStore.getLogsPacketCount(), expectedLogCounts);
        Assert.assertEquals(testStore.getTracePacketCount(), expectedSpanCounts);
    }

    @Test(dependsOnMethods = "validatePacketCounts")
    public void validateMetricValues() {
        String groupKeySuffix = "::" + 10 + "::" + 5;
        String parentGroupKey = parent_resource + groupKeySuffix;
        String child1GroupKey = child1 + groupKeySuffix;
        String child2GroupKey = child2 + groupKeySuffix;
        List<ExportMetricsServiceRequest> parentMetrics = transportStorage.getStoredMetricsPayloads().get(parentGroupKey);
        List<ExportMetricsServiceRequest> child1Metrics = transportStorage.getStoredMetricsPayloads().get(child1GroupKey);
        List<ExportMetricsServiceRequest> child2Metrics = transportStorage.getStoredMetricsPayloads().get(child2GroupKey);
        for (int i=0; i<5; i++) {
            assertExpectedMetricValue(i, getActualMetricValue(parentMetrics.get(i)));
            assertExpectedMetricValue(i, getActualMetricValue(child1Metrics.get(i)));
            assertExpectedMetricValue(i, getActualMetricValue(child2Metrics.get(i)));
        }
    }

    @Test(dependsOnMethods = "validatePacketCounts")
    public void validateMetricAttributes() {
        String groupKeySuffix = "::" + 10 + "::" + 5;
        String parentGroupKey = parent_resource + groupKeySuffix;
        String child1GroupKey = child1 + groupKeySuffix;
        String child2GroupKey = child2 + groupKeySuffix;
        List<ExportMetricsServiceRequest> parentMetrics = transportStorage.getStoredMetricsPayloads().get(parentGroupKey);
        List<ExportMetricsServiceRequest> child1Metrics = transportStorage.getStoredMetricsPayloads().get(child1GroupKey);
        List<ExportMetricsServiceRequest> child2Metrics = transportStorage.getStoredMetricsPayloads().get(child2GroupKey);
        for (int i=0; i<5; i++) {
            List<KeyValue> parentMetricAttrs = getMetricAttributes(parentMetrics.get(i));
            List<KeyValue> child1MetricAttrs = getMetricAttributes(child1Metrics.get(i));
            List<KeyValue> child2MetricAttrs = getMetricAttributes(child2Metrics.get(i));
            Assert.assertTrue(keyExists(parentMetricAttrs, "internal.ip"));
            Assert.assertEquals(getValue(parentMetricAttrs, "internal.ip").length(), 39);
            Assert.assertTrue(keyExists(parentMetricAttrs, "resource.name"));
            Assert.assertFalse(getValue(parentMetricAttrs, "resource.name").isBlank());
            Assert.assertTrue(keyExists(parentMetricAttrs, "resource.ip"));
            Assert.assertTrue(getValue(parentMetricAttrs, "resource.ip").isBlank());
            Assert.assertTrue(keyExists(parentMetricAttrs, "resource.id"));
            Assert.assertEquals(getValue(parentMetricAttrs, "resource.id").length(), 32);
            Assert.assertTrue(keyExists(child1MetricAttrs, "internal.ip"));
            Assert.assertEquals(getValue(child1MetricAttrs, "internal.ip").length(), 39);
            Assert.assertTrue(keyExists(child1MetricAttrs, "resource.name"));
            Assert.assertEquals(getValue(child1MetricAttrs, "resource.name").length(), 11);
            Assert.assertTrue(keyExists(child1MetricAttrs, "resource.ip"));
            Assert.assertEquals(getValue(child1MetricAttrs, "resource.ip").length(), 39);
            Assert.assertTrue(keyExists(child1MetricAttrs, "resource.id"));
            Assert.assertTrue(getValue(child1MetricAttrs, "resource.id").isBlank());
            Assert.assertTrue(keyExists(child2MetricAttrs, "internal.ip"));
            Assert.assertEquals(getValue(child2MetricAttrs, "internal.ip").length(), 39);
            Assert.assertTrue(keyExists(child2MetricAttrs, "resource.name"));
            Assert.assertTrue(getValue(child2MetricAttrs, "resource.name").isBlank());
            Assert.assertTrue(keyExists(child2MetricAttrs, "resource.ip"));
            Assert.assertTrue(getValue(child2MetricAttrs, "resource.ip").isBlank());
            Assert.assertTrue(keyExists(child2MetricAttrs, "resource.id"));
            Assert.assertEquals(getValue(child2MetricAttrs, "resource.id").length(), 36);
        }
    }

    @Test(dependsOnMethods = {"validatePacketCounts"})
    public void validateLogSeverity() {
        List<ExportLogsServiceRequest> child1Logs = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get(child1);
        List<ExportLogsServiceRequest> child2Logs = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get(child2);
        for (int i=0; i<5; i++) {
            String expectedSeverity = i==4 ? "ERROR" : "INFO";
            Assert.assertEquals(getLogSeverity(child1Logs.get(i)), expectedSeverity);
            Assert.assertEquals(getLogSeverity(child2Logs.get(i)), expectedSeverity);
        }
    }

    @Test(dependsOnMethods = {"validatePacketCounts"})
    public void validateLogAttributes() {
        List<ExportLogsServiceRequest> child1Logs = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get(child1);
        List<ExportLogsServiceRequest> child2Logs = transportStorage.getStoredLogsPayloads().get("log_by_ttg_0").get(child2);
        for (int i=0; i<5; i++) {
            List<KeyValue> child1LogAttrs = getLogAttributes(child1Logs.get(i));
            List<KeyValue> child2LogAttrs = getLogAttributes(child2Logs.get(i));
            Assert.assertTrue(keyExists(child1LogAttrs, "log.uuid"));
            Assert.assertEquals(getValue(child1LogAttrs, "log.uuid").length(), 36);
            Assert.assertTrue(keyExists(child1LogAttrs, "resource.name"));
            Assert.assertEquals(getValue(child1LogAttrs, "resource.name").length(), 11);
            Assert.assertTrue(keyExists(child1LogAttrs, "resource.ip"));
            Assert.assertEquals(getValue(child1LogAttrs, "resource.ip").length(), 39);
            Assert.assertTrue(keyExists(child1LogAttrs, "resource.id"));
            Assert.assertTrue(getValue(child1LogAttrs, "resource.id").isBlank());
            Assert.assertTrue(keyExists(child2LogAttrs, "log.uuid"));
            Assert.assertEquals(getValue(child2LogAttrs, "log.uuid").length(), 36);
            Assert.assertTrue(keyExists(child2LogAttrs, "resource.name"));
            Assert.assertTrue(getValue(child2LogAttrs, "resource.name").isBlank());
            Assert.assertTrue(keyExists(child2LogAttrs, "resource.ip"));
            Assert.assertTrue(getValue(child2LogAttrs, "resource.ip").isBlank());
            Assert.assertTrue(keyExists(child2LogAttrs, "resource.id"));
            Assert.assertEquals(getValue(child2LogAttrs, "resource.id").length(), 36);
        }
    }

    @Test(dependsOnMethods = {"validatePacketCounts"})
    public void validateSpanAttributes() {
        String traceGroupName = "customExprRootSpan::group::0";
        transportStorage.getStoredTracesPayloads().get(traceGroupName).forEach(traceServiceRequest -> {
            List<KeyValue> spanAttrs = getAttributesForSpan(traceServiceRequest, "customExprSpan");
            Assert.assertEquals(spanAttrs.size(), 1);
            Assert.assertNotNull(spanAttrs.get(0).getKey());
            Assert.assertEquals(spanAttrs.get(0).getKey(), "incoming.request.id");
            Assert.assertEquals(spanAttrs.get(0).getValue().getStringValue().length(), 32);
        });
    }

    private List<KeyValue> getAttributesForSpan(ExportTraceServiceRequest traceServiceRequest, String spanName) {
        return traceServiceRequest.getResourceSpansList().stream()
                .map(ResourceSpans::getScopeSpansList).flatMap(List::stream)
                .map(ScopeSpans::getSpansList).flatMap(List::stream)
                .filter(span -> span.getName().equals(spanName))
                .findAny().get()
                .getAttributesList();
    }

    private String getLogSeverity(ExportLogsServiceRequest logsServiceRequest) {
        return logsServiceRequest.getResourceLogsList().stream().findAny().get()
                .getScopeLogs(0).getLogRecords(0).getSeverityText();
    }

    private List<KeyValue> getLogAttributes(ExportLogsServiceRequest logsServiceRequest) {
        return logsServiceRequest.getResourceLogsList().stream().findAny().get()
                .getScopeLogs(0).getLogRecords(0).getAttributesList();
    }

    private List<KeyValue> getMetricAttributes(ExportMetricsServiceRequest metricsServiceRequest) {
        return metricsServiceRequest.getResourceMetricsList().stream().findAny().get()
                .getScopeMetrics(0).getMetrics(0).getGauge().getDataPoints(0).getAttributesList();
    }

    private boolean keyExists(List<KeyValue> kvList, String key) {
        for (KeyValue eachKV: kvList) {
            if (eachKV.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private String getValue(List<KeyValue> kvList, String key) {
        for (KeyValue eachKV: kvList) {
            if (eachKV.getKey().equals(key)) {
                return eachKV.getValue().getStringValue();
            }
        }
        return null;
    }

    private int getActualMetricValue(ExportMetricsServiceRequest metricsServiceRequest) {
        return (int) metricsServiceRequest.getResourceMetricsList().stream().findAny().get()
                .getScopeMetrics(0).getMetrics(0).getGauge().getDataPoints(0).getAsInt();
    }

    private void assertExpectedMetricValue(int index, int actual) {
        switch (index) {
            case 0 -> Assert.assertTrue(actual >= 100);
            case 1 -> Assert.assertTrue(actual >= 106);
            case 2 -> Assert.assertTrue(actual >= 109);
            case 3 -> Assert.assertTrue(actual >= 115);
            case 4 -> Assert.assertTrue(actual >= 118);
        }
    }
}
