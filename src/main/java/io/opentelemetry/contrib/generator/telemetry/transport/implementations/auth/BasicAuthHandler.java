package io.opentelemetry.contrib.generator.telemetry.transport.implementations.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthHandler implements AuthHandler {

    private final String USERNAME;
    private final String PASSWORD;
    private String authString;

    public BasicAuthHandler(String USERNAME, String PASSWORD) {
        this.USERNAME = USERNAME;
        this.PASSWORD = PASSWORD;
    }

    @Override
    public String getAuthString() {
        if (authString == null) {
            authString = Base64.getEncoder()
                    .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            authString = "Basic " + authString;
        }
        return authString;
    }
}
