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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * JUnit 5 test class for WatsonxAiChatOptions functionality and configuration. Tests the chat
 * options builder pattern and configuration validation.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
class WatsonxAiChatOptionsTest {

  @Nested
  class BasicOptionsTests {

    @Test
    void createBasicChatOptionsWithDefaultValues() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(1.0)
              .maxTokens(1024)
              .build();

      assertAll(
          "Basic options validation",
          () -> assertEquals("ibm/granite-3-3-8b-instruct", options.getModel()),
          () -> assertEquals(0.7, options.getTemperature()),
          () -> assertEquals(1.0, options.getTopP()),
          () -> assertEquals(1024, options.getMaxTokens()));
    }

    @Test
    void createOptionsWithMinimumRequiredFields() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("ibm/granite-3-8b-instruct").build();

      assertAll(
          "Minimal options validation",
          () -> assertEquals("ibm/granite-3-8b-instruct", options.getModel()),
          () -> assertNull(options.getTemperature()),
          () -> assertNull(options.getTopP()),
          () -> assertNull(options.getMaxTokens()));
    }
  }

  @Nested
  class AdvancedOptionsTests {

    @Test
    void createAdvancedOptionsWithAllParameters() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-8b-instruct")
              .temperature(0.3)
              .topP(0.9)
              .maxTokens(2048)
              .presencePenalty(0.1)
              .stopSequences(List.of("END", "STOP", "###"))
              .seed(42)
              .n(1)
              .build();

      assertAll(
          "Advanced options validation",
          () -> assertEquals("ibm/granite-3-8b-instruct", options.getModel()),
          () -> assertEquals(0.3, options.getTemperature()),
          () -> assertEquals(0.9, options.getTopP()),
          () -> assertEquals(2048, options.getMaxTokens()),
          () -> assertEquals(0.1, options.getPresencePenalty()),
          () -> assertEquals(List.of("END", "STOP", "###"), options.getStopSequences()),
          () -> assertEquals(42, options.getSeed()),
          () -> assertEquals(1, options.getN()));
    }

    @Test
    void handleEmptyStopSequences() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .stopSequences(List.of())
              .build();

      assertNotNull(options.getStopSequences());
      assertTrue(options.getStopSequences().isEmpty());
    }
  }

  @Nested
  class ModelSelectionTests {

    @Test
    void supportDifferentIBMGraniteModels() {
      String[] supportedModels = {
        "ibm/granite-3-3-8b-instruct", "ibm/granite-3-8b-instruct", "ibm/granite-3-2b-instruct"
      };

      for (String model : supportedModels) {
        WatsonxAiChatOptions options =
            WatsonxAiChatOptions.builder().model(model).temperature(0.7).build();

        assertEquals(model, options.getModel(), "Model should be set correctly for: " + model);
      }
    }
  }

  @Nested
  class ParameterRangeTests {

    @Test
    void acceptValidTemperatureRange() {
      assertAll(
          "Temperature range validation",
          () -> {
            WatsonxAiChatOptions lowTemp = WatsonxAiChatOptions.builder().temperature(0.0).build();
            assertEquals(0.0, lowTemp.getTemperature());
          },
          () -> {
            WatsonxAiChatOptions midTemp = WatsonxAiChatOptions.builder().temperature(0.5).build();
            assertEquals(0.5, midTemp.getTemperature());
          },
          () -> {
            WatsonxAiChatOptions highTemp = WatsonxAiChatOptions.builder().temperature(1.0).build();
            assertEquals(1.0, highTemp.getTemperature());
          });
    }

    @Test
    void acceptValidTopPRange() {
      assertAll(
          "TopP range validation",
          () -> {
            WatsonxAiChatOptions lowTopP = WatsonxAiChatOptions.builder().topP(0.1).build();
            assertEquals(0.1, lowTopP.getTopP());
          },
          () -> {
            WatsonxAiChatOptions highTopP = WatsonxAiChatOptions.builder().topP(1.0).build();
            assertEquals(1.0, highTopP.getTopP());
          });
    }

    @Test
    void acceptVariousTokenLimits() {
      assertAll(
          "Token limit validation",
          () -> {
            WatsonxAiChatOptions shortTokens =
                WatsonxAiChatOptions.builder().maxTokens(256).build();
            assertEquals(256, shortTokens.getMaxTokens());
          },
          () -> {
            WatsonxAiChatOptions longTokens =
                WatsonxAiChatOptions.builder().maxTokens(4096).build();
            assertEquals(4096, longTokens.getMaxTokens());
          });
    }
  }

  @Nested
  class ChatBehaviorTests {

    @Test
    void configureCreativeChatBehavior() {
      WatsonxAiChatOptions creative =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.9) // High creativity
              .topP(0.95) // Diverse word choices
              .presencePenalty(0.2) // Encourage new topics
              .build();

      assertAll(
          "Creative behavior validation",
          () -> assertEquals(0.9, creative.getTemperature()),
          () -> assertEquals(0.95, creative.getTopP()),
          () -> assertEquals(0.2, creative.getPresencePenalty()));
    }

    @Test
    void configureFocusedChatBehavior() {
      WatsonxAiChatOptions focused =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.2) // Very focused
              .topP(0.7) // Conservative word choices
              .presencePenalty(0.0) // No repetition penalty
              .build();

      assertAll(
          "Focused behavior validation",
          () -> assertEquals(0.2, focused.getTemperature()),
          () -> assertEquals(0.7, focused.getTopP()),
          () -> assertEquals(0.0, focused.getPresencePenalty()));
    }
  }

  @Nested
  class BuilderPatternTests {

    @Test
    void supportMethodChaining() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(1.0)
              .maxTokens(1024)
              .presencePenalty(0.0)
              .n(1)
              .build();

      assertNotNull(options);
      assertEquals("ibm/granite-3-3-8b-instruct", options.getModel());
    }

    @Test
    void createMultipleInstancesIndependently() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.5)
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-8b-instruct")
              .temperature(0.8)
              .build();

      assertAll(
          "Multiple instances validation",
          () -> assertNotSame(options1, options2),
          () -> assertEquals("ibm/granite-3-3-8b-instruct", options1.getModel()),
          () -> assertEquals("ibm/granite-3-8b-instruct", options2.getModel()),
          () -> assertEquals(0.5, options1.getTemperature()),
          () -> assertEquals(0.8, options2.getTemperature()));
    }
  }

  @Nested
  class ValidationTests {

    @Test
    void validateOptionsState() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .temperature(0.5)
              .topP(0.9)
              .maxTokens(1000)
              .build();

      assertAll(
          "Options validation",
          () -> assertNotNull(options),
          () -> assertEquals("test-model", options.getModel()),
          () -> assertTrue(options.getTemperature() >= 0.0 && options.getTemperature() <= 1.0),
          () -> assertTrue(options.getTopP() >= 0.0 && options.getTopP() <= 1.0),
          () -> assertTrue(options.getMaxTokens() > 0));
    }
  }

  @Nested
  class CopyMethodTests {

    @Test
    void copyCreatesSeparateInstance() {
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .presencePenalty(0.1)
              .seed(42)
              .build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy creates separate instance",
          () -> assertNotSame(original, copy),
          () -> assertEquals(original.getModel(), copy.getModel()),
          () -> assertEquals(original.getTemperature(), copy.getTemperature()),
          () -> assertEquals(original.getTopP(), copy.getTopP()),
          () -> assertEquals(original.getMaxTokens(), copy.getMaxTokens()),
          () -> assertEquals(original.getPresencePenalty(), copy.getPresencePenalty()),
          () -> assertEquals(original.getSeed(), copy.getSeed()));
    }

    @Test
    void copyWithNullFieldsHandledCorrectly() {
      WatsonxAiChatOptions original = WatsonxAiChatOptions.builder().model("test-model").build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy with null fields",
          () -> assertNotSame(original, copy),
          () -> assertEquals(original.getModel(), copy.getModel()),
          () -> assertNull(copy.getTemperature()),
          () -> assertNull(copy.getTopP()),
          () -> assertNull(copy.getMaxTokens()));
    }
  }

  @Nested
  class ToStringMethodTests {

    @Test
    void toStringReturnsJsonRepresentation() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .build();

      String result = options.toString();

      assertAll(
          "ToString validation",
          () -> assertNotNull(result),
          () -> assertTrue(result.startsWith("WatsonxAiChatOptions: ")),
          () -> assertTrue(result.contains("ibm/granite-3-3-8b-instruct")));
    }

    @Test
    void toStringWithMinimalOptions() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      String result = options.toString();

      assertNotNull(result);
      assertTrue(result.startsWith("WatsonxAiChatOptions: "));
    }
  }

  @Nested
  class EqualsMethodTests {

    @Test
    void equalsReturnsTrueForSameInstance() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("test-model").temperature(0.7).build();

      assertEquals(options, options);
    }

    @Test
    void equalsReturnsTrueForEqualOptions() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .build();

      assertEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForDifferentModel() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("model1").temperature(0.7).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("model2").temperature(0.7).build();

      assertNotEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForDifferentTemperature() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("test-model").temperature(0.7).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("test-model").temperature(0.5).build();

      assertNotEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForNull() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      assertNotEquals(options, null);
    }

    @Test
    void equalsReturnsFalseForDifferentClass() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      assertNotEquals(options, "string");
    }

    @Test
    void equalsHandlesNullFields() {
      WatsonxAiChatOptions options1 = WatsonxAiChatOptions.builder().model("test-model").build();

      WatsonxAiChatOptions options2 = WatsonxAiChatOptions.builder().model("test-model").build();

      assertEquals(options1, options2);
    }
  }

  @Nested
  class HashCodeMethodTests {

    @Test
    void hashCodeConsistentForSameInstance() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("test-model").temperature(0.7).topP(0.9).build();

      int hashCode1 = options.hashCode();
      int hashCode2 = options.hashCode();

      assertEquals(hashCode1, hashCode2);
    }

    @Test
    void hashCodeSameForEqualOptions() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .temperature(0.7)
              .topP(0.9)
              .maxTokens(1024)
              .build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    void hashCodeDifferentForDifferentOptions() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("model1").temperature(0.7).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("model2").temperature(0.7).build();

      assertNotEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    void hashCodeHandlesNullFields() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      assertDoesNotThrow(() -> options.hashCode());
    }
  }

  @Nested
  class ResponseFormatTests {

    @Test
    void createOptionsWithJsonObjectResponseFormat() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .responseFormat(TextChatResponseFormat.jsonObject())
              .build();

      assertAll(
          "JSON object response format validation",
          () -> assertNotNull(options.getResponseFormat()),
          () ->
              assertEquals(
                  TextChatResponseFormat.Type.JSON_OBJECT, options.getResponseFormat().getType()));
    }

    @Test
    void createOptionsWithTextResponseFormat() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .responseFormat(TextChatResponseFormat.text())
              .build();

      assertAll(
          "Text response format validation",
          () -> assertNotNull(options.getResponseFormat()),
          () ->
              assertEquals(
                  TextChatResponseFormat.Type.TEXT, options.getResponseFormat().getType()));
    }

    @Test
    void createOptionsWithoutResponseFormat() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("ibm/granite-3-3-8b-instruct").build();

      assertNull(options.getResponseFormat());
    }

    @Test
    void copyIncludesResponseFormat() {
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .responseFormat(TextChatResponseFormat.jsonObject())
              .build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy includes response format",
          () -> assertNotSame(original, copy),
          () -> assertNotNull(copy.getResponseFormat()),
          () ->
              assertEquals(
                  TextChatResponseFormat.Type.JSON_OBJECT, copy.getResponseFormat().getType()));
    }

    @Test
    void equalsConsidersResponseFormat() {
      TextChatResponseFormat sharedFormat = TextChatResponseFormat.jsonObject();

      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("test-model").responseFormat(sharedFormat).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("test-model").responseFormat(sharedFormat).build();

      WatsonxAiChatOptions options3 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .responseFormat(TextChatResponseFormat.text())
              .build();

      assertAll(
          "Equals considers response format",
          () -> assertEquals(options1, options2),
          () -> assertNotEquals(options1, options3));
    }

    @Test
    void hashCodeConsidersResponseFormat() {
      TextChatResponseFormat sharedFormat = TextChatResponseFormat.jsonObject();

      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("test-model").responseFormat(sharedFormat).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("test-model").responseFormat(sharedFormat).build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }
  }

  @Nested
  class GuidedOptionsTests {

    @Test
    void createOptionsWithGuidedChoice() {
      List<String> choices = List.of("yes", "no", "maybe");
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedChoice(choices)
              .build();

      assertAll(
          "Guided choice validation",
          () -> assertNotNull(options.getGuidedChoice()),
          () -> assertEquals(choices, options.getGuidedChoice()),
          () -> assertEquals(3, options.getGuidedChoice().size()));
    }

    @Test
    void createOptionsWithGuidedRegex() {
      String regex = "^[A-Z][a-z]+$";
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedRegex(regex)
              .build();

      assertAll(
          "Guided regex validation",
          () -> assertNotNull(options.getGuidedRegex()),
          () -> assertEquals(regex, options.getGuidedRegex()));
    }

    @Test
    void createOptionsWithGuidedGrammar() {
      String grammar = "root ::= \"Hello\" \" \" name\nname ::= [A-Z][a-z]+";
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedGrammar(grammar)
              .build();

      assertAll(
          "Guided grammar validation",
          () -> assertNotNull(options.getGuidedGrammar()),
          () -> assertEquals(grammar, options.getGuidedGrammar()));
    }

    @Test
    void createOptionsWithGuidedJsonAsMap() {
      Map<String, Object> jsonSchema = new HashMap<>();
      jsonSchema.put("type", "object");
      jsonSchema.put("properties", Map.of("name", Map.of("type", "string")));

      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedJson(jsonSchema)
              .build();

      assertAll(
          "Guided JSON validation",
          () -> assertNotNull(options.getGuidedJson()),
          () -> assertEquals("object", options.getGuidedJson().get("type")),
          () -> assertTrue(options.getGuidedJson().containsKey("properties")));
    }

    @Test
    void createOptionsWithGuidedJsonAsString() {
      String jsonSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedJson(jsonSchema)
              .build();

      assertAll(
          "Guided JSON string validation",
          () -> assertNotNull(options.getGuidedJson()),
          () -> assertEquals("object", options.getGuidedJson().get("type")),
          () -> assertTrue(options.getGuidedJson().containsKey("properties")));
    }

    @Test
    void createOptionsWithEmptyGuidedChoice() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedChoice(List.of())
              .build();

      assertNotNull(options.getGuidedChoice());
      assertTrue(options.getGuidedChoice().isEmpty());
    }

    @Test
    void copyIncludesGuidedOptions() {
      List<String> choices = List.of("option1", "option2");
      String regex = "^test$";
      String grammar = "root ::= \"test\"";
      Map<String, Object> jsonSchema = Map.of("type", "string");

      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .guidedChoice(choices)
              .guidedRegex(regex)
              .guidedGrammar(grammar)
              .guidedJson(jsonSchema)
              .build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy includes guided options",
          () -> assertNotSame(original, copy),
          () -> assertEquals(original.getGuidedChoice(), copy.getGuidedChoice()),
          () -> assertEquals(original.getGuidedRegex(), copy.getGuidedRegex()),
          () -> assertEquals(original.getGuidedGrammar(), copy.getGuidedGrammar()),
          () -> assertEquals(original.getGuidedJson(), copy.getGuidedJson()));
    }

    @Test
    void equalsConsidersGuidedOptions() {
      List<String> choices = List.of("yes", "no");
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .guidedChoice(choices)
              .guidedRegex("^test$")
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .guidedChoice(choices)
              .guidedRegex("^test$")
              .build();

      WatsonxAiChatOptions options3 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .guidedChoice(List.of("different"))
              .guidedRegex("^test$")
              .build();

      assertAll(
          "Equals considers guided options",
          () -> assertEquals(options1, options2),
          () -> assertNotEquals(options1, options3));
    }

    @Test
    void hashCodeConsidersGuidedOptions() {
      List<String> choices = List.of("yes", "no");
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .guidedChoice(choices)
              .guidedRegex("^test$")
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .guidedChoice(choices)
              .guidedRegex("^test$")
              .build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }
  }

  @Nested
  class ChatTemplateKwargsTests {

    @Test
    void createOptionsWithChatTemplateKwargsAsMap() {
      Map<String, Object> kwargs = new HashMap<>();
      kwargs.put("system_prompt", "You are a helpful assistant");
      kwargs.put("max_history", 10);

      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .chatTemplateKwargs(kwargs)
              .build();

      assertAll(
          "Chat template kwargs validation",
          () -> assertNotNull(options.getChatTemplateKwargs()),
          () ->
              assertEquals(
                  "You are a helpful assistant",
                  options.getChatTemplateKwargs().get("system_prompt")),
          () -> assertEquals(10, options.getChatTemplateKwargs().get("max_history")));
    }

    @Test
    void createOptionsWithChatTemplateKwargsAsString() {
      String kwargs = "{\"system_prompt\":\"You are a helpful assistant\",\"max_history\":10}";
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .chatTemplateKwargs(kwargs)
              .build();

      assertAll(
          "Chat template kwargs string validation",
          () -> assertNotNull(options.getChatTemplateKwargs()),
          () ->
              assertEquals(
                  "You are a helpful assistant",
                  options.getChatTemplateKwargs().get("system_prompt")),
          () -> assertEquals(10, options.getChatTemplateKwargs().get("max_history")));
    }

    @Test
    void createOptionsWithoutChatTemplateKwargs() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("ibm/granite-3-3-8b-instruct").build();

      assertNull(options.getChatTemplateKwargs());
    }

    @Test
    void copyIncludesChatTemplateKwargs() {
      Map<String, Object> kwargs = Map.of("key", "value");
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .chatTemplateKwargs(kwargs)
              .build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy includes chat template kwargs",
          () -> assertNotSame(original, copy),
          () -> assertNotNull(copy.getChatTemplateKwargs()),
          () -> assertEquals(original.getChatTemplateKwargs(), copy.getChatTemplateKwargs()));
    }

    @Test
    void equalsConsidersChatTemplateKwargs() {
      Map<String, Object> kwargs = Map.of("key", "value");
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("test-model").chatTemplateKwargs(kwargs).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("test-model").chatTemplateKwargs(kwargs).build();

      WatsonxAiChatOptions options3 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .chatTemplateKwargs(Map.of("different", "value"))
              .build();

      assertAll(
          "Equals considers chat template kwargs",
          () -> assertEquals(options1, options2),
          () -> assertNotEquals(options1, options3));
    }

    @Test
    void hashCodeConsidersChatTemplateKwargs() {
      Map<String, Object> kwargs = Map.of("key", "value");
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder().model("test-model").chatTemplateKwargs(kwargs).build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder().model("test-model").chatTemplateKwargs(kwargs).build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }
  }

  @Nested
  class ReasoningOptionsTests {

    @Test
    void createOptionsWithIncludeReasoningTrue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .includeReasoning(true)
              .build();

      assertAll(
          "Include reasoning true validation",
          () -> assertNotNull(options.isIncludeReasoning()),
          () -> assertTrue(options.isIncludeReasoning()));
    }

    @Test
    void createOptionsWithIncludeReasoningFalse() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .includeReasoning(false)
              .build();

      assertAll(
          "Include reasoning false validation",
          () -> assertNotNull(options.isIncludeReasoning()),
          () -> assertFalse(options.isIncludeReasoning()));
    }

    @Test
    void includeReasoningDefaultsToTrue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("ibm/granite-3-3-8b-instruct").build();

      assertAll(
          "Include reasoning default validation",
          () -> assertNotNull(options.isIncludeReasoning()),
          () -> assertTrue(options.isIncludeReasoning()));
    }

    @Test
    void createOptionsWithReasoningEffortLow() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .reasoningEffort("low")
              .build();

      assertAll(
          "Reasoning effort low validation",
          () -> assertNotNull(options.getReasoningEffort()),
          () -> assertEquals("low", options.getReasoningEffort()));
    }

    @Test
    void createOptionsWithReasoningEffortMedium() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .reasoningEffort("medium")
              .build();

      assertAll(
          "Reasoning effort medium validation",
          () -> assertNotNull(options.getReasoningEffort()),
          () -> assertEquals("medium", options.getReasoningEffort()));
    }

    @Test
    void createOptionsWithReasoningEffortHigh() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .reasoningEffort("high")
              .build();

      assertAll(
          "Reasoning effort high validation",
          () -> assertNotNull(options.getReasoningEffort()),
          () -> assertEquals("high", options.getReasoningEffort()));
    }

    @Test
    void createOptionsWithoutReasoningEffort() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("ibm/granite-3-3-8b-instruct").build();

      assertNull(options.getReasoningEffort());
    }

    @Test
    void copyIncludesReasoningOptions() {
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("ibm/granite-3-3-8b-instruct")
              .includeReasoning(false)
              .reasoningEffort("high")
              .build();

      WatsonxAiChatOptions copy = original.copy();

      assertAll(
          "Copy includes reasoning options",
          () -> assertNotSame(original, copy),
          () -> assertEquals(original.isIncludeReasoning(), copy.isIncludeReasoning()),
          () -> assertEquals(original.getReasoningEffort(), copy.getReasoningEffort()));
    }

    @Test
    void equalsConsidersReasoningOptions() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .includeReasoning(false)
              .reasoningEffort("low")
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .includeReasoning(false)
              .reasoningEffort("low")
              .build();

      WatsonxAiChatOptions options3 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .includeReasoning(true)
              .reasoningEffort("high")
              .build();

      assertAll(
          "Equals considers reasoning options",
          () -> assertEquals(options1, options2),
          () -> assertNotEquals(options1, options3));
    }

    @Test
    void hashCodeConsidersReasoningOptions() {
      WatsonxAiChatOptions options1 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .includeReasoning(false)
              .reasoningEffort("low")
              .build();

      WatsonxAiChatOptions options2 =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .includeReasoning(false)
              .reasoningEffort("low")
              .build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    void setReasoningEffortValidatesAllowedValues() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      assertAll(
          "Reasoning effort validation",
          () -> assertDoesNotThrow(() -> options.setReasoningEffort("low")),
          () -> assertDoesNotThrow(() -> options.setReasoningEffort("medium")),
          () -> assertDoesNotThrow(() -> options.setReasoningEffort("high")),
          () -> assertDoesNotThrow(() -> options.setReasoningEffort(null)));
    }

    @Test
    void setReasoningEffortThrowsForInvalidValue() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test-model").build();

      assertThrows(
          IllegalArgumentException.class,
          () -> options.setReasoningEffort("invalid"),
          "reasoning_effort must be one of [low, medium, high]");
    }
  }

  private static ToolCallback mockToolCallback(String name) {
    return new ToolCallback() {
      @Override
      public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder().name(name).description("desc").build();
      }

      @Override
      public String call(String input) {
        return "result";
      }
    };
  }

  @Nested
  class DirectSetterTests {

    @Test
    void testSetTemperature() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setTemperature(0.5);
      assertEquals(0.5, options.getTemperature());
    }

    @Test
    void testSetTopP() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setTopP(0.95);
      assertEquals(0.95, options.getTopP());
    }

    @Test
    void testSetStopSequences() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setStopSequences(List.of("STOP", "END"));
      assertEquals(List.of("STOP", "END"), options.getStopSequences());
    }

    @Test
    void testSetPresencePenalty() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setPresencePenalty(0.5);
      assertEquals(0.5, options.getPresencePenalty());
    }

    @Test
    void testSetFrequencyPenalty() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setFrequencyPenalty(0.3);
      assertEquals(0.3, options.getFrequencyPenalty());
    }

    @Test
    void testSetSeed() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setSeed(123);
      assertEquals(123, options.getSeed());
    }

    @Test
    void testSetGuidedChoice() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setGuidedChoice(List.of("a", "b"));
      assertEquals(List.of("a", "b"), options.getGuidedChoice());
    }

    @Test
    void testSetGuidedRegex() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setGuidedRegex("[a-z]+");
      assertEquals("[a-z]+", options.getGuidedRegex());
    }

    @Test
    void testSetGuidedGrammar() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setGuidedGrammar("S -> a S b | epsilon");
      assertEquals("S -> a S b | epsilon", options.getGuidedGrammar());
    }

    @Test
    void testSetGuidedJsonString() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setGuidedJson(
          "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
      assertNotNull(options.getGuidedJson());
      assertEquals("object", options.getGuidedJson().get("type"));
    }

    @Test
    void testSetGuidedJsonMap() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Map<String, Object> json = Map.of("type", "object");
      options.setGuidedJson(json);
      assertEquals(json, options.getGuidedJson());
    }

    @Test
    void testSetChatTemplateKwargsString() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setChatTemplateKwargs("{\"add_generation_prompt\":true}");
      assertNotNull(options.getChatTemplateKwargs());
      assertEquals(true, options.getChatTemplateKwargs().get("add_generation_prompt"));
    }

    @Test
    void testSetChatTemplateKwargsMap() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Map<String, Object> kwargs = Map.of("key", "value");
      options.setChatTemplateKwargs(kwargs);
      assertEquals(kwargs, options.getChatTemplateKwargs());
    }

    @Test
    void testSetIncludeReasoning() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setIncludeReasoning(false);
      assertFalse(options.isIncludeReasoning());
    }

    @Test
    void testSetReasoningEffort() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setReasoningEffort("high");
      assertEquals("high", options.getReasoningEffort());
    }

    @Test
    void testSetModel() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setModel("test-model");
      assertEquals("test-model", options.getModel());
    }

    @Test
    void testSetTools() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      List<WatsonxAiChatRequest.TextChatParameterTool> tools = List.of();
      options.setTools(tools);
      assertEquals(tools, options.getTools());
    }

    @Test
    void testSetToolChoiceOption() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setToolChoiceOption("auto");
      assertEquals("auto", options.getToolChoiceOption());
    }

    @Test
    void testSetToolChoice() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      WatsonxAiChatRequest.TextChatToolChoiceTool toolChoice =
          new WatsonxAiChatRequest.TextChatToolChoiceTool(
              ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("test_tool"));
      options.setToolChoice(toolChoice);
      assertEquals(toolChoice, options.getToolChoice());
    }

    @Test
    void testSetInternalToolExecutionEnabled() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setInternalToolExecutionEnabled(true);
      assertTrue(options.getInternalToolExecutionEnabled());
    }

    @Test
    void testSetInternalToolExecutionEnabledFalse() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setInternalToolExecutionEnabled(false);
      assertFalse(options.getInternalToolExecutionEnabled());
    }

    @Test
    void testSetToolContext() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Map<String, Object> ctx = Map.of("key", "val");
      options.setToolContext(ctx);
      assertEquals(ctx, options.getToolContext());
    }

    @Test
    void testSetLogitBias() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Map<String, Number> bias = Map.of("token", 1);
      options.setLogitBias(bias);
      assertEquals(bias, options.getLogitBias());
    }

    @Test
    void testSetLogprobsTrue() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setLogprobs(true);
      assertTrue(options.getLogprobs());
    }

    @Test
    void testSetLogprobsFalse() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setLogprobs(false);
      assertFalse(options.getLogprobs());
    }

    @Test
    void testSetLogprobsFalseWithTopLogprobsThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setTopLogprobs(5);
      assertThrows(
          IllegalArgumentException.class,
          () -> options.setLogprobs(false),
          "logprobs cannot be false when using topLogprobs");
    }

    @Test
    void testSetTopLogprobs() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setLogprobs(true);
      options.setTopLogprobs(5);
      assertEquals(5, options.getTopLogprobs());
    }

    @Test
    void testSetTopLogprobsThrowsWhenLogprobsFalse() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setLogprobs(false);
      assertThrows(
          IllegalArgumentException.class,
          () -> options.setTopLogprobs(5),
          "logprobs cannot be false when using topLogprobs");
    }

    @Test
    void testSetTopLogprobsWhenLogprobsNull() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setTopLogprobs(5);
      assertEquals(5, options.getTopLogprobs());
    }

    @Test
    void testSetMaxTokens() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setMaxTokens(2048);
      assertEquals(2048, options.getMaxTokens());
    }

    @Test
    void testSetMaxCompletionTokens() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setMaxCompletionTokens(4096);
      assertEquals(4096, options.getMaxCompletionTokens());
    }

    @Test
    void testSetN() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setN(3);
      assertEquals(3, options.getN());
    }

    @Test
    void testSetTimeLimit() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setTimeLimit(5000);
      assertEquals(5000, options.getTimeLimit());
    }

    @Test
    void testSetTimeLimitZeroThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(
          IllegalArgumentException.class,
          () -> options.setTimeLimit(0),
          "Time limit must be greater than 0");
    }

    @Test
    void testSetTimeLimitNegativeThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(
          IllegalArgumentException.class,
          () -> options.setTimeLimit(-100),
          "Time limit must be greater than 0");
    }

    @Test
    void testSetResponseFormat() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      TextChatResponseFormat fmt = TextChatResponseFormat.jsonObject();
      options.setResponseFormat(fmt);
      assertEquals(fmt, options.getResponseFormat());
    }

    @Test
    void testSetToolCallbacks() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.setToolCallbacks(List.of(mockToolCallback("test")));
      assertEquals(1, options.getToolCallbacks().size());
    }

    @Test
    void testSetToolCallbacksNullThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(IllegalArgumentException.class, () -> options.setToolCallbacks(null));
    }

    @Test
    void testSetToolCallbacksNullElementThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(Exception.class, () -> options.setToolCallbacks(List.of(null)));
    }

    @Test
    void testSetToolNames() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Set<String> names = Set.of("tool1", "tool2");
      options.setToolNames(names);
      assertEquals(names, options.getToolNames());
    }

    @Test
    void testSetToolNamesNullThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(IllegalArgumentException.class, () -> options.setToolNames(null));
    }

    @Test
    void testSetToolNamesEmptyStringThrows() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertThrows(IllegalArgumentException.class, () -> options.setToolNames(Set.of("")));
    }
  }

  @Nested
  class AdditionalPropertiesTests {

    @Test
    void testAddAdditionalProperty() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.addAdditionalProperty("customKey", "customValue");
      Map<String, Object> props = options.getAdditionalProperties();
      assertEquals("customValue", props.get("custom_key"));
    }

    @Test
    void testAddAdditionalPropertyMultiple() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.addAdditionalProperty("firstKey", 1);
      options.addAdditionalProperty("secondKey", "two");
      Map<String, Object> props = options.getAdditionalProperties();
      assertEquals(2, props.size());
      assertEquals(1, props.get("first_key"));
      assertEquals("two", props.get("second_key"));
    }

    @Test
    void testGetAdditionalPropertiesEmpty() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      Map<String, Object> props = options.getAdditionalProperties();
      assertNotNull(props);
      assertTrue(props.isEmpty());
    }

    @Test
    void testFilterNonSupportedFieldsRemovesModel() {
      Map<String, Object> options = new HashMap<>();
      options.put("model", "test");
      options.put("temperature", 0.7);
      Map<String, Object> filtered = WatsonxAiChatOptions.filterNonSupportedFields(options);
      assertFalse(filtered.containsKey("model"));
      assertEquals(0.7, filtered.get("temperature"));
    }

    @Test
    void testFilterNonSupportedFieldsRemovesNullValues() {
      Map<String, Object> options = new HashMap<>();
      options.put("key1", "value1");
      options.put("key2", null);
      Map<String, Object> filtered = WatsonxAiChatOptions.filterNonSupportedFields(options);
      assertEquals(1, filtered.size());
      assertEquals("value1", filtered.get("key1"));
    }

    @Test
    void testFilterNonSupportedFieldsEmpty() {
      Map<String, Object> options = new HashMap<>();
      Map<String, Object> filtered = WatsonxAiChatOptions.filterNonSupportedFields(options);
      assertTrue(filtered.isEmpty());
    }

    @Test
    void testToMap() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("test-model").temperature(0.5).build();
      Map<String, Object> map = options.toMap();
      assertNotNull(map);
      assertEquals("test-model", map.get("model_id"));
      assertEquals(0.5, map.get("temperature"));
      assertFalse(map.containsKey("additional"));
    }

    @Test
    void testToMapWithAdditionalProperties() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .additionalProperty("customKey", "customValue")
              .build();
      Map<String, Object> map = options.toMap();
      assertNotNull(map);
      assertEquals("customValue", map.get("custom_key"));
    }

    @Test
    void testToSnakeCaseViaAdditionalProperties() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.addAdditionalProperty("myCustomKey", "val");
      Map<String, Object> props = options.getAdditionalProperties();
      assertTrue(props.containsKey("my_custom_key"));
    }

    @Test
    void testToSnakeCaseSimpleKey() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      options.addAdditionalProperty("camelCase", "val");
      Map<String, Object> props = options.getAdditionalProperties();
      assertTrue(props.containsKey("camel_case"));
    }
  }

  @Nested
  class FromOptionsWithTopLogprobsTests {

    @Test
    void testFromOptionsCopiesTopLogprobsWhenLogprobsTrue() {
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder().model("test-model").logProbs(true).topLogprobs(5).build();
      WatsonxAiChatOptions copied = WatsonxAiChatOptions.fromOptions(original);
      assertEquals(5, copied.getTopLogprobs());
      assertTrue(copied.getLogprobs());
    }

    @Test
    void testFromOptionsDoesNotCopyTopLogprobsWhenLogprobsFalse() {
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder().model("test-model").logProbs(false).build();
      WatsonxAiChatOptions copied = WatsonxAiChatOptions.fromOptions(original);
      assertNull(copied.getTopLogprobs());
    }

    @Test
    void testFromOptionsDoesNotCopyTopLogprobsWhenLogprobsNull() {
      WatsonxAiChatOptions original = WatsonxAiChatOptions.builder().model("test-model").build();
      WatsonxAiChatOptions copied = WatsonxAiChatOptions.fromOptions(original);
      assertNull(copied.getTopLogprobs());
    }

    @Test
    void testFromOptionsCopiesAllFields() {
      Map<String, Object> kwargs = Map.of("key", "value");
      Map<String, Number> bias = Map.of("t", 1);
      WatsonxAiChatOptions original =
          WatsonxAiChatOptions.builder()
              .model("test-model")
              .temperature(0.5)
              .topP(0.9)
              .stopSequences(List.of("STOP"))
              .presencePenalty(0.1)
              .guidedChoice(List.of("a"))
              .guidedRegex("[a-z]")
              .guidedGrammar("S->a")
              .guidedJson(Map.of("type", "object"))
              .chatTemplateKwargs(kwargs)
              .includeReasoning(false)
              .reasoningEffort("high")
              .seed(42)
              .internalToolExecutionEnabled(true)
              .logitBias(bias)
              .logProbs(true)
              .maxTokens(1024)
              .maxCompletionTokens(2048)
              .n(3)
              .build();
      original.setTimeLimit(5000);
      original.setFrequencyPenalty(0.2);
      original.setToolChoiceOption("auto");
      original.setToolContext(Map.of("k", "v"));

      WatsonxAiChatOptions copied = WatsonxAiChatOptions.fromOptions(original);

      assertEquals(original.getModel(), copied.getModel());
      assertEquals(original.getTemperature(), copied.getTemperature());
      assertEquals(original.getTopP(), copied.getTopP());
      assertEquals(original.getStopSequences(), copied.getStopSequences());
      assertEquals(original.getPresencePenalty(), copied.getPresencePenalty());
      assertEquals(original.getGuidedChoice(), copied.getGuidedChoice());
      assertEquals(original.getGuidedRegex(), copied.getGuidedRegex());
      assertEquals(original.getGuidedGrammar(), copied.getGuidedGrammar());
      assertEquals(original.getGuidedJson(), copied.getGuidedJson());
      assertEquals(original.getChatTemplateKwargs(), copied.getChatTemplateKwargs());
      assertEquals(original.isIncludeReasoning(), copied.isIncludeReasoning());
      assertEquals(original.getReasoningEffort(), copied.getReasoningEffort());
      assertEquals(original.getSeed(), copied.getSeed());
      assertEquals(
          original.getInternalToolExecutionEnabled(), copied.getInternalToolExecutionEnabled());
      assertEquals(original.getLogitBias(), copied.getLogitBias());
      assertEquals(original.getLogprobs(), copied.getLogprobs());
      assertEquals(original.getMaxTokens(), copied.getMaxTokens());
      assertEquals(original.getMaxCompletionTokens(), copied.getMaxCompletionTokens());
      assertEquals(original.getN(), copied.getN());
      assertEquals(original.getToolChoiceOption(), copied.getToolChoiceOption());
      assertEquals(original.getToolContext(), copied.getToolContext());
    }
  }

  @Nested
  class ToolCallbacksTests {

    @Test
    void testGetToolCallbacksEmptyByDefault() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertNotNull(options.getToolCallbacks());
      assertTrue(options.getToolCallbacks().isEmpty());
    }

    @Test
    void testGetToolCallbacksViaBuilder() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().toolCallbacks(List.of(mockToolCallback("test"))).build();
      assertEquals(1, options.getToolCallbacks().size());
    }
  }

  @Nested
  class GetterReturnValueTests {

    @Test
    void testGetFrequencyPenaltyReturnsExactValue() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("m").build();
      options.setFrequencyPenalty(1.5);
      assertEquals(1.5, options.getFrequencyPenalty());
    }

    @Test
    void testGetInternalToolExecutionEnabledReturnsTrue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(true).build();
      assertTrue(options.getInternalToolExecutionEnabled());
    }

    @Test
    void testGetInternalToolExecutionEnabledReturnsFalse() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(false).build();
      assertFalse(options.getInternalToolExecutionEnabled());
    }

    @Test
    void testGetLogitBiasReturnsExactMap() {
      Map<String, Number> bias = new HashMap<>();
      bias.put("a", 1);
      bias.put("b", -2);
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").logitBias(bias).build();
      assertEquals(bias, options.getLogitBias());
    }

    @Test
    void testGetLogprobsReturnsExactValue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").logProbs(true).build();
      assertTrue(options.getLogprobs());
    }

    @Test
    void testGetMaxCompletionTokensReturnsExactValue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").maxCompletionTokens(4096).build();
      assertEquals(4096, options.getMaxCompletionTokens());
    }

    @Test
    void testGetTimeLimitReturnsExactValue() {
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("m").build();
      options.setTimeLimit(3000);
      assertEquals(3000, options.getTimeLimit());
    }

    @Test
    void testGetToolChoiceReturnsExactObject() {
      WatsonxAiChatRequest.TextChatToolChoiceTool tc =
          new WatsonxAiChatRequest.TextChatToolChoiceTool(
              ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("my_tool"));
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").toolChoice(tc).build();
      assertEquals(tc, options.getToolChoice());
    }

    @Test
    void testGetToolChoiceOptionReturnsExactValue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").toolChoiceOption("required").build();
      assertEquals("required", options.getToolChoiceOption());
    }

    @Test
    void testGetToolContextReturnsExactMap() {
      Map<String, Object> ctx = Map.of("k1", "v1", "k2", 42);
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").toolContext(ctx).build();
      assertEquals(ctx, options.getToolContext());
    }

    @Test
    void testGetToolNamesReturnsExactSet() {
      Set<String> names = Set.of("t1", "t2", "t3");
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").toolNames(names).build();
      assertEquals(names, options.getToolNames());
    }

    @Test
    void testGetToolsReturnsExactList() {
      List<WatsonxAiChatRequest.TextChatParameterTool> tools = List.of();
      WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("m").tools(tools).build();
      assertEquals(tools, options.getTools());
    }

    @Test
    void testGetTopKReturnsNull() {
      WatsonxAiChatOptions options = new WatsonxAiChatOptions();
      assertNull(options.getTopK());
    }

    @Test
    void testGetTopLogprobsReturnsExactValue() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").logProbs(true).topLogprobs(10).build();
      assertEquals(10, options.getTopLogprobs());
    }

    @Test
    void testGetAdditionalPropertiesReturnsNonEmpty() {
      WatsonxAiChatOptions options =
          WatsonxAiChatOptions.builder().model("m").additionalProperty("myKey", "myVal").build();
      Map<String, Object> props = options.getAdditionalProperties();
      assertNotNull(props);
      assertFalse(props.isEmpty());
      assertEquals("myVal", props.get("my_key"));
    }
  }

  @Nested
  class EqualsAndHashCodeFieldTests {

    @Test
    void testEqualsDiffersByTemperature() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().temperature(0.5).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().temperature(0.6).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByTopP() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().topP(0.5).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().topP(0.6).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByStopSequences() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().stopSequences(List.of("A")).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().stopSequences(List.of("B")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByPresencePenalty() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().presencePenalty(0.1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().presencePenalty(0.2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByFrequencyPenalty() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().build();
      o1.setFrequencyPenalty(0.1);
      o2.setFrequencyPenalty(0.2);
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByGuidedChoice() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().guidedChoice(List.of("a")).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().guidedChoice(List.of("b")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByGuidedRegex() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().guidedRegex("a").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().guidedRegex("b").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByGuidedGrammar() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().guidedGrammar("a").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().guidedGrammar("b").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByGuidedJson() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().guidedJson(Map.of("k", "a")).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().guidedJson(Map.of("k", "b")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByChatTemplateKwargs() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().chatTemplateKwargs(Map.of("k", "a")).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().chatTemplateKwargs(Map.of("k", "b")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByIncludeReasoning() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().includeReasoning(true).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().includeReasoning(false).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByReasoningEffort() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().reasoningEffort("low").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().reasoningEffort("high").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersBySeed() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().seed(1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().seed(2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByModel() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().model("a").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().model("b").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByTools() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().tools(List.of()).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().tools(null).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByToolChoiceOption() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().toolChoiceOption("auto").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().toolChoiceOption("required").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByToolChoice() {
      WatsonxAiChatRequest.TextChatToolChoiceTool tc1 =
          new WatsonxAiChatRequest.TextChatToolChoiceTool(
              ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("t1"));
      WatsonxAiChatRequest.TextChatToolChoiceTool tc2 =
          new WatsonxAiChatRequest.TextChatToolChoiceTool(
              ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("t2"));
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().toolChoice(tc1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().toolChoice(tc2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByToolCallbacks() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().toolCallbacks(List.of(mockToolCallback("cb"))).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByToolNames() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().toolNames(Set.of("t1")).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().toolNames(Set.of("t2")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByInternalToolExecutionEnabled() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(true).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(false).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByToolContext() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().toolContext(Map.of("k", "a")).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().toolContext(Map.of("k", "b")).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByLogitBias() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().logitBias(Map.of("t", 1)).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().logitBias(Map.of("t", 2)).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByLogprobs() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().logProbs(true).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().logProbs(false).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByTopLogprobs() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().logProbs(true).topLogprobs(1).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().logProbs(true).topLogprobs(2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByMaxTokens() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().maxTokens(100).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().maxTokens(200).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByMaxCompletionTokens() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().maxCompletionTokens(100).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().maxCompletionTokens(200).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByN() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().n(1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().n(2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByTimeLimit() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().build();
      o1.setTimeLimit(1000);
      o2.setTimeLimit(2000);
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByResponseFormat() {
      TextChatResponseFormat f1 = TextChatResponseFormat.jsonObject();
      TextChatResponseFormat f2 = TextChatResponseFormat.text();
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().responseFormat(f1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().responseFormat(f2).build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testEqualsDiffersByAdditional() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().additionalProperty("k", "a").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().additionalProperty("k", "b").build();
      assertNotEquals(o1, o2);
    }

    @Test
    void testHashCodeDiffersByTemperature() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().temperature(0.5).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().temperature(0.6).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByModel() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().model("a").build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().model("b").build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersBySeed() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().seed(1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().seed(2).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByN() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().n(1).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().n(2).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByMaxTokens() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().maxTokens(100).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().maxTokens(200).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByLogprobs() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().logProbs(true).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().logProbs(false).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByIncludeReasoning() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().includeReasoning(true).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().includeReasoning(false).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByInternalToolExecutionEnabled() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(true).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().internalToolExecutionEnabled(false).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByTopLogprobs() {
      WatsonxAiChatOptions o1 =
          WatsonxAiChatOptions.builder().logProbs(true).topLogprobs(1).build();
      WatsonxAiChatOptions o2 =
          WatsonxAiChatOptions.builder().logProbs(true).topLogprobs(2).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByMaxCompletionTokens() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().maxCompletionTokens(100).build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().maxCompletionTokens(200).build();
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    void testHashCodeDiffersByTimeLimit() {
      WatsonxAiChatOptions o1 = WatsonxAiChatOptions.builder().build();
      WatsonxAiChatOptions o2 = WatsonxAiChatOptions.builder().build();
      o1.setTimeLimit(1000);
      o2.setTimeLimit(2000);
      assertNotEquals(o1.hashCode(), o2.hashCode());
    }
  }
}
