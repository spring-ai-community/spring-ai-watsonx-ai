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

package io.github.springaicommunity.watsonx.autoconfigure.embedding;

import io.github.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for watsonx.ai Embedding Model.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiEmbeddingProperties.CONFIG_PREFIX)
public class WatsonxAiEmbeddingProperties {

  public static final String CONFIG_PREFIX = "spring.ai.watsonx.embedding";
  public static final String DEFAULT_EMBEDDING_ENDPOINT = "/ml/v1/text/embeddings";
  public static final String DEFAULT_VERSION = "2024-08-15";

  private String embeddingEndpoint = DEFAULT_EMBEDDING_ENDPOINT;
  private String version = DEFAULT_VERSION;
  private WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();

  public String getEmbeddingEndpoint() {
    return embeddingEndpoint;
  }

  public void setEmbeddingEndpoint(String embeddingEndpoint) {
    this.embeddingEndpoint = embeddingEndpoint;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public WatsonxAiEmbeddingOptions getOptions() {
    return options;
  }

  public void setOptions(WatsonxAiEmbeddingOptions options) {
    this.options = options;
  }
}
