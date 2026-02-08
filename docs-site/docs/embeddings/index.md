---
sidebar_position: 2
---

# Embedding Models

Watsonx.ai Embedding Models provide powerful text embedding capabilities for semantic search, similarity analysis, and other NLP tasks. The Spring AI Watsonx.ai integration supports various embedding models available in IBM's Watsonx.ai platform.

## Supported Models

The following embedding models are supported:

- **IBM embedding models** - IBM's enterprise-focused embedding models
- **Sentence transformers** - Popular open-source sentence embedding models
- **Custom embedding models** deployed in your Watsonx.ai instance

## Auto-configuration

Spring AI provides Spring Boot auto-configuration for the Watsonx.ai Embedding Model. To enable it, add the following dependency to your project's Maven `pom.xml` file:

:::tip
Check [Maven Central](https://central.sonatype.com/search?namespace=org.springaicommunity&name=spring-ai-starter-model-watsonx-ai) for the latest version.
:::

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>1.X.X</version>
</dependency>
```

Or to your Gradle `build.gradle` build file:

```groovy
dependencies {
    implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:1.X.X'
}
```

## Configuration Properties

The prefix `spring.ai.watsonx.ai.embedding` is used as the property prefix that lets you configure the connection to Watsonx.ai.

| Property                                                       | Default                        | Required | Description                                                   |
| :------------------------------------------------------------- | :----------------------------- | :------- | :------------------------------------------------------------ |
| `spring.ai.watsonx.ai.api-key`                                 |                                | true     | Your Watsonx.ai API key                                       |
| `spring.ai.watsonx.ai.url`                                     |                                | true     | Your Watsonx.ai service URL                                   |
| `spring.ai.watsonx.ai.project-id`                              |                                | true     | Your Watsonx.ai project ID                                    |
| `spring.ai.watsonx.ai.embedding.options.model`                 | `ibm/slate-125m-english-rtrvr` | false    | The embedding model to use                                    |
| `spring.ai.watsonx.ai.embedding.options.truncate-input-tokens` | `null`                         | false    | Whether to truncate input that exceeds model's context length |

:::tip
All properties prefixed with `spring.ai.watsonx.ai.embedding.options` can be overridden at runtime by adding a request specific [runtime options](#runtime-options) to the `EmbeddingRequest`.
:::

## Runtime Options

The `WatsonxAiEmbeddingOptions.java` provides model configurations, such as the model to use and other parameters.

On start-up, the default options can be configured with the `WatsonxAiEmbeddingModel(api, options)` constructor or the `spring.ai.watsonx.ai.embedding.options.*` properties.

At runtime you can override the default options by adding new ones, using the `WatsonxAiEmbeddingOptions.Builder`, to an `EmbeddingRequest`. For example to override the default model for a specific request:

```java
EmbeddingResponse embeddingResponse = embeddingModel.call(
    new EmbeddingRequest(List.of("Hello World", "World is big and salvation is near"),
        WatsonxAiEmbeddingOptions.builder()
            .withModel("ibm/slate-30m-english-rtrvr")
            .build()));
```

## Sample Controller

```java
@RestController
public class EmbeddingController {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public EmbeddingController(WatsonxAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/ai/embedding")
    public Map embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
        return Map.of("embedding", embeddingResponse);
    }
}
```

## Manual Configuration

The `WatsonxAiEmbeddingModel` implements the `EmbeddingModel` and uses the [Low-level WatsonxAiEmbeddingApi](#low-level-watsonxaiembeddingapi) to connect to the Watsonx.ai service.

Add the `spring-ai-watsonx-ai-core` dependency to your project's Maven `pom.xml` file:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>watsonx-ai-core</artifactId>
    <version>1.X.X</version>
</dependency>
```

:::tip
Refer to the [Getting Started](../getting-started/index.md) guide for information about adding dependencies to your build file.
:::

Next, create a `WatsonxAiEmbeddingModel` and use it to compute embeddings:

```java
var watsonxAiApi = new WatsonxAiEmbeddingApi(apiKey, url, projectId);

var embeddingModel = new WatsonxAiEmbeddingModel(watsonxAiApi,
    WatsonxAiEmbeddingOptions.builder()
        .withModel("ibm/slate-125m-english-rtrvr")
        .build());

EmbeddingResponse embeddingResponse = embeddingModel
    .embedForResponse(List.of("Hello World", "World is big and salvation is near"));
```

The `WatsonxAiEmbeddingOptions` provides the configuration information for the embedding requests. The `WatsonxAiEmbeddingOptions.Builder` is fluent options builder.

## Low-level WatsonxAiEmbeddingApi

The `WatsonxAiEmbeddingApi` provides is a lightweight Java client on top of Watsonx.ai [Embeddings API](https://cloud.ibm.com/apidocs/watsonx-ai).

Here is a simple snippet showing how to use the api programmatically:

```java
WatsonxAiEmbeddingApi watsonxAiApi =
    new WatsonxAiEmbeddingApi(apiKey, url, projectId);

WatsonxAiEmbeddingRequest request = WatsonxAiEmbeddingRequest.builder()
    .withModel("ibm/slate-125m-english-rtrvr")
    .withInput(List.of("Hello World", "World is big and salvation is near"))
    .build();

ResponseEntity<WatsonxAiEmbeddingResponse> response = watsonxAiApi.embeddings(request);
```

Follow the `WatsonxAiEmbeddingApi.java`'s JavaDoc for further information.

## WatsonxAiEmbeddingOptions

The `WatsonxAiEmbeddingOptions` class provides various options for configuring embedding requests:

| Option                | Default                        | Description                                                               |
| :-------------------- | :----------------------------- | :------------------------------------------------------------------------ |
| `model`               | `ibm/slate-125m-english-rtrvr` | The embedding model to use                                                |
| `truncateInputTokens` | `null`                         | Whether to truncate input that exceeds the model's maximum context length |

## Use Cases

Embeddings are useful for various NLP tasks:

### Semantic Search

```java
@Service
public class SemanticSearchService {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public SemanticSearchService(WatsonxAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Document> findSimilarDocuments(String query, List<Document> documents) {
        // Generate embedding for the query
        List<Double> queryEmbedding = embeddingModel.embed(query);

        // Calculate similarity scores and return most similar documents
        return documents.stream()
            .map(doc -> {
                List<Double> docEmbedding = embeddingModel.embed(doc.getContent());
                double similarity = calculateCosineSimilarity(queryEmbedding, docEmbedding);
                return new ScoredDocument(doc, similarity);
            })
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .map(ScoredDocument::getDocument)
            .limit(10)
            .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(List<Double> a, List<Double> b) {
        // Implementation of cosine similarity calculation
        // ...
    }
}
```

### Text Classification

```java
@Service
public class TextClassificationService {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public TextClassificationService(WatsonxAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String classifyText(String text, Map<String, List<String>> categories) {
        List<Double> textEmbedding = embeddingModel.embed(text);

        return categories.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Double> categoryEmbedding = embeddingModel.embed(
                        String.join(" ", entry.getValue()));
                    return calculateCosineSimilarity(textEmbedding, categoryEmbedding);
                }))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }
}
```

### Clustering

```java
@Service
public class TextClusteringService {

    private final WatsonxAiEmbeddingModel embeddingModel;

    public TextClusteringService(WatsonxAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Map<Integer, List<String>> clusterTexts(List<String> texts, int numClusters) {
        // Generate embeddings for all texts
        List<List<Double>> embeddings = texts.stream()
            .map(embeddingModel::embed)
            .collect(Collectors.toList());

        // Apply clustering algorithm (e.g., K-means)
        // Implementation depends on your clustering library
        // ...

        return clusteredResults;
    }
}
```
