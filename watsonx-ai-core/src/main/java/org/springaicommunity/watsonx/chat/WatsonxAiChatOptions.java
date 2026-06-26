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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.chat.util.ReasoningEffortType;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.Assert;

/**
 * Options for watsonx Chat API.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiChatOptions implements ToolCallingChatOptions {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiChatOptions.class);

	@JsonIgnore
	private static final JsonHelper JSON_HELPER = new JsonHelper();

	/**
	 * The temperature of the model. Increasing the temperature will make the model answer
	 * more creatively. (Default: 0.7)
	 */
	@JsonProperty("temperature")
	private Double temperature;

	/**
	 * Works together with top-k. A higher value (e.g., 0.95) will lead to more diverse
	 * text, while a lower value (e.g., 0.2) will generate more focused and conservative
	 * text. (Default: 1.0)
	 */
	@JsonProperty("top_p")
	private Double topP;

	/**
	 * Sets when the LLM should stop. (e.g., ["\n\n\n"]) then when the LLM generates three
	 * consecutive line breaks it will terminate. Stop sequences are ignored until after
	 * the number of tokens that are specified in the Min tokens parameter are generated.
	 */
	@JsonProperty("stop")
	private List<String> stopSequences;

	/**
	 * Positive values penalize new tokens based on whether they appear in the text so
	 * far, increasing the model's likelihood to talk about new topics. Possible values is
	 * in the range of -2 < value < 2. The default is 0
	 */
	@JsonProperty("presence_penalty")
	private Double presencePenalty;

	/**
	 * Positive values penalize new tokens based on their existing frequency in the text
	 * so far, decreasing the model's likelihood to repeat the same line verbatim.
	 * Possible values is in the range of -2 < value < 2. The default is 0
	 */
	@JsonProperty("frequency_penalty")
	private Double frequencyPenalty;

	/**
	 * It specifies guided choice sequences to direct the model's response. The model will
	 * choose from the provided list of options.
	 */
	@JsonProperty("guided_choice")
	private List<String> guidedChoice;

	/** It specifies that the output will folow the given regex pattern. */
	@JsonProperty("guided_regex")
	private String guidedRegex;

	/** It specifies that the output will folow the context free grammar. */
	@JsonProperty("guided_grammar")
	private String guidedGrammar;

	/** It specifies that the out will follow the JSON Schema. */
	@JsonProperty("guided_json")
	private Map<String, Object> guidedJson;

	/** Addtional kwargs to pass to the chat template. It is a JSON Schema object. */
	@JsonProperty("chat_template_kwargs")
	private Map<String, Object> chatTemplateKwargs;

	/**
	 * Determines whether to include reasoning_content in the response Defaults to true.
	 */
	@JsonProperty("include_reasoning")
	private Boolean includeReasoning = Boolean.TRUE;

	/**
	 * Determines the reasoning effort. Lower reasoning effort can result in faster
	 * responses, fewer tokens used and shorter reasoning_content in the responses.
	 * Allowed values: [low, medium, high]
	 */
	@JsonProperty("reasoning_effort")
	private String reasoningEffort;

	/**
	 * Produce repeatable results, set the same random seed value every time. (Default:
	 * randomly generated)
	 */
	@JsonProperty("seed")
	private Integer seed;

	/** Model is the identifier of the LLM Model to be used */
	@JsonProperty("model_id")
	private String model;

	/**
	 * Tools to be used for tool calling in the chat completion requests. Currently, only
	 * function type tools are supported.
	 */
	@JsonProperty("tools")
	private List<WatsonxAiChatRequest.TextChatParameterTool> tools;

	/**
	 * Tool choice option for the chat completion requests. By default, the model decides
	 * which tool to use. Optionally, the user can specify a tool to use.
	 */
	@JsonProperty("tool_choice_option")
	private String toolChoiceOption;

	/**
	 * Forces the model to use a specific tool. Specify either
	 * {@link WatsonxAiChatOptions#toolChoiceOption} to allow the model to choose a tool,
	 * or {@link WatsonxAiChatOptions#toolChoice} to force the model to use a specific
	 * tool.
	 */
	@JsonProperty("tool_choice")
	private WatsonxAiChatRequest.TextChatToolChoiceTool toolChoice;

	/**
	 * Collection of {@link ToolCallback}s to be used for tool calling in the chat
	 * completion requests.
	 */
	@JsonIgnore
	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	@JsonIgnore
	private Map<String, Object> toolContext = new HashMap<>();

	/**
	 * Increasing or decreasing probability of tokens being selected during generation; a
	 * positive bias makes a token more likely to appear, while a negative bias makes it
	 * less likely.
	 */
	@JsonProperty("logit_bias")
	private Map<String, Number> logitBias;

	/**
	 * Whether to return log probabilities of the output tokens or not. If true, returns
	 * the log probabilities of each output token returned in the content of message.
	 */
	@JsonProperty("logprobs")
	private Boolean logprobs;

	/**
	 * An integer specifying the number of most likely tokens to return at each token
	 * position, each with an associated log probability. The option
	 * {@link WatsonxAiChatOptions#logprobs} must be set to true if this parameter is
	 * used.
	 */
	@JsonProperty("top_logprobs")
	private Integer topLogprobs;

	/**
	 * The maximum number of tokens to generate in the chat completion. The total length
	 * of input tokens and generated tokens is limited by the model's context length.
	 * (Default: 1024)
	 *
	 * <p>
	 * This value is now deprecated in favor of
	 * {@link WatsonxAiChatOptions#maxCompletionTokens}. If specified together, with
	 * {@link WatsonxAiChatOptions#maxCompletionTokens}, this will be ignored.
	 */
	@JsonProperty("max_tokens")
	private Integer maxTokens;

	/**
	 * The maximum number of tokens to generate in the chat completion. The total length
	 * of input tokens and generated tokens is limited by the model's context length.
	 * (Default: 1024)
	 */
	@JsonProperty("max_completion_tokens")
	private Integer maxCompletionTokens;

	/**
	 * How many chat completion choices to generate for each input message. Note that you
	 * will be charged based on the number of generated tokens across all of the choices.
	 * Keep n as 1 to minimize costs.
	 */
	@JsonProperty("n")
	private Integer n;

	/**
	 * Time limit in milliseconds - if not completed within this time, generation will
	 * stop. The text generated so far will be returned along with the `TIME_LIMIT`` stop
	 * reason. Depending on the users plan, and on the model being used, there may be an
	 * enforced maximum time limit.
	 *
	 * <p>
	 * Value is expected to be greater than {@code 0}
	 */
	@JsonProperty("time_limit")
	private Integer timeLimit;

	/**
	 * An object specifying the format that the model must output. Setting to { "type":
	 * "json_object" } enables JSON mode, which guarantees the message the model generates
	 * is valid JSON.
	 */
	@JsonProperty("response_format")
	private TextChatResponseFormat responseFormat;

	/** Set additional request params (some model have non-predefined options) */
	@JsonProperty("additional")
	private Map<String, Object> additional = new HashMap<>();

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Filter out the non-supported fields from the options.
	 * @param options The options to filter.
	 * @return The filtered options.
	 */
	@Deprecated(forRemoval = true, since = "1.0.2")
	public static Map<String, Object> filterNonSupportedFields(Map<String, Object> options) {
		return options.entrySet()
			.stream()
			.filter(e -> !e.getKey().equals("model"))
			.filter(e -> e.getValue() != null)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	@Override
	public Double getTopP() {
		return this.topP;
	}

	public void setTopP(Double topP) {
		this.topP = topP;
	}

	@Override
	public Integer getTopK() {
		logger.warn("WatsonX AI doesn't support topK, return null for compatibility");
		return null;
	}

	@Override
	public List<String> getStopSequences() {
		return this.stopSequences;
	}

	public void setStopSequences(List<String> stopSequences) {
		this.stopSequences = stopSequences;
	}

	@Override
	public Double getPresencePenalty() {
		return this.presencePenalty;
	}

	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	@Override
	public Double getFrequencyPenalty() {
		return this.frequencyPenalty;
	}

	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public Integer getSeed() {
		return this.seed;
	}

	public void setSeed(Integer seed) {
		this.seed = seed;
	}

	public List<String> getGuidedChoice() {
		return this.guidedChoice;
	}

	public void setGuidedChoice(List<String> guidedChoice) {
		this.guidedChoice = guidedChoice;
	}

	public String getGuidedRegex() {
		return this.guidedRegex;
	}

	public void setGuidedRegex(String guidedRegex) {
		this.guidedRegex = guidedRegex;
	}

	public String getGuidedGrammar() {
		return this.guidedGrammar;
	}

	public void setGuidedGrammar(String guidedGrammar) {
		this.guidedGrammar = guidedGrammar;
	}

	public Map<String, Object> getGuidedJson() {
		return this.guidedJson;
	}

	public void setGuidedJson(String guidedJson) {
		if (guidedJson != null) {
			this.guidedJson = new JsonHelper().fromJsonToMap(guidedJson);
		}
	}

	public void setGuidedJson(Map<String, Object> guidedJson) {
		this.guidedJson = guidedJson;
	}

	public Map<String, Object> getChatTemplateKwargs() {
		return this.chatTemplateKwargs;
	}

	public void setChatTemplateKwargs(String chatTemplateKwargs) {
		if (chatTemplateKwargs != null) {
			this.chatTemplateKwargs = new JsonHelper().fromJsonToMap(chatTemplateKwargs);
		}
	}

	public void setChatTemplateKwargs(Map<String, Object> chatTemplateKwargs) {
		this.chatTemplateKwargs = chatTemplateKwargs;
	}

	public Boolean isIncludeReasoning() {
		return this.includeReasoning;
	}

	public void setIncludeReasoning(Boolean includeReasoning) {
		this.includeReasoning = includeReasoning;
	}

	public String getReasoningEffort() {
		return this.reasoningEffort;
	}

	public void setReasoningEffort(String reasoningEffort) {
		if (reasoningEffort != null) {
			Assert.isTrue(
					ReasoningEffortType.LOW.getReasoningEffort().equals(reasoningEffort)
							|| ReasoningEffortType.MEDIUM.getReasoningEffort().equals(reasoningEffort)
							|| ReasoningEffortType.HIGH.getReasoningEffort().equals(reasoningEffort),
					"reasoning_effort must be one of [low, medium, high]");
		}
		this.reasoningEffort = reasoningEffort;
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setTools(List<WatsonxAiChatRequest.TextChatParameterTool> tools) {
		this.tools = tools;
	}

	public List<WatsonxAiChatRequest.TextChatParameterTool> getTools() {
		return this.tools;
	}

	public void setToolChoiceOption(String toolChoiceOption) {
		this.toolChoiceOption = toolChoiceOption;
	}

	public String getToolChoiceOption() {
		return this.toolChoiceOption;
	}

	public void setToolChoice(WatsonxAiChatRequest.TextChatToolChoiceTool toolChoice) {
		this.toolChoice = toolChoice;
	}

	public WatsonxAiChatRequest.TextChatToolChoiceTool getToolChoice() {
		return this.toolChoice;
	}

	@Override
	public List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	public void setToolContext(Map<String, Object> toolContext) {
		this.toolContext = toolContext;
	}

	public Map<String, Number> getLogitBias() {
		return this.logitBias;
	}

	public void setLogitBias(Map<String, Number> logitBias) {
		this.logitBias = logitBias;
	}

	public Boolean getLogprobs() {
		return this.logprobs;
	}

	public void setLogprobs(Boolean logprobs) {
		if (Boolean.FALSE.equals(logprobs) && this.topLogprobs != null) {
			throw new IllegalArgumentException("logprobs cannot be false when using topLogprobs.");
		}
		this.logprobs = logprobs;
	}

	public Integer getTopLogprobs() {
		return this.topLogprobs;
	}

	public void setTopLogprobs(Integer topLogprobs) {
		if (topLogprobs != null && this.logprobs != null) {
			Assert.isTrue(this.logprobs, "logprobs cannot be false when using topLogprobs.");
		}
		this.topLogprobs = topLogprobs;
	}

	@Override
	public Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Integer getMaxCompletionTokens() {
		return this.maxCompletionTokens;
	}

	public void setMaxCompletionTokens(Integer maxCompletionTokens) {
		this.maxCompletionTokens = maxCompletionTokens;
	}

	public Integer getN() {
		return this.n;
	}

	public void setN(Integer chatCompletions) {
		this.n = chatCompletions;
	}

	public Integer getTimeLimit() {
		return this.timeLimit;
	}

	public void setTimeLimit(Integer timeLimit) {
		Assert.isTrue(timeLimit > 0, "Time limit must be greater than 0");
		this.timeLimit = timeLimit;
	}

	public TextChatResponseFormat getResponseFormat() {
		return this.responseFormat;
	}

	public void setResponseFormat(TextChatResponseFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additional.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> toSnakeCase(entry.getKey()), Map.Entry::getValue));
	}

	@JsonAnySetter
	public void addAdditionalProperty(String key, Object value) {
		this.additional.put(key, value);
	}

	/**
	 * Convert the {@link WatsonxAiChatOptions} object to a {@link Map} of key/value
	 * pairs.
	 * @return The {@link Map} of key/value pairs.
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = JSON_HELPER.convertToMap(this);
		map.remove("additional");
		return map;
	}

	private String toSnakeCase(String input) {
		return input != null ? input.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase() : null;
	}

	@Override
	public Builder mutate() {
		Builder builder = new Builder().temperature(this.temperature)
			.topP(this.topP)
			.stopSequences(this.stopSequences)
			.presencePenalty(this.presencePenalty)
			.frequencyPenalty(this.frequencyPenalty)
			.guidedChoice(this.guidedChoice)
			.guidedRegex(this.guidedRegex)
			.guidedGrammar(this.guidedGrammar)
			.guidedJson(this.guidedJson)
			.chatTemplateKwargs(this.chatTemplateKwargs)
			.includeReasoning(this.includeReasoning)
			.reasoningEffort(this.reasoningEffort)
			.seed(this.seed)
			.model(this.model)
			.tools(this.tools)
			.toolChoiceOption(this.toolChoiceOption)
			.toolChoice(this.toolChoice)
			.toolCallbacks(this.toolCallbacks)
			.toolContext(this.toolContext)
			.logitBias(this.logitBias)
			.logProbs(this.logprobs)
			.maxTokens(this.maxTokens)
			.maxCompletionTokens(this.maxCompletionTokens)
			.n(this.n)
			.responseFormat(this.responseFormat)
			.additionalProperties(this.additional);

		if (this.logprobs != null && this.logprobs && this.topLogprobs != null) {
			builder.topLogprobs(this.topLogprobs);
		}

		return builder;
	}

	@Override
	public String toString() {
		return "WatsonxAiChatOptions: " + new JsonHelper().toJson(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WatsonxAiChatOptions other = (WatsonxAiChatOptions) o;
		return Objects.equals(this.temperature, other.temperature) && Objects.equals(this.topP, other.topP)
				&& Objects.equals(this.stopSequences, other.stopSequences)
				&& Objects.equals(this.presencePenalty, other.presencePenalty)
				&& Objects.equals(this.frequencyPenalty, other.frequencyPenalty)
				&& Objects.equals(this.guidedChoice, other.guidedChoice)
				&& Objects.equals(this.guidedRegex, other.guidedRegex)
				&& Objects.equals(this.guidedGrammar, other.guidedGrammar)
				&& Objects.equals(this.guidedJson, other.guidedJson)
				&& Objects.equals(this.chatTemplateKwargs, other.chatTemplateKwargs)
				&& Objects.equals(this.includeReasoning, other.includeReasoning)
				&& Objects.equals(this.reasoningEffort, other.reasoningEffort) && Objects.equals(this.seed, other.seed)
				&& Objects.equals(this.model, other.model) && Objects.equals(this.tools, other.tools)
				&& Objects.equals(this.toolChoiceOption, other.toolChoiceOption)
				&& Objects.equals(this.toolChoice, other.toolChoice)
				&& Objects.equals(this.toolCallbacks, other.toolCallbacks)
				&& Objects.equals(this.toolContext, other.toolContext)
				&& Objects.equals(this.logitBias, other.logitBias) && Objects.equals(this.logprobs, other.logprobs)
				&& Objects.equals(this.topLogprobs, other.topLogprobs)
				&& Objects.equals(this.maxTokens, other.maxTokens)
				&& Objects.equals(this.maxCompletionTokens, other.maxCompletionTokens)
				&& Objects.equals(this.n, other.n) && Objects.equals(this.timeLimit, other.timeLimit)
				&& Objects.equals(this.responseFormat, other.responseFormat)
				&& Objects.equals(this.additional, other.additional);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.temperature, this.topP, this.stopSequences, this.presencePenalty,
				this.frequencyPenalty, this.guidedChoice, this.guidedRegex, this.guidedGrammar, this.guidedJson,
				this.chatTemplateKwargs, this.includeReasoning, this.reasoningEffort, this.seed, this.model, this.tools,
				this.toolChoiceOption, this.toolChoice, this.toolCallbacks, this.toolContext, this.logitBias,
				this.logprobs, this.topLogprobs, this.maxTokens, this.maxCompletionTokens, this.n, this.timeLimit,
				this.responseFormat, this.additional);
	}

	public static class Builder implements ToolCallingChatOptions.Builder<Builder> {

		WatsonxAiChatOptions options = new WatsonxAiChatOptions();

		@Override
		public Builder clone() {
			try {
				return (Builder) super.clone();
			}
			catch (CloneNotSupportedException e) {
				return mutate();
			}
		}

		private Builder mutate() {
			Builder b = new Builder();
			b.options = this.build().mutate().build();
			return b;
		}

		@Override
		public Builder model(String model) {
			this.options.model = model;
			return this;
		}

		@Override
		public Builder frequencyPenalty(Double frequencyPenalty) {
			this.options.frequencyPenalty = frequencyPenalty;
			return this;
		}

		@Override
		public Builder maxTokens(Integer maxTokens) {
			this.options.maxTokens = maxTokens;
			return this;
		}

		@Override
		public Builder presencePenalty(Double presencePenalty) {
			this.options.presencePenalty = presencePenalty;
			return this;
		}

		@Override
		public Builder stopSequences(List<String> stopSequences) {
			this.options.stopSequences = stopSequences;
			return this;
		}

		@Override
		public Builder temperature(Double temperature) {
			this.options.temperature = temperature;
			return this;
		}

		@Override
		public Builder topK(Integer topK) {
			return this;
		}

		@Override
		public Builder topP(Double topP) {
			this.options.topP = topP;
			return this;
		}

		@Override
		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.options.toolCallbacks = toolCallbacks != null ? toolCallbacks : new ArrayList<>();
			return this;
		}

		@Override
		public Builder toolCallbacks(ToolCallback... toolCallbacks) {
			Assert.notNull(toolCallbacks, "Tool Callbacks cannot be null");
			Assert.noNullElements(toolCallbacks, "Tool Callbacks cannot have null ones");
			this.options.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
			return this;
		}

		@Override
		public Builder toolContext(Map<String, Object> toolContext) {
			if (toolContext == null) {
				this.options.toolContext = new HashMap<>();
			}
			else if (this.options.toolContext == null) {
				this.options.toolContext = toolContext;
			}
			else {
				this.options.toolContext.putAll(toolContext);
			}
			return this;
		}

		@Override
		public Builder toolContext(String key, Object value) {
			if (this.options.toolContext == null) {
				this.options.toolContext = new HashMap<>();
			}
			this.options.toolContext.put(key, value);
			return this;
		}

		@Override
		public Builder combineWith(ChatOptions.Builder<?> other) {
			if (other != null) {
				WatsonxAiChatOptions otherOpts = ((Builder) other).build();
				if (this.options.model == null)
					this.options.model = otherOpts.model;
				if (this.options.temperature == null)
					this.options.temperature = otherOpts.temperature;
				if (this.options.topP == null)
					this.options.topP = otherOpts.topP;
				if (this.options.maxTokens == null)
					this.options.maxTokens = otherOpts.maxTokens;
				if (this.options.presencePenalty == null)
					this.options.presencePenalty = otherOpts.presencePenalty;
				if (this.options.frequencyPenalty == null)
					this.options.frequencyPenalty = otherOpts.frequencyPenalty;
				if (this.options.stopSequences == null)
					this.options.stopSequences = otherOpts.stopSequences;
			}
			return this;
		}

		public Builder guidedChoice(List<String> guidedChoice) {
			this.options.guidedChoice = guidedChoice;
			return this;
		}

		public Builder guidedRegex(String guidedRegex) {
			this.options.guidedRegex = guidedRegex;
			return this;
		}

		public Builder guidedGrammar(String guidedGrammar) {
			this.options.guidedGrammar = guidedGrammar;
			return this;
		}

		public Builder guidedJson(String guidedJson) {
			if (guidedJson != null) {
				this.options.guidedJson = JSON_HELPER.fromJsonToMap(guidedJson);
			}
			return this;
		}

		public Builder guidedJson(Map<String, Object> guidedJson) {
			this.options.guidedJson = guidedJson;
			return this;
		}

		public Builder chatTemplateKwargs(String chatTemplateKwargs) {
			if (chatTemplateKwargs != null) {
				this.options.chatTemplateKwargs = JSON_HELPER.fromJsonToMap(chatTemplateKwargs);
			}
			return this;
		}

		public Builder chatTemplateKwargs(Map<String, Object> chatTemplateKwargs) {
			this.options.chatTemplateKwargs = chatTemplateKwargs;
			return this;
		}

		public Builder includeReasoning(Boolean includeReasoning) {
			this.options.includeReasoning = includeReasoning;
			return this;
		}

		public Builder reasoningEffort(String reasoningEffort) {
			this.options.reasoningEffort = reasoningEffort;
			return this;
		}

		public Builder seed(Integer seed) {
			this.options.seed = seed;
			return this;
		}

		public Builder tools(List<WatsonxAiChatRequest.TextChatParameterTool> tools) {
			this.options.tools = tools;
			return this;
		}

		public Builder toolChoiceOption(String toolChoiceOption) {
			this.options.toolChoiceOption = toolChoiceOption;
			return this;
		}

		public Builder toolChoice(WatsonxAiChatRequest.TextChatToolChoiceTool toolChoice) {
			this.options.toolChoice = toolChoice;
			return this;
		}

		public Builder logitBias(Map<String, Number> logitBias) {
			this.options.logitBias = logitBias;
			return this;
		}

		public Builder logProbs(Boolean logprobs) {
			this.options.logprobs = logprobs;
			return this;
		}

		public Builder topLogprobs(Integer topLogprobs) {
			if (topLogprobs != null) {
				Assert.notNull(this.options.getLogprobs(), "logprobs cannot be null when using topLogprobs.");
				Assert.isTrue(this.options.getLogprobs(), "logprobs cannot be false when using topLogprobs.");
			}
			this.options.topLogprobs = topLogprobs;
			return this;
		}

		public Builder maxCompletionTokens(Integer maxCompletionTokens) {
			this.options.maxCompletionTokens = maxCompletionTokens;
			return this;
		}

		public Builder n(Integer n) {
			this.options.n = n;
			return this;
		}

		public Builder responseFormat(TextChatResponseFormat responseFormat) {
			this.options.responseFormat = responseFormat;
			return this;
		}

		public Builder additionalProperty(String key, Object value) {
			this.options.additional.put(key, value);
			return this;
		}

		public Builder additionalProperties(Map<String, Object> properties) {
			this.options.additional.putAll(properties);
			return this;
		}

		@Override
		public WatsonxAiChatOptions build() {
			return this.options;
		}

	}

}
