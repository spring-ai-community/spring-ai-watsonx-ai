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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
