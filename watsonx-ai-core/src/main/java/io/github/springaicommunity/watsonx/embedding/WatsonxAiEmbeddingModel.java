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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@class EmbeddingModel} implementation that provides access to watsonx supported embedding
 * models.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiEmbeddingModel implements EmbeddingModel {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiEmbeddingModel.class);

  private final WatsonxAiEmbeddingOptions defaultOptions;
  private final RetryTemplate retryTemplate;
  private final WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;

  public WatsonxAiEmbeddingModel(
      WatsonxAiEmbeddingApi watsonxAiEmbeddingApi,
      WatsonxAiEmbeddingOptions defaultOptions,
      RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiEmbeddingApi, "WatsonxAiEmbeddingApi must not be null");
    Assert.notNull(defaultOptions, "WatsonxAiEmbeddingOptions must not be null");
    Assert.notNull(retryTemplate, "RetryTemplate must not be null");
    this.watsonxAiEmbeddingApi = watsonxAiEmbeddingApi;
    this.defaultOptions = defaultOptions;
    this.retryTemplate = retryTemplate;
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    Assert.notNull(request, "EmbeddingRequest must not be null");
    Assert.notEmpty(request.getInstructions(), "EmbeddingRequest instructions must not be empty");

    return this.retryTemplate.execute(
        ctx -> {
          WatsonxAiEmbeddingOptions options = mergeOptions(request.getOptions());

          WatsonxAiEmbeddingRequest watsonxRequest =
              WatsonxAiEmbeddingRequest.builder()
                  .input(request.getInstructions())
                  .modelId(options.getModel())
                  .parameters(createEmbeddingParameters(options))
                  .build();

          ResponseEntity<WatsonxAiEmbeddingResponse> response =
              this.watsonxAiEmbeddingApi.embed(watsonxRequest);

          return toEmbeddingResponse(response.getBody());
        });
  }

  @Override
  public float[] embed(Document document) {
    Assert.notNull(document, "Document must not be null");
    return embed(document.getText());
  }

  @Override
  public float[] embed(String text) {
    Assert.hasText(text, "Text must not be null or empty");

    EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
    EmbeddingResponse response = call(request);

    return response.getResults().isEmpty()
        ? new float[0]
        : response.getResults().get(0).getOutput();
  }

  private WatsonxAiEmbeddingOptions mergeOptions(EmbeddingOptions runtimeOptions) {
    WatsonxAiEmbeddingOptions mergedOptions = this.defaultOptions.toBuilder().build();

    if (runtimeOptions != null) {
      if (runtimeOptions.getModel() != null) {
        mergedOptions.setModel(runtimeOptions.getModel());
      }
      if (runtimeOptions.getDimensions() != null) {
        mergedOptions.setDimensions(runtimeOptions.getDimensions());
      }
      // Handle WatsonxAiEmbeddingOptions specific options
      if (runtimeOptions instanceof WatsonxAiEmbeddingOptions watsonxOptions) {
        if (watsonxOptions.getEncodingFormat() != null) {
          mergedOptions.setEncodingFormat(watsonxOptions.getEncodingFormat());
        }
        if (watsonxOptions.getParameters() != null) {
          mergedOptions.setParameters(watsonxOptions.getParameters());
        }
        if (watsonxOptions.getTruncateInputTokens() != null) {
          mergedOptions.setTruncateInputTokens(watsonxOptions.getTruncateInputTokens());
        }
      }
    }

    return mergedOptions;
  }

  private WatsonxAiEmbeddingRequest.EmbeddingParameters createEmbeddingParameters(
      WatsonxAiEmbeddingOptions options) {
    if (options.getTruncateInputTokens() == null && options.getParameters() == null) {
      return null;
    }

    WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions = null;
    if (options.getParameters() != null && options.getParameters().get("input_text") != null) {
      returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(
              (Boolean) options.getParameters().get("input_text"));
    }

    return new WatsonxAiEmbeddingRequest.EmbeddingParameters(
        options.getTruncateInputTokens(), returnOptions);
  }

  private EmbeddingResponse toEmbeddingResponse(WatsonxAiEmbeddingResponse watsonxResponse) {
    if (watsonxResponse == null || CollectionUtils.isEmpty(watsonxResponse.results())) {
      return new EmbeddingResponse(List.of(), new EmbeddingResponseMetadata("unknown", null));
    }

    List<Embedding> embeddings =
        watsonxResponse.results().stream()
            .map(
                result -> {
                  List<Double> embeddingDoubles = result.embedding();
                  float[] embeddingFloats = new float[embeddingDoubles.size()];
                  for (int i = 0; i < embeddingDoubles.size(); i++) {
                    embeddingFloats[i] = embeddingDoubles.get(i).floatValue();
                  }
                  return new Embedding(
                      embeddingFloats,
                      watsonxResponse.inputTokenCount() != null
                          ? watsonxResponse.inputTokenCount()
                          : 0);
                })
            .toList();

    EmbeddingResponseMetadata metadata =
        new EmbeddingResponseMetadata(
            watsonxResponse.modelId() != null ? watsonxResponse.modelId() : "unknown", null);

    return new EmbeddingResponse(embeddings, metadata);
  }

  public WatsonxAiEmbeddingOptions getDefaultOptions() {
    return this.defaultOptions;
  }
}
