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

package org.springaicommunity.watsonx.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import io.micrometer.observation.tck.TestObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Integration test for WatsonxAiChatModel observation and metrics. This test verifies that the chat
 * model properly integrates with Micrometer's observation framework for monitoring and tracing.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiChatModelObservationIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiChatApi watsonxAiChatApi;

  private WatsonxAiChatModel chatModel;

  private TestObservationRegistry observationRegistry;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String TEXT_ENDPOINT = "/ml/v1/text/chat";
  private static final String STREAM_ENDPOINT = "/ml/v1/text/chat_stream";
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

    watsonxAiChatApi =
        new WatsonxAiChatApi(
            BASE_URL,
            TEXT_ENDPOINT,
            STREAM_ENDPOINT,
            VERSION,
            PROJECT_ID,
            API_KEY,
            restClientBuilder,
            WebClient.builder(),
            errorHandler);

    WatsonxAiChatOptions defaultOptions =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-2b-instruct")
            .temperature(0.7)
            .topP(1.0)
            .maxTokens(1024)
            .presencePenalty(0.0)
            .stopSequences(List.of())
            .logProbs(false)
            .n(1)
            .build();

    observationRegistry = TestObservationRegistry.create();

    chatModel =
        new WatsonxAiChatModel(
            watsonxAiChatApi,
            defaultOptions,
            observationRegistry,
            ToolCallingManager.builder().build(),
            new DefaultToolExecutionEligibilityPredicate(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void chatOperationCreatesObservation() {
    String jsonResponse =
        """
        {
          "id": "cmpl-123456",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Hello! How can I help you today?"
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 8,
            "prompt_tokens": 10,
            "total_tokens": 18
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Prompt prompt = new Prompt(new UserMessage("Hello"));
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getResult()).isNotNull();
    assertThat(response.getResult().getOutput().getText())
        .isEqualTo("Hello! How can I help you today?");

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasContextualNameEqualTo("chat ibm/granite-3-2b-instruct")
        .hasLowCardinalityKeyValue("gen_ai.operation.name", "chat")
        .hasLowCardinalityKeyValue("gen_ai.system", "watsonx-ai")
        .hasLowCardinalityKeyValue("gen_ai.request.model", "ibm/granite-3-2b-instruct")
        .hasLowCardinalityKeyValue("gen_ai.response.model", "ibm/granite-3-2b-instruct")
        .hasHighCardinalityKeyValue("gen_ai.response.finish_reasons", "[\"stop\"]")
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationWithObservationRegistry() {
    String jsonResponse =
        """
        {
          "id": "cmpl-789012",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The capital of France is Paris."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 7,
            "prompt_tokens": 10,
            "total_tokens": 17
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Prompt prompt = new Prompt(new UserMessage("What is the capital of France?"));
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getResult().getOutput().getText())
        .isEqualTo("The capital of France is Paris.");
    assertThat(response.getMetadata()).isNotNull();
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/granite-3-2b-instruct");
    assertThat(response.getMetadata().getUsage().getPromptTokens()).isEqualTo(10);
    assertThat(response.getMetadata().getUsage().getCompletionTokens()).isEqualTo(7);

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationRecordsMetrics() {
    String jsonResponse =
        """
        {
          "id": "cmpl-metrics-123",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "This is a test response for metrics."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 8,
            "prompt_tokens": 5,
            "total_tokens": 13
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Prompt prompt = new Prompt(new UserMessage("Test metrics"));
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getMetadata().getUsage()).isNotNull();
    assertThat(response.getMetadata().getUsage().getTotalTokens()).isEqualTo(13);

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationWithCustomOptions() {
    String jsonResponse =
        """
        {
          "id": "cmpl-custom-456",
          "model_id": "ibm/granite-3-8b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Custom options response."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 4,
            "prompt_tokens": 6,
            "total_tokens": 10
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiChatOptions customOptions =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-8b-instruct")
            .temperature(0.5)
            .maxTokens(2048)
            .build();

    Prompt prompt = new Prompt(new UserMessage("Test custom options"), customOptions);
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getMetadata().getModel()).isEqualTo("ibm/granite-3-8b-instruct");

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasLowCardinalityKeyValue("gen_ai.request.model", "ibm/granite-3-8b-instruct")
        .hasLowCardinalityKeyValue("gen_ai.response.model", "ibm/granite-3-8b-instruct")
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationWithMultipleChoices() {
    String jsonResponse =
        """
        {
          "id": "cmpl-multi-789",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "First response option."
              },
              "finish_reason": "stop"
            },
            {
              "index": 1,
              "message": {
                "role": "assistant",
                "content": "Second response option."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 12,
            "prompt_tokens": 6,
            "total_tokens": 18
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().n(2).build();

    Prompt prompt = new Prompt(new UserMessage("Generate multiple responses"), options);
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(2);

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationPreservesMetadata() {
    String jsonResponse =
        """
        {
          "id": "cmpl-metadata-999",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Metadata test response."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 4,
            "prompt_tokens": 3,
            "total_tokens": 7
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Prompt prompt = new Prompt(new UserMessage("Metadata test"));
    ChatResponse response = chatModel.call(prompt);

    org.assertj.core.api.Assertions.assertThat(response).isNotNull();
    org.assertj.core.api.Assertions.assertThat(response.getMetadata()).isNotNull();
    org.assertj.core.api.Assertions.assertThat(response.getMetadata().getId())
        .isEqualTo("cmpl-metadata-999");
    org.assertj.core.api.Assertions.assertThat(response.getMetadata().getModel())
        .isEqualTo("ibm/granite-3-2b-instruct");
    org.assertj.core.api.Assertions.assertThat((Object) response.getMetadata().get("created"))
        .isEqualTo(1689958352);
    org.assertj.core.api.Assertions.assertThat((Object) response.getMetadata().get("model_version"))
        .isEqualTo("1.0.0");

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }

  @Test
  void chatOperationWithFinishReason() {
    String jsonResponse =
        """
        {
          "id": "cmpl-finish-111",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Complete response."
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 3,
            "prompt_tokens": 4,
            "total_tokens": 7
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    Prompt prompt = new Prompt(new UserMessage("Test finish reason"));
    ChatResponse response = chatModel.call(prompt);

    assertThat(response).isNotNull();
    assertThat(response.getResult().getMetadata().getFinishReason()).isEqualTo("stop");

    io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat(observationRegistry)
        .doesNotHaveAnyRemainingCurrentObservation()
        .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
        .that()
        .hasHighCardinalityKeyValue("gen_ai.response.finish_reasons", "[\"stop\"]")
        .hasBeenStarted()
        .hasBeenStopped();

    mockServer.verify();
  }
}
