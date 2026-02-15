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

package org.springaicommunity.watsonx.autoconfigure.rerank;

import io.micrometer.observation.ObservationRegistry;
import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.rerank.WatsonxAiDocumentReranker;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankApi;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankModel;
import org.springaicommunity.watsonx.rerank.observation.RerankModelObservationConvention;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures watsonx.ai rerank services as part of Spring AI.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
@AutoConfiguration(
    after = {RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class})
@ConditionalOnClass(WatsonxAiRerankApi.class)
@ConditionalOnProperty(
    prefix = WatsonxAiRerankProperties.CONFIG_PREFIX,
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties({
  WatsonxAiConnectionProperties.class,
  WatsonxAiRerankProperties.class
})
@ImportAutoConfiguration(
    classes = {SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class})
public class WatsonxAiRerankAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiRerankApi watsonxAiRerankApi(
      final WatsonxAiConnectionProperties connectionProperties,
      final WatsonxAiRerankProperties rerankProperties,
      final ObjectProvider<RestClient.Builder> restClientObjectProvider,
      ResponseErrorHandler responseErrorHandler) {

    return new WatsonxAiRerankApi(
        connectionProperties.getBaseUrl(),
        rerankProperties.getRerankEndpoint(),
        rerankProperties.getVersion(),
        connectionProperties.getProjectId(),
        connectionProperties.getSpaceId(),
        connectionProperties.getApiKey(),
        restClientObjectProvider.getIfAvailable(RestClient::builder),
        responseErrorHandler);
  }

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiRerankModel watsonxAiRerankModel(
      WatsonxAiRerankApi watsonxAiRerankApi,
      WatsonxAiRerankProperties rerankProperties,
      ObjectProvider<ObservationRegistry> observationRegistry,
      RetryTemplate retryTemplate,
      ObjectProvider<RerankModelObservationConvention> observationConvention) {

    var watsonxAiRerankModel =
        new WatsonxAiRerankModel(
            watsonxAiRerankApi,
            rerankProperties.getOptions(),
            observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
            retryTemplate);

    observationConvention.ifUnique(watsonxAiRerankModel::setObservationConvention);

    return watsonxAiRerankModel;
  }

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiDocumentReranker watsonxAiDocumentReranker(
      WatsonxAiRerankModel watsonxAiRerankModel) {
    return new WatsonxAiDocumentReranker(watsonxAiRerankModel);
  }
}
