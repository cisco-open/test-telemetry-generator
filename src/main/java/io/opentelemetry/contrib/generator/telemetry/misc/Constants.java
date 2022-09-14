package io.opentelemetry.contrib.generator.telemetry.misc;

import java.util.Arrays;
import java.util.List;

public class Constants {

    private Constants() {}

    public static final String SUM = "sum";
    public static final String GAUGE = "gauge";
    public static final String SUMMARY = "summary";
    public static final List<String> validMetricTypes = Arrays.asList(SUM, GAUGE, SUMMARY);
    public static final String CUMULATIVE = "cumulative";
    public static final String DELTA = "delta";

    public static final String INFO_SEVERITY = "INFO";
    public static final String WARN_SEVERITY = "WARN";
    public static final String ERROR_SEVERITY = "ERROR";
    public static final String DEBUG_SEVERITY = "DEBUG";
    public static final String TRACE_SEVERITY = "TRACE";

    public static final String ENTITY_DEFINITION_ENV = "ENTITY_DEFINITION_YAML";
    public static final String METRICS_DEFINITION_ENV = "METRICS_DEFINITION_YAML";
    public static final String LOGS_DEFINITION_ENV = "LOGS_DEFINITION_YAML";
    public static final String TRACES_DEFINITION_YAML = "TRACES_DEFINITION_YAML";

    public static final String ENV_ALPHANUMERIC = "ENV_ALPHANUMERIC";
}
