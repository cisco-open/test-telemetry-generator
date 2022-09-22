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
