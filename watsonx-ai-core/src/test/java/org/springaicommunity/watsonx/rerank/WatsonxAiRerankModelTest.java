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

package org.springaicommunity.watsonx.rerank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

/**
 * JUnit 5 test class for WatsonxAiRerankModel functionality. Tests rerank model operations using
 * mocking for external dependencies.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiRerankModelTest {

  @Mock private WatsonxAiRerankApi watsonxAiRerankApi;

  @Mock private RetryTemplate retryTemplate;

  private WatsonxAiRerankModel rerankModel;
  private WatsonxAiRerankOptions defaultOptions;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    defaultOptions =
        WatsonxAiRerankOptions.builder()
            .model("cross-encoder/ms-marco-minilm-l-12-v2")
            .truncateInputTokens(512)
            .build();

    rerankModel =
        new WatsonxAiRerankModel(
            watsonxAiRerankApi, defaultOptions, ObservationRegistry.NOOP, retryTemplate);

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
      assertNotNull(rerankModel);
      assertEquals(defaultOptions, rerankModel.getDefaultOptions());
    }

    @Test
    void constructorWithNullApiThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WatsonxAiRerankModel(
                  null, defaultOptions, ObservationRegistry.NOOP, retryTemplate),
          "WatsonxAiRerankApi must not be null");
    }

    @Test
    void constructorWithNullOptionsThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WatsonxAiRerankModel(
                  watsonxAiRerankApi, null, ObservationRegistry.NOOP, retryTemplate),
          "WatsonxAiRerankOptions must not be null");
    }

    @Test
    void constructorWithNullObservationRegistryThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiRerankModel(watsonxAiRerankApi, defaultOptions, null, retryTemplate),
          "ObservationRegistry must not be null");
    }

    @Test
    void constructorWithNullRetryTemplateThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WatsonxAiRerankModel(
                  watsonxAiRerankApi, defaultOptions, ObservationRegistry.NOOP, null),
          "RetryTemplate must not be null");
    }
  }

  @Nested
  class RerankMethodTests {

    @Test
    void rerankWithValidRequest() {
      String query = "What is machine learning?";
      List<String> documents =
          List.of(
              "Machine learning is a subset of AI.",
              "Cooking recipes are great.",
              "Deep learning uses neural networks.");

      WatsonxAiRerankResponse.RerankResult result1 =
          new WatsonxAiRerankResponse.RerankResult(0, 0.95, null);
      WatsonxAiRerankResponse.RerankResult result2 =
          new WatsonxAiRerankResponse.RerankResult(2, 0.85, null);
      WatsonxAiRerankResponse.RerankResult result3 =
          new WatsonxAiRerankResponse.RerankResult(1, 0.25, null);

      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse(
              "cross-encoder/ms-marco-minilm-l-12-v2",
              "1.0",
              List.of(result1, result2, result3),
              LocalDateTime.now(),
              15,
              query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results = rerankModel.rerank(query, documents);

      assertNotNull(results);
      assertEquals(3, results.size());
      assertEquals(0.95, results.get(0).score(), 0.001);
      assertEquals(0, results.get(0).index());
      verify(watsonxAiRerankApi, times(1)).rerank(any(WatsonxAiRerankRequest.class));
    }

    @Test
    void rerankWithNullQueryThrowsException() {
      List<String> documents = List.of("Document 1", "Document 2");

      assertThrows(
          IllegalArgumentException.class,
          () -> rerankModel.rerank(null, documents),
          "Query must not be null or empty");
    }

    @Test
    void rerankWithEmptyQueryThrowsException() {
      List<String> documents = List.of("Document 1", "Document 2");

      assertThrows(
          IllegalArgumentException.class,
          () -> rerankModel.rerank("", documents),
          "Query must not be null or empty");
    }

    @Test
    void rerankWithEmptyDocumentsThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> rerankModel.rerank("What is AI?", List.of()),
          "Documents must not be empty");
    }

    @Test
    void rerankWithNullDocumentsThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> rerankModel.rerank("What is AI?", null),
          "Documents must not be empty");
    }

    @Test
    void rerankWithCustomOptions() {
      String query = "Test query";
      List<String> documents = List.of("Document 1");

      WatsonxAiRerankOptions customOptions =
          WatsonxAiRerankOptions.builder().model("custom-model").topN(5).build();

      WatsonxAiRerankResponse.RerankResult result =
          new WatsonxAiRerankResponse.RerankResult(0, 0.9, null);

      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse(
              "custom-model", "1.0", List.of(result), LocalDateTime.now(), 5, query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results =
          rerankModel.rerank(query, documents, customOptions);

      assertNotNull(results);
      assertEquals(1, results.size());
      verify(watsonxAiRerankApi, times(1)).rerank(any(WatsonxAiRerankRequest.class));
    }
  }

  @Nested
  class OptionsMergingTests {

    @Test
    void mergeOptionsWithRuntimeModel() {
      WatsonxAiRerankOptions runtimeOptions =
          WatsonxAiRerankOptions.builder().model("runtime-model").build();

      String query = "Test query";
      List<String> documents = List.of("Document 1");

      WatsonxAiRerankResponse.RerankResult result =
          new WatsonxAiRerankResponse.RerankResult(0, 0.9, null);
      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse(
              "runtime-model", "1.0", List.of(result), LocalDateTime.now(), 5, query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results =
          rerankModel.rerank(query, documents, runtimeOptions);

      assertNotNull(results);
      verify(watsonxAiRerankApi, times(1)).rerank(any(WatsonxAiRerankRequest.class));
    }

    @Test
    void mergeOptionsWithRuntimeTopN() {
      WatsonxAiRerankOptions runtimeOptions = WatsonxAiRerankOptions.builder().topN(3).build();

      String query = "Test query";
      List<String> documents = List.of("Document 1", "Document 2", "Document 3", "Document 4");

      WatsonxAiRerankResponse.RerankResult result1 =
          new WatsonxAiRerankResponse.RerankResult(0, 0.9, null);
      WatsonxAiRerankResponse.RerankResult result2 =
          new WatsonxAiRerankResponse.RerankResult(1, 0.8, null);
      WatsonxAiRerankResponse.RerankResult result3 =
          new WatsonxAiRerankResponse.RerankResult(2, 0.7, null);

      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse(
              "cross-encoder/ms-marco-minilm-l-12-v2",
              "1.0",
              List.of(result1, result2, result3),
              LocalDateTime.now(),
              10,
              query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results =
          rerankModel.rerank(query, documents, runtimeOptions);

      assertEquals(3, results.size());
    }
  }

  @Nested
  class ResponseHandlingTests {

    @Test
    void handleNullResponse() {
      String query = "Test query";
      List<String> documents = List.of("Document 1");

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(null));

      List<WatsonxAiRerankResponse.RerankResult> results = rerankModel.rerank(query, documents);

      assertNotNull(results);
      assertTrue(results.isEmpty());
    }

    @Test
    void handleResponseWithNullResults() {
      String query = "Test query";
      List<String> documents = List.of("Document 1");

      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse("test-model", "1.0", null, LocalDateTime.now(), 5, query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results = rerankModel.rerank(query, documents);

      assertNotNull(results);
      assertTrue(results.isEmpty());
    }

    @Test
    void handleResponseWithEmptyResults() {
      String query = "Test query";
      List<String> documents = List.of("Document 1");

      WatsonxAiRerankResponse mockResponse =
          new WatsonxAiRerankResponse(
              "test-model", "1.0", List.of(), LocalDateTime.now(), 5, query);

      when(watsonxAiRerankApi.rerank(any(WatsonxAiRerankRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      List<WatsonxAiRerankResponse.RerankResult> results = rerankModel.rerank(query, documents);

      assertNotNull(results);
      assertTrue(results.isEmpty());
    }
  }

  @Nested
  class DefaultOptionsTests {

    @Test
    void getDefaultOptionsReturnsCorrectOptions() {
      WatsonxAiRerankOptions retrievedOptions = rerankModel.getDefaultOptions();

      assertAll(
          "Default options validation",
          () -> assertEquals(defaultOptions, retrievedOptions),
          () -> assertEquals("cross-encoder/ms-marco-minilm-l-12-v2", retrievedOptions.getModel()),
          () -> assertEquals(512, retrievedOptions.getTruncateInputTokens()));
    }
  }
}
