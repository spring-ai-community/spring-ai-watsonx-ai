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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Integration test class for WatsonxAiRerankModel using MockRestServiceServer. This test
 * demonstrates integration with the watsonx.ai Rerank API.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
public class WatsonxAiRerankModelIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiRerankApi watsonxAiRerankApi;

  private WatsonxAiRerankModel rerankModel;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String RERANK_ENDPOINT = "/ml/v1/text/rerank";
  private static final String VERSION = "2024-05-31";
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

    watsonxAiRerankApi =
        new WatsonxAiRerankApi(
            BASE_URL,
            RERANK_ENDPOINT,
            VERSION,
            PROJECT_ID,
            SPACE_ID,
            API_KEY,
            restClientBuilder,
            errorHandler);

    WatsonxAiRerankOptions defaultOptions =
        WatsonxAiRerankOptions.builder()
            .model("cross-encoder/ms-marco-minilm-l-12-v2")
            .truncateInputTokens(512)
            .build();

    rerankModel =
        new WatsonxAiRerankModel(
            watsonxAiRerankApi,
            defaultOptions,
            ObservationRegistry.NOOP,
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void rerankWithSingleDocumentTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.95
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 10
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    List<String> documents = List.of("Machine learning is a subset of AI.");
    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("What is machine learning?", documents);

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(0, results.get(0).index());
    assertEquals(0.95, results.get(0).score(), 0.001);

    mockServer.verify();
  }

  @Test
  void rerankWithMultipleDocumentsTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.95
            },
            {
              "index": 2,
              "score": 0.82
            },
            {
              "index": 1,
              "score": 0.15
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 25
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    List<String> documents =
        List.of(
            "Machine learning is a subset of artificial intelligence.",
            "Cooking Italian pasta requires fresh ingredients.",
            "Deep learning uses neural networks with many layers.");

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("What is machine learning?", documents);

    assertNotNull(results);
    assertEquals(3, results.size());

    // First result should be the highest scoring
    assertEquals(0, results.get(0).index());
    assertEquals(0.95, results.get(0).score(), 0.001);

    // Second result
    assertEquals(2, results.get(1).index());
    assertEquals(0.82, results.get(1).score(), 0.001);

    // Third result (lowest score)
    assertEquals(1, results.get(2).index());
    assertEquals(0.15, results.get(2).score(), 0.001);

    mockServer.verify();
  }

  @Test
  void rerankWithCustomOptionsTest() {
    String jsonResponse =
        """
        {
          "model_id": "custom-rerank-model",
          "model_version": "2.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.88,
              "input": {
                "text": "Machine learning is AI"
              }
            },
            {
              "index": 1,
              "score": 0.72,
              "input": {
                "text": "Deep learning uses neural networks"
              }
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 15,
          "query": "What is AI?"
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiRerankOptions customOptions =
        WatsonxAiRerankOptions.builder()
            .model("custom-rerank-model")
            .topN(2)
            .truncateInputTokens(1024)
            .returnInputs(true)
            .returnQuery(true)
            .build();

    List<String> documents =
        List.of("Machine learning is AI", "Deep learning uses neural networks", "Cooking recipes");

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("What is AI?", documents, customOptions);

    assertNotNull(results);
    assertEquals(2, results.size());

    assertEquals(0, results.get(0).index());
    assertEquals(0.88, results.get(0).score(), 0.001);
    assertNotNull(results.get(0).input());
    assertEquals("Machine learning is AI", results.get(0).input().text());

    assertEquals(1, results.get(1).index());
    assertEquals(0.72, results.get(1).score(), 0.001);

    mockServer.verify();
  }

  @Test
  void rerankWithTopNLimitTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.95
            },
            {
              "index": 2,
              "score": 0.85
            },
            {
              "index": 3,
              "score": 0.75
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 30
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiRerankOptions options = WatsonxAiRerankOptions.builder().topN(3).build();

    List<String> documents =
        List.of("Doc 1 - ML", "Doc 2 - Cooking", "Doc 3 - DL", "Doc 4 - NLP", "Doc 5 - CV");

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("Machine learning topics", documents, options);

    assertNotNull(results);
    assertEquals(3, results.size());

    mockServer.verify();
  }

  @Test
  void rerankWithEmptyResponseTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 5
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    List<String> documents = List.of("Some document");
    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("Unrelated query", documents);

    assertNotNull(results);
    assertTrue(results.isEmpty());

    mockServer.verify();
  }

  @Test
  void rerankWithTruncateInputTokensTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.78
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 256
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiRerankOptions options =
        WatsonxAiRerankOptions.builder().truncateInputTokens(256).build();

    String longDocument =
        "This is a very long document that contains multiple sentences and paragraphs. "
            + "It should be truncated by the model if it exceeds the specified token limit. "
            + "The truncation ensures that the model can process the input without issues.";

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("What is this about?", List.of(longDocument), options);

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(0.78, results.get(0).score(), 0.001);

    mockServer.verify();
  }

  @Test
  void rerankWithInputTextInResponseTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.92,
              "input": {
                "text": "Original document text"
              }
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 8
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiRerankOptions options = WatsonxAiRerankOptions.builder().returnInputs(true).build();

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("Query text", List.of("Original document text"), options);

    assertNotNull(results);
    assertEquals(1, results.size());

    WatsonxAiRerankResponse.RerankResult result = results.get(0);
    assertEquals(0, result.index());
    assertEquals(0.92, result.score(), 0.001);
    assertNotNull(result.input());
    assertEquals("Original document text", result.input().text());

    mockServer.verify();
  }

  @Test
  void rerankWithDifferentModelsTest() {
    String jsonResponse =
        """
        {
          "model_id": "ibm/rerank-v1",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 1,
              "score": 0.89
            },
            {
              "index": 0,
              "score": 0.67
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 20
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiRerankOptions options =
        WatsonxAiRerankOptions.builder().model("ibm/rerank-v1").build();

    List<String> documents = List.of("Document A about technology", "Document B about science");

    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("Science and research", documents, options);

    assertNotNull(results);
    assertEquals(2, results.size());

    // Results should be sorted by score
    assertEquals(1, results.get(0).index());
    assertEquals(0.89, results.get(0).score(), 0.001);

    assertEquals(0, results.get(1).index());
    assertEquals(0.67, results.get(1).score(), 0.001);

    mockServer.verify();
  }

  @Test
  void rerankPreservesDefaultOptionsTest() {
    String jsonResponse =
        """
        {
          "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
          "model_version": "1.0.0",
          "results": [
            {
              "index": 0,
              "score": 0.75
            }
          ],
          "created_at": "2024-01-15T10:30:00.000Z",
          "input_token_count": 5
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + RERANK_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Call without runtime options - should use defaults
    List<WatsonxAiRerankResponse.RerankResult> results =
        rerankModel.rerank("Test query", List.of("Test document"));

    assertNotNull(results);
    assertEquals(1, results.size());

    // Verify default options are still intact
    WatsonxAiRerankOptions defaultOptions = rerankModel.getDefaultOptions();
    assertEquals("cross-encoder/ms-marco-minilm-l-12-v2", defaultOptions.getModel());
    assertEquals(512, defaultOptions.getTruncateInputTokens());

    mockServer.verify();
  }
}
