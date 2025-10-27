# helpdesk-core

## Responsibility
Provides the central `HelpdeskEngine`, domain models, and configuration bootstrap (`AppState`, `YamlConfig`). It loads provider implementations for storage, similarity, and LLMs via `SpiLoader` and enforces the canonical request/response contract.

## Public API
- `HelpdeskRequest` / `HelpdeskResponse` — JSON DTOs exchanged with clients or Lambda handlers.
- `LlmClient`, `SimilarityService`, `StorageAdapter` — SPIs resolved at runtime based on `llm.type`, `similarity.type`, and `storage.type`.
- `LlmRequest`, `LlmResponse`, `KnowledgeBestMatch` — support classes used across modules.

## Key Classes
- `HelpdeskEngine` — orchestrates knowledge-base lookups, similarity scoring, and LLM fallback with escalation detection.
- `AppState` — singleton that loads `YamlConfig`, instantiates SPIs, and caches the knowledge base (driven by `APP_CONFIG_PATH` and `ALWAYS_CALL_LLM`).
- `SpiLoader` — helper that discovers `META-INF/services` registrations across modules.

## Extension Points
- Implement `LlmClient`, `StorageAdapter`, or `SimilarityService` and register the implementation class under `META-INF/services` to make it discoverable.
- Custom prompts, thresholds, and escalation phrases are configured via `llm.prompts.*` and `similarity.threshold` in the YAML file referenced by `APP_CONFIG_PATH`.

## Configuration
- Required env vars: `APP_CONFIG_PATH` (YAML path) and optionally `ALWAYS_CALL_LLM` (`true|false`).
- YAML keys consumed directly: `llm.{type,model,temperature,region,prompts.*}`, `storage.{type,bucket,filename,prefix,region}`, `similarity.{type,threshold}`.

## Build & Test
- `mvn -pl helpdesk-core -am test` — runs unit and SPI integration tests with test fixtures.
- `mvn -pl helpdesk-core -am package` — produces the core jar for downstream modules.
- Tests rely on mocked SPIs; set `APP_CONFIG_PATH` to `src/test/resources/test-config.yaml` if running outside Maven.
