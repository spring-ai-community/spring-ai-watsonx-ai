/*
 * Copyright 2026 the original author or authors.
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

package org.springaicommunity.watsonx.rerank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request for the watsonx.ai Rerank API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-rerank">watsonx.ai Text Rerank</a>.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiRerankRequest {

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("project_id")
  private String projectId;

  @JsonProperty("space_id")
  private String spaceId;

  @JsonProperty("inputs")
  private List<RerankInput> inputs;

  @JsonProperty("query")
  private String query;

  @JsonProperty("parameters")
  private RerankParameters parameters;

  public WatsonxAiRerankRequest() {}

  private WatsonxAiRerankRequest(Builder builder) {
    this.model = builder.model;
    this.projectId = builder.projectId;
    this.spaceId = builder.spaceId;
    this.inputs = builder.inputs;
    this.query = builder.query;
    this.parameters = builder.parameters;
  }

  public String model() {
    return model;
  }

  public String projectId() {
    return projectId;
  }

  public String spaceId() {
    return spaceId;
  }

  public List<RerankInput> inputs() {
    return inputs;
  }

  public String query() {
    return query;
  }

  public RerankParameters parameters() {
    return parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .model(this.model)
        .projectId(this.projectId)
        .spaceId(this.spaceId)
        .inputs(this.inputs)
        .query(this.query)
        .parameters(this.parameters);
  }

  public static class Builder {
    private String model;
    private String projectId;
    private String spaceId;
    private List<RerankInput> inputs;
    private String query;
    private RerankParameters parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder spaceId(String spaceId) {
      this.spaceId = spaceId;
      return this;
    }

    public Builder inputs(List<RerankInput> inputs) {
      this.inputs = inputs;
      return this;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder parameters(RerankParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    public WatsonxAiRerankRequest build() {
      return new WatsonxAiRerankRequest(this);
    }
  }

  /** Represents an input document for reranking. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RerankInput(@JsonProperty("text") String text) {

    public static RerankInput of(String text) {
      return new RerankInput(text);
    }
  }

  /** Parameters for the rerank request. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RerankParameters(
      @JsonProperty("truncate_input_tokens") Integer truncateInputTokens,
      @JsonProperty("return_options") RerankReturnOptions returnOptions) {

    public static RerankParameters of(
        Integer truncateInputTokens, RerankReturnOptions returnOptions) {
      return new RerankParameters(truncateInputTokens, returnOptions);
    }
  }

  /** Return options for the rerank response. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RerankReturnOptions(
      @JsonProperty("top_n") Integer topN,
      @JsonProperty("inputs") Boolean inputs,
      @JsonProperty("query") Boolean query) {

    public static RerankReturnOptions of(Integer topN, Boolean inputs, Boolean query) {
      return new RerankReturnOptions(topN, inputs, query);
    }
  }
}
