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

package io.github.springaicommunity.watsonx.autconfigure.chat;

import io.github.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import io.github.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures watsonx.ai services as part of Spring AI.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(WatsonxAiChatApi.class)
@ConditionalOnProperty(
    name = SpringAIModelProperties.CHAT_MODEL,
    havingValue = WatsonxAiChatAutoConfiguration.MODEL_ID,
    matchIfMissing = true)
@EnableConfigurationProperties({WatsonxAiConnectionProperties.class, WatsonxAiChatProperties.class})
public class WatsonxAiChatAutoConfiguration {
  public static final String MODEL_ID = "watsonx-ai";

  @Bean
  @ConditionalOnMissingBean
  public WatsonxAiChatModel watsonxAiChatModel(
      WatsonxAiConnectionProperties connectionProperties, WatsonxAiChatApi watsonxAiChatApi) {
    return new WatsonxAiChatModel();
  }
}
