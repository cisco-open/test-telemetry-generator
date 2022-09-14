package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorInput;
import io.opentelemetry.contrib.generator.telemetry.dto.GeneratorState;
import io.opentelemetry.contrib.generator.telemetry.logs.LogGeneratorThread;
import io.opentelemetry.contrib.generator.telemetry.metrics.MetricGeneratorThread;
import io.opentelemetry.contrib.generator.telemetry.traces.TraceGeneratorThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * After all the generator threads have been scheduled, the generator leaves control to this class to monitor all the
 * data generation threads and shutdown the respective executor service once complete or timed out.
 */
@Slf4j
public class GeneratorsMonitor {

    @Getter
    private final GeneratorInput input;
    private final String requestID;
    private GeneratorState<MetricGeneratorThread> metricsGeneratorState;
    private GeneratorState<LogGeneratorThread> logsGeneratorState;
    private GeneratorState<TraceGeneratorThread> tracesGeneratorState;
    @Getter
    private boolean isMetricsComplete, isLogsComplete, isTracesComplete;
    private long metricsTotalTime, logsTotalTime, tracesTotalTime;

    public GeneratorsMonitor(String requestID, GeneratorInput input) {
        this.requestID = requestID;
        this.input = input;
        isMetricsComplete = !input.isHasMetrics();
        isLogsComplete = !input.isHasLogs();
        isTracesComplete = !input.isHasTraces();
        if (input.isHasMetrics()) {
            metricsGeneratorState = GeneratorsStateProvider.getMetricGeneratorState(requestID);
        }
        if (input.isHasLogs()) {
            logsGeneratorState = GeneratorsStateProvider.getLogGeneratorState(requestID);
        }
        if (input.isHasTraces()) {
            tracesGeneratorState = GeneratorsStateProvider.getTraceGeneratorState(requestID);
        }
    }

    public void monitorThreads() {
        long startTime = System.currentTimeMillis();
        //Find out which of the metrics/logs/traces will run the longest
        long maxWaitTime = getMaxWaitSeconds(input);
        long elapsedSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
        boolean isAllComplete = isMetricsComplete && isLogsComplete && isTracesComplete;

        //Wait for timeout or the composite flag to be true indicating the data generation is complete
        while (elapsedSecs < maxWaitTime && !isAllComplete) {
            sleep(60);
            elapsedSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
            isMetricsComplete = checkMetrics(elapsedSecs);
            isTracesComplete = checkTraces(elapsedSecs);
            isLogsComplete = checkLogs(elapsedSecs);
            isAllComplete = isMetricsComplete && isLogsComplete && isTracesComplete;
        }

        //If the data generation is still not completed, we will wait another 90 seconds
        int finalCounter = isAllComplete ? 0 : 3;
        while (finalCounter > 0 && !isAllComplete) {
            sleep(30);
            isMetricsComplete = checkMetrics(elapsedSecs);
            isLogsComplete = checkLogs(elapsedSecs);
            isTracesComplete = checkTraces(elapsedSecs);
            isAllComplete = isMetricsComplete && isTracesComplete && isLogsComplete;
            finalCounter--;
            elapsedSecs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
        }
        //Force shutdown the executor services if the data generation is not complete yet
        completeMetrics();
        completeTraces();
        completeLogs();
    }

    private long getMaxWaitSeconds(GeneratorInput input) {
        metricsTotalTime = input.isHasMetrics() ? input.getMetricDefinitions().getMaxPostSeconds() : -1;
        logsTotalTime = input.isHasLogs() ? input.getLogDefinitions().getMaxPostSeconds() : -1;
        tracesTotalTime = input.isHasTraces() ? input.getTraceDefinitions().getMaxPostSeconds() : -1;
        return Math.max(Math.max(metricsTotalTime, logsTotalTime), tracesTotalTime);
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    private boolean checkMetrics(long elapsedSecs) {
        if (input.isHasMetrics() && !isMetricsComplete) {
            int currPayload = metricsGeneratorState.getCurrentPayloadCount();
            int totalPayload = metricsGeneratorState.getTotalPayloadCount();
            log.info(requestID + ": " + currPayload + "/" + totalPayload + " of metrics payloads posted (" +
                    (currPayload * 100) / totalPayload + "%). " + elapsedSecs + "/" + metricsTotalTime + " seconds elapsed (" +
                    (elapsedSecs * 100) / metricsTotalTime + "%).");
            if (metricsGeneratorState.isDataGenerationComplete()) {
                metricsGeneratorState.getExecutorService().shutdown();
                isMetricsComplete = true;
            }
            return isMetricsComplete;
        }
        return true;
    }

    private boolean checkLogs(long elapsedSecs) {
        if (input.isHasLogs() && !isLogsComplete) {
            int currPayload = logsGeneratorState.getCurrentPayloadCount();
            int totalPayload = logsGeneratorState.getTotalPayloadCount();
            log.info(requestID + ": " + currPayload + "/" + totalPayload + " of logs payloads posted (" +
                    (currPayload * 100) / totalPayload + "%). " + elapsedSecs + "/" + logsTotalTime + " seconds elapsed (" +
                    (elapsedSecs * 100) / logsTotalTime + "%).");
            if (logsGeneratorState.isDataGenerationComplete()) {
                logsGeneratorState.getExecutorService().shutdown();
                isLogsComplete = true;
            }
            return isLogsComplete;
        }
        return true;
    }

    private boolean checkTraces(long elapsedSecs) {
        if (input.isHasTraces() && !isTracesComplete) {
            int currPayload = tracesGeneratorState.getCurrentPayloadCount();
            int totalPayload = tracesGeneratorState.getTotalPayloadCount();
            log.info(requestID + ": " + currPayload + "/" + totalPayload + " of trace payloads posted (" +
                    (currPayload * 100) / totalPayload + "%). " + elapsedSecs + "/" + tracesTotalTime + " seconds elapsed (" +
                    (elapsedSecs * 100) / tracesTotalTime + "%).");
            if (tracesGeneratorState.isDataGenerationComplete()) {
                tracesGeneratorState.getExecutorService().shutdown();
                isTracesComplete = true;
            }
            return isTracesComplete;
        }
        return true;
    }

    private void completeMetrics() {
        if (input.isHasMetrics() && !isMetricsComplete) {
            log.error(requestID + ": Only " + metricsGeneratorState.getCurrentPayloadCount() + " of " +
                    metricsGeneratorState.getTotalPayloadCount() + " metric payloads were posted. Shutting down threads due to timeout.");
            metricsGeneratorState.getExecutorService().shutdown();
        }
    }

    private void completeLogs() {
        if (input.isHasLogs() && !isLogsComplete) {
            log.error(requestID + ": Only " + logsGeneratorState.getCurrentPayloadCount() + " of " +
                    logsGeneratorState.getTotalPayloadCount() + " log payloads were posted. Shutting down threads due to timeout.");
            logsGeneratorState.getExecutorService().shutdown();
        }
    }

    private void completeTraces() {
        if (input.isHasTraces() && !isTracesComplete) {
            log.error("Only " + tracesGeneratorState.getCurrentPayloadCount() + " of " + tracesGeneratorState.getTotalPayloadCount() +
                    " trace payloads were posted. Shutting down threads due to timeout.");
            tracesGeneratorState.getExecutorService().shutdown();
        }
    }

    public Integer getMetricsCurrentPayloadCount() {
        if (input.isHasMetrics()) {
            return metricsGeneratorState.getCurrentPayloadCount();
        }
        return 0;
    }

    public Integer getLogsCurrentPayloadCount() {
        if (input.isHasLogs()) {
            return logsGeneratorState.getCurrentPayloadCount();
        }
        return 0;
    }

    public Integer getTracesCurrentPayloadCount() {
        if (input.isHasTraces()) {
            return tracesGeneratorState.getCurrentPayloadCount();
        }
        return 0;
    }

    public Integer getMetricsTotalPayloadCount() {
        if (input.isHasMetrics()) {
            return metricsGeneratorState.getTotalPayloadCount();
        }
        return 0;
    }

    public Integer getLogsTotalPayloadCount() {
        if (input.isHasLogs()) {
            return logsGeneratorState.getTotalPayloadCount();
        }
        return 0;
    }

    public Integer getTracesTotalPayloadCount() {
        if (input.isHasTraces()) {
            return tracesGeneratorState.getTotalPayloadCount();
        }
        return 0;
    }

    public void killGenerator() {
        log.info(requestID + ": Request received to kill running generator");
        if (input.isHasMetrics()) {
            metricsGeneratorState.getExecutorService().shutdown();
            log.info(requestID + ": Metrics generator killed successfully");
        }
        if (input.isHasLogs()) {
            logsGeneratorState.getExecutorService().shutdown();
            log.info(requestID + ": Logs generator killed successfully");
        }
        if (input.isHasTraces()) {
            tracesGeneratorState.getExecutorService().shutdown();
            log.info(requestID + ": Traces generator killed successfully");
        }
    }

}
