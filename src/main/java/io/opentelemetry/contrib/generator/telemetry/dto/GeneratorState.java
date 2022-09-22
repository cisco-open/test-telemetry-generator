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

package io.opentelemetry.contrib.generator.telemetry.dto;

import io.opentelemetry.contrib.generator.telemetry.transport.TransportStorage;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Holds data shared by all metrics/logs/traces thread for a single instance of the generator. Since the generators return
 * the control back to the caller after initializing the threads, a public static instance of this in the generator helps with control & monitoring.
 */
@Data
public class GeneratorState<T extends Runnable> {

    private final ScheduledExecutorService executorService;
    private long fixedStartTime;
    private Map<String, T> generatorThreadMap;
    private ConcurrentMap<String, Integer> threadPayloadCounts;
    private int totalPayloadCount;
    private boolean generateData;
    private TransportStorage transportStorage;

    public int getCurrentPayloadCount() {
        return threadPayloadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isDataGenerationComplete() {
        return getCurrentPayloadCount() >= totalPayloadCount;
    }
}
