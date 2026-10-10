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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Tests that every {@link WatsonxAiChatOptions} option is sent to watsonx.ai, for both
 * {@code call()} and {@code stream()}. The chat API is mocked, so no network call is
 * made.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatModelRequestOptionsTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final WatsonxAiChatRequest.TextChatToolChoiceTool GET_TIME = new WatsonxAiChatRequest.TextChatToolChoiceTool(
			ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("getTime"));

	private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

	@Test
	void callSendsEveryOption() {
		when(this.chatApi.chat(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
				WatsonxAiChatResponse.class)));

		chatModel().call(new Prompt("Is Paris the capital of France?", options()));

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).chat(request.capture());
		assertSendsEveryOption(request.getValue());
	}

	@Test
	void streamSendsEveryOption() {
		when(this.chatApi.stream(any())).thenReturn(Flux.just(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""",
				WatsonxAiChatStream.class)));

		chatModel().stream(new Prompt("Is Paris the capital of France?", options())).collectList().block();

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).stream(request.capture());
		assertSendsEveryOption(request.getValue());
	}

	@Test
	void toolChoiceIsSentAsOneObject() {
		String json = JSON_HELPER.toJson(WatsonxAiChatRequest.builder().toolChoice(GET_TIME).build());

		assertThat(json).contains("\"tool_choice\":{\"type\":\"function\",\"function\":{\"name\":\"getTime\"}}");
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

	private static WatsonxAiChatOptions options() {
		WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
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
		assertThat(request.toolChoice()).isSameAs(GET_TIME);
	}

	private WatsonxAiChatModel chatModel() {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(this.chatApi)
			.options(WatsonxAiChatOptions.builder().model("test/model").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.build();
	}

}
