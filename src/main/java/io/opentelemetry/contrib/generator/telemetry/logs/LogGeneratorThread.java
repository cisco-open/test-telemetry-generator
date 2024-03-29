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

package io.opentelemetry.contrib.generator.telemetry.logs;

import io.opentelemetry.contrib.generator.core.dto.GeneratorResource;
import io.opentelemetry.contrib.generator.core.jel.ExpressionProcessor;
import io.opentelemetry.contrib.generator.telemetry.ResourceModelProvider;
import io.opentelemetry.contrib.generator.telemetry.GeneratorsStateProvider;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.jel.MELTExpressionsJELProvider;
import io.opentelemetry.contrib.generator.telemetry.logs.dto.LogDefinition;
import io.opentelemetry.contrib.generator.telemetry.misc.Constants;
import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.logs.v1.LogRecord;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import static io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main thread class which generates and posts log packets to the specified destination via PayloadHandler.
 */
@Slf4j
public class LogGeneratorThread implements Runnable {

    private final String requestID;
    @Getter
    private final LogDefinition logDefinition;
    private final PayloadHandler payloadHandler;
    private final GeneratorState<LogGeneratorThread> logGeneratorState;
    private final ExpressionProcessor jelProcessor;
    private int currentPayloadCount;

    public LogGeneratorThread(LogDefinition logDefinition, PayloadHandler payloadHandler, String requestID) {
        this.requestID = requestID;
        this.logDefinition = logDefinition;
        this.payloadHandler = payloadHandler;
        this.logGeneratorState = GeneratorsStateProvider.getLogGeneratorState(requestID);
        jelProcessor = MELTExpressionsJELProvider.getJelProcessor();
        currentPayloadCount = 0;
    }

    @Override
    public void run() {
        log.debug(requestID + ": Log generator thread invoked for Log Definition type: " + logDefinition);
        if (logGeneratorState.isGenerateData() &&
                logGeneratorState.getThreadPayloadCounts().get(logDefinition.getId()) < logDefinition.getPayloadCount()) {
            List<ResourceLogs> resourceLogsList = new ArrayList<>();
            ResourceLogs resourceLog;
            LogRecord.Builder partialLogRecord = getLog(logDefinition);
            Map<String, List<Resource>> reportingResourcesByType = new HashMap<>();
            for (Map.Entry<String, Integer> resourceTypeWithCount:
                    MapUtils.emptyIfNull(logDefinition.getReportingResourcesCounts()).entrySet()) {
                reportingResourcesByType.put(resourceTypeWithCount.getKey(),
                        getResourceSubsetByPostCount(resourceTypeWithCount.getKey(), resourceTypeWithCount.getValue()));
            }
            for (Map.Entry<String, Map<String, String>> resourceTypeWithFilter:
                    MapUtils.emptyIfNull(logDefinition.getParsedFilteredReportingResources()).entrySet()) {
                reportingResourcesByType.put(resourceTypeWithFilter.getKey(),
                        getFilteredResources(resourceTypeWithFilter.getKey(), resourceTypeWithFilter.getValue()));
            }
            for (Map.Entry<String, List<Resource>> reportingResourceByType : reportingResourcesByType.entrySet()) {
                log.debug(requestID + ": Preparing " + reportingResourceByType.getValue().size() +
                        " resource logs packets for " + reportingResourceByType.getKey());
                for (Resource eachResource: reportingResourceByType.getValue()) {
                    LogRecord logRecord = partialLogRecord.clone().addAllAttributes(getResourceAttributes(logDefinition
                            .getCopyResourceAttributes(), eachResource)).build();
                    List<LogRecord> otelLogs = Collections.nCopies(logDefinition.getCopyCount(), logRecord);
                    resourceLog = ResourceLogs.newBuilder()
                            .setResource(eachResource)
                            .addScopeLogs(ScopeLogs.newBuilder()
                                    .setScope(InstrumentationScope.newBuilder()
                                            .setName(Constants.SELF_NAME)
                                            .setVersion(Constants.SELF_VERSION)
                                            .build())
                                    .addAllLogRecords(otelLogs)
                                    .build())
                            .build();
                    resourceLogsList.add(resourceLog);
                }
                log.info(requestID + ": Sending payload for: " + reportingResourceByType.getKey());
                ExportLogsServiceRequest resourceLogs = ExportLogsServiceRequest.newBuilder().addAllResourceLogs(resourceLogsList).build();
                boolean responseStatus = payloadHandler.postPayload(resourceLogs);
                if (logGeneratorState.getTransportStorage() != null) {
                    logGeneratorState.getTransportStorage().store(logDefinition.getId(),
                            reportingResourceByType.getKey(), resourceLogs, responseStatus);
                }
                log.debug(requestID + ": Complete payload for resource: " + reportingResourceByType.getKey() +
                        " in log Definition" + logDefinition.getId() + ": " + resourceLogs);
                resourceLogsList.clear();
            }
            currentPayloadCount++;
            logGeneratorState.getThreadPayloadCounts().put(logDefinition.getId(), currentPayloadCount);
        }
    }

    private LogRecord.Builder getLog(LogDefinition logDefinition) {
        long nanoTime = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
        String severity = jelProcessor.eval(logDefinition.getSeverityOrderFunction()).toString();
        return LogRecord.newBuilder()
                .setTimeUnixNano(nanoTime)
                .setObservedTimeUnixNano(nanoTime)
                .setSeverityText(severity)
                .addAllAttributes(getEvaluatedAttributes(jelProcessor, logDefinition.getAttributes()))
                .setBody(AnyValue.newBuilder().setStringValue(LogMessageProvider.getLogMessage(severity)).build());
    }

    private List<Resource> getResourceSubsetByPostCount(String resourceName, int resourceCount) {
        int resourceStartIndex = 0;
        //resourceEndIndex is exclusive
        int resourceEndIndex;
        List<GeneratorResource> resourcesInResourceModel = ResourceModelProvider.getResourceModel(requestID).get(resourceName).stream()
                .filter(GeneratorResource::isActive).toList();
        if (resourceCount >= resourcesInResourceModel.size()) {
            resourceEndIndex = resourcesInResourceModel.size();
        } else {
            resourceStartIndex = currentPayloadCount % (resourcesInResourceModel.size() - resourceCount + 1);
            resourceEndIndex = resourceStartIndex + resourceCount;
        }
        return resourcesInResourceModel.subList(resourceStartIndex, resourceEndIndex)
                .stream().map(GeneratorResource::getOTelResource).collect(Collectors.toList());
    }

    private List<Resource> getFilteredResources(String resourceName, Map<String, String> filters) {
        List<Resource> filteredResources = new ArrayList<>();
        List<GeneratorResource> allResources = ResourceModelProvider.getResourceModel(requestID)
                .get(resourceName).stream()
                .filter(GeneratorResource::isActive)
                .toList();
        for (GeneratorResource eachResource: allResources) {
            if (eachResource.getEvaluatedAttributes().entrySet().containsAll(filters.entrySet())) {
                filteredResources.add(eachResource.getOTelResource());
            }
        }
        return filteredResources;
    }
}
