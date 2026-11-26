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

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The documented alternative without a watsonx.governance instance, against a live
 * watsonx.ai: Spring AI's LLM-as-judge evaluators with {@code ibm/granite-4-h-small} as
 * the judge. Needs {@code WATSONX_AI_API_KEY}, {@code WATSONX_AI_PROJECT_ID} and
 * {@code WATSONX_AI_URL}, and is skipped without them.
 *
 * @author Federico Mariani
 */
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_URL", matches = ".+")
class WatsonxAiChatJudgeEvaluatorsIT {

	private static final String QUESTION = "What is the capital of Italy?";

	private static final List<Document> DOCUMENTS = List.of(
			new Document("Rome is the capital city of Italy. It has about 2.8 million inhabitants."),
			new Document("The Colosseum is an ancient amphitheatre in the centre of Rome."));

	@Test
	void relevancyEvaluator() {
		Evaluator relevancy = new RelevancyEvaluator(ChatClient.builder(judge()));

		assertThat(relevancy.evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, "The capital of Italy is Rome."))
			.isPass()).isTrue();
		assertThat(relevancy.evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, "Bananas are rich in potassium."))
			.isPass()).isFalse();
	}

	@Test
	void factCheckingEvaluator() {
		Evaluator factChecking = FactCheckingEvaluator.builder(ChatClient.builder(judge())).build();

		assertThat(factChecking.evaluate(new EvaluationRequest(DOCUMENTS, "Rome is the capital of Italy.")).isPass())
			.isTrue();
		assertThat(factChecking
			.evaluate(new EvaluationRequest(DOCUMENTS, "Milan is the capital of Italy and has 10 million inhabitants."))
			.isPass()).isFalse();
	}

	private static WatsonxAiChatModel judge() {
		WatsonxAiChatApi chatApi = new WatsonxAiChatApi(System.getenv("WATSONX_AI_URL"), "/ml/v1/text/chat",
				"/ml/v1/text/chat_stream", "2024-10-17", System.getenv("WATSONX_AI_PROJECT_ID"), null,
				System.getenv("WATSONX_AI_API_KEY"), RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").temperature(0.0).build())
			.observationRegistry(ObservationRegistry.NOOP)
			.build();
	}

}
