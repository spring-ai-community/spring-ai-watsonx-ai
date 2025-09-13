package io.github.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WatsonxAiEmbeddingParams(
    @JsonProperty("truncate_input_tokens") int truncateInputTokens,
    @JsonProperty("return_options") ReturnOptions returnOptions) {
  public static record ReturnOptions(@JsonProperty("input_text") boolean inputText) {}
}
