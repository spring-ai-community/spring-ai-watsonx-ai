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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.offset;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionResponse.Detection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The watsonx.ai detection evaluators against a live watsonx.ai. Needs
 * {@code WATSONX_AI_API_KEY}, {@code WATSONX_AI_URL} and
 * {@code WATSONX_AI_GOVERNANCE_INSTANCE_ID} (the GUID of a watsonx.governance instance in
 * the same region), and is skipped without them.
 *
 * @author Federico Mariani
 */
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_AI_GOVERNANCE_INSTANCE_ID", matches = ".+")
class WatsonxAiDetectionEvaluatorsIT {

	private static final String QUESTION = "What is the capital of Italy?";

	private static final List<Document> DOCUMENTS = List.of(
			new Document("Rome is the capital city of Italy. It has about 2.8 million inhabitants."),
			new Document("The Colosseum is an ancient amphitheatre in the centre of Rome."));

	private final WatsonxAiDetectionApi detectionApi = detectionApi(System.getenv("WATSONX_AI_GOVERNANCE_INSTANCE_ID"),
			null);

	@Test
	void answerRelevance() {
		Evaluator evaluator = new WatsonxAiAnswerRelevanceEvaluator(this.detectionApi);

		EvaluationResponse relevant = evaluator
			.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome."));
		EvaluationResponse irrelevant = evaluator
			.evaluate(new EvaluationRequest(QUESTION, "Bananas are rich in potassium."));

		assertPasses(relevant, "answer_relevance");
		assertFails(irrelevant, "answer_relevance");
	}

	@Test
	void contextRelevance() {
		Evaluator evaluator = new WatsonxAiContextRelevanceEvaluator(this.detectionApi);

		EvaluationResponse relevant = evaluator.evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, ""));
		EvaluationResponse irrelevant = evaluator
			.evaluate(new EvaluationRequest("How do I bake sourdough bread?", DOCUMENTS, ""));

		assertPasses(relevant, "context_relevance");
		assertFails(irrelevant, "context_relevance");
	}

	@Test
	void faithfulness() {
		Evaluator evaluator = WatsonxAiFaithfulnessEvaluator.builder()
			.detectionApi(this.detectionApi)
			.threshold(0.7)
			.build();

		EvaluationResponse grounded = evaluator.evaluate(new EvaluationRequest(DOCUMENTS,
				"Rome is the capital of Italy and has about 2.8 million inhabitants."));
		EvaluationResponse hallucinated = evaluator.evaluate(
				new EvaluationRequest(DOCUMENTS, "Milan is the capital of Italy and has 10 million inhabitants."));

		assertPasses(grounded, "faithfulness");
		assertFails(hallucinated, "faithfulness");
	}

	// The way Spring AI evaluates a RAG answer: the model answers from the documents, and
	// the evaluators check the answer
	@Test
	@EnabledIfEnvironmentVariable(named = "WATSONX_AI_PROJECT_ID", matches = ".+")
	void answerOfAChatModelFromTheDocuments() {
		String context = DOCUMENTS.stream().map(Document::getText).collect(Collectors.joining("\n"));
		String answer = ChatClient.create(chatModel())
			.prompt()
			.system("Answer in one sentence, using only this context:\n" + context)
			.user(QUESTION)
			.call()
			.content();
		EvaluationRequest request = new EvaluationRequest(QUESTION, DOCUMENTS, answer);

		assertPasses(new WatsonxAiAnswerRelevanceEvaluator(this.detectionApi).evaluate(request), "answer_relevance");
		assertPasses(new WatsonxAiFaithfulnessEvaluator(this.detectionApi).evaluate(request), "faithfulness");
		assertPasses(new WatsonxAiContextRelevanceEvaluator(this.detectionApi).evaluate(request), "context_relevance");
	}

	// The detectors need a watsonx.governance instance
	@Test
	@EnabledIfEnvironmentVariable(named = "WATSONX_AI_PROJECT_ID", matches = ".+")
	void withAProjectInsteadOfAGovernanceInstanceTheDetectorIsNotAccessible() {
		Evaluator evaluator = new WatsonxAiAnswerRelevanceEvaluator(
				detectionApi(null, System.getenv("WATSONX_AI_PROJECT_ID")));

		assertThatExceptionOfType(NonTransientAiException.class)
			.isThrownBy(() -> evaluator.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome.")))
			.withMessageContaining("detector_not_accessible");
	}

	private static void assertPasses(EvaluationResponse evaluation, String detector) {
		assertThat(evaluation.isPass()).as(evaluation.getFeedback()).isTrue();
		assertThat(scoreOf(evaluation)).isGreaterThanOrEqualTo(thresholdOf(evaluation));
		assertDetection(evaluation, detector);
	}

	private static void assertFails(EvaluationResponse evaluation, String detector) {
		assertThat(evaluation.isPass()).as(evaluation.getFeedback()).isFalse();
		assertThat(scoreOf(evaluation)).isLessThan(thresholdOf(evaluation));
		assertDetection(evaluation, detector);
	}

	private static double scoreOf(EvaluationResponse evaluation) {
		return ((Detection) evaluation.getMetadata().get(WatsonxAiDetectionEvaluator.DETECTION_METADATA_KEY)).score();
	}

	private static double thresholdOf(EvaluationResponse evaluation) {
		return (double) evaluation.getMetadata().get(WatsonxAiDetectionEvaluator.THRESHOLD_METADATA_KEY);
	}

	private static void assertDetection(EvaluationResponse evaluation, String detector) {
		Detection detection = (Detection) evaluation.getMetadata()
			.get(WatsonxAiDetectionEvaluator.DETECTION_METADATA_KEY);
		assertThat(detection.detectionType()).isEqualTo(detector);
		assertThat(detection.detectorId()).isNotBlank();
		assertThat(detection.score()).isCloseTo(evaluation.getScore(), offset(1e-6));
	}

	private static WatsonxAiDetectionApi detectionApi(String governanceInstanceId, String projectId) {
		return new WatsonxAiDetectionApi(System.getenv("WATSONX_AI_URL"),
				WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT, WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT,
				WatsonxAiDetectionApi.DEFAULT_VERSION, projectId, null, governanceInstanceId,
				System.getenv("WATSONX_AI_API_KEY"), RestClient.builder(), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	private static WatsonxAiChatModel chatModel() {
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
