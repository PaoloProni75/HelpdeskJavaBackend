# helpdesk-storage-s3

## Responsibility
Implements the `StorageAdapter` SPI by streaming the knowledge base JSON from Amazon S3. It simplifies configuration by relying on explicit YAML keys instead of parsing URIs and caches no state beyond the AWS client.

## Key Classes
- `S3StorageAdapter` (`type=s3`) — builds an AWS SDK v2 `S3Client`, downloads the configured object, and deserializes it into `List<IKnowledge>` via Jackson.

## Public API
- Exports `StorageAdapter` through `META-INF/services/cloud.contoterzi.helpdesk.core.spi.StorageAdapter`.
- Methods:
  - `init(YamlConfig)` expects `storage.bucket`, `storage.filename`, optional `storage.prefix`, and `storage.region`.
  - `loadKnowledgeBase()` returns the knowledge list or raises a runtime exception when the fetch fails.

## Configuration & Credentials
- Provide AWS credentials via the default provider chain (environment variables, profile, or IAM role).
- Ensure the YAML referenced by `APP_CONFIG_PATH` sets:
  ```yaml
  storage:
    type: s3
    bucket: your-bucket
    prefix: optional/folder
    filename: knowledge.json
    region: us-east-1
  ```
- Objects must contain an array that Jackson can bind to the `IKnowledge` interface.

## Build & Test
- `mvn -pl helpdesk-storage-s3 -am test` — runs adapter tests with mocked AWS SDK responses.
- `mvn -pl helpdesk-storage-s3 -am package` — creates the deployable jar and SPI descriptor.
- Integration testing against real S3 requires valid credentials and reachable buckets.
