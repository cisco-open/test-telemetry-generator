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

package io.opentelemetry.contrib.generator.core;

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;
import io.opentelemetry.contrib.generator.core.dto.RuntimeModification;
import io.opentelemetry.contrib.generator.telemetry.EntityModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Runnable implementation whose single thread is started along with the MELT data generators if any of the entity/resource types specified
 * in the entity model definition YAML have runtime modifications assigned to them. If that is true, this will be invoked every 2.5 seconds
 * to perform all the modifications due as per the runtime modification frequency specified by each entity type.
 * @see RuntimeModification DTO
 */
@Slf4j
public class RuntimeModificationsThread implements Runnable {

    private final String requestId;
    private final List<RuntimeModification> runtimeModifications;
    private final Map<String, Long> modificationsUpdateTimestamps; //stores the timestamp of the last time a modification was applied
    /**
     * Start time of when this thread is initialized. Needed for calculating the time before a modification is applied for the first time.
     */
    private final long threadStartTime;
    private Map<String, List<GeneratorEntity>> entityModel;

    public RuntimeModificationsThread(String requestId, List<RuntimeModification> runtimeModifications) {
        this.runtimeModifications = runtimeModifications;
        this.requestId = requestId;
        modificationsUpdateTimestamps = new HashMap<>();
        threadStartTime = System.currentTimeMillis();
        entityModel = EntityModelProvider.getEntityModel(requestId);
    }

    @Override
    public void run() {
        entityModel = EntityModelProvider.getEntityModel(requestId);
        long currTime = System.currentTimeMillis();
        long minsElapsedFromStart = TimeUnit.MILLISECONDS.toMinutes(currTime - threadStartTime);
        for (RuntimeModification eachModification: CollectionUtils.emptyIfNull(runtimeModifications)) {
            long minsElapsed = modificationsUpdateTimestamps.containsKey(eachModification.getModificationId()) ?
                    TimeUnit.MILLISECONDS.toMinutes(currTime - modificationsUpdateTimestamps.get(eachModification.getModificationId())) :
                    minsElapsedFromStart;
            log.debug(requestId + ": Minutes elapsed for " + eachModification.getEntityModificationType() + " on " +
                    eachModification.getEntityType() + " type is " + minsElapsed);
            if (minsElapsed >= eachModification.getModificationFrequencyMinutes()
                    && minsElapsed > eachModification.getStartAfterMinutes()
                    && minsElapsed <= eachModification.getEndAfterMinutes()) {
                switch (eachModification.getEntityModificationType()) {
                    case ADD:
                        executeAddModification(eachModification.getEntityType(), eachModification.getModificationQuantity());
                        break;
                    case REMOVE:
                        executeRemoveModification(eachModification.getEntityType(), eachModification.getModificationQuantity());
                        break;
                    case CHURN:
                        executeChurnModification(eachModification.getEntityType(), eachModification.getModificationQuantity());
                }
                modificationsUpdateTimestamps.put(eachModification.getModificationId(), currTime);
            }
        }
        EntityModelProvider.putEntityModel(requestId, entityModel);
    }

    private void executeAddModification(String type, int quantity) {
        log.info(requestId + ": Executing add modification of " + quantity + " qty on " + type);
        int startIndex = entityModel.get(type).size() - 1;
        while (!entityModel.get(type).get(startIndex).isActive()) {
            startIndex--;
        }
        IntStream.range(startIndex, startIndex + quantity).forEach(idx -> entityModel.get(type).get(idx + 1).setActive(true));
    }

    private void executeRemoveModification(String type, int quantity) {
        log.info(requestId + ": Executing remove modification of " + quantity + " qty on " + type);
        int startIndex = 0;
        while (!entityModel.get(type).get(startIndex).isActive()) {
            startIndex++;
        }
        IntStream.range(startIndex, startIndex + quantity).forEach(idx -> entityModel.get(type).get(idx).setActive(false));
    }

    private void executeChurnModification(String type, int quantity) {
        log.info(requestId + ": Executing churn modification of " + quantity + " qty on " + type);
        executeRemoveModification(type, quantity);
        executeAddModification(type, quantity);
    }
}
