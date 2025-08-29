# Helpdesk LLM Ollama Driver (Unified)

Driver unificato per l'integrazione del sistema helpdesk con Ollama per l'utilizzo di qualsiasi modello LLM open source supportato da Ollama.

## Caratteristiche

- ✅ **Supporto universale**: Funziona con tutti i modelli Ollama (GPT4All, Mistral, Nemotron, Llama2, CodeLlama, ecc.)
- ✅ **Configurazione semplice**: Un solo tipo `ollama` con `modelId` configurabile
- ✅ **API OpenAI-compatible**: Utilizza l'endpoint `/v1/chat/completions` di Ollama
- ✅ **Architettura SPI**: Integrazione perfetta con il sistema helpdesk
- ✅ **Gestione errori**: Retry automatici e error handling robusto

## Configurazione

### 1. Setup Ollama sulla VM

Installa Ollama sulla tua macchina virtuale Ubuntu (8 vCPU, 32 GB):

```bash
# Installa Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Avvia il servizio
sudo systemctl start ollama
sudo systemctl enable ollama

# Configura per accettare connessioni esterne
sudo systemctl edit ollama
```

Aggiungi le seguenti righe:
```ini
[Service]
Environment="OLLAMA_HOST=0.0.0.0:11434"
```

Riavvia il servizio:
```bash
sudo systemctl restart ollama
```

### 2. Scarica i modelli

```bash
# Modelli raccomandati
ollama pull gpt4all          # Generico, buon bilanciamento
ollama pull mistral          # Ottimo per conversazioni (7B)
ollama pull nemotron-mini    # NVIDIA, veloce (4B)
ollama pull llama2           # Meta, versatile
ollama pull codellama        # Specializzato per codice

# Verifica installazione
ollama list
```

### 3. Configurazione del sistema helpdesk

Il driver unificato usa `type: ollama` e il `modelId` specifica quale modello usare:

#### Esempio con GPT4All
```yaml
llm:
  type: ollama
  endpoint: http://YOUR_VM_IP:11434
  modelId: gpt4all
  temperature: 0.4
  maxTokens: 1024
  prompts:
    preamble: >- 
              You are a help desk assistant for an agricultural subcontractor management software. 
              Answer clearly and helpfully only if the question is relevant to the software. 
              If it is not, state that you cannot answer. 
              If the user needs human intervention, include EXACTLY the phrase 'contact support' once in the answer. 
              If no human is required, DO NOT include that phrase.
    template: "%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:"
    contactSupportPhrase: "contact support"
```

#### Esempio con Mistral
```yaml
llm:
  type: ollama
  endpoint: http://YOUR_VM_IP:11434
  modelId: mistral
  temperature: 0.3              # Mistral funziona bene con temperature più basse
  maxTokens: 2048               # Supporta contesti più lunghi
  # ... resto della configurazione identico
```

#### Esempio con Nemotron
```yaml
llm:
  type: ollama
  endpoint: http://YOUR_VM_IP:11434
  modelId: nemotron-mini        # o "nemotron" per la versione completa
  temperature: 0.4
  maxTokens: 1024
  # ... resto della configurazione identico
```

## Build

```bash
# Build solo modulo Ollama unificato
mvn clean package -P ollama

# Build completo con tutti i provider
mvn clean package -P all
```

## Modelli Supportati e Prestazioni

| Modello | Parametri | Tempo Risposta* | Contesto Max | RAM Richiesta | Specializzazione |
|---------|-----------|-----------------|--------------|---------------|-------------------|
| gpt4all | 7B | 3-5 sec | 4K tokens | 8GB | Generale, bilanciato |
| mistral | 7B | 2-3 sec | 8K tokens | 8GB | Conversazioni, supporto |
| nemotron-mini | 4B | 1-2 sec | 4K tokens | 4GB | NVIDIA, veloce |
| nemotron | 70B | 3-4 sec | 4K tokens | 16GB | NVIDIA, qualità alta |
| llama2 | 7B | 2-4 sec | 4K tokens | 8GB | Meta, versatile |
| codellama | 7B | 3-5 sec | 4K tokens | 8GB | Specializzato codice |

*Tempi su VM 8 vCPU/32GB

## Test di connettività

```bash
# Test generico - funziona con qualsiasi modello
curl -X POST http://YOUR_VM_IP:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "NOME_MODELLO",
    "messages": [
      {"role": "user", "content": "Hello, how can you help with technical support?"}
    ],
    "max_tokens": 150
  }'
```

## Esempi di cambio modello

Per cambiare modello, basta modificare il `modelId` nella configurazione:

```yaml
# Da GPT4All a Mistral
llm:
  type: ollama              # rimane uguale
  endpoint: http://vm:11434 # rimane uguale
  modelId: mistral          # cambia solo questo
  temperature: 0.3          # opzionalmente ottimizza i parametri
```

## Raccomandazioni per uso

- **Sviluppo/Test**: `nemotron-mini` (veloce, efficiente)
- **Produzione generale**: `mistral` (bilanciato qualità/velocità)
- **Supporto tecnico**: `gpt4all` (versatile, affidabile)
- **Coding support**: `codellama` (specializzato)
- **Qualità massima**: `nemotron` (richiede più RAM)

## Architettura

Il driver unificato:
- Implementa l'interfaccia `LlmClient` 
- ID provider: `"ollama"`
- Endpoint OpenAI-compatible: `/v1/chat/completions`
- Supporta system prompt, temperature, max_tokens
- Gestione errori con retry automatici
- Fat JAR con tutte le dipendenze

Un solo driver, infinita flessibilità! 🚀