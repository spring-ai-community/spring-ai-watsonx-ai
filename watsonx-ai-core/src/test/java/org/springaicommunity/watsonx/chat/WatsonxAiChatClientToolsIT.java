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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.micrometer.observation.ObservationRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * End-to-end test class for WatsonxAiChatModel tool calling functionality. This test demonstrates
 * integration with Spring AI's tool calling API, including method-based tools with @Tool annotation
 * and function-based tools.
 *
 * @author Federico Mariani
 * @since 1.0.0
 */
public class WatsonxAiChatClientToolsIT {

  private RestClient.Builder restClientBuilder;

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

  /**
   * Weather request record for the weather tool.
   *
   * @param city the city to get weather for
   * @param unit the temperature unit (C or F)
   */
  public record WeatherRequest(String city, String unit) {}

  /**
   * Weather response record.
   *
   * @param temperature the temperature value
   * @param unit the temperature unit
   * @param description weather description
   */
  public record WeatherResponse(double temperature, String unit, String description) {}

  /** Tool class providing weather information - demonstrates method-based tool with @Tool. */
  public static class WeatherTools {

    @Tool(description = "Get the current weather for a given city")
    public WeatherResponse getCurrentWeather(WeatherRequest request) {
      // Simulate weather data
      return switch (request.city().toLowerCase()) {
        case "paris" -> new WeatherResponse(15.0, request.unit(), "Cloudy");
        case "tokyo" -> new WeatherResponse(22.0, request.unit(), "Sunny");
        case "new york" -> new WeatherResponse(18.0, request.unit(), "Rainy");
        default -> new WeatherResponse(20.0, request.unit(), "Clear");
      };
    }
  }

  /** Date/Time tool record - demonstrates simple function-based tool. */
  public static class DateTimeTools {

    @Tool(description = "Get the current date")
    public String getCurrentDate() {
      return LocalDate.now().toString();
    }

    @Tool(description = "Get the day of the week")
    public String getDayOfWeek() {
      return LocalDate.now().getDayOfWeek().toString();
    }
  }

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
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
  void chatClientWithWeatherToolTest() {
    // First response: Model decides to call the weather tool
    String firstJsonResponse =
        """
        {
          "id": "cmpl-tool-123",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                  {
                    "id": "call_abc123",
                    "type": "function",
                    "function": {
                      "name": "getCurrentWeather",
                      "arguments": "{\\"city\\":\\"Paris\\",\\"unit\\":\\"C\\"}"
                    }
                  }
                ]
              },
              "finish_reason": "tool_calls"
            }
          ]
        }
        """;

    // Second response: Model generates final answer using tool result
    String secondJsonResponse =
        """
        {
          "id": "cmpl-tool-456",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958353,
          "created_at": "2023-07-21T16:52:33.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The current weather in Paris is 15.0°C and it's Cloudy."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    // Mock first call (tool call request)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(firstJsonResponse, MediaType.APPLICATION_JSON));

    // Mock second call (final response with tool result)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(secondJsonResponse, MediaType.APPLICATION_JSON));

    // Execute chat with weather tool
    String response =
        chatClient
            .prompt()
            .user("What's the weather like in Paris?")
            .tools(new WeatherTools())
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertTrue(
        response.contains("Paris") && response.contains("15.0") && response.contains("Cloudy"),
        "Response should contain weather information for Paris");

    mockServer.verify();
  }

  @Test
  void chatClientWithFunctionBasedToolTest() {
    // Create a function-based tool using FunctionToolCallback
    // Note: FunctionToolCallback is used when you want to define tools programmatically
    // rather than using @Tool annotations
    Function<WeatherRequest, WeatherResponse> weatherFunction =
        request -> new WeatherResponse(22.0, request.unit(), "Sunny");

    ToolCallback weatherToolCallback =
        FunctionToolCallback.builder("getCurrentWeather", weatherFunction)
            .description("Get the current weather for a given city")
            .inputType(WeatherRequest.class)
            .build();

    // First response: Model decides to call the weather tool
    String firstJsonResponse =
        """
        {
          "id": "cmpl-func-123",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                  {
                    "id": "call_func123",
                    "type": "function",
                    "function": {
                      "name": "getCurrentWeather",
                      "arguments": "{\\"city\\":\\"Tokyo\\",\\"unit\\":\\"C\\"}"
                    }
                  }
                ]
              },
              "finish_reason": "tool_calls"
            }
          ]
        }
        """;

    // Second response: Model generates final answer using tool result
    String secondJsonResponse =
        """
        {
          "id": "cmpl-func-456",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958353,
          "created_at": "2023-07-21T16:52:33.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The weather in Tokyo is currently 22.0°C and Sunny."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    // Mock first call (tool call request)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(firstJsonResponse, MediaType.APPLICATION_JSON));

    // Mock second call (final response with tool result)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(secondJsonResponse, MediaType.APPLICATION_JSON));

    // Execute chat with function-based tool
    // Note: Use .toolCallbacks() for ToolCallback instances, not .tools()
    String response =
        chatClient
            .prompt()
            .user("What's the weather in Tokyo?")
            .toolCallbacks(weatherToolCallback)
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertTrue(
        response.contains("Tokyo") && response.contains("22.0") && response.contains("Sunny"),
        "Response should contain weather information for Tokyo");

    mockServer.verify();
  }

  @Test
  void chatClientWithMultipleToolsTest() {
    // First response: Model decides to call multiple tools
    String firstJsonResponse =
        """
        {
          "id": "cmpl-multi-123",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                  {
                    "id": "call_multi1",
                    "type": "function",
                    "function": {
                      "name": "getCurrentWeather",
                      "arguments": "{\\"city\\":\\"New York\\",\\"unit\\":\\"C\\"}"
                    }
                  },
                  {
                    "id": "call_multi2",
                    "type": "function",
                    "function": {
                      "name": "getCurrentDate",
                      "arguments": "{}"
                    }
                  }
                ]
              },
              "finish_reason": "tool_calls"
            }
          ]
        }
        """;

    // Second response: Model generates final answer using both tool results
    String secondJsonResponse =
        String.format(
            """
        {
          "id": "cmpl-multi-456",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958353,
          "created_at": "2023-07-21T16:52:33.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Today is %s and the weather in New York is 18.0°C with Rainy conditions."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """,
            LocalDate.now());

    // Mock first call (tool call request)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(firstJsonResponse, MediaType.APPLICATION_JSON));

    // Mock second call (final response with tool results)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(secondJsonResponse, MediaType.APPLICATION_JSON));

    // Execute chat with multiple tools
    String response =
        chatClient
            .prompt()
            .user("What's the weather today in New York?")
            .tools(new WeatherTools(), new DateTimeTools())
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertTrue(
        response.contains("New York")
            && response.contains("18.0")
            && response.contains(LocalDate.now().toString()),
        "Response should contain both weather and date information");

    mockServer.verify();
  }

  @Test
  void chatClientWithToolsAndJsonResponseFormatTest() {
    // First response: Model decides to call the weather tool
    String firstJsonResponse =
        """
        {
          "id": "cmpl-tool-json-123",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                  {
                    "id": "call_json123",
                    "type": "function",
                    "function": {
                      "name": "getCurrentWeather",
                      "arguments": "{\\"city\\":\\"Paris\\",\\"unit\\":\\"C\\"}"
                    }
                  }
                ]
              },
              "finish_reason": "tool_calls"
            }
          ]
        }
        """;

    // Second response: Model generates final answer in JSON format
    String secondJsonResponse =
        """
        {
          "id": "cmpl-tool-json-456",
          "model_id": "ibm/granite-3-2b-instruct",
          "created": 1689958353,
          "created_at": "2023-07-21T16:52:33.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "{\\"city\\":\\"Paris\\",\\"temperature\\":15.0,\\"unit\\":\\"C\\",\\"condition\\":\\"Cloudy\\"}"
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    // Mock first call (tool call request)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(firstJsonResponse, MediaType.APPLICATION_JSON));

    // Mock second call (final response with tool result in JSON format)
    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(secondJsonResponse, MediaType.APPLICATION_JSON));

    // Create options with JSON response format
    WatsonxAiChatOptions optionsWithJsonFormat =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-2b-instruct")
            .responseFormat(WatsonxAiChatRequest.TextChatResponseFormat.jsonObject())
            .build();

    // Execute chat with weather tool and JSON response format
    String response =
        chatClient
            .prompt()
            .user("What's the weather like in Paris? Return the result as JSON.")
            .tools(new WeatherTools())
            .options(optionsWithJsonFormat)
            .call()
            .content();

    // Verify the response is valid JSON with weather information
    assertNotNull(response);
    assertTrue(response.contains("city") && response.contains("Paris"));
    assertTrue(response.contains("temperature") && response.contains("15.0"));
    assertTrue(response.contains("condition") && response.contains("Cloudy"));

    mockServer.verify();
  }
}
