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

package org.springaicommunity.watsonx.autoconfigure.moderation;

import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.autoconfigure.chat.WatsonxAiChatAutoConfiguration;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationApi;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationModel;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures Watsonx AI moderation/text detection services as part of Spring AI.
 *
 * @author Federico Mariani
 * @since 1.0.0
 */
@AutoConfiguration(
    after = {
      RestClientAutoConfiguration.class,
      WebClientAutoConfiguration.class,
      SpringAiRetryAutoConfiguration.class
    })
@ConditionalOnClass(WatsonxAiModerationApi.class)
@ConditionalOnProperty(
    name = SpringAIModelProperties.MODERATION_MODEL,
    havingValue = WatsonxAiChatAutoConfiguration.MODEL_ID,
    matchIfMissing = true)
@EnableConfigurationProperties({
  WatsonxAiConnectionProperties.class,
  WatsonxAiModerationProperties.class
})
@ImportAutoConfiguration(
    classes = {SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class})
public class WatsonxAiModerationAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiModerationApi watsonxAiModerationApi(
      final WatsonxAiConnectionProperties connectionProperties,
      final WatsonxAiModerationProperties moderationProperties,
      final ObjectProvider<RestClient.Builder> restClientObjectProvider,
      ResponseErrorHandler responseErrorHandler) {

    return new WatsonxAiModerationApi(
        connectionProperties.getBaseUrl(),
        moderationProperties.getTextDetectionEndpoint(),
        moderationProperties.getVersion(),
        connectionProperties.getProjectId(),
        connectionProperties.getSpaceId(),
        connectionProperties.getApiKey(),
        restClientObjectProvider.getIfAvailable(RestClient::builder),
        responseErrorHandler);
  }

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiModerationModel watsonxAiModerationModel(
      WatsonxAiModerationApi watsonxAiModerationApi,
      WatsonxAiModerationProperties moderationProperties,
      RetryTemplate retryTemplate) {

    return WatsonxAiModerationModel.builder()
        .watsonxAiModerationApi(watsonxAiModerationApi)
        .defaultOptions(moderationProperties.getOptions())
        .retryTemplate(retryTemplate)
        .build();
  }
}
