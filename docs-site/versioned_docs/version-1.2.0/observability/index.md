---
sidebar_position: 7
---

# Observability

Spring AI Watsonx.ai records [Micrometer observations](https://docs.micrometer.io/micrometer/reference/observation.html) for chat, embedding, rerank and text extraction calls. With Spring Boot Actuator and a metrics or tracing backend on the classpath, they appear as metrics and spans without extra code. The models take an `ObservationRegistry`, and Spring Boot auto-configuration supplies it.

## Observations

| Model            | Observation                                    | Convention                                          |
| :--------------- | :--------------------------------------------- | :-------------------------------------------------- |
| Chat             | Spring AI `ChatModelObservationDocumentation`  | `DefaultChatModelObservationConvention`             |
| Embeddings       | Spring AI `EmbeddingModelObservationDocumentation` | Spring AI default convention                    |
| Rerank           | `RerankModelObservationDocumentation`          | `DefaultRerankModelObservationConvention`           |
| Text extraction  | `TextExtractionModelObservationDocumentation`  | `DefaultTextExtractionModelObservationConvention`   |

Chat and embedding observations use the standard Spring AI key names. See the Spring AI observability documentation for those.

### Rerank

Low cardinality: `gen_ai.operation.name`, `gen_ai.system`, `gen_ai.request.model`, `gen_ai.response.model`.

High cardinality: `gen_ai.rerank.document_count`, `gen_ai.rerank.top_n`, `gen_ai.usage.input_tokens`.

### Text extraction

The observation name is `gen_ai.client.operation`.

Low cardinality: `gen_ai.operation.name`, `gen_ai.system`, `gen_ai.request.model`, and `gen_ai.text_extraction.status`, the extraction job status or `unknown`.

High cardinality: `gen_ai.text_extraction.document_name`, `gen_ai.text_extraction.page_count`.

## Custom conventions

With auto-configuration, declare a single bean of the convention type and it is applied to the model: `ChatModelObservationConvention`, `EmbeddingModelObservationConvention`, `RerankModelObservationConvention` or `TextExtractionModelObservationConvention`.

```java
@Bean
RerankModelObservationConvention rerankObservationConvention() {
    return new MyRerankObservationConvention();
}
```

Without auto-configuration, call the model's `setObservationConvention(...)` method:

```java
rerankModel.setObservationConvention(new MyRerankObservationConvention());
```

