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

package org.springaicommunity.watsonx.autoconfigure.evaluation;

import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionApi;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionEvaluator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties of the watsonx.ai detection evaluators.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
@ConfigurationProperties(WatsonxAiEvaluationProperties.CONFIG_PREFIX)
public class WatsonxAiEvaluationProperties {

	public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.evaluation";

	/**
	 * Enable the watsonx.ai detection evaluators. They need a watsonx.governance
	 * instance, so they are off unless enabled.
	 */
	private boolean enabled = false;

	/**
	 * The GUID of the watsonx.governance service instance, in the region of the base URL.
	 * The context relevance, faithfulness and answer relevance detectors need one. When
	 * it is set, no project or space is sent.
	 */
	private String governanceInstanceId;

	/** The endpoint of the detection on context documents. */
	private String contextEndpoint = WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT;

	/** The endpoint of the detection on generated text. */
	private String generatedEndpoint = WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT;

	/**
	 * API version date to use, in YYYY-MM-DD format. See the
	 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API
	 * versioning</a>
	 */
	private String version = WatsonxAiDetectionApi.DEFAULT_VERSION;

	private final Evaluator answerRelevance = new Evaluator();

	private final Evaluator contextRelevance = new Evaluator();

	private final Evaluator faithfulness = new Evaluator();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getGovernanceInstanceId() {
		return this.governanceInstanceId;
	}

	public void setGovernanceInstanceId(String governanceInstanceId) {
		this.governanceInstanceId = governanceInstanceId;
	}

	public String getContextEndpoint() {
		return this.contextEndpoint;
	}

	public void setContextEndpoint(String contextEndpoint) {
		this.contextEndpoint = contextEndpoint;
	}

	public String getGeneratedEndpoint() {
		return this.generatedEndpoint;
	}

	public void setGeneratedEndpoint(String generatedEndpoint) {
		this.generatedEndpoint = generatedEndpoint;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Evaluator getAnswerRelevance() {
		return this.answerRelevance;
	}

	public Evaluator getContextRelevance() {
		return this.contextRelevance;
	}

	public Evaluator getFaithfulness() {
		return this.faithfulness;
	}

	/** The settings of an evaluator. */
	public static class Evaluator {

		/** The score, from 0 to 1, from which the evaluation passes. */
		private double threshold = WatsonxAiDetectionEvaluator.DEFAULT_THRESHOLD;

		public double getThreshold() {
			return this.threshold;
		}

		public void setThreshold(double threshold) {
			this.threshold = threshold;
		}

	}

}
