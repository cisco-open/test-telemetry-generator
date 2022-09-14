package io.opentelemetry.contrib.generator.telemetry.transport;

import com.google.protobuf.GeneratedMessageV3;

public interface PayloadHandler {
    boolean postPayload(GeneratedMessageV3 message);
}
