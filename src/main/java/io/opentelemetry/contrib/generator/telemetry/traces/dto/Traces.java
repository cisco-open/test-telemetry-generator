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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Data
public class Traces {

    private Integer traceCompletionTimeoutSecs;
    private List<RootSpanDefinition> rootSpans;
    private List<SpanDefinition> childSpans;
    @JsonIgnore
    @Getter
    private long maxPostSeconds;

    public void validate(String requestID, Set<String> allEntityTypes) {
        if (traceCompletionTimeoutSecs == null || traceCompletionTimeoutSecs < 30) {
            throw new GeneratorException("Trace completion timeout cannot be less than 30 seconds");
        }
        if (CollectionUtils.emptyIfNull(rootSpans).isEmpty()) {
            throw new GeneratorException("No root spans specified");
        }
        Set<String> rootSpanNames = rootSpans.stream().map(SpanDefinition::getName).collect(Collectors.toSet());
        if (rootSpanNames.size() < rootSpans.size()) {
            throw new GeneratorException("Root span names are not unique");
        }
        Set<String> childSpanNames = childSpans.stream().map(SpanDefinition::getName).collect(Collectors.toSet());
        if (childSpanNames.size() < childSpans.size()) {
            throw new GeneratorException("Child span names are not unique");
        }
        Set<String> allSpanNames = new HashSet<>();
        allSpanNames.addAll(rootSpanNames);
        allSpanNames.addAll(childSpanNames);
        if (allSpanNames.size() < (childSpanNames.size() + rootSpanNames.size())) {
            throw new GeneratorException("Some root spans and child spans have the same name");
        }
        rootSpans.forEach(eachSpan -> eachSpan.validate(requestID, traceCompletionTimeoutSecs, allEntityTypes, childSpanNames));
        childSpans.forEach(eachSpan -> eachSpan.validate(requestID, allEntityTypes, childSpanNames));
    }

    /**
     * For traces, we do some pre-processing after the validation stage and before the data generation starts: <p>
     *     - All the spans in each tree are initialized with their parent & child span nodes <p>
     *     - All the spans in each tree are set up in post order fashion
     *     - If numberOfSpansInTree/timeToPostTree > 0.5 the tree is split up into different parts
     *     - Time offsets are set up for each span node
     */
    public void initTrees(String requestID) {
        log.info(requestID + ": Initializing " + rootSpans.size() + " trace trees");
        initTraceTrees();
        rootSpans.forEach(eachTree -> {
            if (eachTree.getPayloadFrequencySeconds() == null || eachTree.getPayloadFrequencySeconds() <= 0) {
                eachTree.setPayloadFrequencySeconds(getPayloadFrequency(eachTree.getTotalChildNodeCount()));
            }
            eachTree.setTreeParts(getTreeParts(eachTree.getTotalChildNodeCount(), eachTree.getPayloadFrequencySeconds()));
            long totalTreePostSecs = (long) eachTree.getTreeParts() * eachTree.getPayloadFrequencySeconds();
            setSpanTimeOffsets(eachTree, 0, TimeUnit.SECONDS.toMillis(totalTreePostSecs),
                    TimeUnit.SECONDS.toMillis(totalTreePostSecs));
            eachTree.setTreeNodesPostOrder(getTreePostOrder(eachTree));
            eachTree.setSpansIndexMap(getSpansIndexMap(eachTree));
            maxPostSeconds = Math.max(maxPostSeconds, totalTreePostSecs * eachTree.getPayloadCount());
            log.debug(requestID + ": Trace tree initialized as: " + eachTree);
        });
    }

    private void initTraceTrees() {
        Map<String, SpanDefinition> childSpansMap = childSpans.stream()
                .collect(Collectors.toMap(SpanDefinition::getName, Function.identity()));
        for (RootSpanDefinition eachRootSpan: rootSpans) {
            int nodeCount = 1;
            //Initialize each span node with the list of its child span nodes and their total counts
            nodeCount = nodeCount + initChildSpanNodes(eachRootSpan.getName(), childSpansMap, eachRootSpan);
            eachRootSpan.setTotalChildNodeCount(nodeCount);
        }
    }

    private int initChildSpanNodes(String rootSpanName, Map<String, SpanDefinition> childSpansMap, SpanDefinition spanNode) {
        if (CollectionUtils.emptyIfNull(spanNode.getChildSpans()).isEmpty()) {
            spanNode.setTotalChildNodeCount(0);
            return 0;
        }
        spanNode.setChildSpanNodes(new ArrayList<>());
        int allChildrenCount = spanNode.getChildSpans().size();
        for (String eachChild: spanNode.getChildSpans()) {
            spanNode.getChildSpanNodes().add(childSpansMap.get(eachChild));
            childSpansMap.get(eachChild).getParentNodes().put(rootSpanName, spanNode);
            allChildrenCount = allChildrenCount + initChildSpanNodes(rootSpanName, childSpansMap, childSpansMap.get(eachChild));
        }
        spanNode.setTotalChildNodeCount(allChildrenCount);
        return allChildrenCount;
    }

    private int getPayloadFrequency(int totalNodeCount) {
        //If the payload frequency is not provided, we set a frequency based on the total number of nodes
        //while maintaining the hard upper limit of trace completion timeout
        List<Integer> possibleFrequencies = new ArrayList<>();
        int currTime = 15;
        while (currTime <= traceCompletionTimeoutSecs) {
            possibleFrequencies.add(currTime);
            currTime = currTime + 15;
        }
        //At minimum, 2 values (15, 30) will always exist in possibleFrequencies since traceCompletionTimeoutSecs >= 30
        int calculatedIndex = totalNodeCount/5;
        calculatedIndex = Math.min(calculatedIndex, possibleFrequencies.size()-1);
        return possibleFrequencies.get(calculatedIndex);
    }

    private int getTreeParts(int totalNodes, int payloadFrequency) {
        //Whether the payload frequency was user supplied or set by us, in case the number of nodes to be processed
        //in each payload is high, we will split the tree to be sent in parts while maintaining the constraint
        //that the full tree must be sent within the trace completion timeout
        if (totalNodes < 5) {
            return 1;
        }
        int treeParts = 1;
        double nodesPerSec = (double) totalNodes / payloadFrequency;
        while ((payloadFrequency * treeParts) <= traceCompletionTimeoutSecs && nodesPerSec >= 0.5d) {
            treeParts++;
            nodesPerSec = (double) totalNodes / (payloadFrequency * treeParts);
        }
        return (payloadFrequency * treeParts > traceCompletionTimeoutSecs) ? treeParts-1 : treeParts;
    }

    private void setSpanTimeOffsets(SpanDefinition spanNode, long startTimeOffset, long endTimeOffset, long totalTreeTime) {
        if (spanNode.getSpanStartTimePct() != null) {
            startTimeOffset = totalTreeTime * spanNode.getSpanStartTimePct() / 100;
            endTimeOffset = totalTreeTime * spanNode.getSpanEndTimePct() / 100;
        }
        spanNode.setStartTimeMillisOffset(startTimeOffset);
        spanNode.setEndTimeMillisOffset(endTimeOffset);
        int immediateChildrenCount = CollectionUtils.emptyIfNull(spanNode.getChildSpanNodes()).size();
        if (immediateChildrenCount == 0) {
            return;
        }
        long eachChildRange = (endTimeOffset - startTimeOffset)/immediateChildrenCount;
        for (int i=0; i<immediateChildrenCount; i++) {
            setSpanTimeOffsets(spanNode.getChildSpanNodes().get(i), startTimeOffset + (eachChildRange * i),
                    startTimeOffset + (eachChildRange * (i+1)), totalTreeTime);
        }
    }

    private List<SpanDefinition> getTreePostOrder(SpanDefinition spanNode) {
        List<SpanDefinition> treePostOrder = new ArrayList<>();
        if (spanNode.getTotalChildNodeCount() > 0) {
            CollectionUtils.emptyIfNull(spanNode.getChildSpanNodes())
                    .forEach(eachChild -> treePostOrder.addAll(getTreePostOrder(eachChild)));
        }
        treePostOrder.add(spanNode);
        return treePostOrder;
    }

    private Map<String, Integer> getSpansIndexMap(RootSpanDefinition rootSpanDefinition) {
        Map<String, Integer> spanMap = new HashMap<>();
        IntStream.range(0, rootSpanDefinition.getTreeNodesPostOrder().size())
                .forEach(index -> spanMap.put(rootSpanDefinition.getTreeNodesPostOrder().get(index).getName(), index));
        return spanMap;
    }
}
