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

import java.util.List;
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
}
