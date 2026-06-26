/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.watsonx.chat;

import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest.TextChatParameterFunction;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest.TextChatParameterTool;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springaicommunity.watsonx.chat.message.TextChatMessage.TextChatFunctionCall;
import org.springaicommunity.watsonx.chat.message.user.TextChatUserContent;
import org.springaicommunity.watsonx.chat.util.JsonArgumentsNormalizer;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springaicommunity.watsonx.chat.util.audio.AudioFormat;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * {@link ChatModel} and {@link org.springframework.ai.chat.model.StreamingChatModel}
 * implementation that provides access to watsonx supported language models.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

	private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

	private final WatsonxAiChatApi watsonxAiChatApi;

	private final WatsonxAiChatOptions defaultOptions;

	private final ObservationRegistry observationRegistry;

	private final ToolCallingManager toolCallingManager;

	private final RetryTemplate retryTemplate;

	private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	private WatsonxAiChatModel(final WatsonxAiChatApi watsonxAiChatApi, final WatsonxAiChatOptions defaultOptions,
			final ObservationRegistry observationRegistry, final ToolCallingManager toolCallingManager,
			final RetryTemplate retryTemplate) {
		Assert.notNull(watsonxAiChatApi, "Watsonx.ai Chat API must not be null");
		Assert.notNull(defaultOptions, "Default watsonx.ai Chat options must not be null");
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");

		this.watsonxAiChatApi = watsonxAiChatApi;
		this.defaultOptions = defaultOptions;
		this.toolCallingManager = toolCallingManager;
		this.observationRegistry = observationRegistry;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public ChatResponse call(Prompt prompt) {

		Prompt requestPrompt = buildRequestPrompt(prompt);
		return this.internalCall(requestPrompt, null);
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {

		Prompt requestPrompt = buildRequestPrompt(prompt);
		return internalStream(requestPrompt);
	}

	private ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
		WatsonxAiChatRequest createRequest = createRequest(prompt);

		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider("watsonx-ai")
			.build();

		ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				ResponseEntity<WatsonxAiChatResponse> completionEntity = RetryUtils.execute(this.retryTemplate,
						() -> this.watsonxAiChatApi.chat(createRequest));

				var chatCompletion = completionEntity.getBody();

				if (chatCompletion == null) {
					logger.warn("No chat completion returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

				List<WatsonxAiChatResponse.TextChatResultChoice> choices = chatCompletion.choices();
				if (choices == null) {
					logger.warn("No choices returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

				List<Generation> generations = choices.stream().map(choice -> {
					Map<String, Object> metadata = Map.of("id", chatCompletion.id() != null ? chatCompletion.id() : "",
							"role", choice.message().role() != null ? choice.message().role().name() : "", "index",
							choice.index() != null ? choice.index() : 0, "finishReason",
							choice.finishReason() != null ? choice.finishReason() : "", "refusal",
							StringUtils.hasText(choice.message().refusal()) ? choice.message().refusal() : "");
					return buildGeneration(choice, metadata, createRequest);
				}).toList();

				// Current usage
				WatsonxAiChatResponse.TextChatUsage usage = chatCompletion.usage();
				Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
				Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage,
						previousChatResponse);
				ChatResponse chatResponse = new ChatResponse(generations, from(chatCompletion, accumulatedUsage));

				observationContext.setResponse(chatResponse);

				return chatResponse;
			});

		return response;
	}

	private Flux<ChatResponse> internalStream(Prompt prompt) {
		return Flux.deferContextual(contextView -> {
			WatsonxAiChatRequest request = createRequest(prompt);

			Flux<WatsonxAiChatStream> completionChunks = this.watsonxAiChatApi.stream(request);

			final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
				.prompt(prompt)
				.provider("watsonx-ai")
				.build();

			ConcurrentHashMap<String, String> roleTracker = new ConcurrentHashMap<>();

			Flux<ChatResponse> chatResponse = completionChunks.map(chatCompletion -> {
				try {
					String id = chatCompletion.id() == null ? "NO_ID" : chatCompletion.id();

					List<Generation> generations = chatCompletion.choices() != null
							? chatCompletion.choices().stream().map(choice -> {
								if (choice.delta().role() != null) {
									roleTracker.putIfAbsent(id, choice.delta().role().name());
								}

								Map<String, Object> metadata = Map.of("id", id, "role",
										roleTracker.getOrDefault(id, ""), "index",
										choice.index() != null ? choice.index() : 0, "finishReason",
										choice.finishReason() != null ? choice.finishReason() : "", "refusal",
										StringUtils.hasText(choice.delta().refusal()) ? choice.delta().refusal() : "");

								return buildGenerationFromStream(choice, metadata, request);
							}).toList() : List.of();

					WatsonxAiChatResponse.TextChatUsage usage = chatCompletion.usage();
					Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();

					return new ChatResponse(generations, fromStream(chatCompletion, currentChatResponseUsage));
				}
				catch (Exception e) {
					logger.error("Error processing chat completion", e);
					return new ChatResponse(List.of());
				}
			});

			return new MessageAggregator().aggregate(chatResponse, observationContext::setResponse);
		});
	}

	private Generation buildGeneration(WatsonxAiChatResponse.TextChatResultChoice choice, Map<String, Object> metadata,
			WatsonxAiChatRequest request) {

		// Only process tool calls when finish reason is "tool_calls"
		List<AssistantMessage.ToolCall> toolCalls = List.of();
		if ("tool_calls".equals(choice.finishReason()) && choice.message().toolCalls() != null) {
			toolCalls = choice.message().toolCalls().stream().map(toolCall -> {
				return new AssistantMessage.ToolCall(toolCall.id(), "function", toolCall.function().name(),
						JsonArgumentsNormalizer.normalize(toolCall.function().arguments()));
			}).toList();
		}

		var generationMetadataBuilder = ChatGenerationMetadata.builder()
			.finishReason(choice.finishReason() != null ? choice.finishReason() : "");
		if (Boolean.TRUE.equals(request.logprobs())) {
			generationMetadataBuilder.metadata("logprobs", choice.logprobs());
		}

		List<Media> media = new ArrayList<>();
		String textContent = choice.message().content();

		var assistantMessage = AssistantMessage.builder()
			.content(textContent)
			.properties(metadata)
			.toolCalls(toolCalls)
			.media(media)
			.build();
		return new Generation(assistantMessage, generationMetadataBuilder.build());
	}

	private Generation buildGenerationFromStream(WatsonxAiChatStream.TextChatResultChoiceStream choice,
			Map<String, Object> metadata, WatsonxAiChatRequest request) {

		// Only process tool calls when finish reason is "tool_calls"
		// Note: Normalization is already done in WatsonxAiChatChunkMerger for streaming
		List<AssistantMessage.ToolCall> toolCalls = List.of();
		if ("tool_calls".equals(choice.finishReason()) && choice.delta().toolCalls() != null) {
			toolCalls = choice.delta()
				.toolCalls()
				.stream()
				.map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), "function", toolCall.function().name(),
						JsonArgumentsNormalizer.normalize(toolCall.function().arguments())))
				.toList();
		}

		var generationMetadataBuilder = ChatGenerationMetadata.builder()
			.finishReason(choice.finishReason() != null ? choice.finishReason() : "");
		if (Boolean.TRUE.equals(request.logprobs())) {
			generationMetadataBuilder.metadata("logprobs", choice.logprobs());
		}

		List<Media> media = new ArrayList<>();
		String textContent = choice.delta().content();

		var assistantMessage = AssistantMessage.builder()
			.content(textContent)
			.properties(metadata)
			.toolCalls(toolCalls)
			.media(media)
			.build();
		return new Generation(assistantMessage, generationMetadataBuilder.build());
	}

	private WatsonxAiChatRequest createRequest(Prompt prompt) {
		List<TextChatMessage> chatMessages = prompt.getInstructions().stream().map(message -> {
			if (MessageType.ASSISTANT.equals(message.getMessageType())) {
				var assistantMessage = (AssistantMessage) message;

				if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
					List<TextChatMessage.TextChatToolCall> toolCalls = assistantMessage.getToolCalls()
						.stream()
						.map(toolCall -> new TextChatMessage.TextChatToolCall(toolCall.id(), ToolType.FUNCTION,
								new TextChatFunctionCall(toolCall.name(), toolCall.arguments())))
						.toList();

					return List.of(new TextChatMessage(assistantMessage.getText(), null, null, toolCalls));
				}
				return List.of(new TextChatMessage(assistantMessage.getText(), null));
			}
			else if (MessageType.SYSTEM.equals(message.getMessageType())) {

				return List.of(new TextChatMessage(message.getText(), null));
			}
			else if (MessageType.TOOL.equals(message.getMessageType())) {
				var toolResponseMessage = (ToolResponseMessage) message;

				toolResponseMessage.getResponses()
					.forEach(response -> Assert.isTrue(Objects.nonNull(response.id()),
							"Tool response id must not be null"));

				return toolResponseMessage.getResponses()
					.stream()
					.map(toolResponse -> new TextChatMessage(toolResponse.responseData(), toolResponse.name(),
							toolResponse.id()))
					.toList();
			}
			else if (MessageType.USER.equals(message.getMessageType())) {
				var userMessage = (UserMessage) message;
				Object content = userMessage.getText();
				if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
					List<TextChatUserContent> contentList = new ArrayList<>(
							List.of(new TextChatUserContent((String) content)));

					contentList.addAll(userMessage.getMedia().stream().map(this::mapToUserMediaContent).toList());

					content = contentList;
				}

				return List.of(new TextChatMessage(content, null));
			}

			throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
		}).flatMap(List::stream).toList();

		WatsonxAiChatOptions requestOptions = (WatsonxAiChatOptions) Objects.requireNonNull(prompt.getOptions());

		WatsonxAiChatRequest.Builder requestBuilder = WatsonxAiChatRequest.builder()
			.messages(chatMessages)
			.model(requestOptions.getModel())
			.temperature(requestOptions.getTemperature())
			.topP(requestOptions.getTopP())
			.maxTokens(requestOptions.getMaxTokens())
			.n(requestOptions.getN())
			.presencePenalty(requestOptions.getPresencePenalty())
			.frequencyPenalty(requestOptions.getFrequencyPenalty())
			.stopSequences(requestOptions.getStopSequences())
			.logprobs(requestOptions.getLogprobs())
			.topLogprobs(requestOptions.getTopLogprobs())
			.responseFormat(requestOptions.getResponseFormat())
			.reasoningEffort(requestOptions.getReasoningEffort());

		List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
		if (!CollectionUtils.isEmpty(toolDefinitions)) {
			requestBuilder.tools(this.getFunctionTools(toolDefinitions));
		}

		return requestBuilder.build();
	}

	private Prompt buildRequestPrompt(Prompt prompt) {
		if (prompt.getOptions() == null) {
			return prompt.mutate().chatOptions(this.getOptions()).build();
		}
		return prompt;
	}

	private TextChatUserContent mapToUserMediaContent(Media media) {
		var mimeType = media.getMimeType();

		if (MimeTypeUtils.parseMimeType("audio/mp3").equals(mimeType)) {
			return new TextChatUserContent(
					new TextChatUserContent.TextChatUserInputAudio(fromAudioData(media.getData()), AudioFormat.MP3),
					null);
		}
		if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
			return new TextChatUserContent(
					new TextChatUserContent.TextChatUserInputAudio(fromAudioData(media.getData()), AudioFormat.WAV),
					null);
		}
		if (mimeType.getType().equals("video")) {
			return new TextChatUserContent(new TextChatUserContent.TextChatUserVideoUrl(
					this.fromMediaData(media.getMimeType(), media.getData())), null);
		}

		if (mimeType.getType().equals("image")) {
			return new TextChatUserContent(new TextChatUserContent.TextChatUserImageUrl(
					this.fromMediaData(media.getMimeType(), media.getData())), null);
		}

		throw new IllegalArgumentException("Unsupported user media type: " + mimeType);
	}

	private String fromMediaData(MimeType mimeType, Object mediaContentData) {
		if (mediaContentData instanceof String text) {
			return text;
		}

		if (mediaContentData instanceof byte[] bytes) {
			// This is mainly used for image and video URL content.
			return String.format("%s;base64,%s", mimeType.toString(), Base64.getEncoder().encodeToString(bytes));
		}

		throw new IllegalArgumentException(
				"Unsupported media content data type: " + mediaContentData.getClass().getName());
	}

	private String fromAudioData(Object audioData) {
		if (audioData instanceof byte[] bytes) {
			return Base64.getEncoder().encodeToString(bytes);
		}

		throw new IllegalArgumentException("Unsupported audio content data type: " + audioData.getClass().getName());
	}

	private List<WatsonxAiChatRequest.TextChatParameterTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
		var jsonHelper = new JsonHelper();
		return toolDefinitions.stream().map(toolDefinition -> {
			var parameters = jsonHelper.fromJsonToMap(toolDefinition.inputSchema());
			return new TextChatParameterTool(ToolType.FUNCTION,
					new TextChatParameterFunction(toolDefinition.name(), toolDefinition.description(), parameters));
		}).toList();
	}

	private DefaultUsage getDefaultUsage(WatsonxAiChatResponse.TextChatUsage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
	}

	private ChatResponseMetadata from(WatsonxAiChatResponse result, Usage usage) {
		Assert.notNull(result, "WatsonxAi ChatResponse must not be null");
		ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder()
			.id(result.id() != null ? result.id() : "")
			.usage(usage)
			.model(result.model() != null ? result.model() : "")
			.keyValue("created", result.created() != null ? result.created() : 0)
			.keyValue("model_version", result.modelVersion() != null ? result.modelVersion() : "");

		// Add warnings if present
		if (result.system() != null && result.system().warnings() != null) {
			builder.keyValue("warnings", result.system().warnings());
		}

		return builder.build();
	}

	private ChatResponseMetadata fromStream(WatsonxAiChatStream result, Usage usage) {
		Assert.notNull(result, "WatsonxAi ChatStream must not be null");
		ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder()
			.id(result.id() != null ? result.id() : "")
			.usage(usage)
			.model(result.model() != null ? result.model() : "")
			.keyValue("created", result.created() != null ? result.created() : 0)
			.keyValue("model_version", result.modelVersion() != null ? result.modelVersion() : "");

		// Add warnings if present
		if (result.system() != null && result.system().warnings() != null) {
			builder.keyValue("warnings", result.system().warnings());
		}

		return builder.build();
	}

	@Override
	public WatsonxAiChatOptions getOptions() {
		return this.defaultOptions;
	}

	@Override
	@Deprecated(forRemoval = true)
	public WatsonxAiChatOptions getDefaultOptions() {
		return getOptions();
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

		@Deprecated(since = "2.0.0", forRemoval = true)
		public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
			this.toolCallingManager = toolCallingManager;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public WatsonxAiChatModel build() {
			return new WatsonxAiChatModel(this.watsonxAiChatApi, this.options, this.observationRegistry,
					this.toolCallingManager, this.retryTemplate);
		}

	}

}
