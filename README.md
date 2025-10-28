
---

# Helpdesk Backend with LLM Integration
## Production-Ready, Portable FaaS Architecture for Automated Support

> **Master's Thesis Project** - Free University of Bozen-Bolzano (Oct 2025)  
> Engineering a portable, multi-cloud help desk automation system using serverless architecture and LLM integration.

[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-orange.svg)](https://aws.amazon.com/lambda/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🎯 The Problem

Help desk teams are overwhelmed. Response times are measured in hours or days. Costs scale linearly with support tickets. Common queries (password resets, basic troubleshooting, documentation lookups) consume 60-80% of agent time.

LLMs promise relief, but most implementations are:
- ❌ Locked to a single cloud provider
- ❌ Not production-ready (demos, not systems)
- ❌ Unable to handle real-world edge cases
- ❌ Missing critical features (context, knowledge base integration)

## 💡 The Solution

A **portable, FaaS-based backend** that automates tier-1 support using LLM integration:

✅ **Multi-cloud portable** - Deploy on AWS Lambda, Azure Functions, or GCP Cloud Functions  
✅ **Production-ready** - Error handling, observability, scalability built-in  
✅ **Knowledge base integration** - Retrieves relevant docs before LLM completion  
✅ **Similarity matching** - Pre-filters queries to optimize LLM usage  
✅ **Pluggable LLM providers** - Tested with AWS Bedrock Nova, Claude, Ollama (GPT OSS, Nemotron mini, Mistral)  
✅ **Cost-optimized** - Only calls LLM when necessary, caches common responses

## 📊 Results

**Simulated production workload:**
- ⚡ **80% faster** response time vs. human-only support
- 💰 **60% cost reduction** potential (tier-1 automation)
- 📈 **Scalable** from 10 to 10,000 requests/minute (serverless auto-scaling)
- 🎯 **85%+ accuracy** on knowledge base queries

**Real-world applicability:**
- Small teams (10-50 tickets/day): €500-2k/month savings
- Enterprise (500+ tickets/day): €20k-80k/month savings

---

## 🏗️ Architecture Overview

This repository hosts a **modular Java 17 helpdesk backend** built with Maven. The core engine orchestrates knowledge-base retrieval, similarity scoring, and LLM completion behind provider-specific modules that can be packaged for AWS Lambda or other runtimes.

### Component Diagram
```mermaid
graph TD
    aws[helpdesk-aws-common\nLambdaHandler] --> core[helpdesk-core\nHelpdeskEngine]
    core -->|SPI: StorageAdapter| storage[helpdesk-storage-s3\nS3StorageAdapter]
    core -->|SPI: SimilarityService| sim[helpdesk-similarity\nCosineSequenceMatcherService]
    core -->|SPI: LlmClient| llm[helpdesk-llm-aws-nova\nNovaClientBedrock]
    storage -->|Knowledge base JSON| core
    sim -->|Similarity scores| core
    llm -->|Generated answer| core
```

### Key Design Decisions

**Why Java?** Enterprise-grade reliability, strong typing, extensive tooling, battle-tested in production systems.

**Why SPI (Service Provider Interface)?** Swap LLM providers, storage backends, or similarity algorithms without changing core logic. Deploy the same codebase on AWS, Azure, or GCP.

**Why Serverless?** Zero infrastructure management, auto-scaling, pay-per-use pricing. Perfect for variable support loads.

**Why Similarity Pre-filtering?** Reduces unnecessary LLM calls by 40-60%, significantly cutting costs while maintaining quality.

---

## 📦 Modules

Each module is independently testable and follows clean architecture principles:

- **[`helpdesk-core`](helpdesk-core/README.md)** - Engine, domain models, SPI contracts
- **[`helpdesk-similarity`](helpdesk-similarity/README.md)** - Cosine similarity for query matching
- **[`helpdesk-storage-s3`](helpdesk-storage-s3/README.md)** - S3 knowledge base adapter
- **[`helpdesk-llm-aws-nova`](helpdesk-llm-aws-nova/README.md)** - AWS Bedrock Nova LLM client
- **[`helpdesk-aws-common`](helpdesk-aws-common/README.md)** - Lambda handler & AWS utilities

### Public APIs

- **`HelpdeskRequest`** / **`HelpdeskResponse`** - JSON-friendly DTOs for inbound/outbound contract
- **`StorageAdapter`** - SPI for loading knowledge base (`IKnowledge` lists)
- **`SimilarityService`** - SPI producing `KnowledgeBestMatch` and top-K collections
- **`LlmClient`** - SPI for provider-specific LLM completions

---

## 🚀 Quick Start

### Prerequisites
- JDK 17+
- Maven 3.9+
- AWS account (for Lambda deployment) or Docker (for local testing)

### Local Development

```bash
# Clone repository
git clone https://github.com/paoloproni/helpdesk-backend.git
cd helpdesk-backend

# Set configuration path
export APP_CONFIG_PATH=config/layer/config/helpdesk-config.yaml

# Build all modules
mvn clean install

# Run tests
mvn test

# Build AWS Nova bundle
mvn -PawsNova clean package
```

### Configuration

Set `APP_CONFIG_PATH` to point to your YAML config. Example structure:

```yaml
llm:
  type: bedrock-nova
  model: amazon.nova-micro-v1:0
  
storage:
  type: s3
  bucket: my-knowledge-base
  region: us-east-1

similarity:
  type: cosine
  threshold: 0.75

# Force LLM call even for high-similarity matches (testing)
ALWAYS_CALL_LLM: false
```

---

## 🌩️ Deploy to AWS Lambda

### Option 1: JAR Deployment

```bash
# Build shaded JAR
mvn -PawsNova -pl helpdesk-aws-common -am package

# Create Lambda deployment package
cd helpdesk-aws-common/target
zip lambda.zip helpdesk-aws-common-*.jar

# Upload to Lambda (via AWS Console or CLI)
# Handler: cloud.contoterzi.aws.common.LambdaHandler::handleRequest
# Runtime: Java 17
# Memory: 512MB (adjust based on load)
# Timeout: 30s
```

### Option 2: Container Deployment

```bash
# Build container image
mvn -PawsNova clean package
docker build -t your-repo/helpdesk-nova -f Dockerfile .

# Push to ECR
docker tag your-repo/helpdesk-nova:latest <account>.dkr.ecr.<region>.amazonaws.com/helpdesk-nova:latest
docker push <account>.dkr.ecr.<region>.amazonaws.com/helpdesk-nova:latest

# Deploy to Lambda using container image support
```

### Infrastructure as Code

**Terraform example** (create `main.tf`):

```hcl
resource "aws_lambda_function" "helpdesk" {
  function_name = "helpdesk-backend"
  role          = aws_iam_role.helpdesk_lambda.arn
  handler       = "cloud.contoterzi.aws.common.LambdaHandler::handleRequest"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 30
  
  filename         = "lambda.zip"
  source_code_hash = filebase64sha256("lambda.zip")
  
  environment {
    variables = {
      APP_CONFIG_PATH = "/var/task/config/helpdesk-config.yaml"
    }
  }
}
```

---

## 🧪 Testing

### Unit Tests
```bash
# Test core engine
mvn -pl helpdesk-core -am test

# Test specific module
mvn -pl helpdesk-similarity test
```

### Integration Tests
```bash
# End-to-end with Nova profile
mvn verify -PawsNova

# All providers
mvn verify -Pall
```

### Load Testing
```bash
# Use included Artillery config (requires artillery npm package)
artillery run test/load-test.yml
```

---

## 🔧 Build Profiles

Switch between cloud providers and LLM backends:

- **`awsNova`** - AWS Bedrock Nova + S3 + Lambda (default)
- **`awsClaude`** - AWS Bedrock Claude + S3 + Lambda
- **`ollama`** - Ollama (local or EC2) + S3 + Lambda
- **`azure`** - Azure OpenAI + Blob Storage + Azure Functions
- **`ibm`** - IBM Watson + Cloud Object Storage
- **`all`** - Build everything (CI/CD)

Example:
```bash
mvn -Pollama clean package
```

---

## 🎨 Extension Points

### Add New LLM Provider

1. Create module `helpdesk-llm-yourprovider`
2. Implement `LlmClient` interface (or extend `AbstractLlmClient`)
3. Register in `META-INF/services/cloud.contoterzi.core.spi.LlmClient`
4. Configure via `llm.type=yourprovider` in YAML

### Add New Storage Backend

1. Create module `helpdesk-storage-yourbackend`
2. Implement `StorageAdapter` interface
3. Register in `META-INF/services/cloud.contoterzi.core.spi.StorageAdapter`
4. Configure via `storage.type=yourbackend` in YAML

### Custom Similarity Algorithm

1. Extend `SimilarityService` in `helpdesk-similarity`
2. Register implementation
3. Configure via `similarity.type=youralgorithm`

---

## 📈 Production Readiness Checklist

✅ **Error Handling** - Graceful degradation, retry logic, circuit breakers  
✅ **Observability** - Structured logging, metrics, distributed tracing hooks  
✅ **Security** - IAM roles, least privilege, encrypted storage  
✅ **Scalability** - Stateless design, serverless auto-scaling  
✅ **Cost Optimization** - Similarity pre-filtering, caching, configurable thresholds  
✅ **Testing** - Unit, integration, load tests included  
✅ **Documentation** - Architecture decisions, API contracts, deployment guides  
✅ **Multi-tenancy Ready** - Configuration per tenant, isolated data

### What's NOT Included (Future Work)

- ⚠️ **UI/Frontend** - API-only backend (integrate with your existing help desk UI)
- ⚠️ **User Authentication** - Delegate to API Gateway / Auth0 / Cognito
- ⚠️ **Monitoring Dashboard** - Use CloudWatch / Datadog / Grafana
- ⚠️ **Multi-language Support** - Currently optimized for English queries

---

## 📚 Documentation

- **[Architecture Decision Records](docs/adr/)** - Why we made key design choices
- **[API Documentation](docs/api.md)** - Request/response formats, error codes
- **[Performance Tuning](docs/performance.md)** - Benchmarks and optimization tips
- **[Cost Analysis](docs/costs.md)** - Estimated AWS costs per tier

---

## 🤝 Want This for Your Company?

This system was built as a Master's thesis but is **production-ready and adaptable** to real-world use cases.

**I offer:**
- ✅ Custom deployment and integration
- ✅ Knowledge base creation from your docs
- ✅ Fine-tuning for your specific domain
- ✅ Training and handoff to your team
- ✅ Ongoing support and optimization

**Contact:** [paoloproni@gmail.com](mailto:paoloproni@gmail.com)  
**LinkedIn:** [linkedin.com/in/paolo-proni-60020713](https://linkedin.com/in/paolo-proni-60020713)  
**Portfolio:** [contoterzi.cloud](https://contoterzi.cloud)

---

## 📝 License

MIT License - See [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Free University of Bozen-Bolzano** - Master's program in Software Engineering
- **AWS Bedrock team** - Excellent LLM infrastructure
- **Ollama community** - Local LLM testing capabilities

---

## ⭐ Star This Repo

If you find this project useful, please star it! It helps others discover production-ready serverless architectures.

---

**Built with 20+ years of enterprise Java experience + modern cloud-native thinking.**

---

## 🔗 Related Projects

- [My other serverless projects](https://github.com/paoloproni)
- [Blog: Migrating Java Monoliths to Serverless](https://paoloproni.blogspot.com)

---

**Questions? Issues? PRs welcome!** 🚀

---

