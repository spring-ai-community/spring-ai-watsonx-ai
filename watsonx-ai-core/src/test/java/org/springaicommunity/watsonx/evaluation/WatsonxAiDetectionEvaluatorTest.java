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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionResponse.Detection;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionResponse.Evidence;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests the watsonx.ai detection evaluators against responses recorded from watsonx.ai
 * eu-de with a watsonx.governance instance. The detection API is mocked with
 * {@link MockRestServiceServer}.
 *
 * @author Federico Mariani
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiDetectionEvaluatorTest {

	private static final String BASE_URL = "https://eu-de.ml.cloud.ibm.com";

	private static final String VERSION = "2025-10-01";

	private static final String GOVERNANCE_INSTANCE_ID = "test-governance-instance-id";

	private static final String CONTEXT_URL = BASE_URL + "/ml/v1/text/detection/context?version=" + VERSION;

	private static final String GENERATED_URL = BASE_URL + "/ml/v1/text/detection/generated?version=" + VERSION;

	private static final String QUESTION = "What is the capital of Italy?";

	private static final List<Document> DOCUMENTS = List.of(
			new Document("Rome is the capital city of Italy. It has about 2.8 million inhabitants."),
			new Document("The Colosseum is an ancient amphitheatre in the centre of Rome."));

	private static final String RELEVANT_ANSWER = """
			{"detections":[{"detection_type":"answer_relevance","detector_id":"granite_guardian_3_2_5b","detection":"relevant","score":0.9707332327961922}]}""";

	private static final String IRRELEVANT_ANSWER = """
			{"detections":[{"detection_type":"answer_relevance","detector_id":"granite_guardian_3_2_5b","detection":"relevant","score":0.01588916778564442}]}""";

	private static final String RELEVANT_CONTEXT = """
			{"detections":[{"detection_type":"context_relevance","detection":"relevant","detector_id":"granite_guardian_3_2_5b","score":0.9850712977349758}]}""";

	private static final String UNFAITHFUL_ANSWER = """
			{"detections":[{"detection_type":"faithfulness","detection":"factual","detector_id":"granite_guardian_3_2_5b","score":0.01909583806991577}]}""";

	private RestClient.Builder restClientBuilder;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		this.restClientBuilder = RestClient.builder();
		this.server = MockRestServiceServer.bindTo(this.restClientBuilder).build();
	}

	@Test
	void answerRelevanceSendsTheQuestionAndTheAnswerToTheGovernanceInstance() {
		this.server.expect(requestTo(GENERATED_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(WatsonxAiDetectionApi.GOVERNANCE_INSTANCE_ID_HEADER, GOVERNANCE_INSTANCE_ID))
			.andExpect(jsonPath("$.prompt").value(QUESTION))
			.andExpect(jsonPath("$.generated_text").value("The capital of Italy is Rome."))
			.andExpect(jsonPath("$.detectors.answer_relevance").isMap())
			.andExpect(jsonPath("$.detectors.answer_relevance.threshold").doesNotExist())
			.andExpect(jsonPath("$.project_id").doesNotHaveJsonPath())
			.andExpect(jsonPath("$.space_id").doesNotHaveJsonPath())
			.andRespond(withSuccess(RELEVANT_ANSWER, MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiAnswerRelevanceEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome."));

		this.server.verify();
		assertThat(evaluation.isPass()).isTrue();
		assertThat(evaluation.getScore()).isEqualTo(0.9707332f);
		assertThat(evaluation.getFeedback())
			.isEqualTo("answer_relevance score 0.9707332327961922 is at or above the threshold 0.5");
		assertThat(evaluation.getMetadata()).containsEntry(WatsonxAiDetectionEvaluator.THRESHOLD_METADATA_KEY, 0.5)
			.containsEntry(WatsonxAiDetectionEvaluator.DETECTION_METADATA_KEY,
					new Detection("answer_relevance", "relevant", "granite_guardian_3_2_5b", 0.9707332327961922, null))
			.containsEntry(WatsonxAiDetectionEvaluator.DETECTIONS_METADATA_KEY,
					List.of(new Detection("answer_relevance", "relevant", "granite_guardian_3_2_5b", 0.9707332327961922,
							null)));
	}

	// watsonx.ai labels the detection "relevant" whatever its score, without a threshold
	@Test
	void answerRelevanceFailsBelowTheThresholdWhateverTheLabel() {
		this.server.expect(requestTo(GENERATED_URL))
			.andRespond(withSuccess(IRRELEVANT_ANSWER, MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiAnswerRelevanceEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, "Bananas are rich in potassium."));

		assertThat(evaluation.isPass()).isFalse();
		assertThat(evaluation.getScore()).isEqualTo(0.015889168f);
		assertThat(evaluation.getFeedback())
			.isEqualTo("answer_relevance score 0.01588916778564442 is below the threshold 0.5");
	}

	@Test
	void thresholdDecidesWhetherTheEvaluationPasses() {
		this.server.expect(requestTo(GENERATED_URL))
			.andRespond(withSuccess(RELEVANT_ANSWER, MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = WatsonxAiAnswerRelevanceEvaluator.builder()
			.detectionApi(governanceApi())
			.threshold(0.99)
			.build()
			.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome."));

		assertThat(evaluation.isPass()).isFalse();
		assertThat(evaluation.getMetadata()).containsEntry(WatsonxAiDetectionEvaluator.THRESHOLD_METADATA_KEY, 0.99);
	}

	@Test
	void contextRelevanceSendsTheQuestionAndTheDocuments() {
		this.server.expect(requestTo(CONTEXT_URL))
			.andExpect(jsonPath("$.input").value(QUESTION))
			.andExpect(jsonPath("$.context_type").value("docs"))
			.andExpect(jsonPath("$.context.length()").value(2))
			.andExpect(jsonPath("$.context[0]").value(DOCUMENTS.get(0).getText()))
			.andExpect(jsonPath("$.context[1]").value(DOCUMENTS.get(1).getText()))
			.andExpect(jsonPath("$.detectors.context_relevance").isMap())
			.andRespond(withSuccess(RELEVANT_CONTEXT, MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiContextRelevanceEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, ""));

		this.server.verify();
		assertThat(evaluation.isPass()).isTrue();
		assertThat(evaluation.getScore()).isEqualTo(0.9850713f);
	}

	@Test
	void faithfulnessSendsTheAnswerAndTheDocuments() {
		this.server.expect(requestTo(CONTEXT_URL))
			.andExpect(jsonPath("$.input").value("Milan is the capital of Italy."))
			.andExpect(jsonPath("$.context.length()").value(2))
			.andExpect(jsonPath("$.detectors.faithfulness").isMap())
			.andRespond(withSuccess(UNFAITHFUL_ANSWER, MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiFaithfulnessEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, "Milan is the capital of Italy."));

		this.server.verify();
		assertThat(evaluation.isPass()).isFalse();
		assertThat(evaluation.getScore()).isEqualTo(0.019095838f);
	}

	@Test
	void documentsWithoutTextAreNotSentAsContext() {
		this.server.expect(requestTo(CONTEXT_URL))
			.andExpect(jsonPath("$.context.length()").value(1))
			.andRespond(withSuccess(RELEVANT_CONTEXT, MediaType.APPLICATION_JSON));

		new WatsonxAiContextRelevanceEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, List.of(new Document(" "), DOCUMENTS.get(0)), ""));

		this.server.verify();
	}

	// Chat and evaluation share the connection properties, so a project or space may be
	// configured too: watsonx.ai rejects it together with a governance instance
	@Test
	void withAGovernanceInstanceAConfiguredProjectOrSpaceIsNotSent() {
		this.server.expect(requestTo(GENERATED_URL))
			.andExpect(header(WatsonxAiDetectionApi.GOVERNANCE_INSTANCE_ID_HEADER, GOVERNANCE_INSTANCE_ID))
			.andExpect(jsonPath("$.project_id").doesNotHaveJsonPath())
			.andExpect(jsonPath("$.space_id").doesNotHaveJsonPath())
			.andRespond(withSuccess(RELEVANT_ANSWER, MediaType.APPLICATION_JSON));
		this.server.expect(requestTo(CONTEXT_URL))
			.andExpect(header(WatsonxAiDetectionApi.GOVERNANCE_INSTANCE_ID_HEADER, GOVERNANCE_INSTANCE_ID))
			.andExpect(jsonPath("$.project_id").doesNotHaveJsonPath())
			.andExpect(jsonPath("$.space_id").doesNotHaveJsonPath())
			.andRespond(withSuccess(RELEVANT_CONTEXT, MediaType.APPLICATION_JSON));
		WatsonxAiDetectionApi api = new WatsonxAiDetectionApi(BASE_URL, WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT,
				WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT, VERSION, "test-project-id", "test-space-id",
				GOVERNANCE_INSTANCE_ID, "test-api-key", this.restClientBuilder,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

		new WatsonxAiAnswerRelevanceEvaluator(api)
			.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome."));
		new WatsonxAiContextRelevanceEvaluator(api).evaluate(new EvaluationRequest(QUESTION, DOCUMENTS, ""));

		this.server.verify();
	}

	@Test
	void otherClientsOfTheSameBuilderKeepTheirBaseUrlAndGetNoGovernanceInstance() {
		RestClient.Builder shared = RestClient.builder().baseUrl("https://example.com");
		MockRestServiceServer sharedServer = MockRestServiceServer.bindTo(shared).build();
		new WatsonxAiDetectionApi(BASE_URL, WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT,
				WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT, VERSION, null, null, GOVERNANCE_INSTANCE_ID,
				"test-api-key", shared, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		sharedServer.expect(requestTo("https://example.com/other"))
			.andExpect(headerDoesNotExist(WatsonxAiDetectionApi.GOVERNANCE_INSTANCE_ID_HEADER))
			.andRespond(withSuccess());

		shared.build().get().uri("/other").retrieve().toBodilessEntity();

		sharedServer.verify();
	}

	// Rounded numbers could contradict the verdict, such as 0.703 against 0.70
	@Test
	void feedbackHasTheExactScoreAndThreshold() {
		this.server.expect(requestTo(CONTEXT_URL))
			.andRespond(withSuccess(
					"""
							{"detections":[{"detection_type":"faithfulness","detection":"factual","detector_id":"granite_guardian_3_2_5b","score":0.703}]}""",
					MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = WatsonxAiFaithfulnessEvaluator.builder()
			.detectionApi(governanceApi())
			.threshold(0.704)
			.build()
			.evaluate(new EvaluationRequest(DOCUMENTS, "Rome is the capital of Italy."));

		assertThat(evaluation.isPass()).isFalse();
		assertThat(evaluation.getFeedback()).isEqualTo("faithfulness score 0.703 is below the threshold 0.704");
	}

	@Test
	void withoutGovernanceInstanceTheProjectIsSent() {
		this.server.expect(requestTo(GENERATED_URL))
			.andExpect(headerDoesNotExist(WatsonxAiDetectionApi.GOVERNANCE_INSTANCE_ID_HEADER))
			.andExpect(jsonPath("$.project_id").value("test-project-id"))
			.andRespond(withSuccess(RELEVANT_ANSWER, MediaType.APPLICATION_JSON));

		new WatsonxAiAnswerRelevanceEvaluator(api("test-project-id", null))
			.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome."));

		this.server.verify();
	}

	@Test
	void evidenceIsKeptInTheMetadata() {
		this.server.expect(requestTo(CONTEXT_URL))
			.andRespond(withSuccess(
					"""
							{"detections":[{"detection_type":"faithfulness","detection":"factual","detector_id":"faithfulness_125m_v1_en","score":0.9,
							 "evidence":[{"name":"evidence","value":"Rome is the capital city of Italy.","score":0.8,
							  "evidence":[{"name":"evidence","value":"It has about 2.8 million inhabitants.","score":0.6}]}]}]}""",
					MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiFaithfulnessEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(DOCUMENTS, "Rome is the capital of Italy."));

		Detection detection = (Detection) evaluation.getMetadata()
			.get(WatsonxAiDetectionEvaluator.DETECTION_METADATA_KEY);
		assertThat(detection.evidence()).containsExactly(new Evidence("evidence", "Rome is the capital city of Italy.",
				0.8, List.of(new Evidence("evidence", "It has about 2.8 million inhabitants.", 0.6, null))));
	}

	// A detector with a threshold of its own leaves out a detection below it
	@Test
	void noDetectionFailsTheEvaluation() {
		this.server.expect(requestTo(GENERATED_URL))
			.andRespond(withSuccess("{\"detections\":[]}", MediaType.APPLICATION_JSON));

		EvaluationResponse evaluation = new WatsonxAiAnswerRelevanceEvaluator(governanceApi())
			.evaluate(new EvaluationRequest(QUESTION, "Bananas are rich in potassium."));

		assertThat(evaluation.isPass()).isFalse();
		assertThat(evaluation.getScore()).isZero();
		assertThat(evaluation.getFeedback()).isEqualTo("watsonx.ai returned no answer_relevance score");
		assertThat(evaluation.getMetadata()).containsEntry(WatsonxAiDetectionEvaluator.DETECTIONS_METADATA_KEY,
				List.of());
	}

	@Test
	void detectorNotAccessibleIsANonTransientError() {
		this.server.expect(requestTo(GENERATED_URL))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"errors":[{"code":"detector_not_accessible","message":"Detectors 'answer_relevance' is not accessible for authorization type 'project_id'"}],"status_code":401}"""));

		WatsonxAiAnswerRelevanceEvaluator evaluator = new WatsonxAiAnswerRelevanceEvaluator(
				api("test-project-id", null));

		assertThatExceptionOfType(NonTransientAiException.class)
			.isThrownBy(() -> evaluator.evaluate(new EvaluationRequest(QUESTION, "The capital of Italy is Rome.")))
			.withMessageContaining("detector_not_accessible");
	}

	@Test
	void requestsWithoutWhatTheDetectorNeedsAreRejected() {
		WatsonxAiDetectionApi api = governanceApi();

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new WatsonxAiAnswerRelevanceEvaluator(api).evaluate(new EvaluationRequest("", "Rome.")))
			.withMessageContaining("question");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new WatsonxAiAnswerRelevanceEvaluator(api).evaluate(new EvaluationRequest(QUESTION, "")))
			.withMessageContaining("answer");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new WatsonxAiContextRelevanceEvaluator(api)
				.evaluate(new EvaluationRequest(QUESTION, Collections.emptyList(), "")))
			.withMessageContaining("document");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new WatsonxAiFaithfulnessEvaluator(api).evaluate(new EvaluationRequest(DOCUMENTS, "")))
			.withMessageContaining("answer");
		List<Document> tooMany = IntStream.range(0, 101).mapToObj(i -> new Document("Document " + i)).toList();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new WatsonxAiFaithfulnessEvaluator(api).evaluate(new EvaluationRequest(tooMany, "Rome.")))
			.withMessageContaining("at most 100");
	}

	@Test
	void thresholdMustBeAProbability() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> WatsonxAiFaithfulnessEvaluator.builder().detectionApi(governanceApi()).threshold(1.5).build());
		assertThat(WatsonxAiFaithfulnessEvaluator.builder().detectionApi(governanceApi()).build().getThreshold())
			.isEqualTo(WatsonxAiDetectionEvaluator.DEFAULT_THRESHOLD);
		assertThat(new WatsonxAiFaithfulnessEvaluator(governanceApi()).getDetector()).isEqualTo("faithfulness");
	}

	private WatsonxAiDetectionApi governanceApi() {
		return api(null, GOVERNANCE_INSTANCE_ID);
	}

	private WatsonxAiDetectionApi api(String projectId, String governanceInstanceId) {
		return new WatsonxAiDetectionApi(BASE_URL, WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT,
				WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT, VERSION, projectId, null, governanceInstanceId,
				"test-api-key", this.restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

}
