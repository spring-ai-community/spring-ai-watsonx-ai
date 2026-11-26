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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionResponse.Detection;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link Evaluator} backed by a watsonx.ai detector that scores a RAG answer or its
 * context from 0 to 1, where a higher score is better.
 *
 * <p>
 * The evaluation passes when the score reaches the threshold. The threshold is not sent
 * to watsonx.ai, which would leave out a detection below it, so the score is always
 * known. The {@link EvaluationResponse} has the score, a feedback that tells it against
 * the threshold, and these metadata:
 * <ul>
 * <li>{@value #DETECTION_METADATA_KEY}: the {@link Detection} that gave the score, with
 * its label, detector ID and evidence</li>
 * <li>{@value #DETECTIONS_METADATA_KEY}: every {@link Detection} of the response</li>
 * <li>{@value #THRESHOLD_METADATA_KEY}: the threshold</li>
 * </ul>
 *
 * @author Federico Mariani
 * @since 2.2.0
 * @see WatsonxAiAnswerRelevanceEvaluator
 * @see WatsonxAiContextRelevanceEvaluator
 * @see WatsonxAiFaithfulnessEvaluator
 */
public abstract class WatsonxAiDetectionEvaluator implements Evaluator {

	/** The threshold used when none is set. */
	public static final double DEFAULT_THRESHOLD = 0.5;

	/** The metadata key of the detection that gave the score. */
	public static final String DETECTION_METADATA_KEY = "detection";

	/** The metadata key of every detection of the response. */
	public static final String DETECTIONS_METADATA_KEY = "detections";

	/** The metadata key of the threshold. */
	public static final String THRESHOLD_METADATA_KEY = "threshold";

	/** The most context documents that watsonx.ai accepts. */
	static final int MAX_CONTEXT_DOCUMENTS = 100;

	private final String detector;

	private final WatsonxAiDetectionApi detectionApi;

	private final double threshold;

	private final RetryTemplate retryTemplate;

	protected WatsonxAiDetectionEvaluator(String detector, WatsonxAiDetectionApi detectionApi, double threshold,
			RetryTemplate retryTemplate) {
		Assert.hasText(detector, "detector must not be empty");
		Assert.notNull(detectionApi, "detectionApi must not be null");
		Assert.isTrue(threshold >= 0 && threshold <= 1, "threshold must be between 0 and 1");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		this.detector = detector;
		this.detectionApi = detectionApi;
		this.threshold = threshold;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {
		Assert.notNull(evaluationRequest, "evaluationRequest must not be null");
		Map<String, Map<String, Object>> detectors = Map.of(this.detector, Map.of());
		ResponseEntity<WatsonxAiDetectionResponse> response = RetryUtils.execute(this.retryTemplate,
				() -> detect(this.detectionApi, detectors, evaluationRequest));
		return toEvaluationResponse(response.getBody());
	}

	/**
	 * Calls the detection API.
	 * @param detectionApi the detection API
	 * @param detectors the detector of this evaluator, without properties
	 * @param evaluationRequest the evaluation request
	 * @return the response of the detection API
	 */
	protected abstract ResponseEntity<WatsonxAiDetectionResponse> detect(WatsonxAiDetectionApi detectionApi,
			Map<String, Map<String, Object>> detectors, EvaluationRequest evaluationRequest);

	/**
	 * The text of the documents of the request, as context documents.
	 * @param evaluationRequest the evaluation request
	 * @return the text of each document that has text
	 * @throws IllegalArgumentException if no document has text, or if there are more than
	 * watsonx.ai accepts
	 */
	protected static List<String> context(EvaluationRequest evaluationRequest) {
		List<String> context = (evaluationRequest.getDataList() != null)
				? evaluationRequest.getDataList().stream().map(Document::getText).filter(StringUtils::hasText).toList()
				: List.of();
		Assert.isTrue(!context.isEmpty(), "The evaluation request needs at least one document with text as context");
		Assert.isTrue(context.size() <= MAX_CONTEXT_DOCUMENTS, () -> "watsonx.ai accepts at most "
				+ MAX_CONTEXT_DOCUMENTS + " context documents, not " + context.size());
		return context;
	}

	private EvaluationResponse toEvaluationResponse(WatsonxAiDetectionResponse response) {
		List<Detection> detections = (response != null && response.detections() != null) ? response.detections()
				: List.of();
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put(THRESHOLD_METADATA_KEY, this.threshold);
		metadata.put(DETECTIONS_METADATA_KEY, detections);

		Detection detection = detections.stream()
			.filter(candidate -> this.detector.equals(candidate.detectionType()) && candidate.score() != null)
			.findFirst()
			.orElse(null);
		if (detection == null) {
			return new EvaluationResponse(false, 0, "watsonx.ai returned no " + this.detector + " score", metadata);
		}
		metadata.put(DETECTION_METADATA_KEY, detection);

		double score = detection.score();
		boolean pass = score >= this.threshold;
		String feedback = String.format(Locale.ROOT, "%s score %.3f is %s the threshold %.2f", this.detector, score,
				pass ? "at or above" : "below", this.threshold);
		return new EvaluationResponse(pass, (float) score, feedback, metadata);
	}

	/**
	 * @return the name of the watsonx.ai detector, such as {@code answer_relevance}
	 */
	public String getDetector() {
		return this.detector;
	}

	/**
	 * @return the score, from 0 to 1, from which the evaluation passes
	 */
	public double getThreshold() {
		return this.threshold;
	}

	/**
	 * Builder of a detection evaluator.
	 *
	 * @param <E> the type of the evaluator
	 * @param <B> the type of the builder
	 */
	public abstract static class AbstractBuilder<E extends WatsonxAiDetectionEvaluator, B extends AbstractBuilder<E, B>> {

		protected WatsonxAiDetectionApi detectionApi;

		protected double threshold = DEFAULT_THRESHOLD;

		protected RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		protected AbstractBuilder() {
		}

		/**
		 * @param detectionApi the client of the watsonx.ai detection API
		 * @return this builder
		 */
		public B detectionApi(WatsonxAiDetectionApi detectionApi) {
			this.detectionApi = detectionApi;
			return self();
		}

		/**
		 * @param threshold the score, from 0 to 1, from which the evaluation passes
		 * (default {@value WatsonxAiDetectionEvaluator#DEFAULT_THRESHOLD})
		 * @return this builder
		 */
		public B threshold(double threshold) {
			this.threshold = threshold;
			return self();
		}

		/**
		 * @param retryTemplate the retry template of the calls to watsonx.ai
		 * @return this builder
		 */
		public B retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return self();
		}

		@SuppressWarnings("unchecked")
		private B self() {
			return (B) this;
		}

		public abstract E build();

	}

}
