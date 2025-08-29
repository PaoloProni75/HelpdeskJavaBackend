# Ollama Module for Helpdesk (Unified)

This project includes a unified module for integration with Ollama and **any** open source LLM model supported by Ollama.

## Unified Module

### helpdesk-llm-ollama
- **Supported models**: ALL Ollama models (GPT4All, Mistral, Nemotron, Llama2, CodeLlama, etc.)
- **Maven profile**: `ollama`
- **Type ID**: `ollama`
- **Configuration**: `modelId` in YAML config specifies which model to use
- **Advantages**: Single driver, zero code duplication, maximum flexibility

## Build Commands

```bash
# Build unified Ollama module
mvn clean package -P ollama

# Complete build (all providers including Ollama)
mvn clean package -P all
```

## Ollama VM Setup

### 1. Base Installation
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Configure for external connections
sudo systemctl edit ollama
```

Add:
```ini
[Service]
Environment="OLLAMA_HOST=0.0.0.0:11434"
```

### 2. Model Downloads
```bash
# For OSS GPT
ollama pull gpt4all
ollama pull llama2
ollama pull codellama

# For Mistral
ollama pull mistral

# For Nemotron
ollama pull nemotron-mini    # Recommended for 32GB RAM
ollama pull nemotron         # Requires 64GB+ RAM
```

## Example Configurations

### Unified Configuration
```yaml
# All models use the same format!
llm:
  type: ollama                    # Fixed ID
  endpoint: http://VM_IP:11434    # Ollama endpoint
  modelId: MODEL_NAME            # Only change this
  temperature: 0.4               # Optimize for model
  maxTokens: 1024               # Optimize for model
```

### Specific Examples
```yaml
# GPT4All
llm:
  type: ollama
  modelId: gpt4all

# Mistral  
llm:
  type: ollama
  modelId: mistral

# Nemotron
llm:
  type: ollama
  modelId: nemotron-mini

# Llama2
llm:
  type: ollama
  modelId: llama2
```

## VM Performance (8 vCPU, 32GB)

| Model | Response Time | Max Context | RAM Required |
|-------|---------------|-------------|--------------|
| gpt4all | 3-5 sec | 4K tokens | 8GB |
| llama2 | 2-4 sec | 4K tokens | 8GB |
| mistral | 2-3 sec | 8K tokens | 8GB |
| nemotron-mini | 1-2 sec | 4K tokens | 4GB |
| nemotron | 3-4 sec | 4K tokens | 16GB |

## Architecture

All modules share:
- **API Compatibility**: OpenAI-compatible via `/v1/chat/completions`
- **SPI Integration**: Automatic registration with Java SPI
- **Error Handling**: Automatic retries and error management
- **Fat JAR**: Complete builds with all dependencies

## Connectivity Test

```bash
# Generic test for all models
curl -X POST http://VM_IP:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "MODEL_NAME",
    "messages": [
      {"role": "user", "content": "Hello, how can you help with technical support?"}
    ],
    "max_tokens": 150
  }'
```

## Recommendations

- **For development**: `nemotron-mini` (fast, efficient)
- **For general production**: `mistral` (balanced)
- **For specific cases**: `gpt4all` or `llama2` (versatile)

All modules are **production-ready** and follow the same architectural conventions of the helpdesk system.