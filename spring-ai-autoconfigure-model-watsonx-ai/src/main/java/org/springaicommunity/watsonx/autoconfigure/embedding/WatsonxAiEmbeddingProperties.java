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

package org.springaicommunity.watsonx.autoconfigure.embedding;

import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingOptions;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai Embedding Model.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiEmbeddingProperties.CONFIG_PREFIX)
public class WatsonxAiEmbeddingProperties {

  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.embedding";

  private String embeddingEndpoint = "/ml/v1/text/embeddings";

  /**
   * API version date to use, in YYYY-MM-DD format. Example: 2024-10-17. See the <a
   * href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API versioning</a>
   */
  private String version = "2024-10-17";

  /**
   * The default options to use when calling the watsonx.ai Embedding API. These can be overridden
   * by passing options in the request.
   */
  @NestedConfigurationProperty
  private WatsonxAiEmbeddingOptions options =
      WatsonxAiEmbeddingOptions.builder()
          .model("ibm/slate-125m-english-rtrvr")
          .parameters(new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null))
          .build();

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
