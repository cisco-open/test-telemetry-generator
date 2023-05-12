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

package io.opentelemetry.contrib.generator.telemetry.traces;

import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorExceptionHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.RootSpanDefinition;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.Traces;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for initializing and scheduling all the trace generator threads.
 */
@Slf4j
public class TracesGenerator {

    private final PayloadHandler payloadHandler;
    private final String requestID;
    private final TransportStorage transportStorage;
    private final Traces traces;
    private final Map<String, RootSpanDefinition> traceGroups;
    private GeneratorState<TraceGeneratorThread> generatorState;

    public TracesGenerator(Traces traces, PayloadHandler payloadHandler, String requestID, TransportStorage transportStorage) {
        this.payloadHandler = payloadHandler;
        this.requestID = requestID;
        this.transportStorage = transportStorage;
        this.traces = traces;
        traceGroups = getTraceThreadGroups();
        initGeneratorState();
    }

    public void runGenerator() {
        int totalPayloadCount = traceGroups.values().stream()
                .map(eachTree -> eachTree.getCopyCount() * eachTree.getPayloadCount())
                .mapToInt(Integer::intValue).sum();
        generatorState.setTotalPayloadCount(totalPayloadCount);
        GeneratorsStateProvider.putTraceGeneratorState(requestID, generatorState);
        Map<String, TraceGeneratorThread> traceThreadMap = new HashMap<>();
        generatorState.setGeneratorThreadMap(traceThreadMap);
        log.info(requestID + ": Initializing " + traceGroups.size() + " trace generator threads");
        initThreads();
        generatorState.setGenerateData(true);
        log.debug(requestID + ": Flipped generate data flag to true for trace threads");
    }

    private void initGeneratorState() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
                traceGroups.size(),
                runnable -> {
                    Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setUncaughtExceptionHandler(new GeneratorExceptionHandler());
                    return t;
                });
        generatorState = new GeneratorState<>(executorService);
        generatorState.setThreadPayloadCounts(new ConcurrentHashMap<>());
        if (transportStorage != null) {
            transportStorage.initTraceResponseMaps();
            generatorState.setTransportStorage(transportStorage);
        }
    }

    /**
     * To accommodate performance concerns when a high copyCount may have been supplied, we split the copies into different thread groups.
     * The calculation done for this is: <p>
     *     totalNodes = totalNodes_InTree * copyCount <p>
     *     If the totalNodes/totalTime_ForTreePosting_InSeconds > 25, we split the copies into separate thread groups
     *     such that each group adheres to this limit.
     * @return Trace threads groups
     */
    private Map<String, RootSpanDefinition> getTraceThreadGroups() {
        Map<String, RootSpanDefinition> traceGroups = new HashMap<>();
        for (RootSpanDefinition eachTrace: traces.getRootSpans()) {
            double totalCopyNodes = eachTrace.getTotalChildNodeCount() * eachTrace.getCopyCount();
            double postTimeSecs = eachTrace.getTreeParts() * eachTrace.getPayloadFrequencySeconds();
            double totalNodePerSec = totalCopyNodes / postTimeSecs;
            //If there is only a single copy to be posted OR
            //the number of nodes to be handled is within expected performance limits
            //do not split into different thread groups
            if (eachTrace.getCopyCount() == 1 || totalNodePerSec <= 25.0d) {
                traceGroups.put(eachTrace.getName() + "::group::0", eachTrace);
                continue;
            }
            double singleCopyNodes = eachTrace.getTotalChildNodeCount();
            int copiesPerGroup = (int) (25.0d / (singleCopyNodes / postTimeSecs));
            int remainingCopies = eachTrace.getCopyCount() % copiesPerGroup;
            int groups = eachTrace.getCopyCount() / copiesPerGroup;
            eachTrace.setCopyCount(copiesPerGroup);
            for (var group = 0; group < groups; group++) {
                traceGroups.put(eachTrace.getName() + "::group::" + group, eachTrace);
            }
            if (remainingCopies > 0) {
                try {
                    RootSpanDefinition lastGroup = eachTrace.clone();
                    lastGroup.setCopyCount(remainingCopies);
                    traceGroups.put(lastGroup.getName() + "::group::" + (groups), lastGroup);
                } catch (CloneNotSupportedException cloneNotSupportedException) {
                    log.error(requestID + ": Failed to clone trace tree " + eachTrace.getName());
                }
            }
        }
        return traceGroups;
    }

    private void initThreads() {
        TraceGeneratorThread generatorThread;
        for (Map.Entry<String, RootSpanDefinition> eachTreeGroup: traceGroups.entrySet()) {
            generatorThread = new TraceGeneratorThread(eachTreeGroup.getKey(), eachTreeGroup.getValue(), requestID, payloadHandler);
            generatorState.getExecutorService().scheduleAtFixedRate(generatorThread, 10,
                    eachTreeGroup.getValue().getPayloadFrequencySeconds(), TimeUnit.SECONDS);
            generatorState.getGeneratorThreadMap().put(eachTreeGroup.getValue().getName(), generatorThread);
            log.debug(requestID + ": Initialized trace generator thread group " + eachTreeGroup.getKey() +
                    " having payload frequency of " + eachTreeGroup.getValue().getPayloadFrequencySeconds() + " seconds " +
                    " and post count " + eachTreeGroup.getValue().getPayloadCount());
        }
    }
}
