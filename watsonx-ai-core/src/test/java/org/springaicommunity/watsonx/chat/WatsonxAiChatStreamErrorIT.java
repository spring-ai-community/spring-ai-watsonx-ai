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
import static org.assertj.core.api.Assertions.catchThrowable;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Errors that watsonx.ai reports in a chat stream as an {@code event: error} after HTTP
 * 200, against a live watsonx.ai. Needs {@code WATSONX_AI_API_KEY},
 * {@code WATSONX_AI_PROJECT_ID} and {@code WATSONX_AI_URL}, and is skipped without them.
 *
 * @author Federico Mariani
 */
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_URL", matches = ".+")
class WatsonxAiChatStreamErrorIT {

	private static final Prompt HI = new Prompt("Say hi");

	// The model takes n > 1 only with sampling
	@Test
	void invalidArgumentFailsTheStreamAsItFailsTheCall() {
		WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().n(2).temperature(0.0).build();
		WatsonxAiChatModel chatModel = chatModel();

		Throwable callError = catchThrowable(() -> chatModel.call(new Prompt("Say hi", options)));
		Throwable streamError = catchThrowable(
				() -> chatModel.stream(new Prompt("Say hi", options)).collectList().block());

		assertThat(callError).isInstanceOf(NonTransientAiException.class).hasMessageStartingWith("400 - ");
		assertThat(streamError).isInstanceOf(NonTransientAiException.class)
			.hasMessageStartingWith("400 - ")
			.hasMessageContaining("n must be 1 when using greedy sampling");
	}

	@Test
	void unknownLogitBiasTokenFailsTheStream() {
		Throwable streamError = catchThrowable(() -> chatModel()
			.stream(new Prompt("Say hi", WatsonxAiChatOptions.builder().logitBias(Map.of("999999999", 5)).build()))
			.collectList()
			.block());

		assertThat(streamError).isInstanceOf(NonTransientAiException.class)
			.hasMessageStartingWith("400 - ")
			.hasMessageContaining("out-of-vocab");
	}

	@Test
	void validRequestStillStreams() {
		List<ChatResponse> responses = chatModel().stream(HI).collectList().block();

		assertThat(responses.stream()
			.flatMap(response -> response.getResults().stream())
			.map(generation -> generation.getOutput().getText())
			.filter(Objects::nonNull)
			.collect(Collectors.joining())).isNotBlank();
	}

	private static WatsonxAiChatModel chatModel() {
		WatsonxAiChatApi chatApi = new WatsonxAiChatApi(System.getenv("WATSONX_AI_URL"), "/ml/v1/text/chat",
				"/ml/v1/text/chat_stream", "2024-10-17", System.getenv("WATSONX_AI_PROJECT_ID"), null,
				System.getenv("WATSONX_AI_API_KEY"), RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").maxCompletionTokens(10).build())
			.observationRegistry(ObservationRegistry.NOOP)
			.retryTemplate(RetryUtils.SHORT_RETRY_TEMPLATE)
			.build();
	}

}
