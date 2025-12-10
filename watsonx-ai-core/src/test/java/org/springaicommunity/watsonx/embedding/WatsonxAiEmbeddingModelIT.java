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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
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
 * Integration test class for WatsonxAiEmbeddingModel using Spring AI EmbeddingModel APIs. This test
 * demonstrates integration with Spring AI's high-level EmbeddingRequest and EmbeddingResponse APIs.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiEmbeddingModelIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiEmbeddingApi watsonxAiEmbeddingApi;

  private WatsonxAiEmbeddingModel embeddingModel;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String EMBEDDING_ENDPOINT = "/ml/v1/text/embeddings";
  private static final String VERSION = "2024-03-19";
  private static final String PROJECT_ID = "test-project-id";
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

    embeddingModel =
        new WatsonxAiEmbeddingModel(
            watsonxAiEmbeddingApi, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void embeddingRequestWithSingleTextTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
              "input": {
                "text": "Hello world"
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

    EmbeddingRequest request = new EmbeddingRequest(List.of("Hello world"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(1, response.getResults().size());

    float[] embedding = response.getResults().get(0).getOutput();
    assertNotNull(embedding);
    assertEquals(5, embedding.length);
    assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, embedding, 0.001f);

    assertNotNull(response.getMetadata());
    assertEquals("ibm/slate-125m-english-rtrvr", response.getMetadata().getModel());

    assertEquals(2, response.getResults().get(0).getIndex());

    mockServer.verify();
  }

  @Test
  void embeddingRequestWithMultipleTextsTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.1, 0.2, 0.3],
              "input": {
                "text": "First text"
              }
            },
            {
              "embedding": [0.4, 0.5, 0.6],
              "input": {
                "text": "Second text"
              }
            },
            {
              "embedding": [0.7, 0.8, 0.9],
              "input": {
                "text": "Third text"
              }
            }
          ],
          "input_token_count": 6
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    EmbeddingRequest request =
        new EmbeddingRequest(List.of("First text", "Second text", "Third text"), null);

    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(3, response.getResults().size());

    float[] embedding1 = response.getResults().get(0).getOutput();
    assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, embedding1, 0.001f);

    float[] embedding2 = response.getResults().get(1).getOutput();
    assertArrayEquals(new float[] {0.4f, 0.5f, 0.6f}, embedding2, 0.001f);

    float[] embedding3 = response.getResults().get(2).getOutput();
    assertArrayEquals(new float[] {0.7f, 0.8f, 0.9f}, embedding3, 0.001f);

    assertEquals("ibm/slate-125m-english-rtrvr", response.getMetadata().getModel());

    mockServer.verify();
  }

  @Test
  void embeddingRequestWithCustomOptionsTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-30m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.11, 0.22, 0.33, 0.44],
              "input": {
                "text": "Custom model test"
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

    WatsonxAiEmbeddingRequest.EmbeddingReturnOptions returnOptions =
        new WatsonxAiEmbeddingRequest.EmbeddingReturnOptions(true);
    WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
        new WatsonxAiEmbeddingRequest.EmbeddingParameters(1024, returnOptions);

    WatsonxAiEmbeddingOptions customOptions =
        WatsonxAiEmbeddingOptions.builder()
            .model("ibm/slate-30m-english-rtrvr")
            .parameters(parameters)
            .build();

    EmbeddingRequest request = new EmbeddingRequest(List.of("Custom model test"), customOptions);
    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());

    float[] embedding = response.getResults().get(0).getOutput();
    assertArrayEquals(new float[] {0.11f, 0.22f, 0.33f, 0.44f}, embedding, 0.001f);

    assertEquals("ibm/slate-30m-english-rtrvr", response.getMetadata().getModel());

    mockServer.verify();
  }

  @Test
  void embedDocumentTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.15, 0.25, 0.35, 0.45, 0.55, 0.65],
              "input": {
                "text": "This is a test document for embedding"
              }
            }
          ],
          "input_token_count": 7
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Document document = new Document("This is a test document for embedding");
    float[] embedding = embeddingModel.embed(document);

    assertNotNull(embedding);
    assertEquals(6, embedding.length);
    assertArrayEquals(new float[] {0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f}, embedding, 0.001f);

    mockServer.verify();
  }

  @Test
  void embeddingResponseWithMetadataTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.2, 0.4, 0.6, 0.8],
              "input": {
                "text": "Metadata test"
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

    EmbeddingRequest request = new EmbeddingRequest(List.of("Metadata test"), null);
    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertNotNull(response.getMetadata());

    assertEquals("ibm/slate-125m-english-rtrvr", response.getMetadata().getModel());

    assertEquals(1, response.getResults().size());
    float[] embedding = response.getResults().get(0).getOutput();
    assertArrayEquals(new float[] {0.2f, 0.4f, 0.6f, 0.8f}, embedding, 0.001f);

    assertEquals(2, response.getResults().get(0).getIndex());

    mockServer.verify();
  }

  @Test
  void embeddingWithLongTextTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.12, 0.23, 0.34, 0.45, 0.56, 0.67, 0.78, 0.89],
              "input": {
                "text": "This is a very long text that contains multiple sentences and should be properly embedded by the model. It tests the model's ability to handle longer inputs."
              }
            }
          ],
          "input_token_count": 28
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + EMBEDDING_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    String longText =
        "This is a very long text that contains multiple sentences and should be properly "
            + "embedded by the model. It tests the model's ability to handle longer inputs.";
    EmbeddingRequest request = new EmbeddingRequest(List.of(longText), null);

    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());

    float[] embedding = response.getResults().get(0).getOutput();
    assertEquals(8, embedding.length);
    assertArrayEquals(
        new float[] {0.12f, 0.23f, 0.34f, 0.45f, 0.56f, 0.67f, 0.78f, 0.89f}, embedding, 0.001f);

    assertEquals(28, response.getResults().get(0).getIndex());

    mockServer.verify();
  }

  @Test
  void embeddingWithTruncateInputTokensTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/slate-125m-english-rtrvr",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "embedding": [0.3, 0.6, 0.9],
              "input": {
                "text": "Truncated text"
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

    WatsonxAiEmbeddingRequest.EmbeddingParameters parameters =
        new WatsonxAiEmbeddingRequest.EmbeddingParameters(256, null);

    WatsonxAiEmbeddingOptions options =
        WatsonxAiEmbeddingOptions.builder()
            .model("ibm/slate-125m-english-rtrvr")
            .parameters(parameters)
            .build();

    EmbeddingRequest request = new EmbeddingRequest(List.of("Truncated text"), options);
    EmbeddingResponse response = embeddingModel.call(request);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());

    float[] embedding = response.getResults().get(0).getOutput();
    assertArrayEquals(new float[] {0.3f, 0.6f, 0.9f}, embedding, 0.001f);

    mockServer.verify();
  }
}
