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

  /**
   * Decoding is the process that a model uses to choose the tokens in the generated output. Choose
   * one of the following decoding options:
   *
   * <p>Greedy: Selects the token with the highest probability at each step of the decoding process.
   * Greedy decoding produces output that closely matches the most common language in the model's
   * pretraining data and in your prompt text, which is desirable in less creative or fact-based use
   * cases. A weakness of greedy decoding is that it can cause repetitive loops in the generated
   * output.
   *
   * <p>Sampling decoding: Offers more variability in how tokens are selected. With sampling
   * decoding, the model samples tokens, meaning the model chooses a subset of tokens, and then one
   * token is chosen randomly from this subset to be added to the output text. Sampling adds
   * variability and randomness to the decoding process, which can be desirable in creative use
   * cases. However, with greater variability comes a greater risk of incorrect or nonsensical
   * output. (Default: greedy)
   */
  @JsonProperty("decoding_method")
  private String decodingMethod;

  /** Sets the limit of tokens that the LLM follow. (Default: 20) */
  @JsonProperty("max_new_tokens")
  private Integer maxNewTokens;

  /** Sets how many tokens must the LLM generate. (Default: 0) */
  @JsonProperty("min_new_tokens")
  private Integer minNewTokens;

  /**
   * Sets when the LLM should stop. (e.g., ["\n\n\n"]) then when the LLM generates three consecutive
   * line breaks it will terminate. Stop sequences are ignored until after the number of tokens that
   * are specified in the Min tokens parameter are generated.
   */
  @JsonProperty("stop_sequences")
  private List<String> stopSequences;

  /**
   * Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions
   * more strongly, while a lower value (e.g., 1.1) will be more lenient. (Default: 1.0)
   */
  @JsonProperty("repetition_penalty")
  private Double repetitionPenalty;

  /**
   * Produce repeatable results, set the same random seed value every time. (Default: randomly
   * generated)
   */
  @JsonProperty("random_seed")
  private Integer randomSeed;

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
  @JsonIgnore private Set<String> toolNames = new HashSet<>();

  /** Whether to enable the tool execution lifecycle internally in ChatModel. */
  @JsonIgnore private Boolean internalToolExecutionEnabled;

  /**
   * Whether to execute a tool with a given input and context, and return the result back to the
   * LLM.
   */
  @JsonIgnore private Map<String, Object> toolContext = new HashMap<>();

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
        .decodingMethod(fromOptions.getDecodingMethod())
        .maxNewTokens(fromOptions.getMaxNewTokens())
        .minNewTokens(fromOptions.getMinNewTokens())
        .stopSequences(fromOptions.getStopSequences())
        .repetitionPenalty(fromOptions.getRepetitionPenalty())
        .randomSeed(fromOptions.getRandomSeed())
        .model(fromOptions.getModel())
        .toolCallbacks(fromOptions.toolCallbacks)
        .toolNames(fromOptions.getToolNames())
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

  public String getDecodingMethod() {
    return this.decodingMethod;
  }

  public void setDecodingMethod(String decodingMethod) {
    this.decodingMethod = decodingMethod;
  }

  @Override
  @JsonIgnore
  public Integer getMaxTokens() {
    return getMaxNewTokens();
  }

  @JsonIgnore
  public void setMaxTokens(Integer maxTokens) {
    setMaxNewTokens(maxTokens);
  }

  public Integer getMaxNewTokens() {
    return this.maxNewTokens;
  }

  public void setMaxNewTokens(Integer maxNewTokens) {
    this.maxNewTokens = maxNewTokens;
  }

  public Integer getMinNewTokens() {
    return this.minNewTokens;
  }

  public void setMinNewTokens(Integer minNewTokens) {
    this.minNewTokens = minNewTokens;
  }

  @Override
  public List<String> getStopSequences() {
    return this.stopSequences;
  }

  public void setStopSequences(List<String> stopSequences) {
    this.stopSequences = stopSequences;
  }

  @Override
  @JsonIgnore
  public Double getPresencePenalty() {
    return getRepetitionPenalty();
  }

  @JsonIgnore
  public void setPresencePenalty(Double presencePenalty) {
    setRepetitionPenalty(presencePenalty);
  }

  public Double getRepetitionPenalty() {
    return this.repetitionPenalty;
  }

  public void setRepetitionPenalty(Double repetitionPenalty) {
    this.repetitionPenalty = repetitionPenalty;
  }

  public Integer getRandomSeed() {
    return this.randomSeed;
  }

  public void setRandomSeed(Integer randomSeed) {
    this.randomSeed = randomSeed;
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

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additional.entrySet().stream()
        .collect(Collectors.toMap(entry -> toSnakeCase(entry.getKey()), Map.Entry::getValue));
  }

  @JsonAnySetter
  public void addAdditionalProperty(String key, Object value) {
    this.additional.put(key, value);
  }

  @Override
  @JsonIgnore
  public Double getFrequencyPenalty() {
    return null;
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

    public Builder decodingMethod(String decodingMethod) {
      this.options.decodingMethod = decodingMethod;
      return this;
    }

    public Builder maxNewTokens(Integer maxNewTokens) {
      this.options.maxNewTokens = maxNewTokens;
      return this;
    }

    public Builder minNewTokens(Integer minNewTokens) {
      this.options.minNewTokens = minNewTokens;
      return this;
    }

    public Builder stopSequences(List<String> stopSequences) {
      this.options.stopSequences = stopSequences;
      return this;
    }

    public Builder repetitionPenalty(Double repetitionPenalty) {
      this.options.repetitionPenalty = repetitionPenalty;
      return this;
    }

    public Builder randomSeed(Integer randomSeed) {
      this.options.randomSeed = randomSeed;
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
      Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
      this.options.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
      return this;
    }

    public Builder toolNames(Set<String> toolNames) {
      Assert.notNull(toolNames, "Function names must not be null");
      this.options.toolNames = toolNames;
      return this;
    }

    public Builder toolName(String toolName) {
      Assert.hasText(toolName, "Function name must not be empty");
      this.options.toolNames.add(toolName);
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
