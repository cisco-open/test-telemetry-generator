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

package io.opentelemetry.contrib.generator.telemetry.transport.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.contrib.generator.telemetry.transport.implementations.HTTPClient;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.HttpHeaders;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Class to handle OAuth client credentials workflow which will generate the access token to be used with every request.
 * See <a href="https://www.rfc-editor.org/rfc/rfc6749#section-4.4">OAuth2 Client Credentials specification</a>
 * In case of a different implementation of the OAuth, please use a custom implementation.
 */
@Slf4j
public class OAuthHandler implements AuthHandler {

    private final String GET_TOKEN_URL;
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    //Provide null for scope if not required
    private final String SCOPE;
    private final HTTPClient httpClient;
    private String accessToken;
    private long expirySeconds;

    public OAuthHandler(String GET_TOKEN_URL, String CLIENT_ID, String CLIENT_SECRET, String SCOPE) {
        this.GET_TOKEN_URL = GET_TOKEN_URL;
        this.CLIENT_ID = CLIENT_ID;
        this.CLIENT_SECRET = CLIENT_SECRET;
        this.SCOPE = SCOPE;
        httpClient = new HTTPClient();
        accessToken = "";
        expirySeconds = -1;
    }

    @Override
    public String getAuthString() {
        if (expirySeconds < TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())) {
            setAccessToken();
        }
        return "Bearer " + accessToken;
    }

    private void setAccessToken() {
        var body = "grant_type=client_credentials";
        if (SCOPE != null)
        {
            body = body + "&scope=" + SCOPE;
        }
        Optional<String> responseBody = httpClient.postBytes(GET_TOKEN_URL, getHeadersToken(CLIENT_ID, CLIENT_SECRET), body.getBytes());
        if (responseBody.isEmpty()) {
            log.error("Failed to get fresh token. Post data requests may fail.");
            return;
        }
        extractAndSetToken(responseBody.get());
    }

    private void extractAndSetToken(String tokenSecretResponse) {
        var responseMapper = new ObjectMapper();
        boolean failure = false;
        JsonNode responseBody = null;
        try {
            responseBody = responseMapper.readTree(tokenSecretResponse);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse json out of token secret response: " + tokenSecretResponse);
            failure = true;
        }
        if (responseBody == null || !responseBody.has("access_token")) {
            log.error("access_token not found in get access token response body: " + tokenSecretResponse);
            failure = true;
        } else {
            accessToken = responseBody.get("access_token").asText();
        }
        if (responseBody == null || !responseBody.has("expires_in")) {
            log.warn("expires_in field not found in get access token response body: " + tokenSecretResponse);
            log.warn("Will use default expiry time of 60 minutes.");
        } else {
            expirySeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + responseBody.get("expires_in").asLong();
        }
        if (failure) {
            log.error("Failed to get fresh token. Post data requests may fail.");
        }
    }

    private String[] getHeadersToken(String id, String secret) {
        return new String[] {HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8",
                HttpHeaders.AUTHORIZATION, "Basic " + getEncodedBasicAuth(id, secret)};
    }

    private String getEncodedBasicAuth(String id, String secret) {
        String authStr = id + ":" + secret;
        return Base64.getEncoder().encodeToString(authStr.getBytes());
    }

}
