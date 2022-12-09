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
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestTracesGenerator {

    private final String ENTITIES_YAML = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "entities-traces-test.yaml").toString();
    private final String TEST_DEFS_PATH = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions").toString();
    private final String TRACES_YAML = Paths.get(TEST_DEFS_PATH, "trace-definition.yaml").toString();
    private final PayloadHandler payloadStore = new TestPayloadHandler();
    private TestPayloadHandler testStore;

    @Test
    public void generateData() {
        GeneratorInput generatorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML).withTraceDefinitionYAML(TRACES_YAML).build();
        TelemetryGenerator telemetryGenerator = new TelemetryGenerator(generatorInput, payloadStore, true);
        telemetryGenerator.runGenerator();
        testStore = (TestPayloadHandler) payloadStore;
    }

    @Test(dependsOnMethods = "generateData")
    public void testResourceSpansCount() {
        int totalExpectedResourceSpans = 11518;
        Assert.assertEquals(testStore.getTracePacketCount(), totalExpectedResourceSpans, "Mismatch in resource span counts");
    }

    @Test(dependsOnMethods = "generateData")
    public void testSpanCounts() {
        Assert.assertNotNull(testStore.getSpanCount(), "No spans were stored in the test payload handler");
        Map<String, AtomicInteger> spanCounts = testStore.getSpanCount();

        int createAccountCentral = 10 * 70;
        int activateAccountAuthCentral = 10 * 70;
        int activateAccountCentral = 10 * 70;
        int createAccountQueue = 10 * 70;
        int checkCreatedAccountQueue = 10 * 70;
        int createAccountQuery = 10 * 70;
        int createAccountProcessing = 10 * 70;
        int checkAccountDetailsCache = 10 * 70;
        int getAccountDetailsQuery = 10 * 70;
        int getAccountDetailsProcessing = 10 * 70;
        int validateAccountDetails = 10 * 70;
        int createNewAccount = 10 * 70;

        int updateAccountDetailsHTTPRequest = 7;
        int updateAccountDetailsQuery = 7;
        int updateAccountDetailsProcessing = 7;
        int updateAccountDetailsQueue = 7-2;
        int updateAccountDetails = 7;

        int deleteAccountHTTPRequest = 5;
        int deactivateAccountCentral = 5;
        int deleteAccountQuery = 5;
        int deleteAccountProcessing = 5;
        int deleteAccountQueue = 5;
        int deleteAccount = 5;

        int getAccountDetailsHTTPRequest = 10 * 175;
        checkAccountDetailsCache = checkAccountDetailsCache + 10 * 175;
        getAccountDetailsQuery = getAccountDetailsQuery + 10 * 175;
        getAccountDetailsProcessing = getAccountDetailsProcessing + 10 * 175;
        int getAccountDetails = 10 * 175;

        int searchAccountsHTTPRequest = 5 * 3;
        int searchAccountsQuery = 5 * 3;
        int searchAccountsProcessing = 5 * 3;
        int searchAccountsRequest = 5 * 3;

        Assert.assertNotNull(spanCounts.get("createAccountCentral"), "Span createAccountCentral not found in test store");
        Assert.assertNotNull(spanCounts.get("activateAccountAuthCentral"), "Span activateAccountAuthCentral not found in test store");
        Assert.assertNotNull(spanCounts.get("activateAccountCentral"), "Span activateAccountCentral not found in test store");
        Assert.assertNotNull(spanCounts.get("createAccountQueue"), "Span createAccountQueue not found in test store");
        Assert.assertNotNull(spanCounts.get("checkCreatedAccountQueue"), "Span checkCreatedAccountQueue not found in test store");
        Assert.assertNotNull(spanCounts.get("createAccountQuery"), "Span createAccountQuery not found in test store");
        Assert.assertNotNull(spanCounts.get("createAccountProcessing"), "Span createAccountProcessing not found in test store");
        Assert.assertNotNull(spanCounts.get("checkAccountDetailsCache"), "Span checkAccountDetailsCache not found in test store");
        Assert.assertNotNull(spanCounts.get("getAccountDetailsQuery"), "Span getAccountDetailsQuery not found in test store");
        Assert.assertNotNull(spanCounts.get("getAccountDetailsProcessing"), "Span getAccountDetailsProcessing not found in test store");
        Assert.assertNotNull(spanCounts.get("validateAccountDetails"), "Span validateAccountDetails not found in test store");
        Assert.assertNotNull(spanCounts.get("createNewAccount"), "Span createNewAccount not found in test store");
        Assert.assertNotNull(spanCounts.get("updateAccountDetailsHTTPRequest"), "Span updateAccountDetailsHTTPRequest not found in test store");
        Assert.assertNotNull(spanCounts.get("updateAccountDetailsQuery"), "Span updateAccountDetailsQuery not found in test store");
        Assert.assertNotNull(spanCounts.get("updateAccountDetailsProcessing"), "Span updateAccountDetailsProcessing not found in test store");
        Assert.assertNotNull(spanCounts.get("updateAccountDetailsQueue"), "Span updateAccountDetailsQueue not found in test store");
        Assert.assertNotNull(spanCounts.get("updateAccountDetails"), "Span updateAccountDetails not found in test store");
        Assert.assertNotNull(spanCounts.get("deleteAccountHTTPRequest"), "Span deleteAccountHTTPRequest not found in test store");
        Assert.assertNotNull(spanCounts.get("deactivateAccountCentral"), "Span deactivateAccountCentral not found in test store");
        Assert.assertNotNull(spanCounts.get("deleteAccountQuery"), "Span deleteAccountQuery not found in test store");
        Assert.assertNotNull(spanCounts.get("deleteAccountProcessing"), "Span deleteAccountProcessing not found in test store");
        Assert.assertNotNull(spanCounts.get("deleteAccountQueue"), "Span deleteAccountQueue not found in test store");
        Assert.assertNotNull(spanCounts.get("deleteAccount"), "Span deleteAccount not found in test store");
        Assert.assertNotNull(spanCounts.get("getAccountDetailsHTTPRequest"), "Span getAccountDetailsHTTPRequest not found in test store");
        Assert.assertNotNull(spanCounts.get("getAccountDetails"), "Span getAccountDetails not found in test store");
        Assert.assertNotNull(spanCounts.get("searchAccountsHTTPRequest"), "Span searchAccountsHTTPRequest not found in test store");
        Assert.assertNotNull(spanCounts.get("searchAccountsQuery"), "Span searchAccountsQuery not found in test store");
        Assert.assertNotNull(spanCounts.get("searchAccountsProcessing"), "Span searchAccountsProcessing not found in test store");
        Assert.assertNotNull(spanCounts.get("searchAccountsRequest"), "Span searchAccountsRequest not found in test store");

        Assert.assertEquals(spanCounts.get("createAccountCentral").get(), createAccountCentral, "Mismatch in span count for createAccountCentral");
        Assert.assertEquals(spanCounts.get("activateAccountAuthCentral").get(), activateAccountAuthCentral, "Mismatch in span count for activateAccountAuthCentral");
        Assert.assertEquals(spanCounts.get("activateAccountCentral").get(), activateAccountCentral, "Mismatch in span count for activateAccountCentral");
        Assert.assertEquals(spanCounts.get("createAccountQueue").get(), createAccountQueue, "Mismatch in span count for createAccountQueue");
        Assert.assertEquals(spanCounts.get("checkCreatedAccountQueue").get(), checkCreatedAccountQueue, "Mismatch in span count for checkCreatedAccountQueue");
        Assert.assertEquals(spanCounts.get("createAccountQuery").get(), createAccountQuery, "Mismatch in span count for createAccountQuery");
        Assert.assertEquals(spanCounts.get("createAccountProcessing").get(), createAccountProcessing, "Mismatch in span count for createAccountProcessing");
        Assert.assertEquals(spanCounts.get("checkAccountDetailsCache").get(), checkAccountDetailsCache, "Mismatch in span count for checkAccountDetailsCache");
        Assert.assertEquals(spanCounts.get("getAccountDetailsQuery").get(), getAccountDetailsQuery, "Mismatch in span count for getAccountDetailsQuery");
        Assert.assertEquals(spanCounts.get("getAccountDetailsProcessing").get(), getAccountDetailsProcessing, "Mismatch in span count for getAccountDetailsProcessing");
        Assert.assertEquals(spanCounts.get("validateAccountDetails").get(), validateAccountDetails, "Mismatch in span count for validateAccountDetails");
        Assert.assertEquals(spanCounts.get("createNewAccount").get(), createNewAccount, "Mismatch in span count for createNewAccount");
        Assert.assertEquals(spanCounts.get("updateAccountDetailsHTTPRequest").get(), updateAccountDetailsHTTPRequest, "Mismatch in span count for updateAccountDetailsHTTPRequest");
        Assert.assertEquals(spanCounts.get("updateAccountDetailsQuery").get(), updateAccountDetailsQuery, "Mismatch in span count for updateAccountDetailsQuery");
        Assert.assertEquals(spanCounts.get("updateAccountDetailsProcessing").get(), updateAccountDetailsProcessing, "Mismatch in span count for updateAccountDetailsProcessing");
        Assert.assertEquals(spanCounts.get("updateAccountDetailsQueue").get(), updateAccountDetailsQueue, "Mismatch in span count for updateAccountDetailsQueue");
        Assert.assertEquals(spanCounts.get("updateAccountDetails").get(), updateAccountDetails, "Mismatch in span count for updateAccountDetails");
        Assert.assertEquals(spanCounts.get("deleteAccountHTTPRequest").get(), deleteAccountHTTPRequest, "Mismatch in span count for deleteAccountHTTPRequest");
        Assert.assertEquals(spanCounts.get("deactivateAccountCentral").get(), deactivateAccountCentral, "Mismatch in span count for deactivateAccountCentral");
        Assert.assertEquals(spanCounts.get("deleteAccountQuery").get(), deleteAccountQuery, "Mismatch in span count for deleteAccountQuery");
        Assert.assertEquals(spanCounts.get("deleteAccountProcessing").get(), deleteAccountProcessing, "Mismatch in span count for deleteAccountProcessing");
        Assert.assertEquals(spanCounts.get("deleteAccountQueue").get(), deleteAccountQueue, "Mismatch in span count for deleteAccountQueue");
        Assert.assertEquals(spanCounts.get("deleteAccount").get(), deleteAccount, "Mismatch in span count for deleteAccount");
        Assert.assertEquals(spanCounts.get("getAccountDetailsHTTPRequest").get(), getAccountDetailsHTTPRequest, "Mismatch in span count for getAccountDetailsHTTPRequest");
        Assert.assertEquals(spanCounts.get("getAccountDetails").get(), getAccountDetails, "Mismatch in span count for getAccountDetails");
        Assert.assertEquals(spanCounts.get("searchAccountsHTTPRequest").get(), searchAccountsHTTPRequest, "Mismatch in span count for searchAccountsHTTPRequest");
        Assert.assertEquals(spanCounts.get("searchAccountsQuery").get(), searchAccountsQuery, "Mismatch in span count for searchAccountsQuery");
        Assert.assertEquals(spanCounts.get("searchAccountsProcessing").get(), searchAccountsProcessing, "Mismatch in span count for searchAccountsProcessing");
        Assert.assertEquals(spanCounts.get("searchAccountsRequest").get(), searchAccountsRequest, "Mismatch in span count for searchAccountsRequest");
    }

    @Test(dependsOnMethods = "generateData")
    public void testSpanTimes() {
        //Grab a single payload for the trace trees to check
        ExportTraceServiceRequest updateAccountTrace = null;
        ExportTraceServiceRequest deleteAccountTrace = null;
        int index = 0;
        while (updateAccountTrace == null || deleteAccountTrace == null) {
            ExportTraceServiceRequest currentTrace = testStore.getTracePayloads().get(index);
            Set<String> spanNames = currentTrace.getResourceSpansList().stream().map(ResourceSpans::getInstrumentationLibrarySpansList)
                    .flatMap(List::stream).map(InstrumentationLibrarySpans::getSpansList).flatMap(List::stream)
                    .map(Span::getName).collect(Collectors.toSet());
            if (spanNames.contains("updateAccountDetails")) {
                updateAccountTrace = currentTrace;
            } else if (spanNames.contains("deleteAccount")) {
                deleteAccountTrace = currentTrace;
            }
            index++;
        }
        //Total time for updateAccount trace tree is 45 seconds
        long updateTraceTotalTime = TimeUnit.SECONDS.toMillis(45);
        long updateTraceStartTime = getSpanStartTimeEndTime(updateAccountTrace, "updateAccountDetails")[0];
        long updateProcessingExpectedStartTime = updateTraceStartTime + (updateTraceTotalTime * 10 / 100);
        long updateProcessingExpectedEndTime = updateTraceStartTime + (updateTraceTotalTime * 50 / 100);
        long updateQueryExpectedStartTime = updateTraceStartTime + (updateTraceTotalTime * 20 / 100);
        long updateQueryExpectedEndTime = updateTraceStartTime + (updateTraceTotalTime * 45 / 100);
        long updateQueueExpectedStartTime = updateTraceStartTime + (updateTraceTotalTime * 55 / 100);
        long updateQueueExpectedEndTime = updateTraceStartTime + (updateTraceTotalTime * 120 / 100);
        long[] updateProcessingActualTimes = getSpanStartTimeEndTime(updateAccountTrace, "updateAccountDetailsProcessing");
        long[] updateQueryActualTimes = getSpanStartTimeEndTime(updateAccountTrace, "updateAccountDetailsQuery");
        long[] updateQueueActualTimes = getSpanStartTimeEndTime(updateAccountTrace, "updateAccountDetailsQueue");
        Assert.assertEquals(updateProcessingActualTimes[0], updateProcessingExpectedStartTime,
                "Mismatch in start time for updateAccountDetailsProcessing span");
        Assert.assertEquals(updateProcessingActualTimes[1], updateProcessingExpectedEndTime,
                "Mismatch in end time for updateAccountDetailsProcessing span");
        Assert.assertEquals(updateQueryActualTimes[0], updateQueryExpectedStartTime,
                "Mismatch in start time for updateAccountDetailsQuery span");
        Assert.assertEquals(updateQueryActualTimes[1], updateQueryExpectedEndTime,
                "Mismatch in end time for updateAccountDetailsQuery span");
        Assert.assertEquals(updateQueueActualTimes[0], updateQueueExpectedStartTime,
                "Mismatch in start time for updateAccountDetailsQueue span");
        Assert.assertEquals(updateQueueActualTimes[1], updateQueueExpectedEndTime,
                "Mismatch in end time for updateAccountDetailsQueue span");
        //Total time for deleteAccount trace tree is 20 seconds
        long deleteTraceTotalTime = TimeUnit.SECONDS.toMillis(20);
        long deleteTraceStartTime = getSpanStartTimeEndTime(deleteAccountTrace, "deleteAccount")[0];
        long deleteHTTPReqExpectedStartTime = deleteTraceStartTime + (deleteTraceTotalTime * 5 / 100);
        long deleteHTTPReqExpectedEndTime = deleteTraceStartTime + (deleteTraceTotalTime * 130 / 100);
        long deleteProcessingExpectedStartTime = deleteTraceStartTime + (deleteTraceTotalTime * 10 / 100);
        long deleteProcessingExpectedEndTime = deleteTraceStartTime + (deleteTraceTotalTime * 70 / 100);
        long deleteDeactivateExpectedStartTime = deleteTraceStartTime + (deleteTraceTotalTime * 15 / 100);
        long deleteDeactivateExpectedEndTime = deleteTraceStartTime + (deleteTraceTotalTime * 50 / 100);
        long deleteQueryExpectedStartTime = deleteTraceStartTime + (deleteTraceTotalTime * 50 / 100);
        long deleteQueryExpectedEndTime = deleteTraceStartTime + (deleteTraceTotalTime * 75 / 100);
        long[] deleteHTTPReqActualTimes = getSpanStartTimeEndTime(deleteAccountTrace, "deleteAccountHTTPRequest");
        long[] deleteProcessingActualTimes = getSpanStartTimeEndTime(deleteAccountTrace, "deleteAccountProcessing");
        long[] deleteDeactivateActualTimes = getSpanStartTimeEndTime(deleteAccountTrace, "deactivateAccountCentral");
        long[] deleteQueryActualTimes = getSpanStartTimeEndTime(deleteAccountTrace, "deleteAccountQuery");
        Assert.assertEquals(deleteHTTPReqActualTimes[0], deleteHTTPReqExpectedStartTime,
                "Mismatch in start time for deleteAccountHTTPRequest span");
        Assert.assertEquals(deleteHTTPReqActualTimes[1], deleteHTTPReqExpectedEndTime,
                "Mismatch in end time for deleteAccountHTTPRequest span");
        Assert.assertEquals(deleteProcessingActualTimes[0], deleteProcessingExpectedStartTime,
                "Mismatch in start time for deleteAccountProcessing span");
        Assert.assertEquals(deleteProcessingActualTimes[1], deleteProcessingExpectedEndTime,
                "Mismatch in end time for deleteAccountProcessing span");
        Assert.assertEquals(deleteDeactivateActualTimes[0], deleteDeactivateExpectedStartTime,
                "Mismatch in start time for deactivateAccountCentral span");
        Assert.assertEquals(deleteDeactivateActualTimes[1], deleteDeactivateExpectedEndTime,
                "Mismatch in end time for deactivateAccountCentral span");
        Assert.assertEquals(deleteQueryActualTimes[0], deleteQueryExpectedStartTime,
                "Mismatch in start time for deleteAccountQuery span");
        Assert.assertEquals(deleteQueryActualTimes[1], deleteQueryExpectedEndTime,
                "Mismatch in end time for deleteAccountQuery span");
    }

    @Test
    public void testWithOnlyRootSpans() {
        String onlyRootSpanTraces =  Paths.get(TEST_DEFS_PATH, "trace-definition-onlyrootspans.yaml").toString();
        GeneratorInput rootSpansGeneratorInput = new GeneratorInput.YAMLFilesBuilder(ENTITIES_YAML)
                .withTraceDefinitionYAML(onlyRootSpanTraces).build();
        PayloadHandler rootSpansStore = new TestPayloadHandler();
        TelemetryGenerator rootSpansTelemetryGenerator = new TelemetryGenerator(rootSpansGeneratorInput, rootSpansStore, false);
        rootSpansTelemetryGenerator.runGenerator();
        TestPayloadHandler rootSpansTestStore = (TestPayloadHandler) rootSpansStore;
        Assert.assertEquals(90, rootSpansTestStore.getTracePacketCount());
    }

    private long[] getSpanStartTimeEndTime(ExportTraceServiceRequest trace, String spanName) {
        Span span = trace.getResourceSpansList().stream()
                .map(ResourceSpans::getInstrumentationLibrarySpansList)
                .flatMap(List::stream)
                .map(InstrumentationLibrarySpans::getSpansList)
                .flatMap(List::stream)
                .filter(eachSpan -> eachSpan.getName().equals(spanName))
                .findAny().get();
        long[] times = new long[2];
        times[0] = TimeUnit.NANOSECONDS.toMillis(span.getStartTimeUnixNano());
        times[1] = TimeUnit.NANOSECONDS.toMillis(span.getEndTimeUnixNano());
        return times;
    }

}
