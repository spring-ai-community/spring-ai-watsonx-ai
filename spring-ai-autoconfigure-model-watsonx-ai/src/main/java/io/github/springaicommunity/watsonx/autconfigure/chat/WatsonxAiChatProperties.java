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

import io.github.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Chat properties for watsonx.ai Chat.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiChatProperties.CONFIG_PREFIX)
public class WatsonxAiChatProperties {
  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.chat";

  @NestedConfigurationProperty
  private WatsonxAiChatOptions options =
      WatsonxAiChatOptions.builder()
          .model("ibm/granite-3-3-8b-instruct")
          .temperature(0.7)
          .topP(1.0)
          .topK(50)
          .maxTokens(1024)
          .presencePenalty(0.0)
          .stopSequences(List.of())
          .logProbs(false)
          .chatCompletions(1)
          .build();

  public WatsonxAiChatOptions getOptions() {
    return this.options;
  }

  public void setOptions(WatsonxAiChatOptions options) {
    this.options = options;
  }
}
