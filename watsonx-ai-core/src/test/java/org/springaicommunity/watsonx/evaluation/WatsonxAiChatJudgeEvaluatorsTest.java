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

package org.springaicommunity.watsonx.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;

/**
 * The example of the documentation for evaluating without a watsonx.governance instance:
 * Spring AI's own LLM-as-judge evaluators, with a watsonx.ai chat model as the judge. The
 * chat API is mocked.
 *
 * @author Federico Mariani
 */
class WatsonxAiChatJudgeEvaluatorsTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final List<Document> DOCUMENTS = List
		.of(new Document("Rome is the capital city of Italy. It has about 2.8 million inhabitants."));

	private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

	@Test
	void relevancyEvaluatorJudgesWithAWatsonxAiChatModel() {
		answer("YES");
		Evaluator relevancy = new RelevancyEvaluator(judge());

		EvaluationResponse evaluation = relevancy.evaluate(
				new EvaluationRequest("What is the capital of Italy?", DOCUMENTS, "The capital of Italy is Rome."));

		assertThat(evaluation.isPass()).isTrue();
		assertThat(sentPrompt()).contains("What is the capital of Italy?")
			.contains("The capital of Italy is Rome.")
			.contains("Rome is the capital city of Italy.");
	}

	@Test
	void factCheckingEvaluatorJudgesWithAWatsonxAiChatModel() {
		answer("no");
		Evaluator factChecking = FactCheckingEvaluator.builder(judge()).build();

		EvaluationResponse evaluation = factChecking
			.evaluate(new EvaluationRequest(DOCUMENTS, "Milan is the capital of Italy."));

		assertThat(evaluation.isPass()).isFalse();
		assertThat(sentPrompt()).contains("Milan is the capital of Italy.")
			.contains("Rome is the capital city of Italy.");
	}

	// As in the documentation
	private ChatClient.Builder judge() {
		WatsonxAiChatModel chatModel = WatsonxAiChatModel.builder()
			.watsonxAiChatApi(this.chatApi)
			.observationRegistry(ObservationRegistry.NOOP)
			.build();
		return ChatClient.builder(chatModel)
			.defaultOptions(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").temperature(0.0));
	}

	@Test
	void judgeUsesTheOptionsOfTheDocumentation() {
		answer("yes");

		new RelevancyEvaluator(judge()).evaluate(new EvaluationRequest("Q?", DOCUMENTS, "A."));

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).chat(request.capture());
		assertThat(request.getValue().model()).isEqualTo("ibm/granite-4-h-small");
		assertThat(request.getValue().temperature()).isZero();
	}

	private void answer(String text) {
		when(this.chatApi.chat(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"ibm/granite-4-h-small","choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}"""
					.formatted(text),
				WatsonxAiChatResponse.class)));
	}

	private String sentPrompt() {
		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).chat(request.capture());
		return JSON_HELPER.toJson(request.getValue().messages());
	}

}
