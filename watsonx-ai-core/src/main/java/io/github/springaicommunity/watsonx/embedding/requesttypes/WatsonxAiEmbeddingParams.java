package io.github.springaicommunity.watsonx.embedding.requesttypes;

public class WatsonxAiEmbeddingParams {

  private final int truncateInputTokens;
  private WatsonxAiEmbeddingParamsReturnOptions returnOptions;

  public WatsonxAiEmbeddingParams(
      int truncateInputTokens, WatsonxAiEmbeddingParamsReturnOptions returnOptions) {
    this.truncateInputTokens = truncateInputTokens;
    this.returnOptions = returnOptions;
  }
}
