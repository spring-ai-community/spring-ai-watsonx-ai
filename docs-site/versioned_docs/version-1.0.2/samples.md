---
sidebar_position: 8
---

# Sample Applications

This page provides a collection of sample applications demonstrating various features and use cases of Spring AI Watsonx.ai integration.

## Overview

The sample applications showcase:

- Basic chat and embedding functionality
- Advanced features like streaming and function calling
- Integration patterns with Spring Boot
- Real-world use cases and best practices

## Getting Started with Samples

All samples are available in the [samples directory](https://github.com/spring-ai-community/spring-ai-watsonx-ai/tree/main/samples) of the repository.

### Prerequisites

- JDK 17 or later
- Maven 3.9+
- Watsonx.ai API credentials

### Running a Sample

```bash
# Clone the repository
git clone https://github.com/spring-ai-community/spring-ai-watsonx-ai.git
cd spring-ai-watsonx-ai/samples/<sample-name>

# Set environment variables
export SPRING_AI_WATSONX_AI_API_KEY=your_api_key
export SPRING_AI_WATSONX_AI_BASE_URL=your_watsonx_base_url
export SPRING_AI_WATSONX_AI_PROJECT_ID=your_project_id

# Run the sample
mvn spring-boot:run
```

## Available Samples

### Basic Chat Sample

A simple chatbot demonstrating basic conversational AI capabilities.

**Features:**

- Simple prompt-response interaction
- Basic configuration
- Error handling

**Location:** `samples/basic-chat`

```java
@RestController
public class ChatController {

    private final WatsonxAiChatModel chatModel;

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return chatModel.call(message);
    }
}
```

### Streaming Chat Sample

Demonstrates real-time streaming responses for better user experience.

**Features:**

- Server-Sent Events (SSE)
- Streaming responses
- Real-time updates

**Location:** `samples/streaming-chat`

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message) {
    return chatModel.stream(message);
}
```

### Function Calling Sample

Shows how to integrate external tools and functions with the chat model.

**Features:**

- Function definitions
- Tool calling
- Dynamic function execution

**Location:** `samples/function-calling`

```java
@Bean
public FunctionCallback weatherFunction() {
    return FunctionCallback.builder()
        .function("getCurrentWeather", this::getCurrentWeather)
        .description("Get the current weather for a location")
        .inputType(WeatherRequest.class)
        .build();
}
```

### Embedding Sample

Demonstrates text embedding generation for semantic search and similarity analysis.

**Features:**

- Text embedding generation
- Similarity calculation
- Vector storage integration

**Location:** `samples/embeddings`

```java
@Service
public class EmbeddingService {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public List<Double> generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    public double calculateSimilarity(String text1, String text2) {
        List<Double> embedding1 = generateEmbedding(text1);
        List<Double> embedding2 = generateEmbedding(text2);
        return cosineSimilarity(embedding1, embedding2);
    }
}
```

### RAG (Retrieval Augmented Generation) Sample

Demonstrates building a RAG application with document retrieval and generation.

**Features:**

- Document ingestion
- Vector store integration
- Context-aware responses
- Citation support

**Location:** `samples/rag-application`

```java
@Service
public class RagService {

    private final WatsonxAiChatModel chatModel;
    private final VectorStore vectorStore;

    public String answerQuestion(String question) {
        // Retrieve relevant documents
        List<Document> documents = vectorStore.similaritySearch(question);

        // Build context from documents
        String context = buildContext(documents);

        // Generate response with context
        String prompt = String.format(
            "Context: %s\n\nQuestion: %s\n\nAnswer:",
            context, question
        );

        return chatModel.call(prompt);
    }
}
```

### Multi-Model Sample

Shows how to use multiple models for different tasks.

**Features:**

- Multiple model configurations
- Model selection strategies
- Task-specific optimization

**Location:** `samples/multi-model`

```java
@Configuration
public class MultiModelConfig {

    @Bean
    @Primary
    public WatsonxAiChatModel defaultChatModel(WatsonxAiChatApi api) {
        return new WatsonxAiChatModel(api,
            WatsonxAiChatOptions.builder()
                .withModel("ibm/granite-3-3-8b-instruct")
                .build());
    }

    @Bean("codeModel")
    public WatsonxAiChatModel codeChatModel(WatsonxAiChatApi api) {
        return new WatsonxAiChatModel(api,
            WatsonxAiChatOptions.builder()
                .withModel("ibm/granite-20b-code-instruct")
                .build());
    }
}
```

### Prompt Template Sample

Demonstrates using prompt templates for consistent and reusable prompts.

**Features:**

- Template definitions
- Variable substitution
- Template composition

**Location:** `samples/prompt-templates`

```java
@Service
public class PromptTemplateService {

    private final WatsonxAiChatModel chatModel;

    public String generateProductDescription(Product product) {
        PromptTemplate template = new PromptTemplate("""
            Generate a compelling product description for:

            Product Name: {name}
            Category: {category}
            Features: {features}
            Target Audience: {audience}

            The description should be engaging and highlight key benefits.
            """);

        Map<String, Object> variables = Map.of(
            "name", product.getName(),
            "category", product.getCategory(),
            "features", String.join(", ", product.getFeatures()),
            "audience", product.getTargetAudience()
        );

        Prompt prompt = template.create(variables);
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
```

### Conversation Memory Sample

Shows how to maintain conversation context across multiple interactions.

**Features:**

- Conversation history
- Context management
- Memory strategies

**Location:** `samples/conversation-memory`

```java
@Service
public class ConversationService {

    private final WatsonxAiChatModel chatModel;
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    public String chat(String sessionId, String userMessage) {
        List<Message> history = conversations.computeIfAbsent(
            sessionId, k -> new ArrayList<>()
        );

        history.add(new UserMessage(userMessage));

        ChatResponse response = chatModel.call(new Prompt(history));
        Message assistantMessage = response.getResult().getOutput();

        history.add(assistantMessage);

        return assistantMessage.getContent();
    }
}
```

### Spring Boot Web Application Sample

A complete web application demonstrating integration with Spring Boot.

**Features:**

- REST API endpoints
- Web UI
- Configuration management
- Error handling
- Logging and monitoring

**Location:** `samples/web-application`

## Sample Architecture

All samples follow a consistent architecture:

```
sample-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── config/
│   │   │       └── Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   └── test/
├── pom.xml
└── README.md
```

## Best Practices Demonstrated

The samples showcase these best practices:

- **Configuration Management**: Externalized configuration using Spring Boot properties
- **Error Handling**: Proper exception handling and user feedback
- **Testing**: Unit and integration tests for all components
- **Security**: Secure credential management
- **Performance**: Efficient resource usage and caching strategies
- **Observability**: Logging, metrics, and tracing

## Contributing Samples

We welcome contributions of new samples! Please:

1. Follow the existing sample structure
2. Include comprehensive README
3. Add tests
4. Document configuration requirements
5. Submit a pull request

See [Contribution Guidelines](./contribution-guidelines.md) for details.

## Additional Resources

- [GitHub Repository](https://github.com/spring-ai-community/spring-ai-watsonx-ai)
- [Documentation Home](./index.md)
- [Getting Started Guide](./getting-started/index.md)
- [Spring AI Documentation](https://spring.io/projects/spring-ai)
