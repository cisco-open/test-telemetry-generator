package io.opentelemetry.contrib.generator.telemetry.misc;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneratorExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Error occurred in thread " + t.getName(), e);
    }
}
