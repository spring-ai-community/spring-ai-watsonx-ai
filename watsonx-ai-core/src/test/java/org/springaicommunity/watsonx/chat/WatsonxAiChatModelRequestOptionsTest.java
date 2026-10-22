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

package org.springaicommunity.watsonx.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Tests that every {@link WatsonxAiChatOptions} option is sent to watsonx.ai, for both {@code
 * call()} and {@code stream()}. The chat API is mocked, so no network call is made.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatModelRequestOptionsTest {

  private static final WatsonxAiChatRequest.TextChatToolChoiceTool GET_TIME =
      new WatsonxAiChatRequest.TextChatToolChoiceTool(
          ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("getTime"));

  private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

  @Test
  void callSendsEveryOption() {
    when(this.chatApi.chat(any()))
        .thenReturn(
            ResponseEntity.ok(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
                    WatsonxAiChatResponse.class)));

    chatModel(defaultOptions()).call(new Prompt("Is Paris the capital of France?", options()));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertSendsEveryOption(request.getValue());
  }

  @Test
  void streamSendsEveryOption() {
    when(this.chatApi.stream(any()))
        .thenReturn(
            Flux.just(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
                    WatsonxAiChatStream.class)));

    chatModel(defaultOptions()).stream(new Prompt("Is Paris the capital of France?", options()))
        .collectList()
        .block();

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).stream(request.capture());
    assertSendsEveryOption(request.getValue());
  }

  // ChatClient builds every request from a copy() of the model's default options, so
  // they must survive copy(), request after request
  @Test
  void chatClientSendsEveryDefaultOption() {
    mockCallAndStream();
    ChatClient chatClient = ChatClient.create(chatModel(options()));

    chatClient.prompt("Is Paris the capital of France?").call().content();
    chatClient.prompt("Is Rome the capital of Italy?").call().content();
    chatClient.prompt("Is Bern the capital of Switzerland?").stream()
        .content()
        .collectList()
        .block();

    assertEveryRequestSendsEveryOption();
  }

  @Test
  void chatClientWithDefaultOptionsSendsEveryDefaultOption() {
    mockCallAndStream();
    ChatClient chatClient =
        ChatClient.builder(chatModel(options()))
            .defaultOptions(WatsonxAiChatOptions.builder().temperature(0.2).build())
            .build();

    chatClient.prompt("Is Paris the capital of France?").call().content();
    chatClient.prompt("Is Rome the capital of Italy?").call().content();
    chatClient.prompt("Is Bern the capital of Switzerland?").stream()
        .content()
        .collectList()
        .block();

    assertEveryRequestSendsEveryOption();
  }

  @Test
  void copyKeepsTimeLimit() {
    WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().model("test/model").build();
    options.setTimeLimit(5000);

    assertThat(options.copy().getTimeLimit()).isEqualTo(5000);
  }

  @Test
  void builderSetsTimeLimit() {
    assertThat(WatsonxAiChatOptions.builder().timeLimit(5000).build().getTimeLimit())
        .isEqualTo(5000);
    assertThat(WatsonxAiChatOptions.builder().timeLimit(null).build().getTimeLimit()).isNull();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> WatsonxAiChatOptions.builder().timeLimit(0));
  }

  // Some models reject options they don't support (mistral-large-2512 answers 400
  // extra_forbidden for max_completion_tokens and include_reasoning), so an option that
  // isn't set isn't sent
  @Test
  void unsetOptionsAreNotSent() {
    mockCallAndStream();

    chatModel(defaultOptions()).call(new Prompt("Is Paris the capital of France?"));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertThat(ModelOptionsUtils.toJsonString(request.getValue()))
        .doesNotContain("max_completion_tokens")
        .doesNotContain("include_reasoning")
        .doesNotContain("time_limit")
        .doesNotContain("seed");
  }

  @Test
  void toolChoiceIsSentAsOneObject() {
    String json =
        ModelOptionsUtils.toJsonString(WatsonxAiChatRequest.builder().toolChoice(GET_TIME).build());

    assertThat(json)
        .contains("\"tool_choice\":{\"type\":\"function\",\"function\":{\"name\":\"getTime\"}}");
  }

  @Test
  @SuppressWarnings("removal")
  void deprecatedListToolChoiceTakesAtMostOneTool() {
    assertThat(WatsonxAiChatRequest.builder().toolChoice(List.of(GET_TIME)).build().toolChoice())
        .isSameAs(GET_TIME);
    assertThat(WatsonxAiChatRequest.builder().toolChoice(List.of()).build().toolChoice()).isNull();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> WatsonxAiChatRequest.builder().toolChoice(List.of(GET_TIME, GET_TIME)));
  }

  private static final WatsonxAiChatRequest.TextChatParameterTool GET_WEATHER =
      new WatsonxAiChatRequest.TextChatParameterTool(
          ToolType.FUNCTION,
          new WatsonxAiChatRequest.TextChatParameterFunction(
              "get_weather", "Get the current weather for a city", Map.of("type", "object")));

  private static final WatsonxAiChatRequest.TextChatParameterTool GET_TIME_TOOL =
      new WatsonxAiChatRequest.TextChatParameterTool(
          ToolType.FUNCTION,
          new WatsonxAiChatRequest.TextChatParameterFunction(
              "getTime", "Get the time", Map.of("type", "object")));

  record City(String city) {}

  @Test
  void explicitToolsAreSent() {
    mockCallAndStream();

    chatModel(defaultOptions())
        .call(
            new Prompt(
                "What is the weather in Manila?",
                WatsonxAiChatOptions.builder()
                    .model("test/model")
                    .tools(List.of(GET_WEATHER))
                    .build()));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertThat(request.getValue().tools())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(GET_WEATHER);
  }

  @Test
  void toolCallbackWinsOverAnExplicitToolWithTheSameName() {
    mockCallAndStream();
    ToolCallback getWeather =
        FunctionToolCallback.builder("get_weather", (City city) -> "sunny")
            .description("From the tool callback")
            .inputType(City.class)
            .build();

    chatModel(defaultOptions())
        .call(
            new Prompt(
                "What is the weather in Manila?",
                WatsonxAiChatOptions.builder()
                    .model("test/model")
                    .toolCallbacks(List.of(getWeather))
                    .tools(List.of(GET_WEATHER, GET_TIME_TOOL))
                    .build()));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertThat(request.getValue().tools())
        .extracting(tool -> tool.function().name())
        .containsExactly("get_weather", "getTime");
    assertThat(request.getValue().tools().get(0).function().description())
        .isEqualTo("From the tool callback");
  }

  @Test
  void additionalPropertiesAreSentAsTopLevelFields() {
    mockCallAndStream();

    chatModel(defaultOptions())
        .call(
            new Prompt(
                "Answer with the single word yes.",
                WatsonxAiChatOptions.builder()
                    .model("test/model")
                    .additionalProperty("repetitionPenalty", 2.0)
                    .build()));

    ArgumentCaptor<WatsonxAiChatRequest> request =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).chat(request.capture());
    assertThat(ModelOptionsUtils.toJsonString(request.getValue()))
        .contains("\"repetition_penalty\":2.0")
        .doesNotContain("additional");
  }

  @Test
  void additionalPropertyNamedLikeAFieldIsRejected() {
    WatsonxAiChatModel chatModel = chatModel(defaultOptions());
    Prompt prompt =
        new Prompt(
            "Hi",
            WatsonxAiChatOptions.builder()
                .model("test/model")
                .additionalProperty("max_completion_tokens", 3)
                .build());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> chatModel.call(prompt))
        .withMessageContaining("max_completion_tokens");
  }

  // WatsonxAiChatApi adds the project or space with toBuilder()
  @Test
  void additionalPropertiesSurviveToBuilder() {
    WatsonxAiChatRequest request =
        WatsonxAiChatRequest.builder()
            .additionalProperties(Map.of("repetition_penalty", 2.0))
            .build()
            .toBuilder()
            .spaceId("test-space")
            .build();

    assertThat(request.additionalProperties()).containsEntry("repetition_penalty", 2.0);
    assertThat(ModelOptionsUtils.toJsonString(request))
        .contains("\"repetition_penalty\":2.0")
        .contains("\"space_id\"");
  }

  @Test
  void fieldNamesListsEveryJsonField() {
    assertThat(
            Arrays.stream(WatsonxAiChatRequest.class.getDeclaredFields())
                .map(field -> field.getAnnotation(JsonProperty.class))
                .filter(Objects::nonNull)
                .map(JsonProperty::value))
        .containsExactlyInAnyOrderElementsOf(WatsonxAiChatRequest.FIELD_NAMES);
  }

  // ChatClient builds every request from a copy() of the model's default options
  @Test
  void chatClientSendsDefaultToolsAndAdditionalPropertiesEveryRequest() {
    mockCallAndStream();
    ChatClient chatClient =
        ChatClient.create(
            chatModel(
                WatsonxAiChatOptions.builder()
                    .model("test/model")
                    .tools(List.of(GET_WEATHER))
                    .additionalProperty("repetition_penalty", 2.0)
                    .build()));

    chatClient.prompt("Hi").call().content();
    chatClient.prompt("Hi again").call().content();
    chatClient.prompt("And once more").stream().content().collectList().block();

    ArgumentCaptor<WatsonxAiChatRequest> call = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi, times(2)).chat(call.capture());
    ArgumentCaptor<WatsonxAiChatRequest> stream =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).stream(stream.capture());
    List<WatsonxAiChatRequest> requests = new ArrayList<>(call.getAllValues());
    requests.add(stream.getValue());
    assertThat(requests)
        .allSatisfy(
            request -> {
              assertThat(request.tools())
                  .usingRecursiveFieldByFieldElementComparator()
                  .containsExactly(GET_WEATHER);
              assertThat(request.additionalProperties()).containsEntry("repetition_penalty", 2.0);
            });
  }

  private static WatsonxAiChatOptions options() {
    WatsonxAiChatOptions options =
        WatsonxAiChatOptions.builder()
            .model("test/model")
            .maxCompletionTokens(3)
            .seed(42)
            .logitBias(Map.of("1", 0))
            .guidedChoice(List.of("yes", "no"))
            .guidedRegex("yes|no")
            .guidedGrammar("root ::= \"yes\" | \"no\"")
            .guidedJson(Map.of("type", "string"))
            .chatTemplateKwargs(Map.of("thinking", false))
            .includeReasoning(false)
            .reasoningEffort("low")
            .toolChoiceOption("none")
            .toolChoice(GET_TIME)
            .build();
    options.setTimeLimit(10000);
    return options;
  }

  private static void assertSendsEveryOption(WatsonxAiChatRequest request) {
    assertThat(request.maxCompletionTokens()).isEqualTo(3);
    assertThat(request.seed()).isEqualTo(42);
    assertThat(request.logitBias()).isEqualTo(Map.of("1", 0));
    assertThat(request.timeLimit()).isEqualTo(10000);
    assertThat(request.guidedChoice()).containsExactly("yes", "no");
    assertThat(request.guidedRegex()).isEqualTo("yes|no");
    assertThat(request.guidedGrammar()).isEqualTo("root ::= \"yes\" | \"no\"");
    assertThat(request.guidedJson()).isEqualTo(Map.of("type", "string"));
    assertThat(request.chatTemplateKwargs()).isEqualTo(Map.of("thinking", false));
    assertThat(request.includeReasoning()).isFalse();
    assertThat(request.reasoningEffort()).isEqualTo("low");
    assertThat(request.toolChoiceOption()).isEqualTo("none");
    // The options merge copies the tool choice
    assertThat(request.toolChoice()).usingRecursiveComparison().isEqualTo(GET_TIME);
  }

  private void mockCallAndStream() {
    when(this.chatApi.chat(any()))
        .thenReturn(
            ResponseEntity.ok(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
                    WatsonxAiChatResponse.class)));
    when(this.chatApi.stream(any()))
        .thenReturn(
            Flux.just(
                ModelOptionsUtils.jsonToObject(
                    """
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
                    WatsonxAiChatStream.class)));
  }

  private void assertEveryRequestSendsEveryOption() {
    ArgumentCaptor<WatsonxAiChatRequest> call = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi, times(2)).chat(call.capture());
    ArgumentCaptor<WatsonxAiChatRequest> stream =
        ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
    verify(this.chatApi).stream(stream.capture());
    call.getAllValues().forEach(WatsonxAiChatModelRequestOptionsTest::assertSendsEveryOption);
    assertSendsEveryOption(stream.getValue());
  }

  private static WatsonxAiChatOptions defaultOptions() {
    return WatsonxAiChatOptions.builder().model("test/model").build();
  }

  private WatsonxAiChatModel chatModel(WatsonxAiChatOptions defaultOptions) {
    return WatsonxAiChatModel.builder()
        .watsonxAiChatApi(this.chatApi)
        .options(defaultOptions)
        .observationRegistry(ObservationRegistry.NOOP)
        .toolCallingManager(ToolCallingManager.builder().build())
        .build();
  }
}
