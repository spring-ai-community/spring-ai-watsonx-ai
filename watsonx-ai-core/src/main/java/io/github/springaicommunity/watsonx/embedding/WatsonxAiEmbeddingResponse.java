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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from the Watson AI Embedding API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-embeddings">Watson AI Text Embeddings</a>.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiEmbeddingResponse(
    @JsonProperty("model_id") String modelId,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("results") List<Embedding> results,
    @JsonProperty("input_token_count") Integer inputTokenCount) {

  /** Individual embedding result containing the embedding vector and token count. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Embedding(
      @JsonProperty("embedding") List<Double> embedding,
      @JsonProperty("input") EmbeddingInputResult input) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EmbeddingInputResult(@JsonProperty("text") String text) {}
}
