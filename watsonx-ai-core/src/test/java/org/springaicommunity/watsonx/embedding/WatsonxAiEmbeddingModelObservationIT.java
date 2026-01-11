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

package org.springaicommunity.watsonx.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import io.micrometer.observation.tck.TestObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Integration test for WatsonxAiEmbeddingModel observation and metrics. This test verifies that the
 * embedding model properly integrates with Micrometer's observation framework for monitoring and
 * tracing.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiEmbeddingModelObservationIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;

  private WatsonxAiEmbeddingModel embeddingModel;

  private TestObservationRegistry observationRegistry;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String EMBEDDING_ENDPOINT = "/ml/v1/text/embeddings";
  private static final String VERSION = "2024-03-19";
  private static final String PROJECT_ID = "test-project-id";
  private static final String SPACE_ID = "test-space-id";
  private static final String API_KEY = "test-api-key";

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    ResponseErrorHandler errorHandler =
        response -> {
          HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
          return (series == HttpStatus.Series.CLIENT_ERROR
              || series == HttpStatus.Series.SERVER_ERROR);
        };

    watsonxAiEmbeddingApi =
        new WatsonxAiEmbeddingApi(
            BASE_URL,
            EMBEDDING_ENDPOINT,
            VERSION,
            PROJECT_ID,
            SPACE_ID,
            API_KEY,
            restClientBuilder,
            errorHandler);

    WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
        new WatsonxAiEmbeddingRequest.EmbeddingParameters(512, null);

    WatsonxAiEmbeddingOptions defaultOptions =
        WatsonxAiEmbeddingOptions.builder()
            .model("ibm/slate-125m-english-rtrvr")
            .parameters(parameters)
            .build();

    observationRegistry = TestObservationRegistry.create();

    embeddingModel =
        new WatsonxAiEmbeddingModel(
            watsonxAiEmbeddingApi,
            defaultOptions,
            observationRegistry,
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void embeddingOperationCreatesObservation() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
              "input": {
                "text": "Test observation"
              }
            }
          ],
          "input_token_count": 2
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    EmbeddingRequest request = new EmbeddingRequest(List.of("Test observation"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);

    mockServer.verify();
  }

  @Test
  void embeddingOperationWithObservationRegistry() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.15, 0.25, 0.35],
              "input": {
                "text": "Observation test"
              }
            }
          ],
          "input_token_count": 2
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiEmbeddingModel modelWithObservation =
        new WatsonxAiEmbeddingModel(
            watsonxAiEmbeddingApi,
            embeddingModel.getDefaultOptions(),
            observationRegistry,
            RetryUtils.DEFAULT_RETRY_TEMPLATE);

    EmbeddingRequest request = new EmbeddingRequest(List.of("Observation test"), null);
    EmbeddingResponse response = modelWithObservation.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getMetadata()).isNotNull();
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/slate-125m-english-rtrvr");

    mockServer.verify();
  }

  @Test
  void embeddingOperationRecordsMetrics() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.2, 0.4, 0.6, 0.8],
              "input": {
                "text": "Metrics test"
              }
            }
          ],
          "input_token_count": 2
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    EmbeddingRequest request = new EmbeddingRequest(List.of("Metrics test"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);

    float[] embedding = response.getResults().get(0).getOutput();
    assertThat(embedding).hasSize(4);
    assertThat(embedding).containsExactly(0.2f, 0.4f, 0.6f, 0.8f);

    mockServer.verify();
  }

  @Test
  void embeddingOperationWithMultipleInputs() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.1, 0.2],
              "input": {
                "text": "First input"
              }
            },
            {
              "embedding": [0.3, 0.4],
              "input": {
                "text": "Second input"
              }
            }
          ],
          "input_token_count": 4
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    EmbeddingRequest request = new EmbeddingRequest(List.of("First input", "Second input"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(2);
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/slate-125m-english-rtrvr");

    mockServer.verify();
  }

  @Test
  void embeddingOperationWithCustomOptions() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-30m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.5, 0.6, 0.7],
              "input": {
                "text": "Custom options test"
              }
            }
          ],
          "input_token_count": 3
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
        new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, null);

    WatsonxAiEmbeddingOptions customOptions =
        WatsonxAiEmbeddingOptions.builder()
            .model("ibm/slate-30m-english-rtrvr")
            .parameters(parameters)
            .build();

    EmbeddingRequest request = new EmbeddingRequest(List.of("Custom options test"), customOptions);
    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/slate-30m-english-rtrvr");

    mockServer.verify();
  }

  @Test
  void embeddingOperationPreservesMetadata() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.11, 0.22, 0.33, 0.44, 0.55],
              "input": {
                "text": "Metadata preservation test"
              }
            }
          ],
          "input_token_count": 3
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    EmbeddingRequest request = new EmbeddingRequest(List.of("Metadata preservation test"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getMetadata()).isNotNull();
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/slate-125m-english-rtrvr");

    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getIndex()).isEqualTo(3);

    mockServer.verify();
  }
}
