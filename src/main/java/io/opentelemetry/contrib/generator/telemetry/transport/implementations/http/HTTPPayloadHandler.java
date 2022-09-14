package io.opentelemetry.contrib.generator.telemetry.transport.implementations.http;

import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.auth.AuthHandler;
import com.google.protobuf.GeneratedMessageV3;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;

@Slf4j
public class HTTPPayloadHandler implements PayloadHandler {

    private final String VANITY_URL;
    private final AuthHandler authHandler;
    private final HTTPClient httpClient;
    @Getter
    @Setter
    private String metricsURL = "/data/v1beta/metrics";
    @Getter
    @Setter
    private String logsURL = "/data/v1beta/logs";
    @Getter
    @Setter
    private String tracesURL = "/data/v1beta/trace";
    @Getter
    private boolean lastRequestSuccess;

    public HTTPPayloadHandler(String vanityURL, AuthHandler authHandler) {
        this.VANITY_URL = vanityURL;
        httpClient = new HTTPClient();
        this.authHandler = authHandler;
    }

    @Override
    public boolean postPayload(GeneratedMessageV3 message) {
        if (StringUtils.defaultString(VANITY_URL).isBlank() || authHandler == null) {
            log.error("Missing URL or access token");
            return false;
        }
        String URL;
        if (message instanceof ExportMetricsServiceRequest) {
            URL = VANITY_URL + metricsURL;
        } else if (message instanceof ExportLogsServiceRequest) {
            URL = VANITY_URL + logsURL;
        } else {
            URL = VANITY_URL + tracesURL;
        }
        httpClient.postBytes(URL, getHeadersPostData(), message.toByteArray());
        lastRequestSuccess = httpClient.isLastRequestSuccess();
        return lastRequestSuccess;
    }

    protected String[] getHeadersPostData() {
        return new String[] {HttpHeaders.AUTHORIZATION, authHandler.getAuthString(),
                HttpHeaders.CONTENT_TYPE, "application/x-protobuf",
                HttpHeaders.ACCEPT, "application/x-protobuf"};
    }
}
