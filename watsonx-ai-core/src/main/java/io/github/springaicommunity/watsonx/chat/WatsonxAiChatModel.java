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

import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/**
 * {@class ChatModel} and {@class org.springframework.ai.chat.model.StreamingChatModel}
 * implementation that provides access to watsonx supported language models.
 *
 * <p>TODO: add more explanation
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiChatModel implements ChatModel {

  private final WatsonxAiChatApi watsonxAiChatApi;
  private final WatsonxAiChatOptions defaulWatsonxAiChatOptions;

  public WatsonxAiChatModel(final WatsonxAiChatApi watsonxAiChatApi) {
    this(
        watsonxAiChatApi,
        WatsonxAiChatOptions.builder()
            .temperature(0.7)
            .topP(1.0)
            .maxTokens(1024)
            .presencePenalty(0.0)
            .stopSequences(List.of())
            .logProbs(false)
            .chatCompletions(1)
            .build());
  }

  public WatsonxAiChatModel(
      final WatsonxAiChatApi watsonxAiChatApi,
      final WatsonxAiChatOptions defaulWatsonxAiChatOptions) {
    Assert.notNull(watsonxAiChatApi, "Watsonx.ai Chat API must not be null");
    Assert.notNull(defaulWatsonxAiChatOptions, "Default watsonx.ai Chat options must not be null");

    this.watsonxAiChatApi = watsonxAiChatApi;
    this.defaulWatsonxAiChatOptions = defaulWatsonxAiChatOptions;
  }

  @Override
  public ChatResponse call(Prompt prompt) {

    var requestPrompt = buildPrompt(prompt);

    return null;
  }

  @Override
  public Flux<ChatResponse> stream(Prompt prompt) {

    var requestPrompt = buildPrompt(prompt);

    return null;
  }

  private static Prompt buildPrompt(final Prompt prompt) {
    WatsonxAiChatOptions runtimeOptions = WatsonxAiChatOptions.builder().build();
    final ChatOptions promptOptions = prompt.getOptions();

    if (Objects.nonNull(promptOptions)) {
      if (promptOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
        runtimeOptions =
            ModelOptionsUtils.copyToTarget(
                toolCallingChatOptions, ToolCallingChatOptions.class, WatsonxAiChatOptions.class);
      } else {
        runtimeOptions =
            ModelOptionsUtils.copyToTarget(
                promptOptions, ChatOptions.class, WatsonxAiChatOptions.class);
      }
    }

    final WatsonxAiChatOptions requestOptions =
        ModelOptionsUtils.merge(
            runtimeOptions, this.defaulWatsonxAiChatOptions, WatsonxAiChatOptions.class);

    if (Objects.nonNull(runtimeOptions)) {
      requestOptions.setInternalToolExecutionEnabled(
          ModelOptionsUtils.mergeOption(
              runtimeOptions.getInternalToolExecutionEnabled(),
              this.defaulWatsonxAiChatOptions.getInternalToolExecutionEnabled()));
      requestOptions.setToolNames(
          ToolCallingChatOptions.mergeToolNames(
              runtimeOptions.getToolNames(), this.defaulWatsonxAiChatOptions.getToolNames()));
      requestOptions.setToolCallbacks(
          ToolCallingChatOptions.mergeToolCallbacks(
              runtimeOptions.getToolCallbacks(),
              this.defaulWatsonxAiChatOptions.getToolCallbacks()));
      requestOptions.setToolContext(
          ToolCallingChatOptions.mergeToolContext(
              runtimeOptions.getToolContext(), this.defaulWatsonxAiChatOptions.getToolContext()));
    } else {

      requestOptions.setInternalToolExecutionEnabled(
          this.defaulWatsonxAiChatOptions.getInternalToolExecutionEnabled());
      requestOptions.setToolNames(this.defaulWatsonxAiChatOptions.getToolNames());
      requestOptions.setToolCallbacks(this.defaulWatsonxAiChatOptions.getToolCallbacks());
      requestOptions.setToolContext(this.defaulWatsonxAiChatOptions.getToolContext());
    }

    ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());

    return new Prompt(prompt.getInstructions(), requestOptions);
  }

  @Override
  public WatsonxAiChatOptions getDefaultOptions() {
    return WatsonxAiChatOptions.fromOptions(this.getDefaultOptions());
  }
}
