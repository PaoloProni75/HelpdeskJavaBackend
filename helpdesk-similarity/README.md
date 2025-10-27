# helpdesk-similarity

## Responsibility
Supplies cosine-based semantic matching for the helpdesk knowledge base. The module implements `SimilarityService` so the core engine can resolve knowledge entries, fallback thresholds, and top-k suggestions.

## Key Classes
- `CosineSequenceMatcherService` (`id=cosine`) — SPI entry point that scores questions against `IKnowledge` using tokenized cosine similarity and flags when the LLM should be invoked.
- `SequenceMatcherJava` — lightweight tokenizer that builds frequency vectors and computes cosine similarity without external dependencies.

## Public API
- Exports `SimilarityService` via `META-INF/services/cloud.contoterzi.helpdesk.core.spi.SimilarityService`.
- Consumers call `findBestMatch(question, kb, threshold)` and `topK(question, kb, limit)`; the service returns `KnowledgeBestMatch` or ordered suggestions.

## Extension Points
- Add alternative similarity providers by implementing `SimilarityService` in this module or another module and registering the implementation class under `META-INF/services`.
- Configure selection through YAML (`similarity.type`). Threshold defaults to `0.8` but can be overridden via `similarity.threshold`.

## Build & Test
- `mvn -pl helpdesk-similarity -am test` — executes algorithm unit tests.
- `mvn -pl helpdesk-similarity -am package` — produces the service jar with SPI descriptors.
- The module is dependency-free; no additional setup is required beyond JDK 17.
