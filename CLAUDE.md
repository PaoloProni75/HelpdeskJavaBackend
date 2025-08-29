# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a modular helpdesk backend system built with Java 17 and Maven, designed to provide AI-powered support responses using various LLM providers. The system uses a plugin-based architecture with Service Provider Interface (SPI) pattern for extensibility.

## Build System & Commands

### Build Commands
- `mvn clean compile` - Compile all modules
- `mvn clean package` - Build all modules with default profile (core + similarity only)
- `mvn clean package -P awsClaude` - Build with AWS Claude/Bedrock support
- `mvn clean package -P awsNova` - Build with AWS Nova support  
- `mvn clean package -P all` - Build all modules including all LLM providers

### Test Commands
- `mvn test` - Run all tests
- `mvn test -Dtest=ClassName` - Run specific test class
- `mvn test -Dtest=ClassName#methodName` - Run specific test method

### Maven Profiles
- Default: `helpdesk-core`, `helpdesk-similarity` (portable modules)
- `awsClaude`: Adds AWS common, S3 storage, Claude LLM
- `awsNova`: Adds AWS common, S3 storage, Nova LLM  
- `all`: Includes all provider modules (AWS, Azure, IBM, Ollama)

## Architecture

### Core Components
- **helpdesk-core**: Central engine with SPI interfaces and business logic
- **helpdesk-similarity**: Text similarity algorithms using cosine distance
- **helpdesk-aws-common**: AWS Lambda handler and common AWS utilities

### Plugin Architecture
The system uses Java SPI for modularity:
- **LLM Providers**: `helpdesk-llm-aws-claude`, `helpdesk-llm-aws-nova`, etc.
- **Storage Adapters**: `helpdesk-storage-s3` 
- **Similarity Services**: Loaded via `cloud.contoterzi.helpdesk.core.spi.*`

### Key Classes
- `HelpdeskEngine` (helpdesk-core): Main processing engine that coordinates similarity matching, knowledge base lookup, and LLM calls
- `LambdaHandler` (helpdesk-aws-common): AWS Lambda entry point with static engine initialization
- `AppState` (helpdesk-core): Singleton configuration manager
- Service interfaces in `cloud.contoterzi.helpdesk.core.spi.*`

### Request Flow
1. Lambda receives question via `LambdaHandler`
2. `HelpdeskEngine.processQuestion()` finds best knowledge base match using similarity service
3. Based on confidence threshold, either returns KB answer directly or invokes LLM
4. LLM responses checked for escalation phrases (configurable "contact support")

## Configuration

Configuration is managed through YAML files:
- Default config path: `APP_CONFIG_PATH` environment variable
- Test config: `helpdesk-core/src/test/resources/test-config.yaml`
- Lambda config: `config/layer/config/helpdesk-config.yaml`

Key config sections:
- `llm`: LLM provider settings (type, model, prompts, temperature)
- `storage`: Knowledge base storage (S3 bucket details)
- `similarity`: Similarity algorithm settings (threshold, algorithm type)

## Development Notes

### Testing Environment Variables
Tests in helpdesk-core require:
- `APP_CONFIG_PATH`: Path to test configuration file
- `ALWAYS_CALL_LLM`: Set to false for deterministic testing

### SPI Registration
New providers must register via `META-INF/services/` files with fully qualified interface names.

### Shaded JARs
Maven shade plugin is configured to merge SPI service files across modules for proper deployment.