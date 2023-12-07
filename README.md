# test-telemetry-generator

![GitHub](https://img.shields.io/github/license/cisco-open/test-telemetry-generator)

[![Java CI with Gradle](https://github.com/cisco-open/test-telemetry-generator/actions/workflows/gradle.yml/badge.svg)](https://github.com/cisco-open/test-telemetry-generator/actions/workflows/gradle.yml)
![GitHub last commit](https://img.shields.io/github/last-commit/cisco-open/test-telemetry-generator)
![Open Issues](https://img.shields.io/github/issues/cisco-open/test-telemetry-generator)

![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/cisco-open/test-telemetry-generator)

## Purpose

[OpenTelemetry](https://opentelemetry.io/) is an open source collection of tools, APIs and SDKs which can be used to instrument, generate, collect, and export telemetry data (metrics, logs, and traces) to help you analyze your software's performance and behavior. If you are working on platforms that are built to ingest OpenTelemetry (OTel) data and have a need to provide OTel data as input to your platform services for testing or otherwise, either you can actually configure OTel collectors on your infrastructure to export the telemetry data (adding to your infra costs), or you will have to write code to generate hardcoded OTel packets. The issue with the former approach is that you do not know what data (metrics/logs/traces) is being actually sent and the latter would result in long term maintainability issues.  

The test-telemetry-generator tries to solve this requirement by acting like an OpenTelemetry exporter which can generate and export OpenTelemetry data without actually monitoring anything. It provides a relatively readable and flexible way of specifying the telemetry data to be generated via YAML files which can be easily modified at any stage to tune the output OTel data without changing your code at all.  

Although we can see that the key users of this tool would be test-engineers but this can be used by developers to perform quick checks on their platform and sales engineers to demo the platforms without having to write a lot of code or setting up OTel exporters on actual infra.

The telemetry is generated using [release 0.18.0 of the OpenTelemetry protos](https://github.com/open-telemetry/opentelemetry-proto/releases/tag/v0.18.0). 

***

## How to use

This tools can be added to your code as a dependency or executed from the command-line. The repository [wiki](https://github.com/cisco-open/test-telemetry-generator/wiki) will help in understanding how to set up the pre-requisites and consume this tool as per your requirements.

***

## Quickstart

For a quick start with a basic setup you can download the [latest fat-jar](https://github.com/cisco-open/test-telemetry-generator/releases/download/latest/test-telemetry-generator-otel-proto-0.18.0-fatjar.jar) and [example definitions](./example-definitions/) and put them all into one directory. We recommend that you start with the [simple example definitions](./example-definitions/simple/).

You can do all of that in the command line using [curl](https://curl.se/) like the following:

```shell
mkdir my-test-telemetry
cd my-test-telemetry
version=v23.7.0
curl -O -L https://github.com/cisco-open/test-telemetry-generator/releases/download/${version}/test-telemetry-generator-otel-proto-${version}-fatjar.jar
curl -O https://raw.githubusercontent.com/cisco-open/test-telemetry-generator/master/example-definitions/simple/resource-definition.yaml
curl -O https://raw.githubusercontent.com/cisco-open/test-telemetry-generator/master/example-definitions/simple/trace-definition.yaml
curl -O https://raw.githubusercontent.com/cisco-open/test-telemetry-generator/master/example-definitions/cli-target-rest.yaml
```

Your `my-test-telemetry` directory should now contain the following files:

```shell
$ ls
cli-target-rest.yaml  resource-definition.yaml  test-telemetry-generator-otel-proto-0.18.0-fatjar.jar  trace-definition.yaml
```

Next, open the `cli-target-rest.yml` with an editor of your choice and set the `restURL` to your OTLP HTTP endpoint. For example, if you use an [OpenTelemetry
Collector](https://opentelemetry.io/docs/collector/) running on `localhost` with an `otlp` receiver listening on port `4318`, update your target config to look like the following:

```yaml
authMode: "none"
# Set authmode to 'basic' and uncomment username & password if you need authentication
# username: "your-username"
# password: "your-password"
restURL:
  baseURL: "http://localhost:4318"
  metricsPath: "/v1/metrics"
  logsPath: "/v1/logs"
  tracesPath: "/v1/traces"
```

Finally, start the test-telemetry-generator:

```shell
java -jar test-telemetry-generator-otel-proto-0.18.0-fatjar.jar -r resource-definition.yaml -s trace-definition.yaml -t cli-target-rest.yaml
```

If all goes well, you should see test-telemetry-generator printing out some logs for you:

```text
...
10:10:08.525 [main] INFO  i.o.c.g.t.t.dto.Traces - 6jz87jHycn1T7hIE7c7zfKWnsIIBTAwI: Initializing 6 trace trees
10:10:08.530 [main] INFO  i.o.c.g.t.t.TracesGenerator - 6jz87jHycn1T7hIE7c7zfKWnsIIBTAwI: Initializing 6 trace generator threads
...
```

You now have a working setup from which you can build your own telemetry test environment. Update the definition files to your needs and read the [wiki](https://github.com/cisco-open/test-telemetry-generator/wiki) to learn more.

***

## Docker image
```text
# Clone the repo and from the repo root
# Build fat JAR
./gradlew clean build fatJar -x test
# Prepare definition YAML/JSON files and store them in the ./definitions directory
# Build docker file
docker build . -t test_telemetry_generator:local
# Run the container
docker run test_telemetry_generator:local
```

## Support

We are continuously improving the tool and adding more feature support. Please see the [open issues](https://github.com/cisco-open/test-telemetry-generator/issues) to see the list of planned items and feel free to open a new issue in case something that you'd like to see is missing.

***

## Contributors

Built with :heart: by and with the support of:  

* [Severin Neumann](https://github.com/svrnm)
* [Ashish Tyagi](https://github.com/ashish-tyagi)
* [Aarushi Singh](https://github.com/AarushiSingh09)
* [Manpreet Singh](https://github.com/preet-dev)
