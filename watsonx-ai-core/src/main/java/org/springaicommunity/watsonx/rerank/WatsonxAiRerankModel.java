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

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.rerank.observation.DefaultRerankModelObservationConvention;
import org.springaicommunity.watsonx.rerank.observation.RerankModelObservationContext;
import org.springaicommunity.watsonx.rerank.observation.RerankModelObservationConvention;
import org.springaicommunity.watsonx.rerank.observation.RerankModelObservationDocumentation;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Rerank model implementation that provides access to watsonx.ai supported reranking models.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiRerankModel {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiRerankModel.class);

  private static final RerankModelObservationConvention DEFAULT_OBSERVATION_CONVENTION =
      new DefaultRerankModelObservationConvention();

  private static final String PROVIDER = "watsonx-ai";

  private final WatsonxAiRerankApi watsonxAiRerankApi;
  private final WatsonxAiRerankOptions defaultOptions;
  private final RetryTemplate retryTemplate;
  private final ObservationRegistry observationRegistry;
  private RerankModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

  public WatsonxAiRerankModel(
      WatsonxAiRerankApi watsonxAiRerankApi,
      WatsonxAiRerankOptions defaultOptions,
      ObservationRegistry observationRegistry,
      RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiRerankApi, "WatsonxAiRerankApi must not be null");
    Assert.notNull(defaultOptions, "WatsonxAiRerankOptions must not be null");
    Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
    Assert.notNull(retryTemplate, "RetryTemplate must not be null");
    this.watsonxAiRerankApi = watsonxAiRerankApi;
    this.defaultOptions = defaultOptions;
    this.observationRegistry = observationRegistry;
    this.retryTemplate = retryTemplate;
  }

  /**
   * Rerank documents based on their relevance to the query using the default options.
   *
   * @param query the query string to rank against
   * @param documents the list of document texts to rerank
   * @return list of rerank results sorted by relevance score (descending)
   */
  public List<WatsonxAiRerankResponse.RerankResult> rerank(String query, List<String> documents) {
    return rerank(query, documents, null);
  }

  /**
   * Rerank documents based on their relevance to the query.
   *
   * @param query the query string to rank against
   * @param documents the list of document texts to rerank
   * @param runtimeOptions optional runtime options to override defaults
   * @return list of rerank results sorted by relevance score (descending)
   */
  public List<WatsonxAiRerankResponse.RerankResult> rerank(
      String query, List<String> documents, WatsonxAiRerankOptions runtimeOptions) {
    Assert.hasText(query, "Query must not be null or empty");
    Assert.notEmpty(documents, "Documents must not be empty");

    WatsonxAiRerankOptions mergedOptions = mergeOptions(runtimeOptions);

    RerankModelObservationContext observationContext =
        RerankModelObservationContext.builder()
            .query(query)
            .documentCount(documents.size())
            .options(mergedOptions)
            .provider(PROVIDER)
            .build();

    return RerankModelObservationDocumentation.RERANK_MODEL_OPERATION
        .observation(
            this.observationConvention,
            DEFAULT_OBSERVATION_CONVENTION,
            () -> observationContext,
            this.observationRegistry)
        .observe(
            () -> {
              WatsonxAiRerankResponse response =
                  this.retryTemplate.execute(
                      ctx -> {
                        List<WatsonxAiRerankRequest.RerankInput> inputs =
                            documents.stream().map(WatsonxAiRerankRequest.RerankInput::of).toList();

                        WatsonxAiRerankRequest.RerankReturnOptions returnOptions =
                            WatsonxAiRerankRequest.RerankReturnOptions.of(
                                mergedOptions.getTopN(),
                                mergedOptions.getReturnInputs(),
                                mergedOptions.getReturnQuery());

                        WatsonxAiRerankRequest.RerankParameters parameters =
                            WatsonxAiRerankRequest.RerankParameters.of(
                                mergedOptions.getTruncateInputTokens(), returnOptions);

                        WatsonxAiRerankRequest request =
                            WatsonxAiRerankRequest.builder()
                                .model(mergedOptions.getModel())
                                .inputs(inputs)
                                .query(query)
                                .parameters(parameters)
                                .build();

                        ResponseEntity<WatsonxAiRerankResponse> apiResponse =
                            this.watsonxAiRerankApi.rerank(request);

                        return apiResponse.getBody();
                      });

              if (response == null || CollectionUtils.isEmpty(response.results())) {
                return List.of();
              }

              observationContext.setResponse(response);
              return response.results();
            });
  }

  private WatsonxAiRerankOptions mergeOptions(WatsonxAiRerankOptions runtimeOptions) {
    WatsonxAiRerankOptions.Builder builder = this.defaultOptions.toBuilder();

    if (runtimeOptions != null) {
      if (runtimeOptions.getModel() != null) {
        builder.model(runtimeOptions.getModel());
      }
      if (runtimeOptions.getTopN() != null) {
        builder.topN(runtimeOptions.getTopN());
      }
      if (runtimeOptions.getTruncateInputTokens() != null) {
        builder.truncateInputTokens(runtimeOptions.getTruncateInputTokens());
      }
      if (runtimeOptions.getReturnInputs() != null) {
        builder.returnInputs(runtimeOptions.getReturnInputs());
      }
      if (runtimeOptions.getReturnQuery() != null) {
        builder.returnQuery(runtimeOptions.getReturnQuery());
      }
    }

    return builder.build();
  }

  public WatsonxAiRerankOptions getDefaultOptions() {
    return this.defaultOptions;
  }

  public void setObservationConvention(RerankModelObservationConvention observationConvention) {
    Assert.notNull(observationConvention, "observationConvention must not be null");
    this.observationConvention = observationConvention;
  }
}
