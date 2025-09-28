package io.github.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.embedding.requesttypes.WatsonxAiEmbeddingParams;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiEmbeddingRequest {

  @JsonProperty("version")
  String version;

  @JsonProperty("model_id")
  String modelId;

  @JsonProperty("inputs")
  List<String> inputs;

  @JsonProperty("project_id")
  String projectId;

  @JsonProperty("space_id")
  String spaceId;

  @JsonProperty("parameters")
  WatsonxAiEmbeddingParams parameters;

  private WatsonxAiEmbeddingRequest(
      String modelId,
      List<String> inputs,
      String projectId,
      String spaceId,
      WatsonxAiEmbeddingParams parameters) {
    this.modelId = modelId;
    this.inputs = inputs;
    this.projectId = projectId;
    this.spaceId = spaceId;
    this.parameters = parameters;
  }

  public static final class Builder {
    private final List<String> inputs;
    private String model = WatsonxAiEmbeddingOptions.DEFAULT_MODEL;
    private WatsonxAiEmbeddingParams parameters;
    private String spaceId;
    private String projectId;

    public Builder(List<String> inputs) {
      this.inputs = inputs;
    }

    public Builder withModel(String model) {
      this.model = model;
      return this;
    }

    public Builder withSpaceId(String spaceId) {
      this.spaceId = spaceId;
      return this;
    }

    public Builder withParameters(WatsonxAiEmbeddingParams parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public WatsonxAiEmbeddingRequest build() {
      return new WatsonxAiEmbeddingRequest(model, inputs, projectId, spaceId, parameters);
    }
  }
}
