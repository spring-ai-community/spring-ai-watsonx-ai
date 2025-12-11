# Spring AI Watsonx.ai

[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity/spring-ai-starter-model-watsonx-ai.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=org.springaicommunity&name=spring-ai-starter-model-watsonx-ai)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://adoptium.net/temurin/releases/)
[![Build Status](https://github.com/spring-ai-community/spring-ai-watsonx-ai/workflows/PR%20Check/badge.svg)](https://github.com/spring-ai-community/spring-ai-watsonx-ai/actions/workflows/pr-check.yml)
[![Documentation](https://github.com/spring-ai-community/spring-ai-watsonx-ai/workflows/Deploy%20watsonx.ai%20Documentation/badge.svg)](https://github.com/spring-ai-community/spring-ai-watsonx-ai/actions/workflows/publish-docs.yml)

Spring AI Watsonx.ai provides Spring AI integration with IBM's Watsonx.ai platform, enabling developers to leverage powerful foundation models for chat, embeddings, and content moderation in their applications.

## Overview

IBM Watsonx.ai is an enterprise-ready AI platform that provides access to various foundation models including:

- **Chat Models**: IBM Granite, Meta Llama, Mistral AI, and other conversational AI models
- **Embedding Models**: IBM's embedding models for semantic search and similarity analysis
- **Moderation Models**: Content safety detection including HAP, PII, and Granite Guardian detectors

This integration brings these capabilities to Spring Boot applications through familiar Spring AI abstractions.

## Features

- ✅ **Chat Models**: Support for multiple foundation models with streaming capabilities
- ✅ **Embedding Models**: Generate embeddings for semantic search and similarity analysis
- ✅ **Moderation Models**: Content safety with HAP, PII, and Granite Guardian detectors
- ✅ **Spring Boot Auto-configuration**: Zero-configuration setup with Spring Boot
- ✅ **Flexible Configuration**: Runtime parameter overrides and multiple model configurations
- ✅ **Function Calling**: Connect LLMs with external tools and APIs
- ✅ **Reactive Support**: Built-in support for reactive programming with WebFlux

## Quick Start

### Prerequisites

1. Create an account at [IBM Cloud](https://cloud.ibm.com)
2. Set up a Watsonx.ai service instance
3. Generate API keys from the IBM Cloud console

### Installation

Add the Spring AI Watsonx.ai starter to your project:

**Maven:**
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:1.0.0'
```

### Configuration

Configure your application with Watsonx.ai credentials:

**application.yml:**
```yaml
spring:
  ai:
    watsonx:
      ai:
        api-key: ${WATSONX_AI_API_KEY}
        url: ${WATSONX_AI_URL}
        project-id: ${WATSONX_AI_PROJECT_ID}
```

**Environment Variables:**
```bash
export WATSONX_AI_API_KEY=your_api_key_here
export WATSONX_AI_URL=https://us-south.ml.cloud.ibm.com
export WATSONX_AI_PROJECT_ID=your_project_id_here
```

### Basic Usage

#### Chat Model

```java
@RestController
public class ChatController {

    private final WatsonxAiChatModel chatModel;

    public ChatController(WatsonxAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatModel.call(message);
    }

    @GetMapping("/chat/stream")
    public Flux<String> chatStream(@RequestParam String message) {
        return chatModel.stream(new Prompt(message))
            .map(response -> response.getResult().getOutput().getContent());
    }
}
```

#### Embedding Model

```java
@RestController
public class EmbeddingController {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public EmbeddingController(WatsonxAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/embed")
    public List<Double> embed(@RequestParam String text) {
        return embeddingModel.embed(text);
    }
}
```

#### Moderation Model

```java
@RestController
public class ModerationController {

    private final WatsonxAiModerationModel moderationModel;

    public ModerationController(WatsonxAiModerationModel moderationModel) {
        this.moderationModel = moderationModel;
    }

    @PostMapping("/moderate")
    public ModerationResponse moderate(@RequestBody String text) {
        ModerationPrompt prompt = new ModerationPrompt(text);
        return moderationModel.call(prompt);
    }
}
```

## Architecture

The Spring AI Watsonx.ai integration consists of three main modules:

### Core Modules

- **watsonx-ai-core**: Core implementation with API clients and model classes
- **spring-ai-autoconfigure-model-watsonx-ai**: Spring Boot auto-configuration
- **spring-ai-starter-model-watsonx-ai**: Spring Boot starter for easy integration

### Key Components

```
spring-ai-watsonx-ai/
├── watsonx-ai-core/
│   ├── WatsonxAiChatModel        # Chat model implementation
│   ├── WatsonxAiEmbeddingModel   # Embedding model implementation
│   ├── WatsonxAiModerationModel  # Content moderation implementation
│   └── WatsonxAiAuthentication   # IBM Cloud IAM authentication
├── spring-ai-autoconfigure-model-watsonx-ai/
│   └── Auto-configuration classes
└── spring-ai-starter-model-watsonx-ai/
    └── Starter dependencies
```

## Supported Models

A comprehensive list of supported models under the watsonx.ai platform: [watsonx.ai Supported Models](https://www.ibm.com/watsonx/developer/get-started/models)

## Configuration Options

### Chat Model Configuration

```yaml
spring:
  ai:
    watsonx:
      ai:
        chat:
          options:
            model: ibm/granite-13b-chat-v2
            temperature: 0.7
            max-new-tokens: 1024
            top-p: 1.0
            top-k: 50
            repetition-penalty: 1.0
```

### Embedding Model Configuration

```yaml
spring:
  ai:
    watsonx:
      ai:
        embedding:
          options:
            model: ibm/slate-125m-english-rtrvr
            parameters:
                truncate-input-tokens: true
                return-options:
                    input-text: false
```

### Moderation Model Configuration

```yaml
spring:
  ai:
    watsonx:
      ai:
        moderation:
          version: "2025-10-01"
          options:
            # HAP (Hate, Abuse, Profanity) detector
            hap:
              threshold: 0.75
            # PII (Personally Identifiable Information) detector
            pii:
              threshold: 0.8
            # Granite Guardian detector
            granite-guardian:
              threshold: 0.6
```

## Advanced Features

### Function Calling

Connect your LLMs with external tools and APIs:

```java
@Bean
@Description("Get current weather information")
public Function<WeatherRequest, WeatherResponse> getCurrentWeather() {
    return request -> {
        // Implementation to fetch weather data
        return new WeatherResponse(25.0, "sunny", request.location());
    };
}
```

### Multiple Model Configurations

Configure different models for different use cases:

```java
@Configuration
public class MultiModelConfiguration {

    @Bean("creativeChatModel")
    public WatsonxAiChatModel creativeChatModel(WatsonxAiChatApi chatApi) {
        return new WatsonxAiChatModel(chatApi,
            WatsonxAiChatOptions.builder()
                .withModel("meta-llama/llama-3-70b-instruct")
                .withTemperature(1.2)
                .build());
    }
}
```

### Reactive Streaming

Built-in support for reactive programming:

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamResponse(@RequestParam String prompt) {
    return chatModel.stream(new Prompt(prompt))
        .map(response -> response.getResult().getOutput().getContent())
        .map(content -> ServerSentEvent.<String>builder().data(content).build());
}
```

### Content Moderation

The moderation model provides comprehensive content safety detection:

**Available Detectors:**
- **HAP (Hate, Abuse, Profanity)**: Detects hate speech, abusive language, and profanity
- **PII (Personally Identifiable Information)**: Identifies sensitive personal information like emails, phone numbers, addresses
- **Granite Guardian**: IBM's comprehensive content moderation detector for harmful content

**Example Usage:**

```java
@Service
public class ContentModerationService {

    private final WatsonxAiModerationModel moderationModel;

    public ContentModerationService(WatsonxAiModerationModel moderationModel) {
        this.moderationModel = moderationModel;
    }

    public boolean isContentSafe(String userInput) {
        ModerationPrompt prompt = new ModerationPrompt(userInput);
        ModerationResponse response = moderationModel.call(prompt);
        
        // Check if any detector flagged the content
        return !response.getResult().getOutput().getResults().get(0).isFlagged();
    }

    public ContentAnalysis analyzeContent(String text) {
        ModerationPrompt prompt = new ModerationPrompt(text);
        ModerationResponse response = moderationModel.call(prompt);
        
        var result = response.getResult().getOutput().getResults().get(0);
        CategoryScores scores = result.getCategoryScores();
        
        return new ContentAnalysis(
            result.isFlagged(),
            scores.getHate(),
            scores.getHarassment(),
            scores.getSelfHarm(),
            scores.getSexual(),
            scores.getViolence()
        );
    }
}
```

**Response Analysis:**

```java
// Get detailed detection information
WatsonxAiModerationResponseMetadata metadata =
    (WatsonxAiModerationResponseMetadata) response.getMetadata();

List<Map<String, Object>> detections = metadata.getDetections();
for (Map<String, Object> detection : detections) {
    String detectionType = (String) detection.get("detectionType"); // "hap", "pii", etc.
    String detectedText = (String) detection.get("text");
    Float confidenceScore = (Float) detection.get("score");
    Integer startPosition = (Integer) detection.get("start");
    Integer endPosition = (Integer) detection.get("end");
    
    System.out.println(String.format(
        "Detected %s: '%s' (score: %.2f) at position %d-%d",
        detectionType, detectedText, confidenceScore, startPosition, endPosition
    ));
}
```

## Examples

### Customer Support Chatbot

```java
@Service
public class CustomerSupportService {

    private final WatsonxAiChatModel chatModel;

    public String handleQuery(String customerId, String query) {
        var options = WatsonxAiChatOptions.builder()
            .withModel("ibm/granite-13b-chat-v2")
            .withTemperature(0.3)
            .withFunction("getOrderStatus")
            .withFunction("createSupportTicket")
            .build();

        return chatModel.call(new Prompt(buildContextualPrompt(customerId, query), options));
    }
}
```

### Document Analysis

```java
@Service
public class DocumentAnalysisService {

    private final WatsonxAiChatModel chatModel;
    private final WatsonxAiEmbeddingModel embeddingModel;

    public DocumentAnalysis analyzeDocument(String content) {
        // Generate summary
        String summary = chatModel.call("Summarize: " + content);
        
        // Generate embeddings for similarity search
        List<Double> embeddings = embeddingModel.embed(content);
        
        return new DocumentAnalysis(summary, embeddings);
    }
}
```

### Safe Content Pipeline

```java
@Service
public class SafeContentPipeline {

    private final WatsonxAiModerationModel moderationModel;
    private final WatsonxAiChatModel chatModel;

    public String processUserInput(String userInput) {
        // Step 1: Check content safety
        ModerationPrompt moderationPrompt = new ModerationPrompt(userInput);
        ModerationResponse moderationResponse = moderationModel.call(moderationPrompt);
        
        if (moderationResponse.getResult().getOutput().getResults().get(0).isFlagged()) {
            return "Your input contains inappropriate content. Please revise.";
        }
        
        // Step 2: Process safe content with chat model
        return chatModel.call(userInput);
    }
}
```

## Documentation

For comprehensive documentation, examples, and API reference, visit:

- [Full Documentation](https://spring-ai-community.github.io/spring-ai-watsonx-ai)
- [Getting Started Guide](docs/src/main/antora/modules/ROOT/pages/index.adoc)
- [Chat Models](docs/src/main/antora/modules/ROOT/pages/chat/index.adoc)
- [Embedding Models](docs/src/main/antora/modules/ROOT/pages/embeddings/index.adoc)
- [Moderation Models](docs/src/main/antora/modules/ROOT/pages/moderation/watsonx-ai-moderation.adoc)
- [Configuration](docs/src/main/antora/modules/ROOT/pages/configuration.adoc)
- [Examples](docs/src/main/antora/modules/ROOT/pages/examples.adoc)

## Building from Source

### Prerequisites

- Java 17 or later
- Maven 3.8.4 or later

### Build

```bash
git clone https://github.com/spring-ai-community/spring-ai-watsonx-ai.git
cd spring-ai-watsonx-ai
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Build Documentation

```bash
cd docs
mvn clean package
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.adoc) for details on:

- Code of Conduct
- Development setup
- Submitting pull requests
- Reporting issues

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## Community

- [GitHub Discussions](https://github.com/spring-ai-community/spring-ai-watsonx-ai/discussions) - Ask questions and share ideas
- [Issues](https://github.com/spring-ai-community/spring-ai-watsonx-ai/issues) - Report bugs and request features

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full license text.

## Acknowledgments

- [Spring AI](https://spring.io/projects/spring-ai) - The foundational framework
- [IBM Watsonx.ai](https://www.ibm.com/products/watsonx-ai) - The AI platform
- [Spring Community](https://spring.io/community) - The vibrant community
