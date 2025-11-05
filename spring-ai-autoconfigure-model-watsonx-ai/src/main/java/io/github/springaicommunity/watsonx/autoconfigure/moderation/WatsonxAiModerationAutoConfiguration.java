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

package io.github.springaicommunity.watsonx.autoconfigure.moderation;

import io.github.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import io.github.springaicommunity.watsonx.moderation.WatsonxAiModerationApi;
import io.github.springaicommunity.watsonx.moderation.WatsonxAiModerationModel;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
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
 * @since 1.1.0-SNAPSHOT
 */
@AutoConfiguration(
    after = {
      RestClientAutoConfiguration.class,
      WebClientAutoConfiguration.class,
      SpringAiRetryAutoConfiguration.class
    })
@ConditionalOnClass(WatsonxAiModerationApi.class)
@ConditionalOnProperties({
  @ConditionalOnProperty(name = WatsonxAiConnectionProperties.CONFIG_PREFIX + ".apiKey"),
  @ConditionalOnProperty(name = WatsonxAiConnectionProperties.CONFIG_PREFIX + ".projectId")
})
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
