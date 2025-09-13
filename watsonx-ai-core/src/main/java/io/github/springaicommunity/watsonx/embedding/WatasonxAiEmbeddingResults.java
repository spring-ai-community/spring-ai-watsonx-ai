package io.github.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record WatasonxAiEmbeddingResults(
    @JsonProperty("embedding") float[] embedding, @JsonProperty("input") String input) {}
