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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 test class for WatsonxAiChatOptions functionality and configuration. Tests the chat
 * options builder pattern and configuration validation.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
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
}
