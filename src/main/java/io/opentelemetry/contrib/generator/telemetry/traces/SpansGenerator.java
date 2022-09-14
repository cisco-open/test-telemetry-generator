package io.opentelemetry.contrib.generator.telemetry.traces;

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.telemetry.EntityModelProvider;
import io.opentelemetry.contrib.generator.telemetry.jel.JELProvider;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.RootSpanDefinition;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.SpanDefinition;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import jakarta.el.ELProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SpansGenerator {

    private final RootSpanDefinition traceTree;
    private final String groupName;
    private final int groupOffset;
    private final String requestID;
    private final ELProcessor jelProcessor;
    private ByteString[] traceIds;
    private long[] startTimes;
    private long[] endTimes;
    @Getter
    private int currentPostCount;
    @Getter
    private int currentTreePart;

    public SpansGenerator(String groupName, RootSpanDefinition traceTree, String requestID) {
        this.traceTree = traceTree;
        this.groupName = groupName;
        this.requestID = requestID;
        groupOffset = Integer.parseInt(groupName.split("::group::")[1]);
        jelProcessor = JELProvider.getJelProcessor();
        currentPostCount = 0;
        currentTreePart = 0;
    }

    public ExportTraceServiceRequest getTraces() {
        log.debug(requestID + ": Received get traces request for " + groupName);
        if (currentTreePart == 0) {
            initTimesAndTraceId();
            currentPostCount++;
        }
        Map<String, List<List<Span>>> spanCopiesByResource = getSpanCopiesByResource();
        Map<String, List<GeneratorEntity>> entityModel = EntityModelProvider.getEntityModel(requestID);
        List<ResourceSpans> resourceSpansList = new ArrayList<>();
        ResourceSpans resourceSpans;
        for (Map.Entry<String, List<List<Span>>> eachSpanGroup: spanCopiesByResource.entrySet()) {
            for (var copyIndex=0; copyIndex<traceTree.getCopyCount(); copyIndex++) {
                var copyIndexFinal = copyIndex;
                List<GeneratorEntity> validEntities = entityModel.get(eachSpanGroup.getKey()).stream()
                        .filter(GeneratorEntity::isActive).collect(Collectors.toList());
                int resourceIndex = (currentPostCount + copyIndex) % validEntities.size();
                List<Span> spans = eachSpanGroup.getValue().stream().map(list -> list.get(copyIndexFinal)).collect(Collectors.toList());
                resourceSpans = ResourceSpans.newBuilder()
                        .setResource(validEntities.get(resourceIndex).getOTelResource())
                        .addInstrumentationLibrarySpans(InstrumentationLibrarySpans.newBuilder()
                                .setInstrumentationLibrary(InstrumentationLibrary.newBuilder()
                                        .setName("@opentelemetry/test-telemetry-generator")
                                        .setVersion("22.5.0")
                                        .build())
                                .addAllSpans(spans)
                                .build())
                        .build();
                resourceSpansList.add(resourceSpans);
            }
        }
        return ExportTraceServiceRequest.newBuilder().addAllResourceSpans(resourceSpansList).build();
    }

    private Map<String, List<List<Span>>> getSpanCopiesByResource() {
        int[] spanIndices = getSpanIndices();
        log.debug(requestID + ": Preparing " + (spanIndices[1]-spanIndices[0]) + " spans with " + traceTree.getCopyCount() +
                " copies for " + groupName);
        Map<String, List<List<Span>>> spanCopiesByResource = new HashMap<>();
        var spanIndex = spanIndices[0];
        int spanErrorCode = getSpanStatusCode(spanIndex);
        SpanDefinition spanDefinition;
        Span singleSpan;
        while (spanIndex < spanIndices[1] && spanErrorCode != 2) {
            spanDefinition = traceTree.getTreeNodesPostOrder().get(spanIndex);
            spanCopiesByResource.putIfAbsent(spanDefinition.getReportingResource(), new ArrayList<>());
            singleSpan = getSingleSpan(spanDefinition, spanIndex, spanErrorCode == 1);
            //Each span list in the list of lists of map consists of copies of a single span
            //For each new span and its copies, a new list is added
            spanCopiesByResource.get(spanDefinition.getReportingResource()).add(getSpanCopies(singleSpan, spanDefinition));
            spanIndex++;
            spanErrorCode = spanIndex < spanIndices[1] ? getSpanStatusCode(spanIndex) : spanErrorCode;
        }
        if (spanIndex < spanIndices[1]) {
            //If the previous loop ended before reaching the last span, it means we encountered an error span and the trace tree
            //is configured to end the complete trace if an error span is encountered. In such a case, we get the current span
            //and recursively get all parents of that span and mark them as error spans also to complete the trace in error.
            Map<String, List<List<Span>>> errorSpansByResource = getRecursiveErrorSpans(traceTree.getTreeNodesPostOrder().get(spanIndex));
            //We reset the current tree part also
            currentTreePart = 0;
            for (Map.Entry<String, List<List<Span>>> errorSpan: errorSpansByResource.entrySet()) {
                if (!spanCopiesByResource.containsKey(errorSpan.getKey())) {
                    spanCopiesByResource.put(errorSpan.getKey(), errorSpan.getValue());
                } else {
                    spanCopiesByResource.get(errorSpan.getKey()).addAll(errorSpan.getValue());
                }
            }
        }
        return spanCopiesByResource;
    }

    private void initTimesAndTraceId() {
        traceIds = new ByteString[traceTree.getCopyCount()];
        for (var copyIndex=0; copyIndex<traceTree.getCopyCount(); copyIndex++) {
            traceIds[copyIndex] = getId(traceTree.getName(), copyIndex, true);
        }
        int spansSize = traceTree.getTreeNodesPostOrder().size();
        startTimes = new long[spansSize];
        endTimes = new long[spansSize];
        long baseTime = System.currentTimeMillis();
        for (var spanIndex=0; spanIndex<spansSize; spanIndex++) {
            startTimes[spanIndex] = baseTime + traceTree.getTreeNodesPostOrder().get(spanIndex).getStartTimeMillisOffset();
            endTimes[spanIndex] = baseTime + traceTree.getTreeNodesPostOrder().get(spanIndex).getEndTimeMillisOffset();
        }
    }

    private int[] getSpanIndices() {
        int[] indices = new int[2];
        if (traceTree.getTreeParts() == 1) {
            indices[1] = traceTree.getTreeNodesPostOrder().size();
            return indices;
        }
        int spansPerPart = traceTree.getTotalChildNodeCount() / traceTree.getTreeParts();
        indices[0] = currentTreePart * spansPerPart;
        indices[1] = currentTreePart == (traceTree.getTreeParts()-1) ?
                traceTree.getTreeNodesPostOrder().size() : (currentTreePart + 1) * spansPerPart;
        currentTreePart = currentTreePart == (traceTree.getTreeParts()-1) ? 0 : currentTreePart + 1;
        return indices;
    }

    private Span getSingleSpan(SpanDefinition spanDefinition, int spanIndex, boolean isErrorNode) {
        Map<String, Object> modifiedAttrs = new HashMap<>();
        for (Map.Entry<String, Object> eachAttr: spanDefinition.getAttributes().entrySet()) {
            String modifiedExpression = eachAttr.getValue().toString()
                    .replace(":HOLDER:", groupName+"::"+spanDefinition.getName());
            modifiedAttrs.put(eachAttr.getKey(), modifiedExpression);
        }
        return Span.newBuilder()
                .setName(spanDefinition.getName())
                .setKind(spanDefinition.getSpanKind())
                .setStartTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(startTimes[spanIndex]))
                .setEndTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(endTimes[spanIndex]))
                .addAllAttributes(GeneratorUtils.getEvaluatedAttributes(jelProcessor, modifiedAttrs))
                .setStatus(Status.newBuilder().setCode(isErrorNode ?
                        Status.StatusCode.STATUS_CODE_ERROR : Status.StatusCode.STATUS_CODE_OK).build())
                .build();
    }

    private List<Span> getSpanCopies(Span span, SpanDefinition spanDefinition) {
        List<Span> spanCopies = new ArrayList<>();
        Span.Builder eachCopy;
        for (var copyIndex=0; copyIndex<traceTree.getCopyCount(); copyIndex++) {
            eachCopy = Span.newBuilder(span)
                    .setTraceId(traceIds[copyIndex])
                    .setSpanId(getId(span.getName(), copyIndex, false));
            if (spanDefinition.getParentNodes().containsKey(traceTree.getName())) {
                eachCopy.setParentSpanId(getId(spanDefinition.getParentNodes().get(traceTree.getName()).getName(), copyIndex, false));
            }
            spanCopies.add(eachCopy.build());
        }
        return spanCopies;
    }

    private Map<String, List<List<Span>>> getRecursiveErrorSpans(SpanDefinition errorNode) {
        Map<String, List<List<Span>>> errorSpansByResource = new HashMap<>();
        SpanDefinition currentNode = errorNode;
        Span singleErrorSpan;
        while (currentNode != null) {
            errorSpansByResource.putIfAbsent(currentNode.getReportingResource(), new ArrayList<>());
            singleErrorSpan = getSingleSpan(currentNode, traceTree.getSpansIndexMap().get(currentNode.getName()), true);
            errorSpansByResource.get(currentNode.getReportingResource()).add(getSpanCopies(singleErrorSpan, currentNode));
            currentNode = currentNode.getParentNodes().get(traceTree.getName());
        }
        return errorSpansByResource;
    }

    private int getSpanStatusCode(int spanIndex) {
        int spanErrorFreq = traceTree.getTreeNodesPostOrder().get(spanIndex).getErrorFrequency();
        if (spanErrorFreq == 0 || currentPostCount % spanErrorFreq != 0) {
            return 0;
        }
        return traceTree.getSpanErrorEndsTrace() ? 2 : 1;
    }

    private ByteString getId(String spanName, int copyIndex, boolean isTrace) {
        int actualCopyIndex = (groupOffset * traceTree.getCopyCount()) + copyIndex;
        String name = isTrace ?
                "trace-" + spanName + "_" + actualCopyIndex + "_" + currentPostCount :
                spanName + "_" + actualCopyIndex + "_" + currentPostCount;
        return ByteString.copyFromUtf8(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString());
    }

}
