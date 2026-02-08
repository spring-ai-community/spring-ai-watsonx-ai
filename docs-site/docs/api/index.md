---
sidebar_position: 6
---

# API Reference

This section provides detailed API reference documentation for Spring AI Watsonx.ai integration.

## Core APIs

### Chat API

The Chat API provides conversational AI capabilities using Watsonx.ai foundation models.

#### WatsonxAiChatModel

Main interface for chat interactions:

```java
public class WatsonxAiChatModel implements ChatModel {

    public ChatResponse call(Prompt prompt);

    public Flux<ChatResponse> stream(Prompt prompt);
}
```

**Key Methods:**

- `call(Prompt)` - Synchronous chat completion
- `stream(Prompt)` - Streaming chat completion

#### WatsonxAiChatOptions

Configuration options for chat models:

```java
public class WatsonxAiChatOptions implements ChatOptions {

    private String model;
    private Double temperature;
    private Integer maxNewTokens;
    private Double topP;
    private Integer topK;
    private Double repetitionPenalty;
    private List<String> stopSequences;
    // ... additional options
}
```

**Builder Pattern:**

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .withModel("ibm/granite-13b-chat-v2")
    .withTemperature(0.7)
    .withMaxNewTokens(1024)
    .build();
```

### Embedding API

The Embedding API generates vector representations of text for semantic search and similarity analysis.

#### WatsonxAiEmbeddingModel

Main interface for embeddings:

```java
public class WatsonxAiEmbeddingModel implements EmbeddingModel {

    public EmbeddingResponse embedForResponse(List<String> texts);

    public List<Double> embed(String text);
}
```

**Key Methods:**

- `embedForResponse(List<String>)` - Generate embeddings for multiple texts
- `embed(String)` - Generate embedding for single text

#### WatsonxAiEmbeddingOptions

Configuration options for embedding models:

```java
public class WatsonxAiEmbeddingOptions implements EmbeddingOptions {

    private String model;
    private Boolean truncateInputTokens;
}
```

## Request/Response Objects

### ChatRequest

```java
public class WatsonxAiChatRequest {
    private String model;
    private List<Message> messages;
    private WatsonxAiChatOptions options;
}
```

### ChatResponse

```java
public class WatsonxAiChatResponse {
    private List<Generation> generations;
    private ChatResponseMetadata metadata;
}
```

### EmbeddingRequest

```java
public class WatsonxAiEmbeddingRequest {
    private String model;
    private List<String> inputs;
    private WatsonxAiEmbeddingOptions options;
}
```

### EmbeddingResponse

```java
public class WatsonxAiEmbeddingResponse {
    private List<Embedding> embeddings;
    private EmbeddingResponseMetadata metadata;
}
```

## Authentication

### WatsonxAiAuthentication

Handles IBM Cloud IAM authentication:

```java
public class WatsonxAiAuthentication {

    public WatsonxAiAuthentication(String apiKey);

    public String getAccessToken();
}
```

## Configuration Properties

### Connection Properties

```java
@ConfigurationProperties("spring.ai.watsonx.ai")
public class WatsonxAiConnectionProperties {
    private String apiKey;
    private String url;
    private String projectId;
}
```

### Chat Properties

```java
@ConfigurationProperties("spring.ai.watsonx.ai.chat")
public class WatsonxAiChatProperties {
    private boolean enabled = true;
    private WatsonxAiChatOptions options;
}
```

### Embedding Properties

```java
@ConfigurationProperties("spring.ai.watsonx.ai.embedding")
public class WatsonxAiEmbeddingProperties {
    private boolean enabled = true;
    private WatsonxAiEmbeddingOptions options;
}
```

## Auto-Configuration

### WatsonxAiChatAutoConfiguration

Automatically configures chat model beans:

```java
@Configuration
@ConditionalOnClass(WatsonxAiChatModel.class)
@EnableConfigurationProperties({
    WatsonxAiConnectionProperties.class,
    WatsonxAiChatProperties.class
})
public class WatsonxAiChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WatsonxAiChatModel watsonxAiChatModel(
            WatsonxAiChatApi chatApi,
            WatsonxAiChatProperties properties) {
        return new WatsonxAiChatModel(chatApi, properties.getOptions());
    }
}
```

### WatsonxAiEmbeddingAutoConfiguration

Automatically configures embedding model beans:

```java
@Configuration
@ConditionalOnClass(WatsonxAiEmbeddingModel.class)
@EnableConfigurationProperties({
    WatsonxAiConnectionProperties.class,
    WatsonxAiEmbeddingProperties.class
})
public class WatsonxAiEmbeddingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WatsonxAiEmbeddingModel watsonxAiEmbeddingModel(
            WatsonxAiEmbeddingApi embeddingApi,
            WatsonxAiEmbeddingProperties properties) {
        return new WatsonxAiEmbeddingModel(embeddingApi, properties.getOptions());
    }
}
```

## Exception Handling

### Common Exceptions

```java
// Authentication failure
public class WatsonxAiAuthenticationException extends RuntimeException

// API errors
public class WatsonxAiApiException extends RuntimeException

// Rate limiting
public class WatsonxAiRateLimitException extends RuntimeException

// Invalid configuration
public class WatsonxAiConfigurationException extends RuntimeException
```

## Usage Examples

### Basic Chat

```java
@Service
public class ChatService {

    private final WatsonxAiChatModel chatModel;

    public String chat(String message) {
        return chatModel.call(message);
    }
}
```

### Streaming Chat

```java
@Service
public class StreamingChatService {

    private final WatsonxAiChatModel chatModel;

    public Flux<String> streamChat(String message) {
        return chatModel.stream(new Prompt(message))
            .map(response -> response.getResult().getOutput().getContent());
    }
}
```

### Generate Embeddings

```java
@Service
public class EmbeddingService {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public List<Double> generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }
}
```

## See Also

- [Chat Models Guide](../chat/index.md)
- [Embeddings Guide](../embeddings/index.md)
- [Configuration Reference](../configuration.md)
- [JavaDoc](https://javadoc.io/doc/org.springaicommunity/spring-ai-watsonx-ai)
