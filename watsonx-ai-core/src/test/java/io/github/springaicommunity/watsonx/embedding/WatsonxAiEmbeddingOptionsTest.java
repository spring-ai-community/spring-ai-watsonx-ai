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
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertAll(
          "Basic options validation",
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options.getModel()),
          () -> assertNotNull(options.getParameters()),
          () -> assertEquals(512, options.getParameters().truncateInputTokens()),
          () -> assertNull(options.getDimensions()));
    }

    @Test
    void createOptionsWithMinimumRequiredFields() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("ibm/slate-30m-english-rtrvr").build();

      assertAll(
          "Minimal options validation",
          () -> assertEquals("ibm/slate-30m-english-rtrvr", options.getModel()),
          () -> assertNull(options.getParameters()),
          () -> assertNull(options.getDimensions()));
    }

    @Test
    void createEmptyOptions() {
      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();

      assertAll(
          "Empty options validation",
          () -> assertNull(options.getModel()),
          () -> assertNull(options.getParameters()),
          () -> assertNull(options.getDimensions()));
    }
  }

  @Nested
  class AdvancedOptionsTests {

    @Test
    void createAdvancedOptionsWithAllParameters() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, returnOptions);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertAll(
          "Advanced options validation",
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options.getModel()),
          () -> assertNotNull(options.getParameters()),
          () -> assertEquals(1024, options.getParameters().truncateInputTokens()),
          () -> assertNotNull(options.getParameters().returnOptions()),
          () -> assertEquals(true, options.getParameters().returnOptions().inputText()));
    }

    @Test
    void handleNullParameters() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-30m-english-rtrvr")
              .parameters(null)
              .build();

      assertNull(options.getParameters());
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
        WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
            new WatsonxAiEmbeddingRequest.EmbeddingParameters(256, null);

        WatsonxAiEmbeddingOptions options =
            WatsonxAiEmbeddingOptions.builder().model(model).parameters(parameters).build();

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
            WatsonxAiEmbeddingRequest.EmbeddingParameters lowParams =
                new WatsonxAiEmbeddingRequest.EmbeddingParameters(1, null);
            WatsonxAiEmbeddingOptions lowTokens =
                WatsonxAiEmbeddingOptions.builder().parameters(lowParams).build();
            assertEquals(1, lowTokens.getParameters().truncateInputTokens());
          },
          () -> {
            WatsonxAiEmbeddingRequest.EmbeddingParameters midParams =
                new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);
            WatsonxAiEmbeddingOptions midTokens =
                WatsonxAiEmbeddingOptions.builder().parameters(midParams).build();
            assertEquals(512, midTokens.getParameters().truncateInputTokens());
          },
          () -> {
            WatsonxAiEmbeddingRequest.EmbeddingParameters highParams =
                new WatsonxAiEmbeddingRequest.EmbeddingParameters(2048, null);
            WatsonxAiEmbeddingOptions highTokens =
                WatsonxAiEmbeddingOptions.builder().parameters(highParams).build();
            assertEquals(2048, highTokens.getParameters().truncateInputTokens());
          });
    }

    @Test
    void handleParametersWithInputTextFlag() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, returnOptions);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertTrue(options.getParameters().returnOptions().inputText());
    }

    @Test
    void handleParametersWithInputTextFlagFalse() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(false);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, returnOptions);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertFalse(options.getParameters().returnOptions().inputText());
    }
  }

  @Nested
  class BuilderPatternTests {

    @Test
    void supportMethodChaining() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, returnOptions);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(parameters)
              .build();

      assertNotNull(options);
      assertEquals("ibm/slate-125m-english-rtrvr", options.getModel());
    }

    @Test
    void createMultipleInstancesIndependently() {
      WatsonxAiEmbeddingRequest.EmbeddingParameters params1 =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(256, null);
      WatsonxAiEmbeddingRequest.EmbeddingParameters params2 =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

      WatsonxAiEmbeddingOptions options1 =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-125m-english-rtrvr")
              .parameters(params1)
              .build();

      WatsonxAiEmbeddingOptions options2 =
          WatsonxAiEmbeddingOptions.builder()
              .model("ibm/slate-30m-english-rtrvr")
              .parameters(params2)
              .build();

      assertAll(
          "Multiple instances validation",
          () -> assertNotSame(options1, options2),
          () -> assertEquals("ibm/slate-125m-english-rtrvr", options1.getModel()),
          () -> assertEquals("ibm/slate-30m-english-rtrvr", options2.getModel()),
          () -> assertEquals(256, options1.getParameters().truncateInputTokens()),
          () -> assertEquals(512, options2.getParameters().truncateInputTokens()));
    }

    @Test
    void supportToBuilderPattern() {
      WatsonxAiEmbeddingRequest.EmbeddingParameters originalParams =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(256, null);

      WatsonxAiEmbeddingOptions original =
          WatsonxAiEmbeddingOptions.builder()
              .model("original-model")
              .parameters(originalParams)
              .build();

      WatsonxAiEmbeddingRequest.EmbeddingParameters modifiedParams =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

      WatsonxAiEmbeddingOptions modified =
          original.toBuilder().model("modified-model").parameters(modifiedParams).build();

      assertAll(
          "ToBuilder pattern validation",
          () -> assertEquals("original-model", original.getModel()),
          () -> assertEquals(256, original.getParameters().truncateInputTokens()),
          () -> assertEquals("modified-model", modified.getModel()),
          () -> assertEquals(512, modified.getParameters().truncateInputTokens()));
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
    void setParametersDirectly() {
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, null);

      WatsonxAiEmbeddingOptions options = new WatsonxAiEmbeddingOptions();
      options.setParameters(parameters);

      assertEquals(1024, options.getParameters().truncateInputTokens());
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
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, returnOptions);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("test-model").parameters(parameters).build();

      assertAll(
          "Options validation",
          () -> assertNotNull(options),
          () -> assertEquals("test-model", options.getModel()),
          () -> assertNotNull(options.getParameters()),
          () -> assertTrue(options.getParameters().truncateInputTokens() > 0),
          () -> assertTrue(options.getParameters().returnOptions().inputText()));
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
