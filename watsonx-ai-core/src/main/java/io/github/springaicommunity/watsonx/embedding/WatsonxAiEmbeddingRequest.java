/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request for the Watson AI Embedding API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-embeddings">Watson AI Text Embeddings</a>.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiEmbeddingRequest {

  @JsonProperty("inputs")
  private List<String> inputs;

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("project_id")
  private String projectId;

  @JsonProperty("parameters")
  private EmbeddingParameters parameters;

  public WatsonxAiEmbeddingRequest() {}

  private WatsonxAiEmbeddingRequest(Builder builder) {
    this.inputs = builder.inputs;
    this.model = builder.model;
    this.projectId = builder.projectId;
    this.parameters = builder.parameters;
  }

  public List<String> inputs() {
    return inputs;
  }

  public String model() {
    return model;
  }

  public String projectId() {
    return projectId;
  }

  public EmbeddingParameters parameters() {
    return parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .inputs(this.inputs)
        .model(this.model)
        .projectId(this.projectId)
        .parameters(this.parameters);
  }

  public static class Builder {
    private List<String> inputs;
    private String model;
    private String projectId;
    private EmbeddingParameters parameters;

    private Builder() {}

    public Builder inputs(List<String> input) {
      this.inputs = input;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder parameters(EmbeddingParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    public WatsonxAiEmbeddingRequest build() {
      return new WatsonxAiEmbeddingRequest(this);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EmbeddingParameters(
      @JsonProperty("truncate_input_tokens") Integer truncateInputTokens,
      @JsonProperty("return_options") EmbeddingReturnOptions returnOptions) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EmbeddingReturnOptions(@JsonProperty("input_text") Boolean inputText) {}
}
