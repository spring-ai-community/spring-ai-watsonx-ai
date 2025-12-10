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

package org.springaicommunity.watsonx.embedding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

/**
 * JUnit 5 test class for WatsonxAiEmbeddingModel functionality. Tests embedding model operations
 * using mocking for external dependencies.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
class WatsonxAiEmbeddingModelTest {

  @Mock private WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;

  @Mock private RetryTemplate retryTemplate;

  private WatsonxAiEmbeddingModel embeddingModel;
  private WatsonxAiEmbeddingOptions defaultOptions;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create default options for testing with EmbeddingParameters
    WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
        new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

    defaultOptions =
        WatsonxAiEmbeddingOptions.builder()
            .model("ibm/slate-125m-english-rtrvr")
            .parameters(parameters)
            .build();

    // Initialize the embedding model
    embeddingModel =
        new WatsonxAiEmbeddingModel(watsonxAiEmbeddingApi, defaultOptions, retryTemplate);

    // Mock retry template to execute directly without retry logic
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              org.springframework.retry.RetryCallback<Object, Exception> callback =
                  (org.springframework.retry.RetryCallback<Object, Exception>)
                      invocation.getArgument(0);
              return callback.doWithRetry(null);
            })
        .when(retryTemplate)
        .execute(any());
  }

  @Nested
  class ConstructorTests {

    @Test
    void constructorWithValidParameters() {
      assertNotNull(embeddingModel);
      assertEquals(defaultOptions, embeddingModel.getDefaultOptions());
    }

    @Test
    void constructorWithNullApiThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiEmbeddingModel(null, defaultOptions, retryTemplate),
          "WatsonxAiEmbeddingApi must not be null");
    }

    @Test
    void constructorWithNullOptionsThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiEmbeddingModel(watsonxAiEmbeddingApi, null, retryTemplate),
          "WatsonxAiEmbeddingOptions must not be null");
    }

    @Test
    void constructorWithNullRetryTemplateThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiEmbeddingModel(watsonxAiEmbeddingApi, defaultOptions, null),
          "RetryTemplate must not be null");
    }
  }

  @Nested
  class CallMethodTests {

    @Test
    void callWithValidRequest() {

      List<String> instructions = List.of("Hello world", "How are you?");
      EmbeddingRequest request = new EmbeddingRequest(instructions, null);

      WatsonxAiEmbeddingResponse.Embedding result1 =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1, 0.2, 0.3),
              new WatsonxAiEmbeddingResponse.EmbeddingInputResult("Hello world"));
      WatsonxAiEmbeddingResponse.Embedding result2 =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.4, 0.5, 0.6),
              new WatsonxAiEmbeddingResponse.EmbeddingInputResult("How are you?"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse(
              "ibm/slate-125m-english-rtrvr", LocalDateTime.now(), List.of(result1, result2), 7);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertEquals(2, response.getResults().size());

      float[] embedding1 = response.getResults().get(0).getOutput();
      assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, embedding1, 0.001f);

      float[] embedding2 = response.getResults().get(1).getOutput();
      assertArrayEquals(new float[] {0.4f, 0.5f, 0.6f}, embedding2, 0.001f);

      assertEquals("ibm/slate-125m-english-rtrvr", response.getMetadata().getModel());
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }

    @Test
    void callWithNullRequestThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> embeddingModel.call(null),
          "EmbeddingRequest must not be null");
    }

    @Test
    void callWithEmptyInstructionsThrowsException() {
      EmbeddingRequest request = new EmbeddingRequest(List.of(), null);

      assertThrows(
          IllegalArgumentException.class,
          () -> embeddingModel.call(request),
          "EmbeddingRequest instructions must not be empty");
    }

    @Test
    void callWithNullInstructionsThrowsException() {
      EmbeddingRequest request = new EmbeddingRequest(null, null);

      assertThrows(
          IllegalArgumentException.class,
          () -> embeddingModel.call(request),
          "EmbeddingRequest instructions must not be empty");
    }

    @Test
    void callWithCustomOptions() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, returnOptions);

      WatsonxAiEmbeddingOptions customOptions =
          WatsonxAiEmbeddingOptions.builder().model("custom-model").parameters(parameters).build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), customOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1, 0.2), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("custom-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertEquals("custom-model", response.getMetadata().getModel());
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }
  }

  @Nested
  class EmbedDocumentTests {

    @Test
    void embedDocumentWithValidDocument() {
      Document document = new Document("This is a test document");

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1, 0.2, 0.3),
              new WatsonxAiEmbeddingResponse.EmbeddingInputResult("This is a test document"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse(
              "ibm/slate-125m-english-rtrvr", LocalDateTime.now(), List.of(result), 5);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      float[] embedding = embeddingModel.embed(document);

      assertNotNull(embedding);
      assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, embedding, 0.001f);
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }

    @Test
    void embedDocumentWithNullDocumentThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> embeddingModel.embed((Document) null),
          "Document must not be null");
    }
  }

  @Nested
  class EmbedTextTests {

    @Test
    void embedTextWithValidString() {
      String text = "Hello world";

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.7, 0.8, 0.9),
              new WatsonxAiEmbeddingResponse.EmbeddingInputResult("Hello world"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse(
              "ibm/slate-125m-english-rtrvr", LocalDateTime.now(), List.of(result), 2);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      float[] embedding = embeddingModel.embed(text);

      assertNotNull(embedding);
      assertArrayEquals(new float[] {0.7f, 0.8f, 0.9f}, embedding, 0.001f);
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }

    @Test
    void embedTextWithNullTextThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> embeddingModel.embed((String) null),
          "Text must not be null or empty");
    }

    @Test
    void embedTextWithEmptyTextThrowsException() {
      assertThrows(
          NullPointerException.class,
          () -> embeddingModel.embed(""),
          "Text must not be null or empty");
    }
  }

  @Nested
  class OptionsMergingTests {

    @Test
    void mergeOptionsWithRuntimeModel() {
      WatsonxAiEmbeddingOptions runtimeOptions =
          WatsonxAiEmbeddingOptions.builder().model("runtime-model").build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), runtimeOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("runtime-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertEquals("runtime-model", response.getMetadata().getModel());
    }

    @Test
    void mergeOptionsWithRuntimeDimensions() {
      WatsonxAiEmbeddingOptions runtimeOptions = WatsonxAiEmbeddingOptions.builder().build();
      runtimeOptions.setDimensions(768); // This should be ignored for Watson AI

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), runtimeOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse(
              "ibm/slate-125m-english-rtrvr", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);
      assertNotNull(response);
    }

    @Test
    void mergeOptionsWithWatsonxSpecificOptions() {
      WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
          new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, returnOptions);

      WatsonxAiEmbeddingOptions runtimeOptions =
          WatsonxAiEmbeddingOptions.builder().model("custom-model").parameters(parameters).build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), runtimeOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("custom-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertEquals("custom-model", response.getMetadata().getModel());
    }

    @Test
    void mergeOptionsWithEncodingFormat() {
      WatsonxAiEmbeddingOptions runtimeOptions =
          WatsonxAiEmbeddingOptions.builder()
              .model("custom-model")
              .encodingFormat("float") // This should be ignored for Watson AI but not cause errors
              .build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), runtimeOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("custom-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertEquals("custom-model", response.getMetadata().getModel());
    }

    @Test
    void mergeOptionsWithNonWatsonxOptions() {
      // Test that generic EmbeddingOptions (not WatsonxAiEmbeddingOptions) are handled correctly
      org.springframework.ai.embedding.EmbeddingOptions genericOptions =
          new org.springframework.ai.embedding.EmbeddingOptions() {
            @Override
            public String getModel() {
              return "generic-model";
            }

            @Override
            public Integer getDimensions() {
              return 768;
            }
          };

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), genericOptions);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("generic-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertEquals("generic-model", response.getMetadata().getModel());
    }
  }

  @Nested
  class ResponseHandlingTests {

    @Test
    void handleNullResponse() {
      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), null);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(null));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertTrue(response.getResults().isEmpty());
      assertEquals("unknown", response.getMetadata().getModel());
    }

    @Test
    void handleResponseWithNullResults() {
      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), null);

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("test-model", LocalDateTime.now(), null, 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertTrue(response.getResults().isEmpty());
      assertEquals("unknown", response.getMetadata().getModel());
    }

    @Test
    void handleResponseWithNullModelId() {
      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), null);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1, 0.2), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse(null, LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertEquals("unknown", response.getMetadata().getModel());
      assertEquals(1, response.getResults().size());
    }

    @Test
    void handleResponseWithNullInputTokenCount() {
      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), null);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1, 0.2), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));

      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("test-model", LocalDateTime.now(), List.of(result), null);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      assertEquals(0, response.getResults().get(0).getIndex()); // Should default to 0
    }
  }

  @Nested
  class ParameterCreationTests {

    @Test
    void createEmbeddingParametersWithTruncateTokensOnly() {
      WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
          new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("test-model").parameters(parameters).build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), options);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("test-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }

    @Test
    void createEmbeddingParametersWithNullValues() {
      WatsonxAiEmbeddingOptions options =
          WatsonxAiEmbeddingOptions.builder().model("test-model").build();

      EmbeddingRequest request = new EmbeddingRequest(List.of("test"), options);

      WatsonxAiEmbeddingResponse.Embedding result =
          new WatsonxAiEmbeddingResponse.Embedding(
              List.of(0.1), new WatsonxAiEmbeddingResponse.EmbeddingInputResult("test"));
      WatsonxAiEmbeddingResponse mockResponse =
          new WatsonxAiEmbeddingResponse("test-model", LocalDateTime.now(), List.of(result), 1);

      when(watsonxAiEmbeddingApi.embed(any(WatsonxAiEmbeddingRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      EmbeddingResponse response = embeddingModel.call(request);

      assertNotNull(response);
      verify(watsonxAiEmbeddingApi, times(1)).embed(any(WatsonxAiEmbeddingRequest.class));
    }
  }

  @Nested
  class DefaultOptionsTests {

    @Test
    void getDefaultOptionsReturnsCorrectOptions() {
      WatsonxAiEmbeddingOptions retrievedOptions = embeddingModel.getDefaultOptions();

      assertAll(
          "Default options validation",
          () -> assertEquals(defaultOptions, retrievedOptions),
          () -> assertEquals("ibm/slate-125m-english-rtrvr", retrievedOptions.getModel()),
          () -> assertNotNull(retrievedOptions.getParameters()),
          () -> assertEquals(512, retrievedOptions.getParameters().truncateInputTokens()));
    }
  }
}
