---
sidebar_position: 6
---

# API Reference

This section lists the main classes of the Spring AI Watsonx.ai integration. Each model has a high-level class that implements a Spring AI interface, and a low-level `...Api` class that is a thin client for the watsonx.ai REST API. Follow the JavaDoc for the full signatures.

## Models and APIs

| Capability       | Spring AI model class            | Low-level API class              | Guide                                    |
| :--------------- | :------------------------------- | :------------------------------- | :--------------------------------------- |
| Chat             | `WatsonxAiChatModel`             | `WatsonxAiChatApi`               | [Chat Models](../chat/index.md)          |
| Embeddings       | `WatsonxAiEmbeddingModel`        | `WatsonxAiEmbeddingApi`          | [Embeddings](../embeddings/index.md)     |
| Moderation       | `WatsonxAiModerationModel`       | `WatsonxAiModerationApi`         | [Moderation](../moderation/index.md)     |
| Rerank           | `WatsonxAiRerankModel`, `WatsonxAiDocumentReranker` | `WatsonxAiRerankApi` | [Rerank](../rerank/index.md)             |
| Text extraction  | `WatsonxAiTextExtractionModel`, `WatsonxAiDocumentReader` | `WatsonxAiTextExtractionApi` | [Text Extraction](../text-extraction/index.md) |

All classes are in the `watsonx-ai-core` module, in `org.springaicommunity.watsonx` and its sub-packages.

## Chat API

### WatsonxAiChatModel

Implements Spring AI's `ChatModel` and `StreamingChatModel`. Create it with its builder:

```java
WatsonxAiChatModel chatModel = WatsonxAiChatModel.builder()
    .watsonxAiChatApi(chatApi)
    .options(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").build())
    .observationRegistry(observationRegistry)   // optional, default NOOP
    .retryTemplate(retryTemplate)               // optional
    .build();
```

**Key Methods:**

- `call(Prompt)` - Synchronous chat completion
- `stream(Prompt)` - Streaming chat completion, as a `Flux<ChatResponse>`
- `getDefaultOptions()` - The default `WatsonxAiChatOptions`
- `setObservationConvention(...)` - Replace the default [observation convention](../observability/index.md)

### WatsonxAiChatOptions

Implements Spring AI's `ToolCallingChatOptions`. Every option is listed in [Chat Models](../chat/index.md#watsonxaichatoptions).

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .model("ibm/granite-4-h-small")
    .temperature(0.7)
    .maxCompletionTokens(1024)
    .build();
```

### WatsonxAiChatApi

```java
var chatApi = new WatsonxAiChatApi(baseUrl, textEndpoint, streamEndpoint, version,
    projectId, spaceId, apiKey, restClientBuilder, webClientBuilder, responseErrorHandler);

ResponseEntity<WatsonxAiChatResponse> response = chatApi.chat(request);
Flux<WatsonxAiChatStream> stream = chatApi.stream(request);

// Through a deployment (since 2.1.0)
chatApi.deploymentChat("my-deployment", request);
chatApi.deploymentStream("my-deployment", request);
```

The request and response types are `WatsonxAiChatRequest` (built with `WatsonxAiChatRequest.builder()`), `WatsonxAiChatResponse` and `WatsonxAiChatStream`. Messages are `TextChatMessage` objects.

## Embedding API

### WatsonxAiEmbeddingModel

Implements Spring AI's `EmbeddingModel`:

```java
var embeddingModel = new WatsonxAiEmbeddingModel(embeddingApi, options,
    observationRegistry, retryTemplate);

EmbeddingResponse response = embeddingModel.call(
    new EmbeddingRequest(List.of("Hello World"), null));
float[] vector = embeddingModel.embed("Hello World");
```

### WatsonxAiEmbeddingOptions

| Option       | Description                                                           |
| :----------- | :-------------------------------------------------------------------- |
| `model`      | The embedding model                                                    |
| `parameters` | `EmbeddingParameters`, such as `truncateInputTokens` and return options |

watsonx.ai has no `dimensions` or `encodingFormat` parameter: those getters return `null` and log a warning.

### WatsonxAiEmbeddingApi

`embed(WatsonxAiEmbeddingRequest)` returns a `ResponseEntity<WatsonxAiEmbeddingResponse>`.

## Moderation API

### WatsonxAiModerationModel

Implements Spring AI's `ModerationModel`:

```java
var moderationModel = new WatsonxAiModerationModel(moderationApi, retryTemplate)
    .withDefaultOptions(WatsonxAiModerationOptions.builder().hap(0.75f).build());

ModerationResponse response = moderationModel.call(new ModerationPrompt("text to check"));
```

`WatsonxAiModerationOptions` selects the detectors, each with a threshold: `hap`, `pii` and `graniteGuardian`. See [Moderation](../moderation/index.md). `WatsonxAiModerationApi#moderate(WatsonxAiModerationRequest)` is the low-level call.

## Rerank API

### WatsonxAiRerankModel

```java
var rerankModel = new WatsonxAiRerankModel(rerankApi, options, observationRegistry, retryTemplate);

List<WatsonxAiRerankResponse.RerankResult> results = rerankModel.rerank("query", documents);
```

### WatsonxAiDocumentReranker

A Spring AI `DocumentPostProcessor` for RAG pipelines:

```java
var reranker = new WatsonxAiDocumentReranker(rerankModel);
List<Document> ranked = reranker.process(new Query("query"), documents);
```

`WatsonxAiRerankOptions` has `model`, `topN`, `truncateInputTokens`, `returnInputs` and `returnQuery`. `WatsonxAiRerankApi#rerank(WatsonxAiRerankRequest)` is the low-level call.

## Text Extraction API

### WatsonxAiTextExtractionModel

```java
WatsonxAiTextExtractionResponse response = textExtractionModel.extract(resource);
String text = response.getText();

List<Document> documents = textExtractionModel.extractToDocuments(resource);
```

`extract(...)` is overloaded for a `Resource`, a `byte[]` (with a file name) and a `WatsonxAiTextExtractionRequest`, each with optional `WatsonxAiTextExtractionOptions`. `setPollTimeout(Duration)` and `setDeleteFiles(boolean)` change how it waits for a job and cleans up.

### WatsonxAiDocumentReader

Implements Spring AI's `DocumentReader`:

```java
List<Document> documents = new WatsonxAiDocumentReader(textExtractionModel, resource).read();
```

### WatsonxAiTextExtractionApi

| Method                         | Purpose                                                     |
| :----------------------------- | :---------------------------------------------------------- |
| `extract(request)`             | Run the whole flow and return the result                    |
| `createExtraction(request)`    | Start an extraction job                                     |
| `getExtraction(id)`            | Follow a job                                                |
| `deleteExtraction(id)`         | Delete a job                                                |
| `uploadFile(key, resource)`    | Upload a file to the project or space storage               |
| `downloadFile(key)`            | Download a result                                           |
| `deleteFile(key)`              | Delete a file from the storage                              |
| `getStorage()`                 | The bucket and endpoint of the project or space storage     |

See [Text Extraction](../text-extraction/index.md) for the options and requests.

## Authentication

### WatsonxAiAuthentication

Gets IBM Cloud IAM access tokens for an API key. Every `...Api` class creates one from the `apiKey` you pass. The token is cached and refreshed, and concurrent callers share one token request.

```java
public class WatsonxAiAuthentication {

    public WatsonxAiAuthentication(String apiKey);

    public String getAccessToken();
}
```

## Configuration Properties

All prefixes are under `spring.ai.watsonx.ai`.

| Prefix                                     | Class                                  | Reference                                       |
| :----------------------------------------- | :------------------------------------- | :---------------------------------------------- |
| `spring.ai.watsonx.ai`                     | `WatsonxAiConnectionProperties`        | [Configuration](../configuration.md)            |
| `spring.ai.watsonx.ai.chat`                | `WatsonxAiChatProperties`              | [Chat Models](../chat/index.md#configuration-properties) |
| `spring.ai.watsonx.ai.embedding`           | `WatsonxAiEmbeddingProperties`         | [Embeddings](../embeddings/index.md)            |
| `spring.ai.watsonx.ai.moderation`          | `WatsonxAiModerationProperties`        | [Moderation](../moderation/index.md)            |
| `spring.ai.watsonx.ai.rerank`              | `WatsonxAiRerankProperties`            | [Rerank](../rerank/index.md)                    |
| `spring.ai.watsonx.ai.text-extraction`     | `WatsonxAiTextExtractionProperties`    | [Text Extraction](../text-extraction/index.md#configuration-properties) |

## Auto-Configuration

Each capability has an auto-configuration class in `spring-ai-autoconfigure-model-watsonx-ai`: `WatsonxAiChatAutoConfiguration`, `WatsonxAiEmbeddingAutoConfiguration`, `WatsonxAiModerationAutoConfiguration`, `WatsonxAiRerankAutoConfiguration` and `WatsonxAiTextExtractionAutoConfiguration`. Each one creates its `...Api` and model beans with `@ConditionalOnMissingBean`, so you can replace any of them with your own bean.

Every connection check runs at startup: the application fails to start unless exactly one of `project-id` and `space-id` is set.

## Native Image

`WatsonxAiRuntimeHints` registers the reflection and serialization hints that GraalVM native images need for the watsonx.ai request and response types, and for the IBM Cloud SDK's IAM token. The library registers it through `META-INF/spring/aot.factories`, so Spring AOT picks it up without `@ImportRuntimeHints`.

### Registered hints

The hints register all member categories for these types:

| Types | Why |
|-------|-----|
| JSON-annotated classes in the chat, embedding, moderation and text extraction packages | Jackson reads and writes them by reflection |
| `WatsonxAiRerankRequest`, `WatsonxAiRerankResponse`, `WatsonxAiRerankOptions` and their inner classes | Jackson reads and writes them by reflection |
| `com.ibm.cloud.sdk.core.security.IamToken` | The IBM Cloud SDK reads IAM token responses with Gson and ships no reflection metadata. Without this hint the token's fields stay `null`, and requests are sent with `Bearer null` |

The rerank package is registered type by type instead of scanned. Scanning loads every class in a package, and `WatsonxAiDocumentReranker` needs the optional `spring-ai-rag` dependency.

### Limitations

- The hints cover the types in this library. Your own classes that Jackson or Gson reads by reflection need your own hints.
- `WatsonxAiDocumentReranker` needs `spring-ai-rag` on the classpath. It is an optional dependency of the library, so add it yourself if you use the reranker in a native image.
- Native images need 2.1.0 or later. Executables built with 2.0.0 send chat requests as `{}` and authenticate with `Bearer null`. See the [upgrade notes](../upgrade-notes.md).

## See Also

- [Chat Models Guide](../chat/index.md)
- [Embeddings Guide](../embeddings/index.md)
- [Moderation Guide](../moderation/index.md)
- [Rerank Guide](../rerank/index.md)
- [Text Extraction Guide](../text-extraction/index.md)
- [Observability](../observability/index.md)
- [Configuration Reference](../configuration.md)
- [JavaDoc](https://javadoc.io/doc/org.springaicommunity/spring-ai-watsonx-ai)
