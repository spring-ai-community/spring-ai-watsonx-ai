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
import java.util.Objects;
import org.springframework.ai.model.ModelOptionsUtils;

/**
 * Options for watsonx.ai Rerank API. Configuration options that can be passed to control the
 * behavior of the rerank model.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiRerankOptions {

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("top_n")
  private Integer topN;

  @JsonProperty("truncate_input_tokens")
  private Integer truncateInputTokens;

  @JsonProperty("return_inputs")
  private Boolean returnInputs;

  @JsonProperty("return_query")
  private Boolean returnQuery;

  public WatsonxAiRerankOptions() {}

  private WatsonxAiRerankOptions(Builder builder) {
    this.model = builder.model;
    this.topN = builder.topN;
    this.truncateInputTokens = builder.truncateInputTokens;
    this.returnInputs = builder.returnInputs;
    this.returnQuery = builder.returnQuery;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Integer getTopN() {
    return topN;
  }

  public void setTopN(Integer topN) {
    this.topN = topN;
  }

  public Integer getTruncateInputTokens() {
    return truncateInputTokens;
  }

  public void setTruncateInputTokens(Integer truncateInputTokens) {
    this.truncateInputTokens = truncateInputTokens;
  }

  public Boolean getReturnInputs() {
    return returnInputs;
  }

  public void setReturnInputs(Boolean returnInputs) {
    this.returnInputs = returnInputs;
  }

  public Boolean getReturnQuery() {
    return returnQuery;
  }

  public void setReturnQuery(Boolean returnQuery) {
    this.returnQuery = returnQuery;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .model(this.model)
        .topN(this.topN)
        .truncateInputTokens(this.truncateInputTokens)
        .returnInputs(this.returnInputs)
        .returnQuery(this.returnQuery);
  }

  public WatsonxAiRerankOptions copy() {
    return toBuilder().build();
  }

  @Override
  public String toString() {
    return "WatsonxAiRerankOptions: " + ModelOptionsUtils.toJsonString(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WatsonxAiRerankOptions other = (WatsonxAiRerankOptions) o;
    return Objects.equals(this.model, other.model)
        && Objects.equals(this.topN, other.topN)
        && Objects.equals(this.truncateInputTokens, other.truncateInputTokens)
        && Objects.equals(this.returnInputs, other.returnInputs)
        && Objects.equals(this.returnQuery, other.returnQuery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.model, this.topN, this.truncateInputTokens, this.returnInputs, this.returnQuery);
  }

  public static class Builder {
    private String model;
    private Integer topN;
    private Integer truncateInputTokens;
    private Boolean returnInputs;
    private Boolean returnQuery;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder topN(Integer topN) {
      this.topN = topN;
      return this;
    }

    public Builder truncateInputTokens(Integer truncateInputTokens) {
      this.truncateInputTokens = truncateInputTokens;
      return this;
    }

    public Builder returnInputs(Boolean returnInputs) {
      this.returnInputs = returnInputs;
      return this;
    }

    public Builder returnQuery(Boolean returnQuery) {
      this.returnQuery = returnQuery;
      return this;
    }

    public WatsonxAiRerankOptions build() {
      return new WatsonxAiRerankOptions(this);
    }
  }
}
