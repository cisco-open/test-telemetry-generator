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
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.traces.dto.RootSpanDefinition;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Main thread class which generates and posts trace packets to the specified destination via PayloadHandler.
 */
@Slf4j
public class TraceGeneratorThread implements Runnable {

    private final RootSpanDefinition traceTree;
    private final String groupName;
    private final String requestID;
    private final PayloadHandler payloadHandler;
    private final GeneratorState<TraceGeneratorThread> generatorState;
    private final SpansGenerator spansGenerator;

    public TraceGeneratorThread(String groupName, RootSpanDefinition traceTree, String requestID, PayloadHandler payloadHandler) {
        this.groupName = groupName;
        this.traceTree = traceTree;
        this.requestID = requestID;
        this.payloadHandler = payloadHandler;
        generatorState = GeneratorsStateProvider.getTraceGeneratorState(requestID);
        spansGenerator = new SpansGenerator(groupName, traceTree, requestID);
    }

    @Override
    public void run() {
        if (!isPostingComplete() && generatorState.isGenerateData()) {
            ExportTraceServiceRequest traceServiceRequest = spansGenerator.getTraces();
            log.info(requestID + ": Sending payload for: " + groupName);
            log.debug(requestID + ": Complete payload for " + groupName + " is : " + traceServiceRequest);
            boolean responseStatus = payloadHandler.postPayload(traceServiceRequest);
            if (generatorState.getTransportStorage() != null) {
                generatorState.getTransportStorage().store(groupName, traceServiceRequest, responseStatus);
            }
            generatorState.getThreadPayloadCounts().put(groupName, spansGenerator.getCurrentPostCount() * traceTree.getCopyCount());
        }
    }

    private boolean isPostingComplete() {
        if (spansGenerator.getCurrentPostCount() >= traceTree.getPayloadCount()) {
            if (traceTree.getTreeParts() == 1) {
                return true;
            }
            //Once a span has sent its last tree part, current tree part is set to 0 in SpansGenerator
            return spansGenerator.getCurrentTreePart() == 0;
        }
        return false;
    }
}
