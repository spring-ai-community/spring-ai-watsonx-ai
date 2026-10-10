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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.watsonx.chat.util.ChatRole;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;

/**
 * Tests {@link WatsonxAiChatOptions.Builder#combineWith}, which {@link ChatClient} uses
 * to merge per-request options into the default options: values set on the other builder
 * win, and unset ones keep the defaults.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatOptionsCombineWithTest {

	private static WatsonxAiChatOptions defaults() {
		return WatsonxAiChatOptions.builder()
			.model("ibm/granite-4-h-small")
			.temperature(0.7)
			.topP(1.0)
			.maxTokens(1024)
			.toolContext(Map.of("tenant", "a"))
			.build();
	}

	@Nested
	class BuilderTests {

		@Test
		void requestValuesOverrideDefaults() {
			WatsonxAiChatOptions merged = defaults().mutate()
				.combineWith(WatsonxAiChatOptions.builder()
					.model("mistralai/mistral-small-3-1-24b-instruct-2503")
					.temperature(0.1))
				.build();

			assertThat(merged.getModel()).isEqualTo("mistralai/mistral-small-3-1-24b-instruct-2503");
			assertThat(merged.getTemperature()).isEqualTo(0.1);
		}

		@Test
		void unsetRequestValuesKeepDefaults() {
			WatsonxAiChatOptions merged = defaults().mutate().combineWith(WatsonxAiChatOptions.builder()).build();

			assertThat(merged.getModel()).isEqualTo("ibm/granite-4-h-small");
			assertThat(merged.getTemperature()).isEqualTo(0.7);
			assertThat(merged.getTopP()).isEqualTo(1.0);
			assertThat(merged.getMaxTokens()).isEqualTo(1024);
		}

		@Test
		void fieldsBeyondTheCommonOptionsAreMerged() {
			TextChatResponseFormat responseFormat = TextChatResponseFormat.jsonObject();
			WatsonxAiChatOptions merged = defaults().mutate()
				.combineWith(WatsonxAiChatOptions.builder()
					.responseFormat(responseFormat)
					.seed(42)
					.reasoningEffort("low")
					.toolChoiceOption("required")
					.maxCompletionTokens(256)
					.n(2)
					.logProbs(true)
					.topLogprobs(3)
					.guidedChoice(List.of("yes", "no"))
					.additionalProperty("custom", "value"))
				.build();

			assertThat(merged.getResponseFormat()).isSameAs(responseFormat);
			assertThat(merged.getSeed()).isEqualTo(42);
			assertThat(merged.getReasoningEffort()).isEqualTo("low");
			assertThat(merged.getToolChoiceOption()).isEqualTo("required");
			assertThat(merged.getMaxCompletionTokens()).isEqualTo(256);
			assertThat(merged.getN()).isEqualTo(2);
			assertThat(merged.getLogprobs()).isTrue();
			assertThat(merged.getTopLogprobs()).isEqualTo(3);
			assertThat(merged.getGuidedChoice()).containsExactly("yes", "no");
			assertThat(merged.getAdditionalProperties()).containsEntry("custom", "value");
			assertThat(merged.getModel()).isEqualTo("ibm/granite-4-h-small");
		}

		@Test
		void toolContextIsMerged() {
			WatsonxAiChatOptions merged = defaults().mutate()
				.combineWith(WatsonxAiChatOptions.builder().toolContext(Map.of("user", "b")))
				.build();

			assertThat(merged.getToolContext()).containsEntry("tenant", "a").containsEntry("user", "b");
		}

		@Test
		void explicitFalseIncludeReasoningOverridesDefault() {
			WatsonxAiChatOptions merged = defaults().mutate()
				.combineWith(WatsonxAiChatOptions.builder().includeReasoning(false))
				.build();

			assertThat(merged.isIncludeReasoning()).isFalse();
		}

		@Test
		void mutateKeepsTimeLimit() {
			WatsonxAiChatOptions options = defaults();
			options.setTimeLimit(5000);

			assertThat(options.mutate().build().getTimeLimit()).isEqualTo(5000);
		}

	}

	@Nested
	class ChatClientTests {

		@Test
		void chatClientSendsPerRequestModelAndKeepsOtherDefaults() {
			WatsonxAiChatApi api = mock(WatsonxAiChatApi.class);
			when(api.chat(any(WatsonxAiChatRequest.class))).thenReturn(ResponseEntity.ok(response()));
			WatsonxAiChatModel chatModel = WatsonxAiChatModel.builder()
				.watsonxAiChatApi(api)
				.options(defaults())
				.observationRegistry(ObservationRegistry.NOOP)
				.toolCallingManager(ToolCallingManager.builder().build())
				.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
				.build();

			ChatClient.create(chatModel)
				.prompt("Hello")
				.options(WatsonxAiChatOptions.builder()
					.model("mistralai/mistral-small-3-1-24b-instruct-2503")
					.responseFormat(TextChatResponseFormat.jsonObject()))
				.call()
				.content();

			ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
			verify(api).chat(request.capture());
			assertThat(request.getValue().model()).isEqualTo("mistralai/mistral-small-3-1-24b-instruct-2503");
			assertThat(request.getValue().responseFormat().getType())
				.isEqualTo(TextChatResponseFormat.Type.JSON_OBJECT);
			assertThat(request.getValue().temperature()).isEqualTo(0.7);
			assertThat(request.getValue().maxTokens()).isEqualTo(1024);
		}

		private static WatsonxAiChatResponse response() {
			WatsonxAiChatResponse.TextChatResultChoice choice = new WatsonxAiChatResponse.TextChatResultChoice(0,
					new WatsonxAiChatResponse.TextChatResultMessage(ChatRole.ASSISTANT, "pong", null, null), "stop",
					null);
			return new WatsonxAiChatResponse("test-id", "mistralai/mistral-small-3-1-24b-instruct-2503", 1234567890,
					List.of(choice), "2024-01-01", null, new WatsonxAiChatResponse.TextChatUsage(1, 1, 2), null);
		}

	}

}
