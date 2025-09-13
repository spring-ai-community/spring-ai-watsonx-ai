package io.github.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WatsonxAiEmbeddingResponse(
    @JsonProperty("model_id") String modelId,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("input_token_count") int inputTokenCount,
    @JsonProperty("results") List<WatasonxAiEmbeddingResults> embeddingResult) {}
