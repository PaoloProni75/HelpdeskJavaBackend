# helpdesk-llm-aws-nova

## Responsibility
Provides the AWS Bedrock Nova implementation of the `LlmClient` SPI. The module translates `LlmRequest` prompts into Nova's InvokeModel payload, manages basic inference parameters, and normalizes responses back into `LlmResponse`.

## Key Classes
- `NovaClientBedrock` (`id=nova`) — extends `AbstractBedrockDriver`, builds a singleton `BedrockRuntimeClient`, and performs `invokeModel` calls.
- Inherits retry/backoff and exception mapping from `AbstractLlmClient`/`AbstractBedrockDriver` in `helpdesk-aws-common`.

## Public API
- Registered under `META-INF/services/cloud.contoterzi.helpdesk.core.spi.LlmClient`.
- `init(YamlConfig)` reads:
  - `llm.region` (default `us-east-1`)
  - `llm.model` (default `amazon.nova-micro-v1:0`)
  - `llm.maxTokens`, `llm.temperature`, `llm.prompts.preamble`, `llm.prompts.template`
- `ask(LlmRequest)` returns the Nova response body as `LlmResponse#answer` with measured latency in `timeMs`.

## Extension Points
- Customize timeout detection by overriding `isTimeoutException` or expand payload structure before serializing with Jackson.
- Additional Bedrock models can subclass `AbstractBedrockDriver` in this module and register new SPI ids.

## Build & Test
- `mvn -pl helpdesk-llm-aws-nova -am test` — runs unit tests with mocked Bedrock responses.
- `mvn -pl helpdesk-llm-aws-nova -am package` — produces the SPI jar consumed by the `awsNova` profile.
- Integration tests require Bedrock access and appropriate IAM permissions.

## Deployment Notes
- The module is pulled into AWS builds via `-PawsNova`; ensure Bedrock endpoints for the chosen region support Nova.
- When bundling into Lambda, include this jar alongside `helpdesk-aws-common` and `helpdesk-core` (handled automatically by the Maven profile).
