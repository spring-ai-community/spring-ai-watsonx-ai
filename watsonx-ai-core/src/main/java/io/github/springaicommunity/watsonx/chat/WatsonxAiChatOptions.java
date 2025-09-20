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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Options for watsonx Chat API.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiChatOptions implements ToolCallingChatOptions {
  @JsonIgnore private final ObjectMapper mapper = new ObjectMapper();

  /**
   * The temperature of the model. Increasing the temperature will make the model answer more
   * creatively. (Default: 0.7)
   */
  @JsonProperty("temperature")
  private Double temperature;

  /**
   * Works together with top-k. A higher value (e.g., 0.95) will lead to more diverse text, while a
   * lower value (e.g., 0.2) will generate more focused and conservative text. (Default: 1.0)
   */
  @JsonProperty("top_p")
  private Double topP;

  /**
   * Reduces the probability of generating nonsense. A higher value (e.g. 100) will give more
   * diverse answers, while a lower value (e.g. 10) will be more conservative. (Default: 50)
   */
  @JsonProperty("top_k")
  private Integer topK;

  /** Sets the limit of tokens that the LLM follow. (Default: 1024) */
  @JsonProperty("max_completion_tokens")
  private Integer maxCompletionTokens;

  /**
   * Sets when the LLM should stop. (e.g., ["\n\n\n"]) then when the LLM generates three consecutive
   * line breaks it will terminate. Stop sequences are ignored until after the number of tokens that
   * are specified in the Min tokens parameter are generated.
   */
  @JsonProperty("stop")
  private List<String> stopSequences;

  /**
   * Positive values penalize new tokens based on whether they appear in the text so far, increasing
   * the model's likelihood to talk about new topics. Possible values is in the range of -2 < value
   * < 2. The default is 0
   */
  @JsonProperty("presence_penalty")
  private Double presencePenalty;

  /**
   * Positive values penalize new tokens based on their existing frequency in the text so far,
   * decreasing the model's likelihood to repeat the same line verbatim. Possible values is in the
   * range of -2 < value < 2. The default is 0
   */
  @JsonProperty("frequency_penalty")
  private Double frequencyPenalty;

  /**
   * Produce repeatable results, set the same random seed value every time. (Default: randomly
   * generated)
   */
  @JsonProperty("seed")
  private Integer seed;

  /** Model is the identifier of the LLM Model to be used */
  @JsonProperty("model")
  private String model;

  /**
   * Collection of {@link ToolCallback}s to be used for tool calling in the chat completion
   * requests.
   */
  @JsonIgnore private List<ToolCallback> toolCallbacks = new ArrayList<>();

  /**
   * Collection of tool names to be resolved at runtime and used for tool calling in the chat
   * completion requests.
   */
  @JsonProperty("tools")
  @JsonIgnore
  private Set<String> toolNames = new HashSet<>();

  /** Whether to enable the tool execution lifecycle internally in ChatModel. */
  @JsonIgnore private Boolean internalToolExecutionEnabled;

  /**
   * Whether to execute a tool with a given input and context, and return the result back to the
   * LLM.
   */
  @JsonIgnore private Map<String, Object> toolContext = new HashMap<>();

  /**
   * Increasing or decreasing probability of tokens being selected during generation; a positive
   * bias makes a token more likely to appear, while a negative bias makes it less likely.
   */
  @JsonProperty("logit_bias")
  private Map<String, Number> logitBias;

  /**
   * Whether to return log probabilities of the output tokens or not. If true, returns the log
   * probabilities of each output token returned in the content of message.
   */
  @JsonProperty("logprobs")
  private Boolean logprobs;

  /**
   * An integer specifying the number of most likely tokens to return at each token position, each
   * with an associated log probability. The option {@link WatsonxAiChatOptions#logprobs} must be
   * set to true if this parameter is used.
   */
  @JsonProperty("top_logprobs")
  private Integer topLogprobs;

  /**
   * How many chat completion choices to generate for each input message. Note that you will be
   * charged based on the number of generated tokens across all of the choices. Keep n as 1 to
   * minimize costs.
   */
  @JsonProperty("n")
  private Integer chatCompletions;

  /**
   * Time limit in milliseconds - if not completed within this time, generation will stop. The text
   * generated so far will be returned along with the `TIME_LIMIT`` stop reason. Depending on the
   * users plan, and on the model being used, there may be an enforced maximum time limit.
   *
   * <p>Value is expected to be greater than {@code 0}
   */
  @JsonProperty("time_limit")
  private Integer timeLimit;

  /** Set additional request params (some model have non-predefined options) */
  @JsonProperty("additional")
  private Map<String, Object> additional = new HashMap<>();

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Filter out the non-supported fields from the options.
   *
   * @param options The options to filter.
   * @return The filtered options.
   */
  public static Map<String, Object> filterNonSupportedFields(Map<String, Object> options) {
    return options.entrySet().stream()
        .filter(e -> !e.getKey().equals("model"))
        .filter(e -> e.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static WatsonxAiChatOptions fromOptions(WatsonxAiChatOptions fromOptions) {
    return WatsonxAiChatOptions.builder()
        .temperature(fromOptions.getTemperature())
        .topP(fromOptions.getTopP())
        .topK(fromOptions.getTopK())
        .maxTokens(fromOptions.getMaxTokens())
        .stopSequences(fromOptions.getStopSequences())
        .presencePenalty(fromOptions.getPresencePenalty())
        .seed(fromOptions.getSeed())
        .model(fromOptions.getModel())
        .toolCallbacks(fromOptions.toolCallbacks)
        .toolNames(fromOptions.getToolNames())
        .toolContext(fromOptions.getToolContext())
        .internalToolExecutionEnabled(fromOptions.getInternalToolExecutionEnabled())
        .logitBias(fromOptions.getLogitBias())
        .logProbs(fromOptions.getLogprobs())
        .topLogprobs(fromOptions.getTopLogprobs())
        .chatCompletions(fromOptions.getChatCompletions())
        .additionalProperties(fromOptions.getAdditionalProperties())
        .build();
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
    return this.topK;
  }

  public void setTopK(Integer topK) {
    this.topK = topK;
  }

  @Override
  public Integer getMaxTokens() {
    return this.maxCompletionTokens;
  }

  public void setMaxTokens(Integer maxCompletionTokens) {
    this.maxCompletionTokens = maxCompletionTokens;
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

  @Override
  public String getModel() {
    return this.model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public List<ToolCallback> getToolCallbacks() {
    return this.toolCallbacks;
  }

  @Override
  public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
    Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
    Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
    this.toolCallbacks = toolCallbacks;
  }

  @Override
  public Set<String> getToolNames() {
    return this.toolNames;
  }

  @Override
  public void setToolNames(Set<String> toolNames) {
    Assert.notNull(toolNames, "toolNames cannot be null");
    Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
    toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
    this.toolNames = toolNames;
  }

  @Override
  @Nullable
  public Boolean getInternalToolExecutionEnabled() {
    return this.internalToolExecutionEnabled;
  }

  @Override
  public void setInternalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
    this.internalToolExecutionEnabled = internalToolExecutionEnabled;
  }

  @Override
  public Map<String, Object> getToolContext() {
    return this.toolContext;
  }

  @Override
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
    this.logprobs = logprobs;
  }

  public Integer getTopLogprobs() {
    return this.topLogprobs;
  }

  public void setTopLogprobs(Integer topLogprobs) {
    Assert.notNull(logprobs, "logprobs cannot be null when using topLogprobs.");
    Assert.isTrue(logprobs, "logprobs cannot be false when using topLogprobs.");
    this.topLogprobs = topLogprobs;
  }

  public Integer getChatCompletions() {
    return this.chatCompletions;
  }

  public void setChatCompletions(Integer chatCompletions) {
    this.chatCompletions = chatCompletions;
  }

  public Integer getTimeLimit() {
    return this.timeLimit;
  }

  public void setTimeLimit(Integer timeLimit) {
    Assert.isTrue(timeLimit > 0, "Time limit must be greater than 0");
    this.timeLimit = timeLimit;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additional.entrySet().stream()
        .collect(Collectors.toMap(entry -> toSnakeCase(entry.getKey()), Map.Entry::getValue));
  }

  @JsonAnySetter
  public void addAdditionalProperty(String key, Object value) {
    this.additional.put(key, value);
  }

  /**
   * Convert the {@link WatsonxAiChatOptions} object to a {@link Map} of key/value pairs.
   *
   * @return The {@link Map} of key/value pairs.
   */
  public Map<String, Object> toMap() {
    try {
      var json = this.mapper.writeValueAsString(this);
      var map = this.mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
      map.remove("additional");

      return map;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String toSnakeCase(String input) {
    return input != null ? input.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase() : null;
  }

  @Override
  public WatsonxAiChatOptions copy() {
    return fromOptions(this);
  }

  public static class Builder {

    WatsonxAiChatOptions options = new WatsonxAiChatOptions();

    public Builder temperature(Double temperature) {
      this.options.temperature = temperature;
      return this;
    }

    public Builder topP(Double topP) {
      this.options.topP = topP;
      return this;
    }

    public Builder topK(Integer topK) {
      this.options.topK = topK;
      return this;
    }

    public Builder maxTokens(Integer maxCompletionTokens) {
      this.options.maxCompletionTokens = maxCompletionTokens;
      return this;
    }

    public Builder stopSequences(List<String> stopSequences) {
      this.options.stopSequences = stopSequences;
      return this;
    }

    public Builder presencePenalty(Double presencePenalty) {
      this.options.presencePenalty = presencePenalty;
      return this;
    }

    public Builder seed(Integer seed) {
      this.options.seed = seed;
      return this;
    }

    public Builder model(String model) {
      this.options.model = model;
      return this;
    }

    public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
      this.options.toolCallbacks = toolCallbacks;
      return this;
    }

    public Builder toolCallbacks(ToolCallback... toolCallbacks) {
      Assert.notNull(toolCallbacks, "Tool Callbacks cannot be null");
      Assert.noNullElements(toolCallbacks, "Tool Callbacks cannot have null ones");
      this.options.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
      return this;
    }

    public Builder toolNames(Set<String> toolNames) {
      Assert.notNull(toolNames, "Tool Names must not be null");
      this.options.toolNames = toolNames;
      return this;
    }

    public Builder toolName(String toolName) {
      Assert.hasText(toolName, "Tool Name must not be empty");
      this.options.toolNames.add(toolName);
      return this;
    }

    public Builder internalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
      this.options.internalToolExecutionEnabled = internalToolExecutionEnabled;
      return this;
    }

    public Builder toolContext(Map<String, Object> toolContext) {
      if (this.options.toolContext == null) {
        this.options.toolContext = toolContext;
      } else {
        this.options.toolContext.putAll(toolContext);
      }
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
      Assert.notNull(this.options.getLogprobs(), "logprobs cannot be null when using topLogprobs.");
      Assert.isTrue(this.options.getLogprobs(), "logprobs cannot be false when using topLogprobs.");
      this.options.topLogprobs = topLogprobs;
      return this;
    }

    public Builder chatCompletions(Integer chatCompletions) {
      this.options.chatCompletions = chatCompletions;
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

    public WatsonxAiChatOptions build() {
      return this.options;
    }
  }
}
