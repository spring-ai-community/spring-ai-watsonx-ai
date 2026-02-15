---
sidebar_position: 1
---

# Watsonx.ai AI Models

IBM Watsonx.ai models provide a comprehensive suite of AI capabilities including chat models, embedding models, and moderation. This documentation covers the integration of Watsonx.ai services with Spring AI applications.

## Overview

Watsonx.ai offers several powerful AI models:

- **Chat Models**: Conversational AI models for building intelligent chatbots and assistants
- **Embedding Models**: Text embedding models for semantic search and similarity analysis
- **Rerank Models**: Document reranking for improved search relevance in RAG pipelines
- **Moderation Models**: Content moderation capabilities for safe and responsible AI

## Quick Start

To get started with Watsonx.ai models in your application, add the following dependency:

:::tip
Check [Maven Central](https://central.sonatype.com/search?namespace=org.springaicommunity&name=spring-ai-starter-model-watsonx-ai) for the latest version.
:::

### Maven

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>1.X.X</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:1.X.X'
```

Configure your Watsonx.ai credentials in `application.yml`:

```yaml
spring:
  ai:
    watsonx:
    ai:
      api-key: ${WATSONX_AI_API_KEY}
      base-url: ${WATSONX_AI_BASE_URL}
      project-id: ${WATSONX_AI_PROJECT_ID}
```

Or in `application.properties`:

```properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.base-url=${WATSONX_AI_BASE_URL}
spring.ai.watsonx.ai.project-id=${WATSONX_AI_PROJECT_ID}
```

Set your credentials as environment variables:

```bash
export WATSONX_AI_API_KEY=<INSERT API KEY HERE>
export WATSONX_AI_BASE_URL=<INSERT WATSONX AI BASE URL HERE>
export WATSONX_AI_PROJECT_ID=<INSERT PROJECT ID HERE>
```

:::tip
For more configuration options, see the [Configuration Guide](./configuration.md).
:::

## Source Code

The source code for this project is available on GitHub at [spring-ai-community/spring-ai-watsonx-ai](https://github.com/spring-ai-community/spring-ai-watsonx-ai).

## Available Services

| Service                                    | Description                                                                |
| :----------------------------------------- | :------------------------------------------------------------------------- |
| [Chat Models](./chat/index.md)             | Build conversational AI applications with Watsonx.ai's various chat models |
| [Embedding Models](./embeddings/index.md)  | Generate text embeddings for semantic search and text similarity analysis  |
| [Rerank Models](./rerank/index.md)         | Document reranking for improved search relevance in RAG pipelines          |
| [Moderation Models](./moderation/index.md) | Content moderation capabilities for safe and responsible AI applications   |

## Architecture

The Spring AI Watsonx.ai integration consists of three main modules:

- **watsonx-ai-core**: Core implementation with API clients and model classes
- **spring-ai-autoconfigure-model-watsonx-ai**: Spring Boot auto-configuration
- **spring-ai-starter-model-watsonx-ai**: Spring Boot starter for easy integration

## Supported Models

### Chat Models

- IBM Granite models
- Meta Llama models
- Mistral AI models
- And other foundation models available in Watsonx.ai

### Embedding Models

- IBM's embedding models
- Sentence transformers
- Custom embedding models deployed in Watsonx.ai

### Moderation Models

- IBM's content moderation models
- Toxicity detection
- Hate speech detection
- PII (Personally Identifiable Information) detection
- Custom moderation models

## Getting Help

If you encounter issues or have questions:

- Check the [GitHub Issues](https://github.com/spring-ai-community/spring-ai-watsonx-ai/issues)
- Review the [Spring AI documentation](https://spring.io/projects/spring-ai)
- Consult the [IBM Watsonx.ai documentation](https://cloud.ibm.com/apidocs/watsonx-ai)
