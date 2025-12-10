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

import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingApi;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingModel;
import org.springframework.ai.model.SpringAIModelProperties;
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
 * Auto-configures watsonx.ai embedding services as part of Spring AI.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@AutoConfiguration(
    after = {RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class})
@ConditionalOnClass(WatsonxAiEmbeddingApi.class)
@ConditionalOnProperty(
    name = SpringAIModelProperties.EMBEDDING_MODEL,
    havingValue = WatsonxAiEmbeddingAutoConfiguration.MODEL_ID,
    matchIfMissing = true)
@EnableConfigurationProperties({
  WatsonxAiConnectionProperties.class,
  WatsonxAiEmbeddingProperties.class
})
@ImportAutoConfiguration(
    classes = {SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class})
public class WatsonxAiEmbeddingAutoConfiguration {

  public static final String MODEL_ID = "watsonx-ai";

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiEmbeddingApi watsonxAiEmbeddingApi(
      final WatsonxAiConnectionProperties connectionProperties,
      final WatsonxAiEmbeddingProperties embeddingProperties,
      final ObjectProvider<RestClient.Builder> restClientObjectProvider,
      ResponseErrorHandler responseErrorHandler) {

    return new WatsonxAiEmbeddingApi(
        connectionProperties.getBaseUrl(),
        embeddingProperties.getEmbeddingEndpoint(),
        embeddingProperties.getVersion(),
        connectionProperties.getProjectId(),
        connectionProperties.getApiKey(),
        restClientObjectProvider.getIfAvailable(RestClient::builder),
        responseErrorHandler);
  }

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiEmbeddingModel watsonxAiEmbeddingModel(
      WatsonxAiEmbeddingApi watsonxAiEmbeddingApi,
      WatsonxAiEmbeddingProperties embeddingProperties,
      RetryTemplate retryTemplate) {

    return new WatsonxAiEmbeddingModel(
        watsonxAiEmbeddingApi, embeddingProperties.getOptions(), retryTemplate);
  }
}
