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

package io.opentelemetry.contrib.generator.telemetry.transport.implementations;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Slf4j
public class HTTPClient {

    private final HttpClient httpClient;
    @Getter
    private boolean lastRequestSuccess;

    public HTTPClient() {
        httpClient = HttpClient.newBuilder().build();
    }

    public Optional<String> postBytes(String URL, String[] headers, byte[] requestBody) {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .headers(headers)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String responseBody = response.body();
            lastRequestSuccess = status == 200 || status == 201 || status == 204;
            log.info("Response code for " + URL + ": " + response.statusCode());
            log.debug("Response body for " + URL + ": \n" + responseBody);
            return Optional.of(responseBody);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to post data to " + URL, e);
            lastRequestSuccess = false;
        }
        return Optional.empty();
    }
}
