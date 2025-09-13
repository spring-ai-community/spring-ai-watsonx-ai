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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Options for watsonx Embedding API.
 *
 * <p>TODO: add more explanation
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiEmbeddingOptions {

  public static final String DEFAULT_MODEL = "ibm/slate-30m-english-rtrvr";

  @JsonProperty("model")
  private String model;

  public static WatsonxAiEmbeddingOptions create() {
    return new WatsonxAiEmbeddingOptions();
  }

  public static WatsonxAiEmbeddingOptions fromOptions(WatsonxAiEmbeddingOptions fromOptions) {
    return new WatsonxAiEmbeddingOptions().withModel(fromOptions.getModel());
  }

  public WatsonxAiEmbeddingOptions withModel(String model) {
    this.model = model;
    return this;
  }

  public String getModel() {
    return this.model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
