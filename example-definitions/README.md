# Example Definitions

In the subfolders you will find different sets of example definitions you can use
with the test-telemetry-generator:

| Folder | Description |
|--------|-------------|
| [simple](./simple) | A very simple example, start here to learn how to use test-telemetry-generator |
| [demo](./demo) | An example that simulates (parts of) the [OpenTelemetry demo](https://github.com/open-telemetry/opentelemetry-demo) |
| [qa](./qa) | A complex example that is used for running quality assurance tests against an OTLP backend |


Note that the target configuration does not change across examples, so you can
use the same [grpc](./cli-target-grpc.yaml) or [rest](./cli-target-rest.yaml)
configuration in all cases.
