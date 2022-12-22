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

package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.cli.CLIProcessor;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.testng.annotations.Test;

import java.nio.file.Paths;

public class TestCLIProcessor {

    private final String cliResourcesPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
            "test-definitions", "cli").toString();
    private final String ENTITIES_YAML = Paths.get(cliResourcesPath, "entity-definition.yaml").toString();
    private final String METRICS_YAML = Paths.get(cliResourcesPath, "metrics-cli-test.yaml").toString();
    private final String LOGS_YAML = Paths.get(cliResourcesPath, "logs-cli-test.yaml").toString();
    private final String TRACES_YAML = Paths.get(cliResourcesPath, "traces-cli-test.yaml").toString();

    @Test
    public void testAllGeneratorsNoAuthREST() throws ParseException {
        String noAuthTargetYAML = Paths.get(cliResourcesPath, "target-noauth.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noAuthTargetYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test
    public void testMetricsTracesBasicAuthGRPC() throws ParseException {
        String basicAuthTargetYAML = Paths.get(cliResourcesPath, "target-basicauth.yaml").toString();
        String[] cliArgs = new String[] {
                "--entityDefinition", ENTITIES_YAML, "--metricDefinition", METRICS_YAML,
                "--spanDefinition", TRACES_YAML, "--target", basicAuthTargetYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = MissingOptionException.class)
    public void testWithoutEntityDefinition() throws ParseException {
        String noAuthTargetYAML = Paths.get(cliResourcesPath, "target-noauth.yaml").toString();
        String[] cliArgs = new String[] { METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noAuthTargetYAML };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithOnlyEntityDefinition() throws ParseException {
        String noAuthTargetYAML = Paths.get(cliResourcesPath, "target-noauth.yaml").toString();
        String[] cliArgs = new String[] { "-e", ENTITIES_YAML, "-t", noAuthTargetYAML };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = MissingOptionException.class)
    public void testWithoutTargetAuthYAML() throws ParseException {
        String[] cliArgs = new String[] { "-e", ENTITIES_YAML, METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithoutAuthMode() throws ParseException {
        String noAuthModeYAML = Paths.get(cliResourcesPath, "negative", "no-auth-mode.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noAuthModeYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithInvalidAuthMode() throws ParseException {
        String invalidAuthModeYAML = Paths.get(cliResourcesPath, "negative", "invalid-auth-mode.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", invalidAuthModeYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithoutEndpoint() throws ParseException {
        String noEndpointYAML = Paths.get(cliResourcesPath, "negative", "without-endpoint.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noEndpointYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithOnlyGRPCHost() throws ParseException {
        String onlyGRPCHostYAML = Paths.get(cliResourcesPath, "negative", "only-grpc-host.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", onlyGRPCHostYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithOnlyGRPCPort() throws ParseException {
        String onlyGRPCPortYAML = Paths.get(cliResourcesPath, "negative", "only-grpc-port.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", onlyGRPCPortYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test
    public void testWithAuthModeNoneAndBasicCredentials() throws ParseException {
        String noneAuthModeYAML = Paths.get(cliResourcesPath, "negative", "none-auth-mode.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noneAuthModeYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithAuthModeBasicAndNoUsername() throws ParseException {
        String noUsernameYAML = Paths.get(cliResourcesPath, "negative", "no-username.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noUsernameYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithAuthModeBasicAndNoPassword() throws ParseException {
        String noPasswordYAML = Paths.get(cliResourcesPath, "negative", "no-password.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noPasswordYAML
        };
        CLIProcessor.main(cliArgs);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testWithAuthModeBasicAndNoCredentials() throws ParseException {
        String noCredsYAML = Paths.get(cliResourcesPath, "negative", "no-credentials.yaml").toString();
        String[] cliArgs = new String[] {
                "-e", ENTITIES_YAML, "-m", METRICS_YAML, "-l", LOGS_YAML, "-s", TRACES_YAML, "-t", noCredsYAML
        };
        CLIProcessor.main(cliArgs);
    }
}
