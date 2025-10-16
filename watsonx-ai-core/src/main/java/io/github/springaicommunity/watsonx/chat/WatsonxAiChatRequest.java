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
import java.util.Map;

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
    @JsonProperty("tools") List<TextChatParameterTool> tools,
    @JsonProperty("frequency_penalty") Double frequencyPenalty, 
    @JsonProperty("logit_bias") Map<String, Number> logitBias,
    @JsonProperty("logprobs") Boolean logprobs,
    @JsonProperty("top_logprobs") Integer topLogprobs,
    @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
    @JsonProperty("max_tokens") Integer maxTokens,
    @JsonProperty("n") Integer n,
    @JsonProperty("presence_penalty") Double presencePenalty,
    @JsonProperty("seed") Integer seed,
    @JsonProperty("stop") List<String> stop,
    @JsonProperty("temperature") Double temperature,
    @JsonProperty("top_p") Double topP,
    @JsonProperty("time_limit") Integer timeLimit) {

  public WatsonxAiChatRequest(List<TextChatMessage> messages) {
    this(null, null, null, messages, null, null);
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatParameterTool(
      @JsonProperty("type") ToolType type,
      @JsonProperty("function") TextChatParameterFunction function) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatParameterFunction(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("parameters") String parameters) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatToolChoiceTool(
      @JsonProperty("type") ToolType type,
      @JsonProperty("function") TextChatToolChoiceFunction function) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatToolChoiceFunction(@JsonProperty("name") String name) {}
}
