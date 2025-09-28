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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.Assert;

/**
 * {@class EmbeddingModel} implementation that provides access to watsonx supported embedding
 * models.
 *
 * <p>TODO: add more explanation
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public final class WatsonxAiEmbeddingModel implements EmbeddingModel {

  private final WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;

  private WatsonxAiEmbeddingOptions defaulWatsonxAiEmbeddingOptions =
      WatsonxAiEmbeddingOptions.createWithDefaultModel();

  public WatsonxAiEmbeddingModel(WatsonxAiEmbeddingApi watsonxAiEmbeddingApi) {
    this.watsonxAiEmbeddingApi = watsonxAiEmbeddingApi;
  }

  public WatsonxAiEmbeddingModel(
      WatsonxAiEmbeddingApi watsonxAiEmbeddingApi, WatsonxAiEmbeddingOptions defaultOptions) {
    this.watsonxAiEmbeddingApi = watsonxAiEmbeddingApi;
    this.defaulWatsonxAiEmbeddingOptions = defaultOptions;
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    Assert.notEmpty(
        request.getInstructions(), "The request must contain at least one instruction.");
    // Implement this for watsonx support.
    WatsonxAiEmbeddingRequest watsonxAiEmbeddingRequest =
        watsonxAiEmbeddingRequest(request.getInstructions(), request.getOptions());
    // Call the watsonx API with the constructed request and get the response.
    WatsonxAiEmbeddingResponse watsonxAiEmbeddingResponse =
        watsonxAiEmbeddingApi.getEmbeddings(watsonxAiEmbeddingRequest).getBody();

    AtomicInteger counter = new AtomicInteger();
    List<Embedding> embeddings =
        response.results().stream()
            .map(result -> new Embedding(result.embedding, counter.getAndIncrement()))
            .toList();

    return new EmbeddingResponse(null);
  }

  @Override
  public float[] embed(Document document) {
    return embed(document.getFormattedContent());
  }

  WatsonxAiEmbeddingRequest watsonxAiEmbeddingRequest(
      List<String> input, EmbeddingOptions options) {
    // Assert.notEmpty(input, "The input must contain at least one item.");
    // Assert.notNull(options, "The options are required.");

    WatsonxAiEmbeddingOptions watsonxAiEmbeddingOptions =
        options instanceof WatsonxAiEmbeddingOptions
            ? (WatsonxAiEmbeddingOptions) options
            : WatsonxAiEmbeddingOptions.create();

    if (watsonxAiEmbeddingOptions.getModel() == null) {
      watsonxAiEmbeddingOptions = defaulWatsonxAiEmbeddingOptions;
    }

    return new WatsonxAiEmbeddingRequest.Builder(input)
        .withModel(watsonxAiEmbeddingOptions.getModel())
        .build();
  }
}
