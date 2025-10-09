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

import io.github.springaicommunity.watsonx.chat.message.TextChatMessage;
import io.github.springaicommunity.watsonx.chat.message.TextChatMessage.TextChatFunctionCall;
import io.github.springaicommunity.watsonx.chat.message.user.TextChatUserContent;
import io.github.springaicommunity.watsonx.chat.util.ToolType;
import io.github.springaicommunity.watsonx.chat.util.audio.AudioFormat;
import io.micrometer.observation.ObservationRegistry;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
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

  private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION =
      new DefaultChatModelObservationConvention();
  private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER =
      ToolCallingManager.builder().build();

  private final WatsonxAiChatApi watsonxAiChatApi;
  private final WatsonxAiChatOptions defaulWatsonxAiChatOptions;
  private final ObservationRegistry observationRegistry;
  private final ToolCallingManager toolCallingManager;
  private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;
  private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

  public WatsonxAiChatModel(
      final WatsonxAiChatApi watsonxAiChatApi,
      final ObservationRegistry observationRegistry,
      final ToolCallingManager toolCallingManager,
      final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
    this(
        watsonxAiChatApi,
        WatsonxAiChatOptions.builder()
            .temperature(0.7)
            .topP(1.0)
            .maxTokens(1024)
            .presencePenalty(0.0)
            .stopSequences(List.of())
            .logProbs(false)
            .n(1)
            .build(),
        observationRegistry,
        toolCallingManager,
        toolExecutionEligibilityPredicate);
  }

  public WatsonxAiChatModel(
      final WatsonxAiChatApi watsonxAiChatApi,
      final WatsonxAiChatOptions defaulWatsonxAiChatOptions,
      final ObservationRegistry observationRegistry,
      final ToolCallingManager toolCallingManager,
      final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
    Assert.notNull(watsonxAiChatApi, "Watsonx.ai Chat API must not be null");
    Assert.notNull(defaulWatsonxAiChatOptions, "Default watsonx.ai Chat options must not be null");
    Assert.notNull(observationRegistry, "observationRegistry must not be null");
    Assert.notNull(toolCallingManager, "toolCallingManager must not be null");

    this.watsonxAiChatApi = watsonxAiChatApi;
    this.defaulWatsonxAiChatOptions = defaulWatsonxAiChatOptions;
    this.toolCallingManager = toolCallingManager;
    this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
    this.observationRegistry = observationRegistry;
  }

  @Override
  public ChatResponse call(Prompt prompt) {

    var createRequest = createRequest(prompt);

    watsonxAiChatApi.chat(createRequest);
    return null;
  }

  @Override
  public Flux<ChatResponse> stream(Prompt prompt) {

    var createRequest = createRequest(prompt);

    watsonxAiChatApi.stream(createRequest);

    return null;
  }

  private WatsonxAiChatRequest createRequest(Prompt prompt) {

    final Prompt requestPrompt = buildPrompt(prompt);

    List<TextChatMessage> chatMessages =
        requestPrompt.getInstructions().stream()
            .map(
                message -> {
                  if (MessageType.ASSISTANT.equals(message.getMessageType())) {
                    var assistantMessage = (AssistantMessage) message;

                    List<TextChatMessage.TextChatToolCall> toolCalls;
                    if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                      toolCalls =
                          assistantMessage.getToolCalls().stream()
                              .map(
                                  toolCall ->
                                      new TextChatMessage.TextChatToolCall(
                                          toolCall.id(),
                                          ToolType.FUNCTION,
                                          new TextChatFunctionCall(
                                              toolCall.name(), toolCall.arguments())))
                              .toList();

                      return List.of(
                          new TextChatMessage(
                              assistantMessage.getText(),
                              assistantMessage.getMessageType().name(),
                              null,
                              toolCalls));
                    }
                  } else if (MessageType.SYSTEM.equals(message.getMessageType())) {

                    return List.of(
                        new TextChatMessage(message.getText(), message.getMessageType().name()));
                  } else if (MessageType.TOOL.equals(message.getMessageType())) {
                    var toolResponseMessage = (ToolResponseMessage) message;

                    toolResponseMessage
                        .getResponses()
                        .forEach(
                            response ->
                                Assert.isTrue(
                                    Objects.nonNull(response.id()),
                                    "Tool response id must not be null"));

                    return toolResponseMessage.getResponses().stream()
                        .map(
                            toolResponse ->
                                new TextChatMessage(
                                    toolResponse.name(),
                                    toolResponse.responseData(),
                                    toolResponse.id()))
                        .toList();
                  } else if (MessageType.USER.equals(message.getMessageType())) {
                    var userMessage = (UserMessage) message;
                    var content = userMessage.getText();
                    List<Media> userMedia = userMessage.getMedia();
                    if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
                      List<TextChatUserContent> contentList =
                          userMedia.stream()
                              .map(
                                  media -> {
                                    if (media.getMimeType().startsWith("image/")) {
                                      return new TextChatUserContent(
                                          new TextChatUserContent.TextChatUserImageUrl(
                                              media.getUrl()));
                                    } else if (media.getType().startsWith("video/")) {
                                      return new TextChatUserContent(
                                          new TextChatUserContent.TextChatUserVideoUrl(
                                              media.getUrl()));
                                    } else if (media.getType().startsWith("audio/")) {
                                      return new TextChatUserContent(
                                          new TextChatUserContent.TextChatUserInputAudio(
                                              media.getData(), null));
                                    }
                                    throw new IllegalArgumentException(
                                        "Unsupported media type: " + media.getType());
                                  })
                              .toList();
                      // contentList.addAll(
                      //     userMessage.getMedia().stream().map(this::mapToMediaContent).toList());

                      // content = contentList;
                    }

                    return null;
                  }

                  throw new IllegalArgumentException(
                      "Unsupported message type: " + message.getMessageType());
                })
            .flatMap(List::stream)
            .toList();

    return null;
  }

  private Prompt buildPrompt(final Prompt prompt) {
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

  private TextChatUserContent mapToUserMediaContent(Media media) {
    var mimeType = media.getMimeType();

    if (MimeTypeUtils.parseMimeType("audio/mp3").equals(mimeType)) {
      return new TextChatUserContent(
          new TextChatUserContent.TextChatUserInputAudio(
              fromMediaData(media.getData()), AudioFormat.MP3),
          null);
    }
    if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
      return new TextChatUserContent(
          new TextChatUserContent.TextChatUserInputAudio(
              fromMediaData(media.getData()), AudioFormat.MP3),
          null);
    }
    if (MimeTypeUtils.) {
      return new MediaContent(
          new MediaContent.InputFile(
              media.getName(), this.fromMediaData(media.getMimeType(), media.getData())));
    } else {
      return new MediaContent(
          new MediaContent.ImageUrl(this.fromMediaData(media.getMimeType(), media.getData())));
    }
  }

  private String fromMediaData(Object mediaContentData) {
    if (mediaContentData instanceof String text) {

      return text;
    } else if (mediaContentData instanceof URI uri) {

      return uri.getPath();
    }

    throw new IllegalArgumentException(
        "Unsupported media content data type: " + mediaContentData.getClass().getName());
  }

  @Override
  public WatsonxAiChatOptions getDefaultOptions() {
    return WatsonxAiChatOptions.fromOptions(this.getDefaultOptions());
  }

  public void setObservationConvention(ChatModelObservationConvention observationConvention) {
    Assert.notNull(observationConvention, "observationConvention must not be null");
    this.observationConvention = observationConvention;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private WatsonxAiChatApi watsonxAiChatApi;
    private WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().build();
    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
    private ToolCallingManager toolCallingManager = DEFAULT_TOOL_CALLING_MANAGER;
    private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate =
        new DefaultToolExecutionEligibilityPredicate();

    public Builder watsonxAiChatApi(WatsonxAiChatApi watsonxAiChatApi) {
      this.watsonxAiChatApi = watsonxAiChatApi;
      return this;
    }

    public Builder options(WatsonxAiChatOptions options) {
      this.options = options;
      return this;
    }

    public Builder observationRegistry(ObservationRegistry observationRegistry) {
      this.observationRegistry = observationRegistry;
      return this;
    }

    public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
      this.toolCallingManager = toolCallingManager;
      return this;
    }

    public Builder toolExecutionEligibilityPredicate(
        ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
      this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
      return this;
    }

    public WatsonxAiChatModel build() {
      return new WatsonxAiChatModel(
          this.watsonxAiChatApi,
          this.options,
          this.observationRegistry,
          this.toolCallingManager,
          this.toolExecutionEligibilityPredicate);
    }
  }
}
