/*
 * Copyright 2025-2026 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
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
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link EmbeddingModel} implementation that provides access to watsonx supported embedding models.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiEmbeddingModel implements EmbeddingModel {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiEmbeddingModel.class);

  private static final EmbeddingModelObservationConvention DEFAULT_OBSERVATION_CONVENTION =
      new DefaultEmbeddingModelObservationConvention();

  private final WatsonxAiEmbeddingOptions defaultOptions;
  private final RetryTemplate retryTemplate;
  private final WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;
  private final ObservationRegistry observationRegistry;
  private EmbeddingModelObservationConvention observationConvention =
      DEFAULT_OBSERVATION_CONVENTION;

  public WatsonxAiEmbeddingModel(
      WatsonxAiEmbeddingApi watsonxAiEmbeddingApi,
      WatsonxAiEmbeddingOptions defaultOptions,
      ObservationRegistry observationRegistry,
      RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiEmbeddingApi, "WatsonxAiEmbeddingApi must not be null");
    Assert.notNull(defaultOptions, "WatsonxAiEmbeddingOptions must not be null");
    Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
    Assert.notNull(retryTemplate, "RetryTemplate must not be null");
    this.watsonxAiEmbeddingApi = watsonxAiEmbeddingApi;
    this.defaultOptions = defaultOptions;
    this.observationRegistry = observationRegistry;
    this.retryTemplate = retryTemplate;
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    Assert.notNull(request, "EmbeddingRequest must not be null");
    Assert.notEmpty(request.getInstructions(), "EmbeddingRequest instructions must not be empty");

    EmbeddingModelObservationContext observationContext =
        EmbeddingModelObservationContext.builder()
            .embeddingRequest(request)
            .provider("watsonx-ai")
            .build();

    return EmbeddingModelObservationDocumentation.EMBEDDING_MODEL_OPERATION
        .observation(
            this.observationConvention,
            DEFAULT_OBSERVATION_CONVENTION,
            () -> observationContext,
            this.observationRegistry)
        .observe(
            () -> {
              EmbeddingResponse response =
                  this.retryTemplate.execute(
                      ctx -> {
                        WatsonxAiEmbeddingOptions options = mergeOptions(request.getOptions());

                        WatsonxAiEmbeddingRequest watsonxRequest =
                            WatsonxAiEmbeddingRequest.builder()
                                .inputs(request.getInstructions())
                                .model(options.getModel())
                                .parameters(createEmbeddingParameters(options))
                                .build();

                        ResponseEntity<WatsonxAiEmbeddingResponse> apiResponse =
                            this.watsonxAiEmbeddingApi.embed(watsonxRequest);

                        return toEmbeddingResponse(apiResponse.getBody());
                      });

              observationContext.setResponse(response);
              return response;
            });
  }

  @Override
  public float[] embed(Document document) {
    Assert.notNull(document, "Document must not be null");
    return embed(document.getText());
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
      }
    }

    return mergedOptions;
  }

  private WatsonxAiEmbeddingRequest.EmbeddingParameters createEmbeddingParameters(
      WatsonxAiEmbeddingOptions options) {
    if (options.getParameters() == null) {
      return null;
    }

    WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions = null;
    if (options.getParameters() != null && options.getParameters().returnOptions() != null) {
      returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(
              (Boolean) options.getParameters().returnOptions().inputText());
    }

    return new WatsonxAiEmbeddingRequest.EmbeddingParameters(
        options.getParameters().truncateInputTokens(), returnOptions);
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
            watsonxResponse.model() != null ? watsonxResponse.model() : "unknown", null);

    return new EmbeddingResponse(embeddings, metadata);
  }

  public WatsonxAiEmbeddingOptions getDefaultOptions() {
    return this.defaultOptions;
  }

  public void setObservationConvention(EmbeddingModelObservationConvention observationConvention) {
    Assert.notNull(observationConvention, "observationConvention must not be null");
    this.observationConvention = observationConvention;
  }
}
