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

package io.opentelemetry.contrib.generator.telemetry.transport.implementations.rest;

import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.AuthHandler;
import com.google.protobuf.GeneratedMessageV3;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.HTTPClient;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;

@Slf4j
public class RESTPayloadHandler implements PayloadHandler {

    private final String ENDPOINT_URL;
    private final AuthHandler authHandler;
    private final HTTPClient httpClient;
    @Getter
    @Setter
    private String metricsURL = "";
    @Getter
    @Setter
    private String logsURL = "";
    @Getter
    @Setter
    private String tracesURL = "";
    @Getter
    private boolean lastRequestSuccess;

    public RESTPayloadHandler(String endpointURL, AuthHandler authHandler) {
        this.ENDPOINT_URL = endpointURL;
        httpClient = new HTTPClient();
        this.authHandler = authHandler;
    }

    @Override
    public boolean postPayload(GeneratedMessageV3 message) {
        if (StringUtils.defaultString(ENDPOINT_URL).isBlank() || authHandler == null) {
            log.error("Missing URL or access token");
            return false;
        }
        String URL;
        if (message instanceof ExportMetricsServiceRequest) {
            URL = ENDPOINT_URL + metricsURL;
        } else if (message instanceof ExportLogsServiceRequest) {
            URL = ENDPOINT_URL + logsURL;
        } else {
            URL = ENDPOINT_URL + tracesURL;
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
