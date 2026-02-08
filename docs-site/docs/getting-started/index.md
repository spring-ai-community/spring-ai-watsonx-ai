---
sidebar_position: 2
---

# Getting Started

This guide will help you get started with Spring AI Watsonx.ai integration in your Spring Boot application.

## Prerequisites

Before you begin, ensure you have:

- JDK 17 or later
- Maven 3.9+ or Gradle 7.5+
- An IBM Cloud account with Watsonx.ai access
- Watsonx.ai API credentials

## Create IBM Cloud Account

1. Visit [IBM Cloud](https://cloud.ibm.com)
2. Sign up for a free account or log in
3. Navigate to the Watsonx.ai service
4. Create a new Watsonx.ai instance

## Obtain API Credentials

1. Go to your Watsonx.ai service instance
2. Navigate to **Service Credentials**
3. Create new credentials or use existing ones
4. Note down:
   - API Key
   - Service URL
   - Project ID

## Add Dependencies

Add the Spring AI Watsonx.ai starter to your project:

:::tip
Check [Maven Central](https://central.sonatype.com/search?namespace=org.springaicommunity&name=spring-ai-starter-model-watsonx-ai) for the latest version.
:::

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>1.X.X</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:1.X.X'
```

## Configure Application

Add your Watsonx.ai credentials to `application.properties`:

```properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.url=${WATSONX_AI_URL}
spring.ai.watsonx.ai.project-id=${WATSONX_AI_PROJECT_ID}
```

Or use `application.yml`:

```yaml
spring:
  ai:
    watsonx:
      ai:
        api-key: ${WATSONX_AI_API_KEY}
        url: ${WATSONX_AI_URL}
        project-id: ${WATSONX_AI_PROJECT_ID}
```

## Set Environment Variables

Set your credentials as environment variables:

```bash
export WATSONX_AI_API_KEY=your_api_key_here
export WATSONX_AI_URL=https://us-south.ml.cloud.ibm.com
export WATSONX_AI_PROJECT_ID=your_project_id_here
```

## Create Your First Chat Application

Create a simple Spring Boot application:

```java
package com.example.watsonxdemo;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class WatsonxDemoApplication {

    private final ChatModel chatModel;

    public WatsonxDemoApplication(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "Hello!") String message) {
        return chatModel.call(message);
    }

    public static void main(String[] args) {
        SpringApplication.run(WatsonxDemoApplication.class, args);
    }
}
```

## Run Your Application

Start your application:

```bash
mvn spring-boot:run
```

Or with Gradle:

```bash
gradle bootRun
```

## Test Your Application

Test the chat endpoint:

```bash
curl "http://localhost:8080/chat?message=What is Spring AI?"
```

You should receive a response from the Watsonx.ai model!

## Next Steps

Now that you have a basic application running, explore more features:

- [Chat Models](../chat/index.md) - Learn about chat model capabilities
- [Embeddings](../embeddings/index.md) - Generate text embeddings
- [Configuration](../configuration.md) - Advanced configuration options
- [Samples](../samples.md) - Explore sample applications

## Common Issues

### Authentication Errors

If you see authentication errors:

1. Verify your API key is correct
2. Check that the URL matches your region
3. Ensure the project ID is valid
4. Confirm your IBM Cloud account has Watsonx.ai access

### Connection Timeouts

If you experience timeouts:

1. Check your network connection
2. Verify the Watsonx.ai service is available
3. Increase timeout settings in configuration

### Dependency Conflicts

If you have dependency conflicts:

1. Check Spring Boot version compatibility
2. Exclude conflicting transitive dependencies
3. Use dependency management to align versions

## Additional Resources

- [Documentation Home](../index.md)
- [Configuration Guide](../configuration.md)
- [Spring AI Documentation](https://spring.io/projects/spring-ai)
- [IBM Watsonx.ai Documentation](https://cloud.ibm.com/apidocs/watsonx-ai)
- [GitHub Repository](https://github.com/spring-ai-community/spring-ai-watsonx-ai)
