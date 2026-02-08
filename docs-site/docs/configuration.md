---
sidebar_position: 2
---

# Configuration

This section covers the configuration options for Spring AI Watsonx.ai integration, including connection properties, authentication, and advanced configuration scenarios.

## Connection Properties

The Spring AI Watsonx.ai integration requires several connection properties to authenticate and connect to your Watsonx.ai service instance.

### Basic Configuration

Add these properties to your `application.properties` or `application.yml` file:

**application.properties**

```properties
# Required connection properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.url=${WATSONX_AI_URL}
spring.ai.watsonx.ai.project-id=${WATSONX_AI_PROJECT_ID}

# Optional: Enable specific models
spring.ai.watsonx.ai.chat.enabled=true
spring.ai.watsonx.ai.embedding.enabled=true
spring.ai.watsonx.ai.text-extraction.enabled=true
```

**application.yml**

```yaml
spring:
  ai:
    watsonx:
      ai:
        api-key: ${WATSONX_AI_API_KEY}
        url: ${WATSONX_AI_URL}
        project-id: ${WATSONX_AI_PROJECT_ID}
        chat:
          enabled: true
          options:
            model: ibm/granite-13b-chat-v2
            temperature: 0.7
        embedding:
          enabled: true
          options:
            model: ibm/slate-125m-english-rtrvr
        text-extraction:
          enabled: true
```

### Environment Variables

It's recommended to use environment variables for sensitive configuration:

```bash
export WATSONX_AI_API_KEY=your_api_key_here
export WATSONX_AI_URL=https://us-south.ml.cloud.ibm.com
export WATSONX_AI_PROJECT_ID=your_project_id_here
```

## Authentication

Watsonx.ai uses IBM Cloud IAM (Identity and Access Management) for authentication. The integration supports several authentication methods:

### API Key Authentication (Recommended)

The simplest method is using an API key:

```properties
spring.ai.watsonx.ai.api-key=your_api_key_here
```

### Service Credentials

You can also configure using service credentials from IBM Cloud:

```yaml
spring:
  ai:
    watsonx:
      ai:
        credentials:
          apikey: your_api_key_here
          url: https://us-south.ml.cloud.ibm.com
```

## Chat Model Configuration

Configure chat model specific properties:

| Property                                               | Default                   | Description                                   |
| :----------------------------------------------------- | :------------------------ | :-------------------------------------------- |
| `spring.ai.watsonx.ai.chat.options.model`              | `ibm/granite-13b-chat-v2` | The foundation model to use                   |
| `spring.ai.watsonx.ai.chat.options.temperature`        | `0.7`                     | Sampling temperature (0.0 to 2.0)             |
| `spring.ai.watsonx.ai.chat.options.max-new-tokens`     | `1024`                    | Maximum number of tokens to generate          |
| `spring.ai.watsonx.ai.chat.options.top-p`              | `1.0`                     | Nucleus sampling parameter                    |
| `spring.ai.watsonx.ai.chat.options.top-k`              | `50`                      | Top-K sampling parameter                      |
| `spring.ai.watsonx.ai.chat.options.repetition-penalty` | `1.0`                     | Repetition penalty (> 1.0 reduces repetition) |
| `spring.ai.watsonx.ai.chat.options.stop-sequences`     |                           | List of sequences that stop generation        |
| `spring.ai.watsonx.ai.chat.options.presence-penalty`   | `0.0`                     | Presence penalty (-2.0 to 2.0)                |
| `spring.ai.watsonx.ai.chat.options.frequency-penalty`  | `0.0`                     | Frequency penalty (-2.0 to 2.0)               |

Example configuration:

```yaml
spring:
  ai:
    watsonx:
      ai:
        chat:
          options:
            model: meta-llama/llama-3-70b-instruct
            temperature: 0.3
            max-new-tokens: 2048
            top-p: 0.9
            repetition-penalty: 1.1
            stop-sequences:
              - "Human:"
              - "AI:"
```

## Embedding Model Configuration

Configure embedding model specific properties:

| Property                                                       | Default                        | Description                               |
| :------------------------------------------------------------- | :----------------------------- | :---------------------------------------- |
| `spring.ai.watsonx.ai.embedding.options.model`                 | `ibm/slate-125m-english-rtrvr` | The embedding model to use                |
| `spring.ai.watsonx.ai.embedding.options.truncate-input-tokens` | `null`                         | Truncate input if it exceeds model limits |

Example configuration:

```yaml
spring:
  ai:
    watsonx:
      ai:
        embedding:
          options:
            model: ibm/slate-30m-english-rtrvr
            truncate-input-tokens: true
```

## Advanced Configuration

### Custom HTTP Client Configuration

You can customize the HTTP client used for API calls:

```java
@Configuration
public class WatsonxAiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebClient watsonxAiWebClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
}
```

### Retry Configuration

Configure retry behavior for failed requests:

```yaml
spring:
  ai:
    watsonx:
      ai:
        retry:
          max-attempts: 3
          backoff:
            delay: 1000ms
            multiplier: 2.0
            max-delay: 10000ms
```

### Connection Pool Configuration

Configure connection pooling for better performance:

```yaml
spring:
  ai:
    watsonx:
      ai:
        connection:
          pool:
            max-connections: 100
            max-connections-per-route: 20
            connection-timeout: 30000ms
            read-timeout: 60000ms
```

## Multiple Model Configurations

You can configure multiple models for different use cases:

```java
@Configuration
public class MultiModelConfiguration {

    @Bean
    @Primary
    public WatsonxAiChatModel defaultChatModel(WatsonxAiChatApi chatApi) {
        return new WatsonxAiChatModel(chatApi,
            WatsonxAiChatOptions.builder()
                .withModel("ibm/granite-13b-chat-v2")
                .withTemperature(0.7)
                .build());
    }

    @Bean("creativeChatModel")
    public WatsonxAiChatModel creativeChatModel(WatsonxAiChatApi chatApi) {
        return new WatsonxAiChatModel(chatApi,
            WatsonxAiChatOptions.builder()
                .withModel("meta-llama/llama-3-70b-instruct")
                .withTemperature(1.2)
                .withMaxNewTokens(2048)
                .build());
    }

    @Bean("analyticalChatModel")
    public WatsonxAiChatModel analyticalChatModel(WatsonxAiChatApi chatApi) {
        return new WatsonxAiChatModel(chatApi,
            WatsonxAiChatOptions.builder()
                .withModel("ibm/granite-20b-code-instruct")
                .withTemperature(0.1)
                .build());
    }
}
```

Usage:

```java
@Service
public class MultiModelService {

    private final WatsonxAiChatModel defaultChatModel;
    private final WatsonxAiChatModel creativeChatModel;
    private final WatsonxAiChatModel analyticalChatModel;

    public MultiModelService(
            WatsonxAiChatModel defaultChatModel,
            @Qualifier("creativeChatModel") WatsonxAiChatModel creativeChatModel,
            @Qualifier("analyticalChatModel") WatsonxAiChatModel analyticalChatModel) {
        this.defaultChatModel = defaultChatModel;
        this.creativeChatModel = creativeChatModel;
        this.analyticalChatModel = analyticalChatModel;
    }

    public String generateCreativeContent(String prompt) {
        return creativeChatModel.call(prompt);
    }

    public String analyzeCode(String code) {
        return analyticalChatModel.call("Analyze this code: " + code);
    }
}
```

## Profile-Specific Configuration

Configure different settings for different environments:

**application-dev.yml**

```yaml
spring:
  ai:
    watsonx:
      ai:
        chat:
          options:
            temperature: 1.0 # More creative for development
            max-new-tokens: 512 # Shorter responses for testing
```

**application-prod.yml**

```yaml
spring:
  ai:
    watsonx:
      ai:
        chat:
          options:
            temperature: 0.3 # More deterministic for production
            max-new-tokens: 2048 # Longer responses for production use
        retry:
          max-attempts: 5 # More retries in production
```

## Configuration Validation

Spring Boot will validate your configuration on startup. Common configuration errors include:

- Missing required properties (api-key, url, project-id)
- Invalid model names
- Out-of-range parameter values
- Network connectivity issues

Enable debug logging to troubleshoot configuration issues:

```yaml
logging:
  level:
    org.springaicommunity.watsonx: DEBUG
    org.springframework.ai: DEBUG
```

## Security Considerations

1. **Never hardcode API keys** in your source code
2. **Use environment variables** or secure configuration management
3. **Rotate API keys regularly**
4. **Limit project permissions** to only what's needed
5. **Monitor API usage** for unusual patterns

Example secure configuration:

```yaml
spring:
  ai:
    watsonx:
      ai:
        api-key: ${WATSONX_AI_API_KEY:#{null}}
        url: ${WATSONX_AI_URL:#{null}}
        project-id: ${WATSONX_AI_PROJECT_ID:#{null}}

# Fail startup if required properties are missing
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    readiness-state:
      enabled: true
    liveness-state:
      enabled: true
```
