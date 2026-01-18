/*
 * Copyright 2026 the original author or authors.
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
import java.util.Map;
import org.springframework.ai.model.ModelOptionsUtils;

/**
 * Response format configuration for Watsonx AI Chat API.
 *
 * @author Tristan Mahinay
 * @since 1.0.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextChatResponseFormat {

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
      this.jsonSchema = JsonSchema.builder().schema(schema).build();
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
      this.jsonSchema = JsonSchema.builder().schema(schema).build();
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
   * finish_reason="length", which indicates the generation exceeded max_tokens or the conversation
   * exceeded the max context length.
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
