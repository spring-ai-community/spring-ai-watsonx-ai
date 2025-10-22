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

package io.github.springaicommunity.watsonx.embedding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 test class for WatsonxAiEmbeddingOptions functionality and configuration. Tests the
 * embedding options builder pattern and configuration validation.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiEmbeddingOptionsTest {

  @Nested
  class BasicOptionsTests {

    @Test
    void createBasicEmbeddingOptionsWithDefaultValues() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .truncateInputTokens(512)
              .build();

      assertAll(
          "Basic options validation",
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options.getModel()),
          () -> assertEquals(512, options.getTruncateInputTokens()),
          () -> assertNull(options.getParameters()),
          () -> assertNull(options.getDimensions()));
    }

    @Test
    void createOptionsWithMinimumRequiredFields() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("ibm/slate-30m-english-rtrvr").build();

      assertAll(
          "Minimal options validation",
          () -> assertEquals("ibm/slate-30m-english-rtrvr", options.getModel()),
          () -> assertNull(options.getTruncateInputTokens()),
          () -> assertNull(options.getParameters()),
          () -> assertNull(options.getDimensions()));
    }

    @Test
    void createEmptyOptions() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();

      assertAll(
          "Empty options validation",
          () -> assertNull(options.getModel()),
          () -> assertNull(options.getTruncateInputTokens()),
          () -> assertNull(options.getParameters()),
          () -> assertNull(options.getDimensions()));
    }
  }

  @Nested
  class AdvancedOptionsTests {

    @Test
    void createAdvancedOptionsWithAllParameters() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("input_text", true);
      parameters.put("custom_param", "value");

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .truncateInputTokens(1024)
              .parameters(parameters)
              .build();

      assertAll(
          "Advanced options validation",
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options.getModel()),
          () -> assertEquals(1024, options.getTruncateInputTokens()),
          () -> assertNotNull(options.getParameters()),
          () -> assertEquals(true, options.getParameters().get("input_text")),
          () -> assertEquals("value", options.getParameters().get("custom_param")));
    }

    @Test
    void handleEmptyParameters() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-30m-english-rtrvr")
              .parameters(new HashMap<>())
              .build();

      assertNotNull(options.getParameters());
      assertTrue(options.getParameters().isEmpty());
    }
  }

  @Nested
  class ModelSelectionTests {

    @Test
    void supportDifferentIBMSlateModels() {
      String[] supportedModels = {
        "ibm/slate-125m-english-rtrvr",
        "ibm/slate-30m-english-rtrvr",
        "sentence-transformers/all-minilm-l6-v2"
      };

      for (String model : supportedModels) {
        WatsonxAiEmbeddingOptions options =
            WatsonxAiEmbeddingOptions.builder().model(model).truncateInputTokens(256).build();

        assertEquals(model, options.getModel(), "Model should be set correctly for: " + model);
      }
    }
  }

  @Nested
  class ParameterTests {

    @Test
    void acceptValidTruncateInputTokensRange() {
      assertAll(
          "TruncateInputTokens range validation",
          () -> {
            WatsonxAiEmbeddingOptions lowTokens =
                WatsonxAiEmbeddingOptions.builder().truncateInputTokens(1).build();
            assertEquals(1, lowTokens.getTruncateInputTokens());
          },
          () -> {
            WatsonxAiEmbeddingOptions midTokens =
                WatsonxAiEmbeddingOptions.builder().truncateInputTokens(512).build();
            assertEquals(512, midTokens.getTruncateInputTokens());
          },
          () -> {
            WatsonxAiEmbeddingOptions highTokens =
                WatsonxAiEmbeddingOptions.builder().truncateInputTokens(2048).build();
            assertEquals(2048, highTokens.getTruncateInputTokens());
          });
    }

    @Test
    void handleParametersWithInputTextFlag() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("input_text", true);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertTrue((Boolean) options.getParameters().get("input_text"));
    }

    @Test
    void handleParametersWithInputTextFlagFalse() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("input_text", false);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertFalse((Boolean) options.getParameters().get("input_text"));
    }
  }

  @Nested
  class BuilderPatternTests {

    @Test
    void supportMethodChaining() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("input_text", true);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .truncateInputTokens(512)
              .parameters(parameters)
              .build();

      assertNotNull(options);
      assertEquals("ibm/slate-125m-english-rtrvr", options.getModel());
    }

    @Test
    void createMultipleInstancesIndependently() {
      WatsonxAiEmbeddingOptions options1 =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .truncateInputTokens(256)
              .build();

      WatsonxAiEmbeddingOptions options2 =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-30m-english-rtrvr")
              .truncateInputTokens(512)
              .build();

      assertAll(
          "Multiple instances validation",
          () -> assertNotSame(options1, options2),
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options1.getModel()),
          () -> assertEquals("ibm/slate-30m-english-rtrvr", options2.getModel()),
          () -> assertEquals(256, options1.getTruncateInputTokens()),
          () -> assertEquals(512, options2.getTruncateInputTokens()));
    }

    @Test
    void supportToBuilderPattern() {
      WatsonxAiEmbeddingOptions original =
          WatsonxAiEmbeddingOptions.builder()
              .model("original-model")
              .truncateInputTokens(256)
              .build();

      WatsonxAiEmbeddingOptions modified =
          original.toBuilder().model("modified-model").truncateInputTokens(512).build();

      assertAll(
          "ToBuilder pattern validation",
          () -> assertEquals("original-model", original.getModel()),
          () -> assertEquals(256, original.getTruncateInputTokens()),
          () -> assertEquals("modified-model", modified.getModel()),
          () -> assertEquals(512, modified.getTruncateInputTokens()));
    }
  }

  @Nested
  class SetterTests {

    @Test
    void setModelDirectly() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setModel("test-model");

      assertEquals("test-model", options.getModel());
    }

    @Test
    void setTruncateInputTokensDirectly() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setTruncateInputTokens(1024);

      assertEquals(1024, options.getTruncateInputTokens());
    }

    @Test
    void setParametersDirectly() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("test", "value");

      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setParameters(parameters);

      assertEquals("value", options.getParameters().get("test"));
    }

    @Test
    void setDimensionsIgnored() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setDimensions(768); // Should be ignored

      assertNull(options.getDimensions()); // Watson AI doesn't support dimensions
    }

    @Test
    void setEncodingFormatIgnored() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setEncodingFormat("float"); // Should be ignored

      assertNull(options.getEncodingFormat()); // Watson AI doesn't support encoding format
    }
  }

  @Nested
  class ValidationTests {

    @Test
    void validateOptionsState() {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("input_text", true);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("test-model")
              .truncateInputTokens(512)
              .parameters(parameters)
              .build();

      assertAll(
          "Options validation",
          () -> assertNotNull(options),
          () -> assertEquals("test-model", options.getModel()),
          () -> assertTrue(options.getTruncateInputTokens() > 0),
          () -> assertNotNull(options.getParameters()),
          () -> assertTrue((Boolean) options.getParameters().get("input_text")));
    }

    @Test
    void validateBuilderIgnoresUnsupportedParameters() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("test-model")
              .encodingFormat("float") // Should be ignored
              .build();

      assertEquals("test-model", options.getModel());
      assertNull(options.getEncodingFormat());
    }
  }

  @Nested
  class UnsupportedParametersTests {

    @Test
    void dimensionsAlwaysReturnsNull() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("test-model").build();

      assertNull(options.getDimensions());
    }

    @Test
    void encodingFormatAlwaysReturnsNull() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("test-model").build();

      assertNull(options.getEncodingFormat());
    }
  }
}
