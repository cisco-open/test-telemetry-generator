package io.opentelemetry.contrib.generator.telemetry.cli.dto;

import lombok.Data;

@Data
public class RestURL {

    private String baseURL;
    private String metricsPath;
    private String logsPath;
    private String tracesPath;
}
