# helpdesk-aws-common-quarkus

> Module directory: `helpdesk-aws-common/`. Provides the AWS Lambda entry point and Bedrock support classes that integrate cleanly with Quarkus native images or classic JVM packaging.

## Responsibility
Bootstraps `HelpdeskEngine` inside AWS Lambda (`LambdaHandler`) and offers shared infrastructure for Bedrock-based LLM clients (`AbstractBedrockDriver`). It also configures test-time environment variables through Maven Surefire.

## Key Classes
- `LambdaHandler` — implements `RequestHandler<Map<String,Object>, HelpdeskResponse>`, handles warm-up events, and delegates to `HelpdeskEngine` while guarding initialization through a lock.
- `AbstractBedrockDriver` — extends `AbstractLlmClient` to standardize retry logic, timeout detection, and exception mapping for Bedrock clients.

## Public API
- Lambda handler signature: `cloud.contoterzi.aws.common.LambdaHandler::handleRequest`.
- Shared base class for Bedrock providers: extend `AbstractBedrockDriver` and implement `callTheLLM(LlmRequest)` plus `id()`; register via `META-INF/services/cloud.contoterzi.helpdesk.core.spi.LlmClient` in the concrete module.

## Configuration
- Inherited env vars: `APP_CONFIG_PATH`, `ALWAYS_CALL_LLM`; these are injected during tests through the module POM.
- Packaging expects shaded jars so Lambda contains `helpdesk-core`, storage, similarity, and LLM dependencies.

## Build & Test
- `mvn -pl helpdesk-aws-common -am test` — validates the handler with mocked dependencies.
- `mvn -pl helpdesk-aws-common -am package` — produces the Lambda-ready jar (pulled automatically when using `-PawsNova`).
- To run locally with Quarkus or SAM, set the two env vars above and provide mock AWS credentials.

## Deployment
- Use the instructions in the repository root README for zip or container-image deployment.
- When building native images (Quarkus), ensure SPI resources are retained (`META-INF/services`) so the handler can resolve providers at runtime.
