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

package org.springaicommunity.watsonx.autoconfigure.chat;

import io.micrometer.observation.ObservationRegistry;
import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
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
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configures watsonx.ai services as part of Spring AI.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
@AutoConfiguration(
    after = {
      RestClientAutoConfiguration.class,
      WebClientAutoConfiguration.class,
      SpringAiRetryAutoConfiguration.class,
      ToolCallingAutoConfiguration.class
    })
@ConditionalOnClass(WatsonxAiChatApi.class)
@ConditionalOnProperty(
    name = SpringAIModelProperties.CHAT_MODEL,
    havingValue = WatsonxAiChatAutoConfiguration.MODEL_ID,
    matchIfMissing = true)
@EnableConfigurationProperties({WatsonxAiConnectionProperties.class, WatsonxAiChatProperties.class})
@ImportAutoConfiguration(
    classes = {
      SpringAiRetryAutoConfiguration.class,
      RestClientAutoConfiguration.class,
      WebClientAutoConfiguration.class,
      ToolCallingAutoConfiguration.class
    })
public class WatsonxAiChatAutoConfiguration {
  public static final String MODEL_ID = "watsonx-ai";

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiChatApi watsonxAiChatApi(
      final WatsonxAiConnectionProperties connectionProperties,
      final WatsonxAiChatProperties chatProperties,
      final ObjectProvider<RestClient.Builder> restClientObjectProvider,
      final ObjectProvider<WebClient.Builder> webClienObjectProvider,
      ResponseErrorHandler responseErrorHandler) {

    return new WatsonxAiChatApi(
        connectionProperties.getBaseUrl(),
        chatProperties.getTextEndpoint(),
        chatProperties.getStreamEndpoint(),
        chatProperties.getVersion(),
        connectionProperties.getProjectId(),
        connectionProperties.getApiKey(),
        restClientObjectProvider.getIfAvailable(RestClient::builder),
        webClienObjectProvider.getIfAvailable(WebClient::builder),
        responseErrorHandler);
  }

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiChatModel watsonxChatModel(
      WatsonxAiChatApi watsonxAiChatApi,
      WatsonxAiChatProperties chatProperties,
      ObjectProvider<ObservationRegistry> observationRegistry,
      ToolCallingManager toolCallingManager,
      RetryTemplate retryTemplate,
      ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate,
      ObjectProvider<ChatModelObservationConvention> observationConvention) {

    var watsonxAiChatModel =
        WatsonxAiChatModel.builder()
            .watsonxAiChatApi(watsonxAiChatApi)
            .options(chatProperties.getOptions())
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .toolCallingManager(toolCallingManager)
            .toolExecutionEligibilityPredicate(
                toolExecutionEligibilityPredicate.getIfUnique(
                    () -> new DefaultToolExecutionEligibilityPredicate()))
            .retryTemplate(retryTemplate)
            .build();

    observationConvention.ifUnique(watsonxAiChatModel::setObservationConvention);

    return watsonxAiChatModel;
  }
}
