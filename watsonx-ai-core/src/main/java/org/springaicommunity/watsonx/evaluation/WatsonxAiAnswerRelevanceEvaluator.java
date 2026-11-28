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
 * Evaluates whether the response answers the question, with the watsonx.ai
 * {@code answer_relevance} detector on generated text. The user text of the
 * {@link EvaluationRequest} is the question and its response content is the answer. The
 * score is the probability, from 0 to 1, that the answer is relevant to the question.
 *
 * <pre>{@code
 * EvaluationResponse evaluation = new WatsonxAiAnswerRelevanceEvaluator(detectionApi)
 * 	.evaluate(new EvaluationRequest("What is the capital of Italy?", "The capital of Italy is Rome."));
 * }</pre>
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
public class WatsonxAiAnswerRelevanceEvaluator extends WatsonxAiDetectionEvaluator {

	/** The name of the watsonx.ai detector. */
	public static final String DETECTOR = "answer_relevance";

	/**
	 * Creates the evaluator with the default threshold.
	 * @param detectionApi the client of the watsonx.ai detection API
	 */
	public WatsonxAiAnswerRelevanceEvaluator(WatsonxAiDetectionApi detectionApi) {
		this(detectionApi, DEFAULT_THRESHOLD, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	private WatsonxAiAnswerRelevanceEvaluator(WatsonxAiDetectionApi detectionApi, double threshold,
			RetryTemplate retryTemplate) {
		super(DETECTOR, detectionApi, threshold, retryTemplate);
	}

	@Override
	protected ResponseEntity<WatsonxAiDetectionResponse> detect(WatsonxAiDetectionApi detectionApi,
			Map<String, Map<String, Object>> detectors, EvaluationRequest evaluationRequest) {
		Assert.hasText(evaluationRequest.getUserText(), "The user text, which is the question, must not be empty");
		Assert.hasText(evaluationRequest.getResponseContent(),
				"The response content, which is the answer, must not be empty");
		return detectionApi.detectGenerated(new WatsonxAiDetectionGeneratedRequest(evaluationRequest.getUserText(),
				evaluationRequest.getResponseContent(), detectors));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractBuilder<WatsonxAiAnswerRelevanceEvaluator, Builder> {

		private Builder() {
		}

		@Override
		public WatsonxAiAnswerRelevanceEvaluator build() {
			return new WatsonxAiAnswerRelevanceEvaluator(this.detectionApi, this.threshold, this.retryTemplate);
		}

	}

}
