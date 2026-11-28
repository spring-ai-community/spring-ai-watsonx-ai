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

import java.util.Map;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * Evaluates whether the response is grounded in the documents, with the watsonx.ai
 * {@code faithfulness} detector, also called groundedness: the watsonx.ai counterpart of
 * Spring AI's {@code FactCheckingEvaluator}. The response content of the
 * {@link EvaluationRequest} is the answer and its documents are the context; the user
 * text is not used. The score is the probability, from 0 to 1, that the answer is
 * faithful to the context, so a low score points to a hallucination.
 *
 * <pre>{@code
 * EvaluationResponse evaluation = new WatsonxAiFaithfulnessEvaluator(detectionApi)
 * 	.evaluate(new EvaluationRequest(retrievedDocuments, answer));
 * }</pre>
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
public class WatsonxAiFaithfulnessEvaluator extends WatsonxAiDetectionEvaluator {

	/** The name of the watsonx.ai detector. */
	public static final String DETECTOR = "faithfulness";

	/**
	 * Creates the evaluator with the default threshold.
	 * @param detectionApi the client of the watsonx.ai detection API
	 */
	public WatsonxAiFaithfulnessEvaluator(WatsonxAiDetectionApi detectionApi) {
		this(detectionApi, DEFAULT_THRESHOLD, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	private WatsonxAiFaithfulnessEvaluator(WatsonxAiDetectionApi detectionApi, double threshold,
			RetryTemplate retryTemplate) {
		super(DETECTOR, detectionApi, threshold, retryTemplate);
	}

	@Override
	protected ResponseEntity<WatsonxAiDetectionResponse> detect(WatsonxAiDetectionApi detectionApi,
			Map<String, Map<String, Object>> detectors, EvaluationRequest evaluationRequest) {
		Assert.hasText(evaluationRequest.getResponseContent(),
				"The response content, which is the answer, must not be empty");
		return detectionApi.detectContext(new WatsonxAiDetectionContextRequest(evaluationRequest.getResponseContent(),
				detectors, context(evaluationRequest)));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractBuilder<WatsonxAiFaithfulnessEvaluator, Builder> {

		private Builder() {
		}

		@Override
		public WatsonxAiFaithfulnessEvaluator build() {
			return new WatsonxAiFaithfulnessEvaluator(this.detectionApi, this.threshold, this.retryTemplate);
		}

	}

}
