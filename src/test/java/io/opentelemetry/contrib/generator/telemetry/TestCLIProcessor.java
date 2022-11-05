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

import io.opentelemetry.contrib.generator.telemetry.cli.CLIProcessor;
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

}
