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

import java.util.List;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Chat properties for watsonx.ai.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiChatProperties.CONFIG_PREFIX)
public final class WatsonxAiChatProperties {
  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.chat";

  private String textEndpoint = "/ml/v1/text/chat";

  private String streamEndpoint = "/ml/v1/text/chat_stream";

  /**
   * API version date to use, in YYYY-MM-DD format. Example: 2024-10-17. See the <a
   * href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API versioning</a>
   */
  private String version = "2024-10-17";

  /**
   * The default options to use when calling the watsonx.ai Chat API. These can be overridden by
   * passing options in the request.
   */
  @NestedConfigurationProperty
  private WatsonxAiChatOptions options =
      WatsonxAiChatOptions.builder()
          .model("ibm/granite-3-3-8b-instruct")
          .temperature(0.7)
          .topP(1.0)
          .maxCompletionTokens(1024)
          .presencePenalty(0.0)
          .stopSequences(List.of())
          .logProbs(false)
          .n(1)
          .build();

  public String getTextEndpoint() {
    return this.textEndpoint;
  }

  public void setTextEndpoint(String textEndpoint) {
    this.textEndpoint = textEndpoint;
  }

  public String getStreamEndpoint() {
    return this.streamEndpoint;
  }

  public void setStreamEndpoint(String streamEndpoint) {
    this.streamEndpoint = streamEndpoint;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public WatsonxAiChatOptions getOptions() {
    return this.options;
  }

  public void setOptions(WatsonxAiChatOptions options) {
    this.options = options;
  }
}
