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
