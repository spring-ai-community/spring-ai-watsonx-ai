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

package org.springaicommunity.watsonx.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Tests that every Spring AI message is sent to watsonx.ai with its own role, for example an
 * earlier answer of the model as {@code assistant}, not {@code system}.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatModelMessageRolesTest {

  // A conversation with every message type, as chat memory and tool calling build it
  private static final List<Message> CONVERSATION =
      List.of(
          new SystemMessage("Be terse."),
          new UserMessage("Hi"),
          AssistantMessage.builder().content("Hello! How can I help you today?").build(),
          new UserMessage("What time is it in UTC?"),
          AssistantMessage.builder()
              .content("")
              .toolCalls(
                  List.of(
                      new AssistantMessage.ToolCall(
                          "call-1", "function", "getTime", "{\"timeZone\":\"UTC\"}")))
              .build(),
          ToolResponseMessage.builder()
              .responses(
                  List.of(new ToolResponseMessage.ToolResponse("call-1", "getTime", "12:00")))
              .build());

  private static final List<String> ROLES =
      List.of("system", "user", "assistant", "user", "assistant", "tool");

  private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

  @Test
  void callSendsEveryMessageWithItsRole() {
    when(this.chatApi.chat(any()))
        .thenReturn(
            ResponseEntity.ok(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"It's 12:00."},"finish_reason":"stop"}]}""",
                    WatsonxAiChatResponse.class)));

    chatModel().call(new Prompt(CONVERSATION));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertThat(sentRoles(request.getValue())).containsExactlyElementsOf(ROLES);
  }

  @Test
  void streamSendsEveryMessageWithItsRole() {
    when(this.chatApi.stream(any()))
        .thenReturn(
            Flux.just(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"It's 12:00."},"finish_reason":"stop"}]}""",
                    WatsonxAiChatStream.class)));

    chatModel().stream(new Prompt(CONVERSATION)).collectList().block();

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).stream(request.capture());
    assertThat(sentRoles(request.getValue())).containsExactlyElementsOf(ROLES);
  }

  @Test
  void assistantMessageKeepsItsText() {
    when(this.chatApi.chat(any()))
        .thenReturn(
            ResponseEntity.ok(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"Tokyo."},"finish_reason":"stop"}]}""",
                    WatsonxAiChatResponse.class)));

    chatModel()
        .call(
            new Prompt(
                List.of(
                    new UserMessage("Hi"),
                    AssistantMessage.builder().content("Hello! How can I help you today?").build(),
                    new UserMessage("What is the capital of Japan?"))));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    List<Map<String, Object>> messages = sentMessages(request.getValue());
    assertThat(messages.get(1))
        .containsEntry("role", "assistant")
        .containsEntry("content", "Hello! How can I help you today?");
  }

  /** The roles as watsonx.ai receives them, in the JSON request body. */
  private static List<Object> sentRoles(WatsonxAiChatRequest request) {
    return sentMessages(request).stream().map(message -> message.get("role")).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> sentMessages(WatsonxAiChatRequest request) {
    return (List<Map<String, Object>>)
        ModelOptionsUtils.jsonToMap(ModelOptionsUtils.toJsonString(request)).get("messages");
  }

  private WatsonxAiChatModel chatModel() {
    return WatsonxAiChatModel.builder()
        .watsonxAiChatApi(this.chatApi)
        .options(WatsonxAiChatOptions.builder().model("test/model").build())
        .observationRegistry(ObservationRegistry.NOOP)
        .toolCallingManager(ToolCallingManager.builder().build())
        .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
        .build();
  }
}
