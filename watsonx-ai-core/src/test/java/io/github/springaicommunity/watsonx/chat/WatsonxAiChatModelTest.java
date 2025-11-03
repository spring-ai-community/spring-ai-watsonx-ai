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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;

/**
 * Test class for WatsonxAiChatModel to simulate chat functionality and options.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
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
        new WatsonxAiChatModel(
            watsonxAiChatApi,
            defaultOptions,
            ObservationRegistry.NOOP,
            ToolCallingManager.builder().build(),
            new DefaultToolExecutionEligibilityPredicate(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void chatModelInitialization() {
    assertNotNull(chatModel);
    assertNotNull(chatModel.getDefaultOptions());
    assertEquals("ibm/granite-3-3-8b-instruct", chatModel.getDefaultOptions().getModel());
    assertEquals(0.7, chatModel.getDefaultOptions().getTemperature());
    assertEquals(1.0, chatModel.getDefaultOptions().getTopP());
    assertEquals(1024, chatModel.getDefaultOptions().getMaxTokens());
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
                io.github.springaicommunity.watsonx.chat.util.ChatRole.ASSISTANT,
                "Hello! How can I help you today?",
                null,
                null),
            "stop");

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
  void chatModelBuilder() {
    WatsonxAiChatModel customChatModel =
        WatsonxAiChatModel.builder()
            .watsonxAiChatApi(watsonxAiChatApi)
            .options(WatsonxAiChatOptions.builder().model("custom-model").temperature(0.8).build())
            .observationRegistry(ObservationRegistry.NOOP)
            .toolCallingManager(ToolCallingManager.builder().build())
            .toolExecutionEligibilityPredicate(new DefaultToolExecutionEligibilityPredicate())
            .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
            .build();

    assertNotNull(customChatModel);
    assertEquals("custom-model", customChatModel.getDefaultOptions().getModel());
    assertEquals(0.8, customChatModel.getDefaultOptions().getTemperature());
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

    WatsonxAiChatOptions copy = original.copy();

    assertNotNull(copy);
    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getTemperature(), copy.getTemperature());
    assertNotSame(original, copy);
  }
}
