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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingOptions;

/**
 * Options for watsonx Embedding API. Configuration options that can be passed to control the
 * behavior of the embedding model.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiEmbeddingOptions implements EmbeddingOptions {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiEmbeddingOptions.class);

  private String model;
  private Map<String, Object> parameters;
  private Integer truncateInputTokens;

  public WatsonxAiEmbeddingOptions() {}

  private WatsonxAiEmbeddingOptions(Builder builder) {
    this.model = builder.model;
    this.parameters = builder.parameters;
    this.truncateInputTokens = builder.truncateInputTokens;
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

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public Integer getTruncateInputTokens() {
    return truncateInputTokens;
  }

  public void setTruncateInputTokens(Integer truncateInputTokens) {
    this.truncateInputTokens = truncateInputTokens;
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
    return new Builder()
        .model(this.model)
        .parameters(this.parameters)
        .truncateInputTokens(this.truncateInputTokens);
  }

  public static class Builder {
    private String model;
    private Map<String, Object> parameters;
    private Integer truncateInputTokens;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder truncateInputTokens(Integer truncateInputTokens) {
      this.truncateInputTokens = truncateInputTokens;
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
