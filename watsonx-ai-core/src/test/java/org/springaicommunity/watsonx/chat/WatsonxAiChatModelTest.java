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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;

/**
 * Test class for WatsonxAiChatModel to simulate chat functionality and options.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiChatModelTest {

  @Mock private WatsonxAiChatApi watsonxAiChatApi;

  private WatsonxAiChatModel chatModel;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create default options for testing
    WatsonxAiChatOptions defaultOptions =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
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
        WatsonxAiChatModel.builder()
            .watsonxAiChatApi(watsonxAiChatApi)
            .options(defaultOptions)
            .observationRegistry(ObservationRegistry.NOOP)
            .toolCallingManager(ToolCallingManager.builder().build())
            .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
            .build();
  }

  @Test
  void chatModelInitialization() {
    assertNotNull(chatModel);
    assertNotNull(chatModel.getOptions());
    assertEquals("ibm/granite-3-3-8b-instruct", chatModel.getOptions().getModel());
    assertEquals(0.7, chatModel.getOptions().getTemperature());
    assertEquals(1.0, chatModel.getOptions().getTopP());
    assertEquals(1024, chatModel.getOptions().getMaxTokens());
  }

  @Test
  void chatOptionsConfiguration() {

    WatsonxAiChatOptions customOptions =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-8b-instruct")
            .temperature(0.5)
            .topP(0.9)
            .maxTokens(2048)
            .presencePenalty(0.1)
            .stopSequences(List.of("END", "STOP"))
            .logProbs(true)
            .n(2)
            .seed(12345)
            .build();

    assertEquals("ibm/granite-3-8b-instruct", customOptions.getModel());
    assertEquals(0.5, customOptions.getTemperature());
    assertEquals(0.9, customOptions.getTopP());
    assertEquals(2048, customOptions.getMaxTokens());
    assertEquals(0.1, customOptions.getPresencePenalty());
    assertEquals(List.of("END", "STOP"), customOptions.getStopSequences());
    assertTrue(customOptions.getLogprobs());
    assertEquals(2, customOptions.getN());
    assertEquals(12345, customOptions.getSeed());
  }

  @Test
  void chatModelCallSimulation() {

    WatsonxAiChatResponse.TextChatResultChoice choice =
        new WatsonxAiChatResponse.TextChatResultChoice(
            0,
            new WatsonxAiChatResponse.TextChatResultMessage(
                org.springaicommunity.watsonx.chat.util.ChatRole.ASSISTANT,
                "Hello! How can I help you today?",
                null,
                null),
            "stop",
            null);

    WatsonxAiChatResponse mockResponse =
        new WatsonxAiChatResponse(
            "test-id",
            "ibm/granite-3-3-8b-instruct",
            1234567890,
            List.of(choice),
            "2024-01-01",
            null,
            new WatsonxAiChatResponse.TextChatUsage(10, 15, 25),
            null);

    when(watsonxAiChatApi.chat(any(WatsonxAiChatRequest.class)))
        .thenReturn(ResponseEntity.ok(mockResponse));

    Prompt prompt = new Prompt(new UserMessage("Hello"));
    ChatResponse response = chatModel.call(prompt);

    assertNotNull(response);
    assertFalse(response.getResults().isEmpty());

    Generation generation = response.getResult();
    assertNotNull(generation);
    assertNotNull(generation.getOutput());
    assertEquals("Hello! How can I help you today?", generation.getOutput().getText());

    verify(watsonxAiChatApi, times(1)).chat(any(WatsonxAiChatRequest.class));
  }

  @Test
  void chatModelCallAddsLogprobsMetadataWhenRequested() {
    WatsonxAiChatResponse.TextChatLogProbs logprobs =
        new WatsonxAiChatResponse.TextChatLogProbs(
            List.of(
                new WatsonxAiChatResponse.TextChatLogProbsContent(
                    "Hello",
                    -0.123,
                    List.of(72, 101, 108, 108, 111),
                    List.of(
                        new WatsonxAiChatResponse.TextChatTopLogProbs(
                            "Hello", -0.123, List.of(72, 101, 108, 108, 111)),
                        new WatsonxAiChatResponse.TextChatTopLogProbs(
                            "Hi", -1.456, List.of(72, 105))))),
            null);

    WatsonxAiChatResponse.TextChatResultChoice choice =
        new WatsonxAiChatResponse.TextChatResultChoice(
            0,
            new WatsonxAiChatResponse.TextChatResultMessage(
                org.springaicommunity.watsonx.chat.util.ChatRole.ASSISTANT, "Hello", null, null),
            "stop",
            logprobs);

    WatsonxAiChatResponse mockResponse =
        new WatsonxAiChatResponse(
            "test-id",
            "ibm/granite-3-3-8b-instruct",
            1234567890,
            List.of(choice),
            "2024-01-01",
            null,
            new WatsonxAiChatResponse.TextChatUsage(10, 1, 11),
            null);

    when(watsonxAiChatApi.chat(any(WatsonxAiChatRequest.class)))
        .thenReturn(ResponseEntity.ok(mockResponse));

    Prompt prompt =
        new Prompt(
            new UserMessage("Hello"),
            WatsonxAiChatOptions.builder().logProbs(true).topLogprobs(2).build());

    ChatResponse response = chatModel.call(prompt);
    var metadataLogprobs =
        (WatsonxAiChatResponse.TextChatLogProbs) response.getResult().getMetadata().get("logprobs");

    assertEquals(logprobs, metadataLogprobs);
    assertEquals("Hello", metadataLogprobs.content().get(0).token());
    assertEquals(-0.123, metadataLogprobs.content().get(0).logprob());
    assertEquals(List.of(72, 101, 108, 108, 111), metadataLogprobs.content().get(0).bytes());
    assertEquals("Hi", metadataLogprobs.content().get(0).topLogprobs().get(1).token());
    assertEquals(-1.456, metadataLogprobs.content().get(0).topLogprobs().get(1).logprob());

    ArgumentCaptor<WatsonxAiChatRequest> requestCaptor =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(watsonxAiChatApi).chat(requestCaptor.capture());
    assertTrue(requestCaptor.getValue().logprobs());
    assertEquals(2, requestCaptor.getValue().topLogprobs());
  }

  @Test
  void chatModelCallDoesNotAddLogprobsMetadataWhenNotRequested() {
    WatsonxAiChatResponse.TextChatLogProbs logprobs =
        new WatsonxAiChatResponse.TextChatLogProbs(
            List.of(
                new WatsonxAiChatResponse.TextChatLogProbsContent(
                    "Hello", -0.123, List.of(72, 101, 108, 108, 111), List.of())),
            null);

    WatsonxAiChatResponse.TextChatResultChoice choice =
        new WatsonxAiChatResponse.TextChatResultChoice(
            0,
            new WatsonxAiChatResponse.TextChatResultMessage(
                org.springaicommunity.watsonx.chat.util.ChatRole.ASSISTANT, "Hello", null, null),
            "stop",
            logprobs);

    WatsonxAiChatResponse mockResponse =
        new WatsonxAiChatResponse(
            "test-id",
            "ibm/granite-3-3-8b-instruct",
            1234567890,
            List.of(choice),
            "2024-01-01",
            null,
            new WatsonxAiChatResponse.TextChatUsage(10, 1, 11),
            null);

    when(watsonxAiChatApi.chat(any(WatsonxAiChatRequest.class)))
        .thenReturn(ResponseEntity.ok(mockResponse));

    ChatResponse response = chatModel.call(new Prompt(new UserMessage("Hello")));

    assertFalse(response.getResult().getMetadata().containsKey("logprobs"));
  }

  @Test
  void deserializeChatResponseWithLogprobs() throws Exception {
    String json =
        """
        {
          "id": "chat-id",
          "model_id": "ibm/granite-3-3-8b-instruct",
          "created": 1234567890,
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Hello"
              },
              "finish_reason": "stop",
              "logprobs": {
                "content": [
                  {
                    "token": "Hello",
                    "logprob": -0.123,
                    "bytes": [72, 101, 108, 108, 111],
                    "top_logprobs": [
                      {
                        "token": "Hello",
                        "logprob": -0.123,
                        "bytes": [72, 101, 108, 108, 111]
                      }
                    ]
                  }
                ],
                "refusal": []
              }
            }
          ],
          "usage": {
            "prompt_tokens": 10,
            "completion_tokens": 1,
            "total_tokens": 11
          }
        }
        """;

    WatsonxAiChatResponse response = new JsonHelper().fromJson(json, WatsonxAiChatResponse.class);

    WatsonxAiChatResponse.TextChatLogProbs logprobs = response.choices().get(0).logprobs();
    assertNotNull(logprobs);
    assertEquals("Hello", logprobs.content().get(0).token());
    assertEquals(-0.123, logprobs.content().get(0).logprob());
    assertEquals(List.of(72, 101, 108, 108, 111), logprobs.content().get(0).bytes());
    assertEquals("Hello", logprobs.content().get(0).topLogprobs().get(0).token());
  }

  @Test
  void chatModelBuilder() {
    WatsonxAiChatModel customChatModel =
        WatsonxAiChatModel.builder()
            .watsonxAiChatApi(watsonxAiChatApi)
            .options(WatsonxAiChatOptions.builder().model("custom-model").temperature(0.8).build())
            .observationRegistry(ObservationRegistry.NOOP)
            .toolCallingManager(ToolCallingManager.builder().build())
            .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
            .build();

    assertNotNull(customChatModel);
    assertEquals("custom-model", customChatModel.getOptions().getModel());
    assertEquals(0.8, customChatModel.getOptions().getTemperature());
  }

  @Test
  void optionsToMapConversion() {
    WatsonxAiChatOptions options =
        WatsonxAiChatOptions.builder()
            .model("test-model")
            .temperature(0.5)
            .topP(0.9)
            .maxTokens(1000)
            .build();

    var optionsMap = options.toMap();
    assertNotNull(optionsMap);
    assertEquals("test-model", optionsMap.get("model_id"));
    assertEquals(0.5, optionsMap.get("temperature"));
    assertEquals(0.9, optionsMap.get("top_p"));
    assertEquals(1000, optionsMap.get("max_tokens"));
  }

  @Test
  void optionsCopy() {
    WatsonxAiChatOptions original =
        WatsonxAiChatOptions.builder().model("original-model").temperature(0.3).build();

    WatsonxAiChatOptions copy = original.mutate().build();

    assertNotNull(copy);
    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getTemperature(), copy.getTemperature());
    assertNotSame(original, copy);
  }
}
