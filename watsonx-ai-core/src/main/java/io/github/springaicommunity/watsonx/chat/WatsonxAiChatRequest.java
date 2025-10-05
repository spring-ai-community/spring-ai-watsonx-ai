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

package io.github.springaicommunity.watsonx.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.message.TextChatMessage;
import io.github.springaicommunity.watsonx.chat.util.ToolType;
import java.util.List;

/**
 * Request for the Watsonx AI Chat API. Full documentation can be found at <a
 * href=https://cloud.ibm.com/apidocs/watsonx-ai#text-chat-request>watsonx.ai Chat Request</a>.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiChatRequest(
    @JsonProperty("model_id") String modelId,
    @JsonProperty("project_id") String projectId,
    @JsonProperty("tool_choice_option") String toolChoiceOption,
    @JsonProperty("messages") List<TextChatMessage> messages,
    @JsonProperty("tool_choice") List<TextChatToolChoiceTool> toolChoice,
    @JsonProperty("tools") List<TextChatParameterTool> tools) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record TextChatParameterTool(
      @JsonProperty("type") ToolType type,
      @JsonProperty("function") TextChatParameterFunction function) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record TextChatParameterFunction(
      @JsonProperty("name") String name, @JsonProperty("description") String description) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record TextChatToolChoiceTool(
      @JsonProperty("type") ToolType type,
      @JsonProperty("function") TextChatToolChoiceFunction function) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record TextChatToolChoiceFunction(@JsonProperty("name") String name) {}
}
