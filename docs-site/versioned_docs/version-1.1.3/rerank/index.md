---
sidebar_position: 3
---

# Rerank Models

Watsonx.ai Rerank Models provide document reranking capabilities that improve the relevance of search results by scoring and reordering documents based on their semantic relevance to a query. This is particularly useful in Retrieval-Augmented Generation (RAG) pipelines to ensure the most relevant documents are prioritized.

## Overview

Document reranking is a critical step in modern search and RAG systems. While initial retrieval (e.g., vector similarity search) efficiently retrieves candidate documents, reranking uses more sophisticated models to precisely score each document's relevance to the query.

The watsonx.ai rerank integration provides:

- **Standalone Reranking**: Direct API access for scoring and reordering documents
- **RAG Integration**: `DocumentPostProcessor` implementation for seamless integration with Spring AI's RAG pipeline

## Supported Models

The following rerank models are supported:

- **cross-encoder/ms-marco-minilm-l-12-v2** - A cross-encoder model trained on MS MARCO passage ranking dataset (default)
- Other reranking models available in your Watsonx.ai instance

## Configuration Properties

### Rerank Properties

The prefix `spring.ai.watsonx.ai.rerank` is used as the property prefix for configuring the Watsonx.ai rerank model.

| Property                                                    | Description                                     | Default                               |
| :---------------------------------------------------------- | :---------------------------------------------- | :------------------------------------ |
| `spring.ai.watsonx.ai.rerank.enabled`                       | Enable or disable the rerank auto-configuration | true                                  |
| `spring.ai.watsonx.ai.rerank.rerank-endpoint`               | The rerank API endpoint                         | /ml/v1/text/rerank                    |
| `spring.ai.watsonx.ai.rerank.version`                       | API version date in YYYY-MM-DD format           | 2024-05-31                            |
| `spring.ai.watsonx.ai.rerank.options.model`                 | ID of the model to use for reranking            | cross-encoder/ms-marco-minilm-l-12-v2 |
| `spring.ai.watsonx.ai.rerank.options.top-n`                 | Limit results to top N documents                | -                                     |
| `spring.ai.watsonx.ai.rerank.options.truncate-input-tokens` | Maximum tokens before truncation                | 512                                   |
| `spring.ai.watsonx.ai.rerank.options.return-inputs`         | Include original text in response               | false                                 |
| `spring.ai.watsonx.ai.rerank.options.return-query`          | Include query in response                       | false                                 |

## Runtime Options

The `WatsonxAiRerankOptions.java` class provides options for configuring rerank requests.

On start-up, the options specified by `spring.ai.watsonx.ai.rerank.options` are used, but you can override these at runtime.

```java
WatsonxAiRerankOptions options = WatsonxAiRerankOptions.builder()
    .model("cross-encoder/ms-marco-minilm-l-12-v2")
    .topN(5)
    .truncateInputTokens(512)
    .returnInputs(true)
    .build();

List<RerankResult> results = rerankModel.rerank(query, documents, options);
```

## WatsonxAiRerankOptions

The `WatsonxAiRerankOptions` class provides various options for configuring rerank requests:

| Option                | Default                                 | Description                                                |
| :-------------------- | :-------------------------------------- | :--------------------------------------------------------- |
| `model`               | `cross-encoder/ms-marco-minilm-l-12-v2` | The rerank model to use                                    |
| `topN`                | `null` (return all)                     | Limit results to the top N highest-scoring documents       |
| `truncateInputTokens` | `512`                                   | Maximum number of tokens per input before truncation       |
| `returnInputs`        | `false`                                 | Whether to include the original input text in the response |
| `returnQuery`         | `false`                                 | Whether to include the query in the response               |

## Standalone Reranking

The `WatsonxAiRerankModel` provides direct access to the reranking API:

```java
@RestController
public class RerankController {

    private final WatsonxAiRerankModel rerankModel;

    public RerankController(WatsonxAiRerankModel rerankModel) {
        this.rerankModel = rerankModel;
    }

    @PostMapping("/ai/rerank")
    public List<RerankResult> rerank(
            @RequestParam String query,
            @RequestBody List<String> documents) {

        return rerankModel.rerank(query, documents);
    }
}
```

### Example Response

The rerank API returns results sorted by relevance score (descending):

```java
List<String> documents = List.of(
    "Machine learning is a subset of artificial intelligence.",
    "Cooking Italian pasta requires fresh ingredients.",
    "Deep learning uses neural networks with many layers."
);

List<RerankResult> results = rerankModel.rerank(
    "What is machine learning?",
    documents
);

// Results are sorted by score (highest first)
for (RerankResult result : results) {
    System.out.println("Index: " + result.index() +
                       ", Score: " + result.score());
}
// Output:
// Index: 0, Score: 0.95  (Machine learning document)
// Index: 2, Score: 0.82  (Deep learning document)
// Index: 1, Score: 0.12  (Cooking document)
```

## RAG Integration with DocumentPostProcessor

The `WatsonxAiDocumentReranker` implements Spring AI's `DocumentPostProcessor` interface, enabling seamless integration with RAG pipelines.

### Basic RAG Integration

```java
@Configuration
public class RagConfiguration {

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStore vectorStore,
            WatsonxAiDocumentReranker documentReranker) {

        return RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(20)  // Retrieve more candidates for reranking
                .build())
            .documentPostProcessors(documentReranker)  // Rerank the results
            .build();
    }
}
```

### Using with ChatClient

```java
@Service
public class RagService {

    private final ChatClient chatClient;

    public RagService(
            ChatModel chatModel,
            VectorStore vectorStore,
            WatsonxAiDocumentReranker documentReranker) {

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(20)
                .build())
            .documentPostProcessors(documentReranker)
            .build();

        this.chatClient = ChatClient.builder(chatModel)
            .defaultAdvisors(advisor)
            .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

### Custom Reranker Configuration

You can create a reranker with custom options:

```java
@Bean
public WatsonxAiDocumentReranker customDocumentReranker(
        WatsonxAiRerankModel rerankModel) {

    WatsonxAiRerankOptions options = WatsonxAiRerankOptions.builder()
        .topN(5)  // Return only top 5 documents
        .truncateInputTokens(1024)
        .build();

    return new WatsonxAiDocumentReranker(rerankModel, options);
}
```

### Accessing Rerank Scores

When documents are reranked, the rerank score is stored in the document's metadata:

```java
// After reranking, documents have the score in metadata
List<Document> rerankedDocs = documentReranker.process(query, documents);

for (Document doc : rerankedDocs) {
    Double rerankScore = (Double) doc.getMetadata()
        .get(WatsonxAiDocumentReranker.RERANK_SCORE_METADATA_KEY);

    System.out.println("Document: " + doc.getId() +
                       ", Rerank Score: " + rerankScore);
}
```

For RAG integration, also add the Spring AI RAG dependency:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-rag</artifactId>
</dependency>
```
