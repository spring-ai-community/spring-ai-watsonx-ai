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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springframework.ai.model.ModelOptionsUtils;

/**
 * Request for the Watsonx AI Chat API. Full documentation can be found at <a
 * href=https://cloud.ibm.com/apidocs/watsonx-ai#text-chat-request>watsonx.ai Chat Request</a>.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiChatRequest {

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("project_id")
  private String projectId;

  @JsonProperty("tool_choice_option")
  private String toolChoiceOption;

  @JsonProperty("messages")
  private List<TextChatMessage> messages;

  @JsonProperty("tool_choice")
  private List<TextChatToolChoiceTool> toolChoice;

  @JsonProperty("tools")
  private List<TextChatParameterTool> tools;

  @JsonProperty("frequency_penalty")
  private Double frequencyPenalty;

  @JsonProperty("logit_bias")
  private Map<String, Number> logitBias;

  @JsonProperty("logprobs")
  private Boolean logprobs;

  @JsonProperty("top_logprobs")
  private Integer topLogprobs;

  @JsonProperty("max_completion_tokens")
  private Integer maxCompletionTokens;

  @JsonProperty("max_tokens")
  private Integer maxTokens;

  @JsonProperty("n")
  private Integer n;

  @JsonProperty("presence_penalty")
  private Double presencePenalty;

  @JsonProperty("seed")
  private Integer seed;

  @JsonProperty("stop")
  private List<String> stopSequences;

  @JsonProperty("temperature")
  private Double temperature;

  @JsonProperty("top_p")
  private Double topP;

  @JsonProperty("time_limit")
  private Integer timeLimit;

  @JsonProperty("response_format")
  private TextChatResponseFormat responseFormat;

  public WatsonxAiChatRequest() {}

  private WatsonxAiChatRequest(Builder builder) {
    this.model = builder.model;
    this.projectId = builder.projectId;
    this.toolChoiceOption = builder.toolChoiceOption;
    this.messages = builder.messages;
    this.toolChoice = builder.toolChoice;
    this.tools = builder.tools;
    this.frequencyPenalty = builder.frequencyPenalty;
    this.logitBias = builder.logitBias;
    this.logprobs = builder.logprobs;
    this.topLogprobs = builder.topLogprobs;
    this.maxCompletionTokens = builder.maxCompletionTokens;
    this.maxTokens = builder.maxTokens;
    this.n = builder.n;
    this.presencePenalty = builder.presencePenalty;
    this.seed = builder.seed;
    this.stopSequences = builder.stopSequences;
    this.temperature = builder.temperature;
    this.topP = builder.topP;
    this.timeLimit = builder.timeLimit;
    this.responseFormat = builder.responseFormat;
  }

  // Getters
  public String model() {
    return model;
  }

  public String projectId() {
    return projectId;
  }

  public String toolChoiceOption() {
    return toolChoiceOption;
  }

  public List<TextChatMessage> messages() {
    return messages;
  }

  public List<TextChatToolChoiceTool> toolChoice() {
    return toolChoice;
  }

  public List<TextChatParameterTool> tools() {
    return tools;
  }

  public Double frequencyPenalty() {
    return frequencyPenalty;
  }

  public Map<String, Number> logitBias() {
    return logitBias;
  }

  public Boolean logprobs() {
    return logprobs;
  }

  public Integer topLogprobs() {
    return topLogprobs;
  }

  public Integer maxCompletionTokens() {
    return maxCompletionTokens;
  }

  public Integer maxTokens() {
    return maxTokens;
  }

  public Integer n() {
    return n;
  }

  public Double presencePenalty() {
    return presencePenalty;
  }

  public Integer seed() {
    return seed;
  }

  public List<String> stopSequences() {
    return stopSequences;
  }

  public Double temperature() {
    return temperature;
  }

  public Double topP() {
    return topP;
  }

  public Integer timeLimit() {
    return timeLimit;
  }

  public TextChatResponseFormat responseFormat() {
    return responseFormat;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .model(this.model)
        .projectId(this.projectId)
        .toolChoiceOption(this.toolChoiceOption)
        .messages(this.messages)
        .toolChoice(this.toolChoice)
        .tools(this.tools)
        .frequencyPenalty(this.frequencyPenalty)
        .logitBias(this.logitBias)
        .logprobs(this.logprobs)
        .topLogprobs(this.topLogprobs)
        .maxCompletionTokens(this.maxCompletionTokens)
        .maxTokens(this.maxTokens)
        .n(this.n)
        .presencePenalty(this.presencePenalty)
        .seed(this.seed)
        .stopSequences(this.stopSequences)
        .temperature(this.temperature)
        .topP(this.topP)
        .timeLimit(this.timeLimit)
        .responseFormat(this.responseFormat);
  }

  public static class Builder {
    private String model;
    private String projectId;
    private String toolChoiceOption;
    private List<TextChatMessage> messages;
    private List<TextChatToolChoiceTool> toolChoice;
    private List<TextChatParameterTool> tools;
    private Double frequencyPenalty;
    private Map<String, Number> logitBias;
    private Boolean logprobs;
    private Integer topLogprobs;
    private Integer maxCompletionTokens;
    private Integer maxTokens;
    private Integer n;
    private Double presencePenalty;
    private Integer seed;
    private List<String> stopSequences;
    private Double temperature;
    private Double topP;
    private Integer timeLimit;
    private TextChatResponseFormat responseFormat;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder toolChoiceOption(String toolChoiceOption) {
      this.toolChoiceOption = toolChoiceOption;
      return this;
    }

    public Builder messages(List<TextChatMessage> messages) {
      this.messages = messages;
      return this;
    }

    public Builder toolChoice(List<TextChatToolChoiceTool> toolChoice) {
      this.toolChoice = toolChoice;
      return this;
    }

    public Builder tools(List<TextChatParameterTool> tools) {
      this.tools = tools;
      return this;
    }

    public Builder frequencyPenalty(Double frequencyPenalty) {
      this.frequencyPenalty = frequencyPenalty;
      return this;
    }

    public Builder logitBias(Map<String, Number> logitBias) {
      this.logitBias = logitBias;
      return this;
    }

    public Builder logprobs(Boolean logprobs) {
      this.logprobs = logprobs;
      return this;
    }

    public Builder topLogprobs(Integer topLogprobs) {
      this.topLogprobs = topLogprobs;
      return this;
    }

    public Builder maxCompletionTokens(Integer maxCompletionTokens) {
      this.maxCompletionTokens = maxCompletionTokens;
      return this;
    }

    public Builder maxTokens(Integer maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder n(Integer n) {
      this.n = n;
      return this;
    }

    public Builder presencePenalty(Double presencePenalty) {
      this.presencePenalty = presencePenalty;
      return this;
    }

    public Builder seed(Integer seed) {
      this.seed = seed;
      return this;
    }

    public Builder stopSequences(List<String> stopSequences) {
      this.stopSequences = stopSequences;
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder topP(Double topP) {
      this.topP = topP;
      return this;
    }

    public Builder timeLimit(Integer timeLimit) {
      this.timeLimit = timeLimit;
      return this;
    }

    public Builder responseFormat(TextChatResponseFormat responseFormat) {
      this.responseFormat = responseFormat;
      return this;
    }

    public WatsonxAiChatRequest build() {
      return new WatsonxAiChatRequest(this);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TextChatResponseFormat {

    @JsonProperty("type")
    private Type type;

    @JsonProperty("json_schema")
    private JsonSchema jsonSchema;

    @JsonIgnore private String schema;

    public TextChatResponseFormat() {}

    public TextChatResponseFormat(Type type) {
      this.type = type;
    }

    public TextChatResponseFormat(Type type, String schema) {
      this.type = type;
      this.schema = schema;
      if (schema != null) {
        this.jsonSchema = JsonSchema.builder().schema(schema).strict(true).build();
      }
    }

    public static TextChatResponseFormat text() {
      return new TextChatResponseFormat(Type.TEXT);
    }

    public static TextChatResponseFormat jsonObject() {
      return new TextChatResponseFormat(Type.JSON_OBJECT);
    }

    public Type getType() {
      return this.type;
    }

    public void setType(Type type) {
      this.type = type;
    }

    public JsonSchema getJsonSchema() {
      return this.jsonSchema;
    }

    public void setJsonSchema(JsonSchema jsonSchema) {
      this.jsonSchema = jsonSchema;
    }

    public String getSchema() {
      return this.schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
      if (schema != null) {
        this.jsonSchema = JsonSchema.builder().schema(schema).strict(true).build();
      }
    }

    public static Builder builder() {
      return new Builder();
    }

    /**
     * Used to enable JSON mode, which guarantees the message the model generates is valid JSON.
     *
     * <p>Important: when using JSON mode, you must also instruct the model to produce JSON yourself
     * via a system or user message. Without this, the model may generate an unending stream of
     * whitespace until the generation reaches the token limit, resulting in a long-running and
     * seemingly "stuck" request. Also note that the message content may be partially cut off if
     * finish_reason="length", which indicates the generation exceeded max_tokens or the
     * conversation exceeded the max context length.
     */
    public enum Type {
      /** The message is a text string. */
      @JsonProperty("text")
      TEXT,

      /** The message is a JSON object. */
      @JsonProperty("json_object")
      JSON_OBJECT,

      /** The message is a JSON object that conforms to a specific schema. */
      @JsonProperty("json_schema")
      JSON_SCHEMA
    }

    /**
     * JSON schema object that describes the format of the JSON object. Only applicable when type is
     * 'json_schema'.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonSchema {

      @JsonProperty("name")
      private String name;

      @JsonProperty("schema")
      private Map<String, Object> schema;

      @JsonProperty("strict")
      private Boolean strict;

      public JsonSchema() {}

      public JsonSchema(String name, Map<String, Object> schema, Boolean strict) {
        this.name = name;
        this.schema = schema;
        this.strict = strict;
      }

      public String getName() {
        return this.name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public Map<String, Object> getSchema() {
        return this.schema;
      }

      public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
      }

      public Boolean getStrict() {
        return this.strict;
      }

      public void setStrict(Boolean strict) {
        this.strict = strict;
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {
        private String name;
        private Map<String, Object> schema;
        private Boolean strict;

        public Builder name(String name) {
          this.name = name;
          return this;
        }

        public Builder schema(String schema) {
          this.schema = ModelOptionsUtils.jsonToMap(schema);
          return this;
        }

        public Builder schema(Map<String, Object> schema) {
          this.schema = schema;
          return this;
        }

        public Builder strict(Boolean strict) {
          this.strict = strict;
          return this;
        }

        public JsonSchema build() {
          return new JsonSchema(this.name, this.schema, this.strict);
        }
      }
    }

    public static class Builder {
      private Type type;
      private String schema;

      public Builder type(Type type) {
        this.type = type;
        return this;
      }

      public Builder schema(String schema) {
        this.type = Type.JSON_SCHEMA;
        this.schema = schema;
        return this;
      }

      public TextChatResponseFormat build() {
        return new TextChatResponseFormat(this.type, this.schema);
      }
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TextChatParameterTool {
    @JsonProperty("type")
    private final ToolType type;

    @JsonProperty("function")
    private final TextChatParameterFunction function;

    public TextChatParameterTool(ToolType type, TextChatParameterFunction function) {
      this.type = type;
      this.function = function;
    }

    public ToolType type() {
      return type;
    }

    public TextChatParameterFunction function() {
      return function;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TextChatParameterFunction {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("parameters")
    private final Map<String, Object> parameters;

    public TextChatParameterFunction(
        String name, String description, Map<String, Object> parameters) {
      this.name = name;
      this.description = description;
      this.parameters = parameters;
    }

    public String name() {
      return name;
    }

    public String description() {
      return description;
    }

    public Map<String, Object> parameters() {
      return parameters;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TextChatToolChoiceTool {
    @JsonProperty("type")
    private final ToolType type;

    @JsonProperty("function")
    private final TextChatToolChoiceFunction function;

    public TextChatToolChoiceTool(ToolType type, TextChatToolChoiceFunction function) {
      this.type = type;
      this.function = function;
    }

    public ToolType type() {
      return type;
    }

    public TextChatToolChoiceFunction function() {
      return function;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TextChatToolChoiceFunction {
    @JsonProperty("name")
    private final String name;

    public TextChatToolChoiceFunction(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }
  }
}
