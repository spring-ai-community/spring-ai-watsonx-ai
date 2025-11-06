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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Test class for WatsonxAiChatModel using Spring AI ChatClient APIs. This test demonstrates
 * integration with Spring AI's high-level ChatClient, ChatRequest (Prompt), and ChatResponse APIs.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@RestClientTest
public class WatsonxAiChatClientTest {

  @Autowired private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiChatApi watsonxAiChatApi;

  private WatsonxAiChatModel chatModel;

  private ChatClient chatClient;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String TEXT_ENDPOINT = "/ml/v1/text/chat";
  private static final String STREAM_ENDPOINT = "/ml/v1/text/chat_stream";
  private static final String VERSION = "2024-03-19";
  private static final String PROJECT_ID = "test-project-id";
  private static final String API_KEY = "test-api-key";

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    // Create a simple ResponseErrorHandler for testing
    ResponseErrorHandler errorHandler =
        response -> {
          HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
          return (series == HttpStatus.Series.CLIENT_ERROR
              || series == HttpStatus.Series.SERVER_ERROR);
        };

    // Initialize the WatsonxAiChatApi
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

    // Create default options for the chat model
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

    // Initialize the chat model
    chatModel =
        new WatsonxAiChatModel(
            watsonxAiChatApi,
            defaultOptions,
            ObservationRegistry.NOOP,
            ToolCallingManager.builder().build(),
            new DefaultToolExecutionEligibilityPredicate(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE);

    // Initialize the ChatClient with the chat model
    chatClient = ChatClient.builder(chatModel).build();
  }

  @Test
  void chatClientWithSystemUserAndCustomOptionsTest() {
    // Mock response from watsonx.ai documentation
    // https://cloud.ibm.com/apidocs/watsonx-ai-cp/watsonx-ai-cp-2.2.1
    String jsonResponse =
        """
        {
          "id": "cmpl-123456",
          "model_id": "ibm/granite-3-8b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Hello! I'm a helpful coding assistant. How can I help you today?"
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "completion_tokens": 15,
            "prompt_tokens": 25,
            "total_tokens": 40
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create custom options for this request
    ChatOptions customOptions =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-8b-instruct")
            .temperature(0.5)
            .maxTokens(2048)
            .build();

    // Use ChatClient with system prompt, user message, and custom options
    String response =
        chatClient
            .prompt()
            .system("You are a helpful coding assistant.")
            .user("Hello")
            .options(customOptions)
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertEquals("Hello! I'm a helpful coding assistant. How can I help you today?", response);

    mockServer.verify();
  }

  @Test
  void chatClientWithChatResponseAndMetadataTest() {
    // Mock response from watsonx.ai documentation with warnings
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
          },
          "system": {
            "warnings": [
              {
                "message": "The framework TF 1.1 is deprecated.",
                "id": "2fc54cf1-252f-424b-b52d-5cdd98149871",
                "more_info": "https://example.com/deprecation-info",
                "additional_properties": {
                  "severity": "medium",
                  "deprecated_version": "1.1"
                }
              }
            ]
          }
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Use ChatClient with chatResponse() to get full response with metadata
    ChatResponse chatResponse =
        chatClient.prompt().user("What is the capital of France?").call().chatResponse();

    // Verify the ChatResponse
    assertNotNull(chatResponse);
    assertNotNull(chatResponse.getResult());
    assertEquals("The capital of France is Paris.", chatResponse.getResult().getOutput().getText());

    // Verify metadata
    assertNotNull(chatResponse.getMetadata());
    assertEquals("cmpl-789012", chatResponse.getMetadata().getId());
    assertEquals("ibm/granite-3-2b-instruct", chatResponse.getMetadata().getModel());

    // Verify usage
    assertNotNull(chatResponse.getMetadata().getUsage());
    assertEquals(10, chatResponse.getMetadata().getUsage().getPromptTokens());
    assertEquals(7, chatResponse.getMetadata().getUsage().getCompletionTokens());
    assertEquals(17, chatResponse.getMetadata().getUsage().getTotalTokens());

    // Verify warnings are accessible in metadata
    List<WatsonxAiChatResponse.Warning> warnings = chatResponse.getMetadata().get("warnings");
    assertNotNull(warnings, "Warnings should be accessible from ChatResponse metadata");
    assertEquals(1, warnings.size());

    WatsonxAiChatResponse.Warning warning = warnings.get(0);
    assertEquals("The framework TF 1.1 is deprecated.", warning.message());
    assertEquals("2fc54cf1-252f-424b-b52d-5cdd98149871", warning.id());
    assertEquals("https://example.com/deprecation-info", warning.moreInfo());
    assertNotNull(warning.additionalProperties());
    assertEquals("medium", warning.additionalProperties().get("severity"));
    assertEquals("1.1", warning.additionalProperties().get("deprecated_version"));

    mockServer.verify();
  }

  @Test
  void chatResponseWithMultipleChoicesTest() {
    // Mock response with multiple choices
    String jsonResponse =
        """
        {
          "id": "cmpl-multi-456",
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

    // Create a prompt with multiple choices
    ChatOptions optionsWithMultipleChoices =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-2b-instruct")
            .n(2) // Request 2 choices
            .build();

    Prompt prompt = new Prompt("Generate a response", optionsWithMultipleChoices);
    ChatResponse chatResponse = chatModel.call(prompt);

    // Verify multiple generations
    assertNotNull(chatResponse);
    assertNotNull(chatResponse.getResults());
    assertEquals(2, chatResponse.getResults().size());

    // Verify first choice
    assertEquals("First response option.", chatResponse.getResults().get(0).getOutput().getText());

    // Verify second choice
    assertEquals("Second response option.", chatResponse.getResults().get(1).getOutput().getText());

    mockServer.verify();
  }
}
