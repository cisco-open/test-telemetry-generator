package io.opentelemetry.contrib.generator.telemetry.transport.implementations.http;

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
