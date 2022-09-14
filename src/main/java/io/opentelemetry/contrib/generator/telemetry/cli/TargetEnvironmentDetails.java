package io.opentelemetry.contrib.generator.telemetry.cli;

import lombok.Data;

@Data
public class TargetEnvironmentDetails {

    private String username;
    private String password;
    private String restURL;
    private String gRPCHost;
    private String gRPCPort;
}
