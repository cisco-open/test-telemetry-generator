# test-telemetry-generator

![GitHub all releases](https://img.shields.io/github/downloads/cisco-open/test-telemetry-generator/total)
![GitHub](https://img.shields.io/github/license/cisco-open/test-telemetry-generator)

[![Java CI with Gradle](https://github.com/cisco-open/test-telemetry-generator/actions/workflows/gradle.yml/badge.svg)](https://github.com/cisco-open/test-telemetry-generator/actions/workflows/gradle.yml)
![GitHub last commit](https://img.shields.io/github/last-commit/cisco-open/test-telemetry-generator)
![Open Issues](https://img.shields.io/github/issues/cisco-open/test-telemetry-generator)
![Open PRs](https://img.shields.io/github/issues-pr/cisco-open/test-telemetry-generator)

![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/cisco-open/test-telemetry-generator)

## Purpose

[OpenTelemetry](https://opentelemetry.io/) is an open source collection of tools, APIs and SDKs which can be used to instrument, generate, collect, and export telemetry data (metrics, logs, and traces) to help you analyze your software's performance and behavior. If you are working on platforms that are built to ingest OpenTelemetry (OTel) data and have a need to provide OTel data as input to your platform services for testing or otherwise, either you can actually configure OTel collectors on your infrastructure to export the telemetry data (adding to your infra costs), or you will have to write code to generate hardcoded OTel packets. The issue with the former approach is that you do not know what data (metrics/logs/traces) is being actually sent and the latter would result in long term maintainability issues.  

The test-telemetry-generator tries to solve this requirement by acting like an OpenTelemetry exporter which can generate and export OpenTelemetry data without actually monitoring anything. It provides a relatively readable and flexible way of specifying the telemetry data to be generated via YAML files which can be easily modified at any stage to tune the output OTel data without changing your code at all.  

Although we can see that the key users of this tool would be test-engineers but this can be used by developers to perform quick checks on their platform and sales engineers to demo the platforms without having to write a lot of code or setting up OTel exporters on actual infra.

***

## How to use

This tools can be added to your code as a dependency or executed from the command-line. The repository [wiki](https://github.com/cisco-open/test-telemetry-generator/wiki) will help in understanding how to set up the pre-requisites and consume this tool as per your requirements.

***

## Support 

We are continuously improving the tool and adding more feature support. Please see the [open issues](https://github.com/cisco-open/test-telemetry-generator/issues) to see the list of planned items and feel free to open a new issue in case something that you'd like to see is missing.

***

## Contributors

Built with :heart: by and with the support of:  
* [Severin Neumann](https://github.com/svrnm)
* [Ashish Tyagi](https://github.com/ashish-tyagi)
* [Aarushi Singh](https://github.com/AarushiSingh09)
* [Manpreet Singh](https://github.com/preet-dev)

