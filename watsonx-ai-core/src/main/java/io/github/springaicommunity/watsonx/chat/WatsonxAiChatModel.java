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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
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

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiChatModel.class);

  private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION =
      new DefaultChatModelObservationConvention();
  private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER =
      ToolCallingManager.builder().build();

  private final WatsonxAiChatApi watsonxAiChatApi;
  private final WatsonxAiChatOptions defaulWatsonxAiChatOptions;
  private final ObservationRegistry observationRegistry;
  private final ToolCallingManager toolCallingManager;
  private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;
  private final RetryTemplate retryTemplate;
  private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

  public WatsonxAiChatModel(
      final WatsonxAiChatApi watsonxAiChatApi,
      final ObservationRegistry observationRegistry,
      final ToolCallingManager toolCallingManager,
      final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate,
      final RetryTemplate retryTemplate) {
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
        toolExecutionEligibilityPredicate,
        retryTemplate);
  }

  public WatsonxAiChatModel(
      final WatsonxAiChatApi watsonxAiChatApi,
      final WatsonxAiChatOptions defaulWatsonxAiChatOptions,
      final ObservationRegistry observationRegistry,
      final ToolCallingManager toolCallingManager,
      final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate,
      final RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiChatApi, "Watsonx.ai Chat API must not be null");
    Assert.notNull(defaulWatsonxAiChatOptions, "Default watsonx.ai Chat options must not be null");
    Assert.notNull(observationRegistry, "observationRegistry must not be null");
    Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
    Assert.notNull(
        toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate must not be null");
    Assert.notNull(retryTemplate, "retryTemplate must not be null");

    this.watsonxAiChatApi = watsonxAiChatApi;
    this.defaulWatsonxAiChatOptions = defaulWatsonxAiChatOptions;
    this.toolCallingManager = toolCallingManager;
    this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
    this.observationRegistry = observationRegistry;
    this.retryTemplate = retryTemplate;
  }

  @Override
  public ChatResponse call(Prompt prompt) {

    watsonxAiChatApi.chat(createRequest);
    return null;
  }

  @Override
  public Flux<ChatResponse> stream(Prompt prompt) {

    var createRequest = createRequest(prompt);

    watsonxAiChatApi.stream(createRequest);

    return null;
  }

  public ChatResponse call(Prompt prompt, ChatResponse previousChatResponse) {
    WatsonxAiChatRequest createRequest = createRequest(prompt);

    ChatModelObservationContext observationContext =
        ChatModelObservationContext.builder().prompt(prompt).provider("watsonx-ai").build();

    ChatResponse response =
        ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
            .observation(
                this.observationConvention,
                DEFAULT_OBSERVATION_CONVENTION,
                () -> observationContext,
                this.observationRegistry)
            .observe(
                () -> {
                  ResponseEntity<WatsonxAiChatResponse> completionEntity =
                      this.retryTemplate.execute(ctx -> this.watsonxAiChatApi.chat(createRequest));

                  var chatCompletion = completionEntity.getBody();

                  if (chatCompletion == null) {
                    logger.warn("No chat completion returned for prompt: {}", prompt);
                    return new ChatResponse(List.of());
                  }

                  List<WatsonxAiChatResponse.TextChatResultChoice> choices =
                      chatCompletion.choices();
                  if (choices == null) {
                    logger.warn("No choices returned for prompt: {}", prompt);
                    return new ChatResponse(List.of());
                  }

                  // @formatter:off
                  List<Generation> generations =
                      choices.stream()
                          .map(
                              choice -> {
                                Map<String, Object> metadata =
                                    Map.of(
                                        "id",
                                            chatCompletion.id() != null ? chatCompletion.id() : "",
                                        "role",
                                            choice.message().role() != null
                                                ? choice.message().role().name()
                                                : "",
                                        "index", choice.index() != null ? choice.index() : 0,
                                        "finishReason", getFinishReasonJson(choice.finishReason()),
                                        "refusal",
                                            StringUtils.hasText(choice.message().refusal())
                                                ? choice.message().refusal()
                                                : "",
                                        "annotations",
                                            choice.message().annotations() != null
                                                ? choice.message().annotations()
                                                : List.of(Map.of()));
                                return buildGeneration(choice, metadata, request);
                              })
                          .toList();
                  // @formatter:on

                  // TODO: Rate limit

                  // Current usage
                  WatsonxAiChatResponse.TextChatUsage usage = chatCompletion.usage();
                  Usage currentChatResponseUsage =
                      usage != null ? getDefaultUsage(usage) : new EmptyUsage();
                  Usage accumulatedUsage =
                      UsageCalculator.getCumulativeUsage(
                          currentChatResponseUsage, previousChatResponse);
                  ChatResponse chatResponse =
                      new ChatResponse(
                          generations, from(chatCompletion, rateLimit, accumulatedUsage));

                  observationContext.setResponse(chatResponse);

                  return chatResponse;
                });
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
                    Object content = userMessage.getText();
                    if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
                      List<TextChatUserContent> contentList =
                          new ArrayList<>(List.of(new TextChatUserContent((String) content)));

                      contentList.addAll(
                          userMessage.getMedia().stream()
                              .map(this::mapToUserMediaContent)
                              .toList());

                      content = contentList;
                    }

                    return List.of(new TextChatMessage(content, message.getMessageType().name()));
                  }

                  throw new IllegalArgumentException(
                      "Unsupported message type: " + message.getMessageType());
                })
            .flatMap(List::stream)
            .toList();

    WatsonxAiChatRequest request = new WatsonxAiChatRequest(chatMessages);

    WatsonxAiChatOptions requestOptions = (WatsonxAiChatOptions) requestPrompt.getOptions();

    request = ModelOptionsUtils.merge(requestOptions, request, WatsonxAiChatRequest.class);

    // Add the tool definitions to the request's tools parameter.
    List<ToolDefinition> toolDefinitions =
        this.toolCallingManager.resolveToolDefinitions(requestOptions);
    if (!CollectionUtils.isEmpty(toolDefinitions)) {
      request =
          ModelOptionsUtils.merge(
              this.getFunctionTools(toolDefinitions), request, WatsonxAiChatRequest.class);
    }

    return request;
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
              fromAudioData(media.getData()), AudioFormat.MP3),
          null);
    }
    if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
      return new TextChatUserContent(
          new TextChatUserContent.TextChatUserInputAudio(
              fromAudioData(media.getData()), AudioFormat.MP3),
          null);
    }
    if (mimeType.getType().startsWith("video/")) {
      return new TextChatUserContent(
          new TextChatUserContent.TextChatUserVideoUrl(
              this.fromMediaData(media.getMimeType(), media.getData())),
          null);
    }

    if (mimeType.getType().startsWith("image/")) {
      return new TextChatUserContent(
          new TextChatUserContent.TextChatUserImageUrl(
              this.fromMediaData(media.getMimeType(), media.getData())),
          null);
    }

    throw new IllegalArgumentException("Unsupported user media type: " + mimeType);
  }

  private String fromMediaData(MimeType mimeType, Object mediaContentData) {
    if (mediaContentData instanceof String text) {

      return text;
    }

    if (mediaContentData instanceof byte[] bytes) {

      // This is mainly used for image and video URL content.
      return String.format(
          "data:%s;base64,%s", mimeType.toString(), Base64.getEncoder().encodeToString(bytes));
    }

    throw new IllegalArgumentException(
        "Unsupported media content data type: " + mediaContentData.getClass().getName());
  }

  private String fromAudioData(Object audioData) {
    if (audioData instanceof byte[] bytes) {

      return Base64.getEncoder().encodeToString(bytes);
    }

    throw new IllegalArgumentException(
        "Unsupported audio content data type: " + audioData.getClass().getName());
  }

  private List<WatsonxAiChatRequest.TextChatParameterTool> getFunctionTools(
      List<ToolDefinition> toolDefinitions) {
    return toolDefinitions.stream()
        .map(
            toolDefinition ->
                new WatsonxAiChatRequest.TextChatParameterTool(
                    ToolType.FUNCTION,
                    new WatsonxAiChatRequest.TextChatParameterFunction(
                        toolDefinition.name(),
                        toolDefinition.description(),
                        toolDefinition.inputSchema())))
        .toList();
  }

  private Generation buildGeneration(WatsonxAiChatResponse.TextChatResultChoice choice, Map<String, Object> metadata, WatsonxAiChatRequest request) {
		List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of()
				: choice.message()
					.toolCalls()
					.stream()
					.map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), "function",
							toolCall.function().name(), toolCall.function().arguments()))
					.toList();

		var generationMetadataBuilder = ChatGenerationMetadata.builder()
			.finishReason(getFinishReasonJson(choice.finishReason()));

		List<Media> media = new ArrayList<>();
		String textContent = choice.message().content();
		var audioOutput = choice.message().audioOutput();
		if (audioOutput != null) {
			String mimeType = String.format("audio/%s", request.audioParameters().format().name().toLowerCase());
			byte[] audioData = Base64.getDecoder().decode(audioOutput.data());
			Resource resource = new ByteArrayResource(audioData);
			Media.builder().mimeType(MimeTypeUtils.parseMimeType(mimeType)).data(resource).id(audioOutput.id()).build();
			media.add(Media.builder()
				.mimeType(MimeTypeUtils.parseMimeType(mimeType))
				.data(resource)
				.id(audioOutput.id())
				.build());
			if (!StringUtils.hasText(textContent)) {
				textContent = audioOutput.transcript();
			}
			generationMetadataBuilder.metadata("audioId", audioOutput.id());
			generationMetadataBuilder.metadata("audioExpiresAt", audioOutput.expiresAt());
		}

		if (Boolean.TRUE.equals(request.())) {
			generationMetadataBuilder.metadata("logprobs", choice.logprobs());
		}

		var assistantMessage = new AssistantMessage(textContent, metadata, toolCalls, media);
		return new Generation(assistantMessage, generationMetadataBuilder.build());
	}

	private String getFinishReasonJson(WatsonxAiChatResponse. finishReason) {
		if (finishReason == null) {
			return "";
		}
		// Return enum name for backward compatibility
		return finishReason.name();
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
    private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

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

    public Builder retryTemplate(RetryTemplate retryTemplate) {
      this.retryTemplate = retryTemplate;
      return this;
    }

    public WatsonxAiChatModel build() {
      return new WatsonxAiChatModel(
          this.watsonxAiChatApi,
          this.options,
          this.observationRegistry,
          this.toolCallingManager,
          this.toolExecutionEligibilityPredicate,
          this.retryTemplate);
    }
  }
}
