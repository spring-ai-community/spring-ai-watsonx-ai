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

package org.springaicommunity.watsonx.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingRequest.EmbeddingParameters;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Options for watsonx Embedding API. Configuration options that can be passed to control the
 * behavior of the embedding model.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiEmbeddingOptions implements EmbeddingOptions {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiEmbeddingOptions.class);

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("parameters")
  @NestedConfigurationProperty
  private EmbeddingParameters parameters;

  public WatsonxAiEmbeddingOptions() {}

  private WatsonxAiEmbeddingOptions(Builder builder) {
    this.model = builder.model;
    this.parameters = builder.parameters;
  }

  @Override
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public Integer getDimensions() {
    logger.warn("Watson AI API doesn't support dimensions parameter");
    return null;
  }

  public void setDimensions(Integer dimensions) {
    logger.warn("Watson AI API doesn't support dimensions parameter");
  }

  public EmbeddingParameters getParameters() {
    return parameters;
  }

  public void setParameters(EmbeddingParameters parameters) {
    this.parameters = parameters;
  }

  public String getEncodingFormat() {
    logger.warn("Watson AI API doesn't support encoding format parameter");
    return null;
  }

  public void setEncodingFormat(String encodingFormat) {
    logger.warn("Watson AI API doesn't support encoding format parameter");
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder().model(this.model).parameters(this.parameters);
  }

  public static class Builder {
    private String model;
    private EmbeddingParameters parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder parameters(EmbeddingParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder encodingFormat(String encodingFormat) {
      logger.warn("Watson AI API doesn't support encoding format parameter");
      return this;
    }

    public WatsonxAiEmbeddingOptions build() {
      return new WatsonxAiEmbeddingOptions(this);
    }
  }
}
